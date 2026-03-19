package com.airticket.service;

import com.airticket.dto.ConnectingFlightSearchRequest;
import com.airticket.dto.FlightSearchRequest;
import com.airticket.mapper.FlightMapper;
import com.airticket.mapper.TicketMapper;
import com.airticket.model.ConnectingFlight;
import com.airticket.model.Flight;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightService {

    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_DELAYED = "DELAYED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_DEPARTED = "DEPARTED";
    private static final String STATUS_LANDED = "LANDED";
    private static final ZoneId SEARCH_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private FlightMapper flightMapper;

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private MessageService messageService;

    @Autowired
    private AirlineService airlineService;

    public List<Flight> getAllFlights() {
        return flightMapper.findAllWithAssociations();
    }

    public Flight getFlightById(Long id) {
        Flight flight = flightMapper.findById(id);
        if (flight != null) {
            updateFlightStatusBasedOnTime(flight);
        }
        return flight;
    }

    public List<Flight> searchFlights(FlightSearchRequest request) {
        String departureQuery = normalizeAirportQuery(request.getDepartureCity());
        String arrivalQuery = normalizeAirportQuery(request.getArrivalCity());
        if (departureQuery == null || arrivalQuery == null || request.getDepartureDate() == null) {
            return List.of();
        }

        Instant departureTimeStart = toUtcStartOfDay(request.getDepartureDate());
        Instant departureTimeEnd = departureTimeStart.plus(Duration.ofDays(1));

        return flightMapper.searchFlights(
            departureQuery,
            arrivalQuery,
            departureTimeStart,
            departureTimeEnd
        );
    }

    public List<ConnectingFlight> searchConnectingFlights(ConnectingFlightSearchRequest request) {
        List<ConnectingFlight> result = new ArrayList<>();

        List<Flight> directFlights = searchFlights(request);
        for (Flight flight : directFlights) {
            result.add(new ConnectingFlight(List.of(flight)));
        }

        if (request.isIncludeConnectingFlights()) {
            result.addAll(findConnectingFlights(
                request.getDepartureCity(),
                request.getArrivalCity(),
                request.getDepartureDate()
            ));
        }

        return result.stream()
            .sorted(Comparator.comparing(ConnectingFlight::getTotalDurationMinutes))
            .collect(Collectors.toList());
    }

    public List<ConnectingFlight> findConnectingFlights(String departureCity, String arrivalCity, LocalDate departureDate) {
        List<ConnectingFlight> result = new ArrayList<>();
        if (departureCity == null || arrivalCity == null || departureDate == null) {
            return result;
        }

        Instant departureTimeStart = toUtcStartOfDay(departureDate);
        Instant departureTimeEnd = departureTimeStart.plus(Duration.ofDays(1));

        List<Flight> firstLegFlights = flightMapper.findFlightsFromCity(
            normalizeAirportQuery(departureCity),
            departureTimeStart,
            departureTimeEnd
        );
        if (firstLegFlights == null || firstLegFlights.isEmpty()) {
            return result;
        }

        Instant now = Instant.now();
        List<Flight> availableFirstLegFlights = firstLegFlights.stream()
            .filter(flight -> flight.getDepartureTimeUtc() != null)
            .filter(flight -> now.isBefore(flight.getDepartureTimeUtc()))
            .toList();

        for (Flight firstLeg : availableFirstLegFlights) {
            if (firstLeg.getArrivalTimeUtc() == null) {
                continue;
            }

            Instant firstLegArrival = firstLeg.getArrivalTimeUtc();
            Instant earliestSecondLegDeparture = firstLegArrival.plusSeconds(3600);
            String intermediateCity = firstLeg.getArrivalAirport() != null
                ? firstLeg.getArrivalAirport().getCity()
                : null;

            List<Flight> secondLegFlights = new ArrayList<>();
            appendSecondLegCandidates(
                secondLegFlights,
                intermediateCity,
                arrivalCity,
                departureDate,
                earliestSecondLegDeparture
            );
            appendSecondLegCandidates(
                secondLegFlights,
                intermediateCity,
                arrivalCity,
                departureDate.plusDays(1),
                earliestSecondLegDeparture
            );

            for (Flight secondLeg : secondLegFlights) {
                if (secondLeg.getDepartureTimeUtc() == null) {
                    continue;
                }

                boolean sameAirport = firstLeg.getArrivalAirportId().equals(secondLeg.getDepartureAirportId());
                boolean sameCityDifferentAirport =
                    !sameAirport &&
                    firstLeg.getArrivalAirport() != null &&
                    secondLeg.getDepartureAirport() != null &&
                    firstLeg.getArrivalAirport().getCity() != null &&
                    secondLeg.getDepartureAirport().getCity() != null &&
                    firstLeg.getArrivalAirport().getCity().trim()
                        .equalsIgnoreCase(secondLeg.getDepartureAirport().getCity().trim());

                if (!sameAirport && !sameCityDifferentAirport) {
                    continue;
                }

                long requiredConnectionMinutes = sameCityDifferentAirport ? 120 : 60;
                Instant minimumSecondLegDeparture = firstLegArrival.plusSeconds(requiredConnectionMinutes * 60);
                if (secondLeg.getDepartureTimeUtc().isBefore(minimumSecondLegDeparture)) {
                    continue;
                }

                ConnectingFlight connectingFlight = new ConnectingFlight(List.of(firstLeg, secondLeg));
                if (sameCityDifferentAirport) {
                    connectingFlight.setType("CONNECTING_INTERCITY_TRANSFER");
                }
                result.add(connectingFlight);
            }
        }

        return result;
    }

    public Flight createFlight(Flight flight) {
        validateFlightNumber(flight);

        if (flightMapper.countByFlightNumber(flight.getFlightNumber()) > 0) {
            throw new RuntimeException(messageService.getMessage("flight.duplicate.flightNumber", flight.getFlightNumber()));
        }

        flightMapper.insert(flight);
        return flight;
    }

    public Flight updateFlight(Flight flight) {
        Flight existingFlight = flightMapper.findById(flight.getId());
        if (existingFlight == null) {
            throw new RuntimeException("Flight not found with id: " + flight.getId());
        }

        validateFlightNumber(flight);

        if (flightMapper.countByFlightNumberExcludeId(flight.getFlightNumber(), flight.getId()) > 0) {
            throw new RuntimeException(messageService.getMessage("flight.duplicate.flightNumber", flight.getFlightNumber()));
        }

        if (flight.getAvailableSeats() == null) {
            flight.setAvailableSeats(existingFlight.getAvailableSeats());
        }

        if (flight.getStatus() == null || flight.getStatus().isEmpty()) {
            flight.setStatus(existingFlight.getStatus());
        }

        flightMapper.update(flight);
        return flightMapper.findById(flight.getId());
    }

    public void deleteFlight(Long id) {
        int ticketCount = ticketMapper.countByFlightId(id);
        if (ticketCount > 0) {
            throw new RuntimeException(messageService.getMessage("flight.delete.hasTickets", ticketCount));
        }

        flightMapper.deleteById(id);
    }

    public boolean reserveSeat(Long flightId) {
        Flight flight = flightMapper.findById(flightId);
        if (flight == null || flight.getAvailableSeats() <= 0) {
            return false;
        }

        updateFlightStatusBasedOnTime(flight);
        if (STATUS_DEPARTED.equals(flight.getStatus()) || STATUS_LANDED.equals(flight.getStatus())) {
            throw new RuntimeException(messageService.getMessage("flight.booking.departedFlight"));
        }

        flightMapper.decreaseAvailableSeats(flightId, 1);
        return true;
    }

    public void releaseSeat(Long flightId) {
        flightMapper.increaseAvailableSeats(flightId, 1);
    }

    public void cancelFlight(Long flightId, String reason) {
        Flight flight = flightMapper.findById(flightId);
        if (flight != null) {
            flight.setStatus(STATUS_CANCELLED);
            flightMapper.update(flight);
        }
    }

    private void updateFlightStatusBasedOnTime(Flight flight) {
        if (flight == null) {
            return;
        }

        Instant now = Instant.now();
        String currentStatus = flight.getStatus();
        if (STATUS_CANCELLED.equals(currentStatus) || STATUS_DELAYED.equals(currentStatus)) {
            return;
        }

        String newStatus = null;
        if (flight.getArrivalTimeUtc() != null && now.isAfter(flight.getArrivalTimeUtc())) {
            newStatus = STATUS_LANDED;
        } else if (flight.getDepartureTimeUtc() != null && now.isAfter(flight.getDepartureTimeUtc())) {
            newStatus = STATUS_DEPARTED;
        } else if (!STATUS_SCHEDULED.equals(currentStatus)) {
            newStatus = STATUS_SCHEDULED;
        }

        if (newStatus != null && !newStatus.equals(currentStatus)) {
            flight.setStatus(newStatus);
            flightMapper.updateStatus(flight.getId(), newStatus);
        }
    }

    public void updateAllFlightStatuses() {
        List<Flight> flights = flightMapper.findAll();
        for (Flight flight : flights) {
            updateFlightStatusBasedOnTime(flight);
        }
    }

    private void validateFlightNumber(Flight flight) {
        if (flight.getAirlineId() == null || flight.getFlightNumber() == null) {
            return;
        }

        var airline = airlineService.getAirlineById(flight.getAirlineId());
        if (airline == null) {
            throw new RuntimeException("Airline not found");
        }

        if (!airlineService.validateFlightNumber(flight.getFlightNumber(), airline.getCode())) {
            throw new RuntimeException(
                messageService.getMessage("flight.invalidFlightNumber", airline.getCode())
            );
        }
    }

    private void appendSecondLegCandidates(
        List<Flight> secondLegFlights,
        String intermediateCity,
        String arrivalCity,
        LocalDate departureDate,
        Instant earliestSecondLegDeparture
    ) {
        Instant departureTimeStart = toUtcStartOfDay(departureDate);
        Instant departureTimeEnd = departureTimeStart.plus(Duration.ofDays(1));
        String normalizedArrival = normalizeAirportQuery(arrivalCity);

        if (intermediateCity != null) {
            secondLegFlights.addAll(flightMapper.findConnectingFlightsFromCityToCity(
                normalizeAirportQuery(intermediateCity),
                normalizedArrival,
                departureTimeStart,
                departureTimeEnd,
                earliestSecondLegDeparture
            ));
        } else {
            secondLegFlights.addAll(flightMapper.findFlightsToCity(
                normalizedArrival,
                departureTimeStart,
                departureTimeEnd,
                earliestSecondLegDeparture
            ));
        }
    }

    private String normalizeAirportQuery(String query) {
        if (query == null) {
            return null;
        }

        String normalized = query.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.matches("[A-Za-z]{3,4}")) {
            return normalized.toUpperCase();
        }
        return normalized;
    }

    private Instant toUtcStartOfDay(LocalDate date) {
        return date.atStartOfDay(SEARCH_ZONE).toInstant();
    }
}
