package com.airticket.ai.tool;

import com.airticket.ai.dto.AirportCandidateView;
import com.airticket.ai.dto.FlightItineraryView;
import com.airticket.ai.dto.FlightOptionView;
import com.airticket.ai.dto.FlightSearchToolResult;
import com.airticket.ai.dto.LocationResolveResult;
import com.airticket.dto.ConnectingFlightSearchRequest;
import com.airticket.model.Airport;
import com.airticket.model.ConnectingFlight;
import com.airticket.model.Flight;
import com.airticket.service.AirportService;
import com.airticket.service.FlightService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FlightAssistantTools {

    private static final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Map<String, List<String>> ALIAS_TO_AIRPORT_CODES = createAliasMap();

    private final FlightService flightService;
    private final AirportService airportService;

    public FlightAssistantTools(FlightService flightService, AirportService airportService) {
        this.flightService = flightService;
        this.airportService = airportService;
    }

    @Tool("""
        查询真实可订的航班行程。
        使用前提：
        - 仅当出发地、目的地、出发日期都已经明确时才调用
        - date 必须是 YYYY-MM-DD 格式的绝对日期，不能是“明天”“下周五”这类相对时间

        参数说明：
        - from: 用户表达的出发地原文，可以是城市名、机场名、机场三字码、别名或俗称，例如“魔都”“上海”“PVG”
        - to: 用户表达的目的地原文，可以是城市名、机场名、机场三字码、别名或俗称，例如“东京”“NRT”“羽田”
        - date: YYYY-MM-DD 格式的出发日期

        工具会在内部完成城市/机场三字码映射、模糊匹配和歧义检测，然后查询后端真实航班数据。
        返回结果可能同时包含直飞和中转行程，请优先阅读 itineraries 字段：
        - itineraries: 完整可推荐行程，可能是 1 段直飞，也可能是 2 段中转
        - flights: 仅保留直飞单段航班，主要用于兼容旧前端

        如果地名仍然不够明确，结果中会标记 clarificationRequired=true，此时你必须先向用户追问。
        如果没有查到符合条件的行程，必须明确说明未查到，绝对不能编造航班数据。
        """)
    public FlightSearchToolResult searchFlights(
        @P("出发地原文，允许城市名、机场名、机场三字码、别名或俗称") String from,
        @P("目的地原文，允许城市名、机场名、机场三字码、别名或俗称") String to,
        @P("出发日期，必须是 YYYY-MM-DD 格式") String date
    ) {
        FlightSearchToolResult result = new FlightSearchToolResult();
        result.setDepartureDate(date);

        LocationResolveResult departure = resolveLocationInternal(from);
        LocationResolveResult arrival = resolveLocationInternal(to);
        result.setDeparture(departure);
        result.setArrival(arrival);

        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(date);
        } catch (Exception ex) {
            result.setSearchExecuted(false);
            result.setHasResults(false);
            result.setMessage("出发日期格式无效，请使用 YYYY-MM-DD。");
            return result;
        }

        if (!departure.isResolved() || departure.isClarificationRequired()) {
            result.setSearchExecuted(false);
            result.setHasResults(false);
            result.setMessage("出发地仍不明确，请先向用户确认具体城市或机场。");
            return result;
        }

        if (!arrival.isResolved() || arrival.isClarificationRequired()) {
            result.setSearchExecuted(false);
            result.setHasResults(false);
            result.setMessage("目的地仍不明确，请先向用户确认具体城市或机场。");
            return result;
        }

        ConnectingFlightSearchRequest request = new ConnectingFlightSearchRequest();
        request.setDepartureCity(departure.getCanonicalQuery());
        request.setArrivalCity(arrival.getCanonicalQuery());
        request.setDepartureDate(parsedDate);
        request.setIncludeConnectingFlights(true);

        List<ConnectingFlight> itineraries = flightService.searchConnectingFlights(request);
        List<FlightOptionView> directFlights = itineraries.stream()
            .map(ConnectingFlight::getFlights)
            .filter(Objects::nonNull)
            .filter(segments -> segments.size() == 1)
            .map(segments -> segments.get(0))
            .map(this::toFlightOptionView)
            .toList();

        result.setSearchExecuted(true);
        result.setHasResults(!itineraries.isEmpty());
        result.setFlights(directFlights);
        result.setItineraries(itineraries.stream().map(this::toItineraryView).toList());

        if (itineraries.isEmpty()) {
            result.setMessage("未查到符合条件的真实行程。可建议用户查看前后两天、接受中转，或更换出发/到达机场。");
        } else {
            result.setMessage("已返回真实航班行程数据，请优先根据 itineraries 字段生成推荐，必要时可区分直飞与中转。");
        }

        return result;
    }

    private LocationResolveResult resolveLocationInternal(String location) {
        LocationResolveResult result = new LocationResolveResult();
        result.setOriginalInput(location);

        String normalized = normalize(location);
        if (normalized == null) {
            result.setResolved(false);
            result.setClarificationRequired(true);
            result.setMessage("地点为空，请补充城市或机场。");
            return result;
        }

        Airport exactCodeAirport = airportService.findByCode(normalized.toUpperCase(Locale.ROOT));
        if (exactCodeAirport != null) {
            result.setResolved(true);
            result.setCanonicalQuery(exactCodeAirport.getCode());
            result.setMessage("已按具体机场精确匹配。");
            result.setCandidates(List.of(toCandidate(exactCodeAirport)));
            return result;
        }

        List<Airport> aliasCandidates = resolveAliasCandidates(normalized);
        if (!aliasCandidates.isEmpty()) {
            return buildResolvedResult(location, aliasCandidates, "已按常见别名或俗称完成映射。");
        }

        List<Airport> allAirports = airportService.findAll();
        List<ScoredAirport> matches = allAirports.stream()
            .map(airport -> new ScoredAirport(airport, scoreAirport(airport, normalized)))
            .filter(scored -> scored.score() > 0)
            .sorted(Comparator.comparingInt(ScoredAirport::score).reversed()
                .thenComparing(scored -> safe(scored.airport().getCode())))
            .toList();

        if (matches.isEmpty()) {
            result.setResolved(false);
            result.setClarificationRequired(true);
            result.setMessage("未识别到可用机场，请让用户换一种更标准的城市名或机场名。");
            return result;
        }

        int bestScore = matches.get(0).score();
        List<Airport> topCandidates = matches.stream()
            .filter(match -> match.score() == bestScore || match.score() >= bestScore - 5)
            .limit(5)
            .map(ScoredAirport::airport)
            .toList();

        Set<String> distinctCities = topCandidates.stream()
            .map(airport -> normalize(safe(airport.getCity())))
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (distinctCities.size() > 1 && bestScore < 90) {
            result.setResolved(false);
            result.setClarificationRequired(true);
            result.setCandidates(topCandidates.stream().map(this::toCandidate).toList());
            result.setMessage("匹配到了多个可能地点，请让用户确认具体城市或机场。");
            return result;
        }

        return buildResolvedResult(location, topCandidates, "已按模糊匹配完成地点归一化。");
    }

    private LocationResolveResult buildResolvedResult(String originalInput, List<Airport> airports, String message) {
        LocationResolveResult result = new LocationResolveResult();
        result.setOriginalInput(originalInput);
        result.setResolved(true);
        result.setMessage(message);
        result.setCandidates(airports.stream().map(this::toCandidate).toList());

        Map<String, List<Airport>> groupedByCity = airports.stream()
            .collect(Collectors.groupingBy(airport -> safe(airport.getCity()), LinkedHashMap::new, Collectors.toList()));

        if (groupedByCity.size() == 1) {
            List<Airport> sameCityAirports = groupedByCity.values().iterator().next();
            if (sameCityAirports.size() == 1) {
                result.setCanonicalQuery(sameCityAirports.get(0).getCode());
                result.setCityLevelSearch(false);
                result.setClarificationRequired(false);
            } else {
                result.setCanonicalQuery(sameCityAirports.get(0).getCity());
                result.setCityLevelSearch(true);
                result.setClarificationRequired(false);
            }
            return result;
        }

        result.setResolved(false);
        result.setClarificationRequired(true);
        result.setCanonicalQuery(null);
        result.setMessage("候选机场分属多个城市，请先确认具体地点。");
        return result;
    }

    private List<Airport> resolveAliasCandidates(String normalized) {
        List<String> airportCodes = ALIAS_TO_AIRPORT_CODES.get(normalized);
        if (airportCodes == null || airportCodes.isEmpty()) {
            return List.of();
        }

        List<Airport> airports = airportCodes.stream()
            .map(airportService::findByCode)
            .filter(Objects::nonNull)
            .toList();

        if (airports.isEmpty()) {
            return List.of();
        }
        return airports;
    }

    private int scoreAirport(Airport airport, String normalizedInput) {
        if (airport == null || normalizedInput == null) {
            return 0;
        }

        String code = normalize(airport.getCode());
        String city = normalize(airport.getCity());
        String name = normalize(airport.getName());
        String country = normalize(airport.getCountry());

        if (normalizedInput.equals(code)) {
            return 100;
        }
        if (normalizedInput.equals(city)) {
            return 95;
        }
        if (normalizedInput.equals(name)) {
            return 92;
        }
        if (containsEither(name, normalizedInput)) {
            return 85;
        }
        if (containsEither(city, normalizedInput)) {
            return 82;
        }
        if (containsEither(country, normalizedInput)) {
            return 55;
        }
        return 0;
    }

    private boolean containsEither(String left, String right) {
        return left != null && right != null && (left.contains(right) || right.contains(left));
    }

    private AirportCandidateView toCandidate(Airport airport) {
        AirportCandidateView view = new AirportCandidateView();
        view.setCode(airport.getCode());
        view.setName(airport.getName());
        view.setCity(airport.getCity());
        view.setCountry(airport.getCountry());
        view.setTimeZone(airport.getTimeZone());
        return view;
    }

    private FlightItineraryView toItineraryView(ConnectingFlight itinerary) {
        FlightItineraryView view = new FlightItineraryView();
        List<Flight> segments = itinerary.getFlights() == null ? List.of() : itinerary.getFlights();
        List<FlightOptionView> segmentViews = segments.stream().map(this::toFlightOptionView).toList();
        Flight firstSegment = segments.isEmpty() ? null : segments.get(0);
        Flight lastSegment = segments.isEmpty() ? null : segments.get(segments.size() - 1);

        view.setType(itinerary.getType());
        view.setSegmentCount(segmentViews.size());
        view.setSegmentIds(segments.stream()
            .map(Flight::getId)
            .filter(Objects::nonNull)
            .toList());
        view.setSegments(segmentViews);
        view.setFlightNumberSummary(segments.stream()
            .map(Flight::getFlightNumber)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(" + ")));
        view.setDepartureAirportCode(firstSegment != null && firstSegment.getDepartureAirport() != null
            ? firstSegment.getDepartureAirport().getCode()
            : null);
        view.setDepartureAirportName(firstSegment != null && firstSegment.getDepartureAirport() != null
            ? firstSegment.getDepartureAirport().getName()
            : null);
        view.setDepartureCity(firstSegment != null && firstSegment.getDepartureAirport() != null
            ? firstSegment.getDepartureAirport().getCity()
            : null);
        view.setDepartureTimeUtc(formatUtc(itinerary.getDepartureTimeUtc()));
        view.setDepartureTimeLocal(formatLocal(
            itinerary.getDepartureTimeUtc(),
            resolveZoneId(view.getDepartureAirportCode())
        ));
        view.setArrivalAirportCode(lastSegment != null && lastSegment.getArrivalAirport() != null
            ? lastSegment.getArrivalAirport().getCode()
            : null);
        view.setArrivalAirportName(lastSegment != null && lastSegment.getArrivalAirport() != null
            ? lastSegment.getArrivalAirport().getName()
            : null);
        view.setArrivalCity(lastSegment != null && lastSegment.getArrivalAirport() != null
            ? lastSegment.getArrivalAirport().getCity()
            : null);
        view.setArrivalTimeUtc(formatUtc(itinerary.getArrivalTimeUtc()));
        view.setArrivalTimeLocal(formatLocal(
            itinerary.getArrivalTimeUtc(),
            resolveZoneId(view.getArrivalAirportCode())
        ));
        view.setTotalPrice(itinerary.getTotalPrice());
        view.setTotalDurationMinutes(itinerary.getTotalDurationMinutes());
        view.setAvailableSeats(itinerary.getAvailableSeats());
        view.setConnectionInfo(itinerary.getConnectionInfo());
        view.setTransferNotice(itinerary.getTransferNotice());
        view.setRequiresAirportSwitch(itinerary.isRequiresAirportSwitch());
        return view;
    }

    private FlightOptionView toFlightOptionView(Flight flight) {
        FlightOptionView view = new FlightOptionView();
        view.setId(flight.getId());
        view.setFlightNumber(flight.getFlightNumber());
        view.setAirlineName(flight.getAirline() != null ? flight.getAirline().getName() : null);
        view.setDepartureAirportCode(flight.getDepartureAirport() != null ? flight.getDepartureAirport().getCode() : null);
        view.setDepartureAirportName(flight.getDepartureAirport() != null ? flight.getDepartureAirport().getName() : null);
        view.setDepartureCity(flight.getDepartureAirport() != null ? flight.getDepartureAirport().getCity() : null);
        view.setArrivalAirportCode(flight.getArrivalAirport() != null ? flight.getArrivalAirport().getCode() : null);
        view.setArrivalAirportName(flight.getArrivalAirport() != null ? flight.getArrivalAirport().getName() : null);
        view.setArrivalCity(flight.getArrivalAirport() != null ? flight.getArrivalAirport().getCity() : null);
        view.setDepartureTimeUtc(formatUtc(flight.getDepartureTimeUtc()));
        view.setArrivalTimeUtc(formatUtc(flight.getArrivalTimeUtc()));
        view.setDepartureTimeLocal(formatLocal(
            flight.getDepartureTimeUtc(),
            resolveZoneId(flight.getDepartureAirport() != null ? flight.getDepartureAirport().getCode() : null)
        ));
        view.setArrivalTimeLocal(formatLocal(
            flight.getArrivalTimeUtc(),
            resolveZoneId(flight.getArrivalAirport() != null ? flight.getArrivalAirport().getCode() : null)
        ));
        view.setPrice(flight.getPrice());
        view.setAvailableSeats(flight.getAvailableSeats());
        view.setStatus(flight.getStatus());
        view.setAircraftType(flight.getAircraftType());
        return view;
    }

    private String formatUtc(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private String formatLocal(Instant instant, ZoneId zoneId) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(zoneId).format(LOCAL_TIME_FORMATTER);
    }

    private ZoneId resolveZoneId(String airportCode) {
        if (airportCode == null || airportCode.isBlank()) {
            return ZoneId.of("Asia/Shanghai");
        }
        Airport airport = airportService.findByCode(airportCode);
        if (airport == null || airport.getTimeZone() == null || airport.getTimeZone().isBlank()) {
            return ZoneId.of("Asia/Shanghai");
        }
        return ZoneId.of(airport.getTimeZone());
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.replace("机场", "")
            .replace("international airport", "")
            .replace("airport", "")
            .replace("国际", "")
            .replace("  ", " ")
            .trim();
    }

    private String safe(String text) {
        return text == null ? "" : text;
    }

    private static Map<String, List<String>> createAliasMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("魔都", List.of("PVG", "SHA"));
        map.put("帝都", List.of("PEK", "PKX"));
        map.put("妖都", List.of("CAN"));
        map.put("shanghai", List.of("PVG", "SHA"));
        map.put("beijing", List.of("PEK", "PKX"));
        map.put("tokyo", List.of("HND", "NRT"));
        map.put("london", List.of("LHR"));
        map.put("new york", List.of("JFK"));
        map.put("nyc", List.of("JFK"));
        map.put("los angeles", List.of("LAX"));
        map.put("la", List.of("LAX"));
        map.put("san francisco", List.of("SFO"));
        map.put("seoul", List.of("ICN"));
        map.put("singapore", List.of("SIN"));
        map.put("bangkok", List.of("BKK"));
        map.put("paris", List.of("CDG"));
        map.put("frankfurt", List.of("FRA"));
        map.put("amsterdam", List.of("AMS"));
        map.put("sydney", List.of("SYD"));
        map.put("melbourne", List.of("MEL"));
        map.put("toronto", List.of("YYZ"));
        map.put("vancouver", List.of("YVR"));
        map.put("dubai", List.of("DXB"));
        return map;
    }

    private record ScoredAirport(Airport airport, int score) {
    }
}
