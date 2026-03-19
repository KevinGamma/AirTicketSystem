package com.airticket.mapper;

import com.airticket.model.Flight;
import com.airticket.dto.FlightDisplayDto;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

@Mapper
public interface FlightMapper {
    
    @Select("SELECT f.*, " +
            "da.code as departureAirportCode, da.name as departureAirportName, da.city as departureCity, da.timezone as departureTimeZone, " +
            "aa.code as arrivalAirportCode, aa.name as arrivalAirportName, aa.city as arrivalCity, aa.timezone as arrivalTimeZone, " +
            "al.code as airlineCode, al.name as airlineName, al.logo_url as airlineLogoUrl " +
            "FROM flights f " +
            "LEFT JOIN airports da ON f.departure_airport_id = da.id " +
            "LEFT JOIN airports aa ON f.arrival_airport_id = aa.id " +
            "LEFT JOIN airlines al ON f.airline_id = al.id")
    @Results({
        @Result(property = "departureTimeUtc", column = "departure_time"),
        @Result(property = "arrivalTimeUtc", column = "arrival_time"),
        @Result(property = "departureAirportId", column = "departure_airport_id"),
        @Result(property = "arrivalAirportId", column = "arrival_airport_id"),
        @Result(property = "airlineId", column = "airline_id"),
        @Result(property = "departureAirport.id", column = "departure_airport_id"),
        @Result(property = "departureAirport.code", column = "departureAirportCode"),
        @Result(property = "departureAirport.name", column = "departureAirportName"),
        @Result(property = "departureAirport.city", column = "departureCity"),
        @Result(property = "departureAirport.timeZone", column = "departureTimeZone"),
        @Result(property = "arrivalAirport.id", column = "arrival_airport_id"),
        @Result(property = "arrivalAirport.code", column = "arrivalAirportCode"),
        @Result(property = "arrivalAirport.name", column = "arrivalAirportName"),
        @Result(property = "arrivalAirport.city", column = "arrivalCity"),
        @Result(property = "arrivalAirport.timeZone", column = "arrivalTimeZone"),
        @Result(property = "airline.id", column = "airline_id"),
        @Result(property = "airline.code", column = "airlineCode"),
        @Result(property = "airline.name", column = "airlineName"),
        @Result(property = "airline.logoUrl", column = "airlineLogoUrl")
    })
    List<Flight> findAll();

    List<Flight> findAllWithAssociations();
    
    @Select("SELECT f.*, " +
            "da.code as departureAirportCode, da.name as departureAirportName, da.city as departureCity, da.timezone as departureTimeZone, " +
            "aa.code as arrivalAirportCode, aa.name as arrivalAirportName, aa.city as arrivalCity, aa.timezone as arrivalTimeZone, " +
            "al.code as airlineCode, al.name as airlineName, al.logo_url as airlineLogoUrl " +
            "FROM flights f " +
            "LEFT JOIN airports da ON f.departure_airport_id = da.id " +
            "LEFT JOIN airports aa ON f.arrival_airport_id = aa.id " +
            "LEFT JOIN airlines al ON f.airline_id = al.id " +
            "WHERE f.id = #{id}")
    @Results({
        @Result(property = "departureTimeUtc", column = "departure_time"),
        @Result(property = "arrivalTimeUtc", column = "arrival_time"),
        @Result(property = "departureAirport.id", column = "departure_airport_id"),
        @Result(property = "departureAirport.code", column = "departureAirportCode"),
        @Result(property = "departureAirport.name", column = "departureAirportName"),
        @Result(property = "departureAirport.city", column = "departureCity"),
        @Result(property = "departureAirport.timeZone", column = "departureTimeZone"),
        @Result(property = "arrivalAirport.id", column = "arrival_airport_id"),
        @Result(property = "arrivalAirport.code", column = "arrivalAirportCode"),
        @Result(property = "arrivalAirport.name", column = "arrivalAirportName"),
        @Result(property = "arrivalAirport.city", column = "arrivalCity"),
        @Result(property = "arrivalAirport.timeZone", column = "arrivalTimeZone"),
        @Result(property = "airline.id", column = "airline_id"),
        @Result(property = "airline.code", column = "airlineCode"),
        @Result(property = "airline.name", column = "airlineName"),
        @Result(property = "airline.logoUrl", column = "airlineLogoUrl")
    })
    Flight findById(Long id);

    List<Flight> searchFlights(@Param("departureCity") String departureCity,
                              @Param("arrivalCity") String arrivalCity,
                              @Param("departureTimeStart") Instant departureTimeStart,
                              @Param("departureTimeEnd") Instant departureTimeEnd);
    
    @Insert("INSERT INTO flights (flight_number, airline_id, departure_airport_id, arrival_airport_id, " +
            "departure_time, arrival_time, total_seats, available_seats, price, aircraft_type) " +
            "VALUES (#{flightNumber}, #{airlineId}, #{departureAirportId}, #{arrivalAirportId}, " +
            "#{departureTimeUtc}, #{arrivalTimeUtc}, #{totalSeats}, #{availableSeats}, #{price}, #{aircraftType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Flight flight);
    
    @Update("UPDATE flights SET flight_number = #{flightNumber}, airline_id = #{airlineId}, " +
            "departure_airport_id = #{departureAirportId}, arrival_airport_id = #{arrivalAirportId}, " +
            "departure_time = #{departureTimeUtc}, arrival_time = #{arrivalTimeUtc}, " +
            "total_seats = #{totalSeats}, available_seats = #{availableSeats}, " +
            "price = #{price}, status = #{status}, aircraft_type = #{aircraftType} " +
            "WHERE id = #{id}")
    int update(Flight flight);
    
    @Update("UPDATE flights SET available_seats = available_seats - #{seats} WHERE id = #{flightId}")
    int decreaseAvailableSeats(@Param("flightId") Long flightId, @Param("seats") int seats);
    
    @Update("UPDATE flights SET available_seats = available_seats + #{seats} WHERE id = #{flightId}")
    int increaseAvailableSeats(@Param("flightId") Long flightId, @Param("seats") int seats);
    
    @Delete("DELETE FROM flights WHERE id = #{id}")
    int deleteById(Long id);
    
    @Update("UPDATE flights SET status = #{status} WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    
    @Select("SELECT f.*, " +
            "da.code as departureAirportCode, da.name as departureAirportName, da.city as departureCity, da.timezone as departureTimeZone, " +
            "aa.code as arrivalAirportCode, aa.name as arrivalAirportName, aa.city as arrivalCity, aa.timezone as arrivalTimeZone, " +
            "al.code as airlineCode, al.name as airlineName, al.logo_url as airlineLogoUrl " +
            "FROM flights f " +
            "LEFT JOIN airports da ON f.departure_airport_id = da.id " +
            "LEFT JOIN airports aa ON f.arrival_airport_id = aa.id " +
            "LEFT JOIN airlines al ON f.airline_id = al.id " +
            "WHERE f.departure_airport_id = #{departureAirportId} " +
            "AND f.arrival_airport_id = #{arrivalAirportId} " +
            "AND f.id != #{excludeFlightId} " +
            "AND f.status = 'SCHEDULED' " +
            "AND f.available_seats > 0 " +
            "AND f.departure_time > NOW()")
    @Results({
        @Result(property = "departureTimeUtc", column = "departure_time"),
        @Result(property = "arrivalTimeUtc", column = "arrival_time"),
        @Result(property = "departureAirport.id", column = "departure_airport_id"),
        @Result(property = "departureAirport.code", column = "departureAirportCode"),
        @Result(property = "departureAirport.name", column = "departureAirportName"),
        @Result(property = "departureAirport.city", column = "departureCity"),
        @Result(property = "departureAirport.timeZone", column = "departureTimeZone"),
        @Result(property = "arrivalAirport.id", column = "arrival_airport_id"),
        @Result(property = "arrivalAirport.code", column = "arrivalAirportCode"),
        @Result(property = "arrivalAirport.name", column = "arrivalAirportName"),
        @Result(property = "arrivalAirport.city", column = "arrivalCity"),
        @Result(property = "arrivalAirport.timeZone", column = "arrivalTimeZone"),
        @Result(property = "airline.id", column = "airline_id"),
        @Result(property = "airline.code", column = "airlineCode"),
        @Result(property = "airline.name", column = "airlineName"),
        @Result(property = "airline.logoUrl", column = "airlineLogoUrl")
    })
    List<Flight> findAvailableFlightsForReschedule(@Param("departureAirportId") Long departureAirportId,
                                                   @Param("arrivalAirportId") Long arrivalAirportId,
                                                   @Param("excludeFlightId") Long excludeFlightId);

    @Select("<script>" +
            "SELECT f.*, " +
            "da.code as departureAirportCode, da.name as departureAirportName, da.city as departureCity, da.timezone as departureTimeZone, " +
            "aa.code as arrivalAirportCode, aa.name as arrivalAirportName, aa.city as arrivalCity, aa.timezone as arrivalTimeZone, " +
            "al.code as airlineCode, al.name as airlineName, al.logo_url as airlineLogoUrl " +
            "FROM flights f " +
            "LEFT JOIN airports da ON f.departure_airport_id = da.id " +
            "LEFT JOIN airports aa ON f.arrival_airport_id = aa.id " +
            "LEFT JOIN airlines al ON f.airline_id = al.id " +
            "WHERE f.departure_airport_id = #{departureAirportId} " +
            "AND f.id != #{excludeFlightId} " +
            "AND f.status = 'SCHEDULED' " +
            "AND f.available_seats > 0 " +
            "AND f.departure_time > NOW() " +
            "<if test='includeConnecting == false'>" +
            "AND f.arrival_airport_id = #{arrivalAirportId} " +
            "</if>" +
            "<if test='includeConnecting == true'>" +
            "AND (f.arrival_airport_id = #{arrivalAirportId} " +
            "OR EXISTS (SELECT 1 FROM flights connecting_flight " +
            "           WHERE connecting_flight.departure_airport_id = f.arrival_airport_id " +
            "           AND connecting_flight.arrival_airport_id = #{arrivalAirportId} " +
            "           AND connecting_flight.status = 'SCHEDULED' " +
            "           AND connecting_flight.available_seats > 0 " +
            "           AND connecting_flight.departure_time > f.arrival_time)) " +
            "</if>" +
            "<if test='date != null'>" +
            "AND DATE(CONVERT_TZ(f.departure_time, '+00:00', '+08:00')) = #{date} " +
            "</if>" +
            "ORDER BY f.departure_time ASC" +
            "</script>")
    @Results({
        @Result(property = "departureTimeUtc", column = "departure_time"),
        @Result(property = "arrivalTimeUtc", column = "arrival_time"),
        @Result(property = "departureAirport.id", column = "departure_airport_id"),
        @Result(property = "departureAirport.code", column = "departureAirportCode"),
        @Result(property = "departureAirport.name", column = "departureAirportName"),
        @Result(property = "departureAirport.city", column = "departureCity"),
        @Result(property = "departureAirport.timeZone", column = "departureTimeZone"),
        @Result(property = "arrivalAirport.id", column = "arrival_airport_id"),
        @Result(property = "arrivalAirport.code", column = "arrivalAirportCode"),
        @Result(property = "arrivalAirport.name", column = "arrivalAirportName"),
        @Result(property = "arrivalAirport.city", column = "arrivalCity"),
        @Result(property = "arrivalAirport.timeZone", column = "arrivalTimeZone"),
        @Result(property = "airline.id", column = "airline_id"),
        @Result(property = "airline.code", column = "airlineCode"),
        @Result(property = "airline.name", column = "airlineName"),
        @Result(property = "airline.logoUrl", column = "airlineLogoUrl")
    })
    List<Flight> findAvailableFlightsForRescheduleWithFilters(@Param("departureAirportId") Long departureAirportId,
                                                               @Param("arrivalAirportId") Long arrivalAirportId,
                                                               @Param("excludeFlightId") Long excludeFlightId,
                                                               @Param("date") LocalDate date,
                                                               @Param("includeConnecting") Boolean includeConnecting);
    
    @Select("SELECT f.*, " +
            "da.code as departureAirportCode, da.name as departureAirportName, da.city as departureCity, da.timezone as departureTimeZone, " +
            "aa.code as arrivalAirportCode, aa.name as arrivalAirportName, aa.city as arrivalCity, aa.timezone as arrivalTimeZone, " +
            "al.code as airlineCode, al.name as airlineName, al.logo_url as airlineLogoUrl " +
            "FROM flights f " +
            "LEFT JOIN airports da ON f.departure_airport_id = da.id " +
            "LEFT JOIN airports aa ON f.arrival_airport_id = aa.id " +
            "LEFT JOIN airlines al ON f.airline_id = al.id " +
            "WHERE f.departure_airport_id = #{departureAirportId} " +
            "AND f.arrival_airport_id = #{arrivalAirportId} " +
            "AND f.id != #{excludeFlightId} " +
            "AND f.status = 'SCHEDULED' " +
            "AND f.available_seats > 0 " +
            "AND f.departure_time > NOW() " +
            "ORDER BY f.departure_time ASC")
    @Results({
        @Result(property = "departureTimeUtc", column = "departure_time"),
        @Result(property = "arrivalTimeUtc", column = "arrival_time"),
        @Result(property = "departureAirport.id", column = "departure_airport_id"),
        @Result(property = "departureAirport.code", column = "departureAirportCode"),
        @Result(property = "departureAirport.name", column = "departureAirportName"),
        @Result(property = "departureAirport.city", column = "departureCity"),
        @Result(property = "departureAirport.timeZone", column = "departureTimeZone"),
        @Result(property = "arrivalAirport.id", column = "arrival_airport_id"),
        @Result(property = "arrivalAirport.code", column = "arrivalAirportCode"),
        @Result(property = "arrivalAirport.name", column = "arrivalAirportName"),
        @Result(property = "arrivalAirport.city", column = "arrivalCity"),
        @Result(property = "arrivalAirport.timeZone", column = "arrivalTimeZone"),
        @Result(property = "airline.id", column = "airline_id"),
        @Result(property = "airline.code", column = "airlineCode"),
        @Result(property = "airline.name", column = "airlineName"),
        @Result(property = "airline.logoUrl", column = "airlineLogoUrl")
    })
    List<Flight> findConnectingFlights(@Param("departureAirportId") Long departureAirportId,
                                       @Param("arrivalAirportId") Long arrivalAirportId,
                                       @Param("excludeFlightId") Long excludeFlightId);
    
    @Delete("DELETE FROM flights")
    int deleteAll();
    
    @Update("UPDATE flights SET available_seats = total_seats")
    int resetAvailableSeats();


    List<Flight> findFlightsFromCity(@Param("departureCity") String departureCity,
                                     @Param("departureTimeStart") Instant departureTimeStart,
                                     @Param("departureTimeEnd") Instant departureTimeEnd);

    List<Flight> findFlightsToCity(@Param("arrivalCity") String arrivalCity,
                                   @Param("departureTimeStart") Instant departureTimeStart,
                                   @Param("departureTimeEnd") Instant departureTimeEnd,
                                   @Param("earliestDepartureTime") Instant earliestDepartureTime);
    
    List<Flight> findConnectingFlightsFromCityToCity(@Param("departureCity") String departureCity,
                                                    @Param("arrivalCity") String arrivalCity,
                                                    @Param("departureTimeStart") Instant departureTimeStart,
                                                    @Param("departureTimeEnd") Instant departureTimeEnd,
                                                    @Param("earliestDepartureTime") Instant earliestDepartureTime);

    @Select("SELECT tcf.connecting_flight_id " +
            "FROM ticket_connecting_flights tcf " +
            "JOIN tickets t ON t.id = tcf.ticket_id " +
            "WHERE t.flight_id = #{flightId} " +
            "GROUP BY tcf.connecting_flight_id " +
            "ORDER BY MIN(tcf.sequence_number)")
    List<Long> findConnectingFlightIdsByFlightId(@Param("flightId") Long flightId);

    @Select("SELECT f.id, f.flight_number as flightNumber, f.total_seats as totalSeats, " +
            "f.available_seats as availableSeats, f.price, f.status, f.aircraft_type as aircraftType, " +
            "CAST(f.departure_time AS CHAR) as departureTime, CAST(f.arrival_time AS CHAR) as arrivalTime, " +
            "da.code as departureAirportCode, da.name as departureAirportName, " +
            "da.city as departureCity, da.country as departureCountry, da.timezone as departureTimeZone, " +
            "aa.code as arrivalAirportCode, aa.name as arrivalAirportName, " +
            "aa.city as arrivalCity, aa.country as arrivalCountry, aa.timezone as arrivalTimeZone, " +
            "al.name as airlineName, " +
            "'N/A' as duration " +
            "FROM flights f " +
            "LEFT JOIN airports da ON f.departure_airport_id = da.id " +
            "LEFT JOIN airports aa ON f.arrival_airport_id = aa.id " +
            "LEFT JOIN airlines al ON f.airline_id = al.id")
    List<FlightDisplayDto> findAllDisplay();

    @Select("SELECT f.*, " +
            "da.code as departureAirportCode, da.name as departureAirportName, da.city as departureCity, da.timezone as departureTimeZone, " +
            "aa.code as arrivalAirportCode, aa.name as arrivalAirportName, aa.city as arrivalCity, aa.timezone as arrivalTimeZone, " +
            "al.code as airlineCode, al.name as airlineName, al.logo_url as airlineLogoUrl " +
            "FROM flights f " +
            "LEFT JOIN airports da ON f.departure_airport_id = da.id " +
            "LEFT JOIN airports aa ON f.arrival_airport_id = aa.id " +
            "LEFT JOIN airlines al ON f.airline_id = al.id " +
            "WHERE f.departure_time >= #{startTime} AND f.departure_time <= #{endTime} " +
            "AND f.status = 'SCHEDULED'")
    @Results({
        @Result(property = "departureTimeUtc", column = "departure_time"),
        @Result(property = "arrivalTimeUtc", column = "arrival_time"),
        @Result(property = "departureAirport.id", column = "departure_airport_id"),
        @Result(property = "departureAirport.code", column = "departureAirportCode"),
        @Result(property = "departureAirport.name", column = "departureAirportName"),
        @Result(property = "departureAirport.city", column = "departureCity"),
        @Result(property = "departureAirport.timeZone", column = "departureTimeZone"),
        @Result(property = "arrivalAirport.id", column = "arrival_airport_id"),
        @Result(property = "arrivalAirport.code", column = "arrivalAirportCode"),
        @Result(property = "arrivalAirport.name", column = "arrivalAirportName"),
        @Result(property = "arrivalAirport.city", column = "arrivalCity"),
        @Result(property = "arrivalAirport.timeZone", column = "arrivalTimeZone"),
        @Result(property = "airline.id", column = "airline_id"),
        @Result(property = "airline.code", column = "airlineCode"),
        @Result(property = "airline.name", column = "airlineName"),
        @Result(property = "airline.logoUrl", column = "airlineLogoUrl")
    })
    List<Flight> findFlightsByDepartureTimeRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);
    
    @Select("SELECT f.*, " +
            "da.code as departureAirportCode, da.name as departureAirportName, da.city as departureCity, da.timezone as departureTimeZone, " +
            "aa.code as arrivalAirportCode, aa.name as arrivalAirportName, aa.city as arrivalCity, aa.timezone as arrivalTimeZone, " +
            "al.code as airlineCode, al.name as airlineName, al.logo_url as airlineLogoUrl " +
            "FROM flights f " +
            "LEFT JOIN airports da ON f.departure_airport_id = da.id " +
            "LEFT JOIN airports aa ON f.arrival_airport_id = aa.id " +
            "LEFT JOIN airlines al ON f.airline_id = al.id " +
            "WHERE f.arrival_time >= #{startTime} AND f.arrival_time <= #{endTime} " +
            "AND f.status = 'SCHEDULED'")
    @Results({
        @Result(property = "departureTimeUtc", column = "departure_time"),
        @Result(property = "arrivalTimeUtc", column = "arrival_time"),
        @Result(property = "departureAirport.id", column = "departure_airport_id"),
        @Result(property = "departureAirport.code", column = "departureAirportCode"),
        @Result(property = "departureAirport.name", column = "departureAirportName"),
        @Result(property = "departureAirport.city", column = "departureCity"),
        @Result(property = "departureAirport.timeZone", column = "departureTimeZone"),
        @Result(property = "arrivalAirport.id", column = "arrival_airport_id"),
        @Result(property = "arrivalAirport.code", column = "arrivalAirportCode"),
        @Result(property = "arrivalAirport.name", column = "arrivalAirportName"),
        @Result(property = "arrivalAirport.city", column = "arrivalCity"),
        @Result(property = "arrivalAirport.timeZone", column = "arrivalTimeZone"),
        @Result(property = "airline.id", column = "airline_id"),
        @Result(property = "airline.code", column = "airlineCode"),
        @Result(property = "airline.name", column = "airlineName"),
        @Result(property = "airline.logoUrl", column = "airlineLogoUrl")
    })
    List<Flight> findFlightsByArrivalTimeRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Select("SELECT COUNT(*) FROM flights WHERE flight_number = #{flightNumber}")
    int countByFlightNumber(@Param("flightNumber") String flightNumber);
    
    @Select("SELECT COUNT(*) FROM flights WHERE flight_number = #{flightNumber} AND id != #{excludeId}")
    int countByFlightNumberExcludeId(@Param("flightNumber") String flightNumber, @Param("excludeId") Long excludeId);

}
