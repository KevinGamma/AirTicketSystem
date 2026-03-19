package com.airticket.config;

import com.airticket.mapper.AirportMapper;
import com.airticket.mapper.FlightMapper;
import com.airticket.mapper.UserMapper;
import com.airticket.model.Airport;
import com.airticket.model.Flight;
import com.airticket.model.User;
import com.airticket.service.AirlineService;
import com.airticket.service.AirportService;
import com.airticket.service.DatabaseOptimizationService;
import com.airticket.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class DataInitializer {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired 
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AirportMapper airportMapper;

    @Autowired
    private FlightMapper flightMapper;
    
    @Autowired
    private AirlineService airlineService;
    
    @Autowired
    private AirportService airportService;

    @Autowired
    private DatabaseOptimizationService databaseOptimizationService;
    
    @Autowired
    private com.airticket.mapper.AirlineMapper airlineMapper;

    @Bean
    public ApplicationRunner init() {
        return args -> {
            User adminUser = userService.findByUsername("admin");
            if (adminUser == null) {
                System.out.println("Creating admin user...");
                userService.createUser("admin", "admin123", "admin@airticket.com", "System Administrator", "ADMIN");
                System.out.println("Admin user created with password: admin123");
            } else {
                System.out.println("Admin user already exists");
                String newPassword = passwordEncoder.encode("admin123");
                System.out.println("Updating admin password. New hash: " + newPassword);
                adminUser.setPassword(newPassword);
                userMapper.update(adminUser);
                System.out.println("Admin password updated in database");
            }
            airportService.initializeCommonAirports();
            airlineService.initializeDefaultAirlines();
            initializeFlights();
            updateExistingFlightAircraftTypes();
            databaseOptimizationService.optimizeDatabase();
        };
    }
    
    private void initializeAirports() {
        updateExistingAirportsToChinese();
        addAdditionalChineseAirports();
    }
    
    private void updateExistingAirportsToChinese() {
        System.out.println("Updating existing airports to Chinese names...");
        
        
        Airport pekAirport = airportMapper.findById(1L);
        if (pekAirport != null && "Beijing".equals(pekAirport.getCity())) {
            pekAirport.setName("北京首都国际机场");
            pekAirport.setCity("北京");
            pekAirport.setCountry("中国");
            pekAirport.setTimeZone("Asia/Shanghai");
            airportMapper.update(pekAirport);
            System.out.println("Updated airport: " + pekAirport.getName());
        }
        
        Airport pvgAirport = airportMapper.findById(2L);
        if (pvgAirport != null && "Shanghai".equals(pvgAirport.getCity())) {
            pvgAirport.setName("上海浦东国际机场");
            pvgAirport.setCity("上海");
            pvgAirport.setCountry("中国");
            pvgAirport.setTimeZone("Asia/Shanghai");
            airportMapper.update(pvgAirport);
            System.out.println("Updated airport: " + pvgAirport.getName());
        }
        
        Airport canAirport = airportMapper.findById(3L);
        if (canAirport != null && "Guangzhou".equals(canAirport.getCity())) {
            canAirport.setName("广州白云国际机场");
            canAirport.setCity("广州");
            canAirport.setCountry("中国");
            canAirport.setTimeZone("Asia/Shanghai");
            airportMapper.update(canAirport);
            System.out.println("Updated airport: " + canAirport.getName());
        }
        
        Airport laxAirport = airportMapper.findById(4L);
        if (laxAirport != null && "Los Angeles".equals(laxAirport.getCity())) {
            laxAirport.setName("洛杉矶国际机场");
            laxAirport.setCity("洛杉矶");
            laxAirport.setCountry("美国");
            laxAirport.setTimeZone("America/Los_Angeles");
            airportMapper.update(laxAirport);
            System.out.println("Updated airport: " + laxAirport.getName());
        }
        
        Airport jfkAirport = airportMapper.findById(5L);
        if (jfkAirport != null && "New York".equals(jfkAirport.getCity())) {
            jfkAirport.setName("约翰·肯尼迪国际机场");
            jfkAirport.setCity("纽约");
            jfkAirport.setCountry("美国");
            jfkAirport.setTimeZone("America/New_York");
            airportMapper.update(jfkAirport);
            System.out.println("Updated airport: " + jfkAirport.getName());
        }
        
        Airport nrtAirport = airportMapper.findById(6L);
        if (nrtAirport != null && "Tokyo".equals(nrtAirport.getCity())) {
            nrtAirport.setName("成田国际机场");
            nrtAirport.setCity("东京");
            nrtAirport.setCountry("日本");
            nrtAirport.setTimeZone("Asia/Tokyo");
            airportMapper.update(nrtAirport);
            System.out.println("Updated airport: " + nrtAirport.getName());
        }
        
        Airport icnAirport = airportMapper.findById(7L);
        if (icnAirport != null && "Seoul".equals(icnAirport.getCity())) {
            icnAirport.setName("仁川国际机场");
            icnAirport.setCity("首尔");
            icnAirport.setCountry("韩国");
            icnAirport.setTimeZone("Asia/Seoul");
            airportMapper.update(icnAirport);
            System.out.println("Updated airport: " + icnAirport.getName());
        }
        
        Airport sinAirport = airportMapper.findById(8L);
        if (sinAirport != null && "Singapore".equals(sinAirport.getCity())) {
            sinAirport.setName("新加坡樟宜机场");
            sinAirport.setCity("新加坡");
            sinAirport.setCountry("新加坡");
            airportMapper.update(sinAirport);
            System.out.println("Updated airport: " + sinAirport.getName());
        }
    }
    
    private void addAdditionalChineseAirports() {
        if (airportMapper.findAll().size() < 30) {
            System.out.println("Adding additional Chinese airports...");
            Airport[] additionalAirports = {
                new Airport("SZX", "深圳宝安国际机场", "深圳", "中国", "Asia/Shanghai"),
                new Airport("CTU", "成都双流国际机场", "成都", "中国", "Asia/Shanghai"),
                new Airport("KMG", "昆明长水国际机场", "昆明", "中国", "Asia/Shanghai"),
                new Airport("XIY", "西安咸阳国际机场", "西安", "中国", "Asia/Shanghai"),
                new Airport("HGH", "杭州萧山国际机场", "杭州", "中国", "Asia/Shanghai"),
                new Airport("NKG", "南京禄口国际机场", "南京", "中国", "Asia/Shanghai"),
                new Airport("WUH", "武汉天河国际机场", "武汉", "中国", "Asia/Shanghai"),
                new Airport("TSN", "天津滨海国际机场", "天津", "中国", "Asia/Shanghai"),
                new Airport("CKG", "重庆江北国际机场", "重庆", "中国", "Asia/Shanghai"),
                new Airport("URC", "乌鲁木齐地窝堡国际机场", "乌鲁木齐", "中国", "Asia/Shanghai"),
                new Airport("TNA", "济南遥墙国际机场", "济南", "中国", "Asia/Shanghai"),
                new Airport("KWE", "贵阳龙洞堡国际机场", "贵阳", "中国", "Asia/Shanghai"),
                new Airport("SJW", "石家庄正定国际机场", "石家庄", "中国", "Asia/Shanghai"),
                new Airport("TYN", "太原武宿国际机场", "太原", "中国", "Asia/Shanghai"),
                new Airport("SHE", "沈阳桃仙国际机场", "沈阳", "中国", "Asia/Shanghai"),
                new Airport("CGQ", "长春龙嘉国际机场", "长春", "中国", "Asia/Shanghai"),
                new Airport("HRB", "哈尔滨太平国际机场", "哈尔滨", "中国", "Asia/Shanghai"),
                new Airport("HFE", "合肥新桥国际机场", "合肥", "中国", "Asia/Shanghai"),
                new Airport("FOC", "福州长乐国际机场", "福州", "中国", "Asia/Shanghai"),
                new Airport("NNC", "南昌昌北国际机场", "南昌", "中国", "Asia/Shanghai"),
                new Airport("CSX", "长沙黄花国际机场", "长沙", "中国", "Asia/Shanghai"),
                new Airport("NNG", "南宁吴圩国际机场", "南宁", "中国", "Asia/Shanghai"),
                new Airport("HAK", "海口美兰国际机场", "海口", "中国", "Asia/Shanghai"),
                new Airport("LHW", "兰州中川国际机场", "兰州", "中国", "Asia/Shanghai"),
                new Airport("INC", "银川河东国际机场", "银川", "中国", "Asia/Shanghai"),
                new Airport("XNN", "西宁曹家堡机场", "西宁", "中国", "Asia/Shanghai"),
                new Airport("LSA", "拉萨贡嘎机场", "拉萨", "中国", "Asia/Shanghai"),
                new Airport("PKX", "北京大兴国际机场", "北京", "中国", "Asia/Shanghai"),
                new Airport("SHA", "上海虹桥国际机场", "上海", "中国", "Asia/Shanghai"),
                new Airport("ZUH", "珠海金湾机场", "珠海", "中国", "Asia/Shanghai"),
                new Airport("TFU", "成都天府国际机场", "成都", "中国", "Asia/Shanghai")
            };
            
            for (Airport airport : additionalAirports) {
                if (airportMapper.findByCode(airport.getCode()) == null) {
                    airportMapper.insert(airport);
                    System.out.println("Added airport: " + airport.getName());
                }
            }
        } else {
            System.out.println("Additional airports already exist");
        }
    }
    
    private void initializeFlights() {
        if (flightMapper.findAll().isEmpty()) {
            initializeFlightsForceNew();
        } else {
            System.out.println("Flights already exist");
        }
    }

    private int generateMonthlyFlightSchedule() {
        Map<String, Airport> airportByCode = loadAirportMap();
        List<RouteTemplate> routeTemplates = buildRouteTemplates();
        LocalDate baseDate = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1);
        int createdCount = 0;

        for (int dayOffset = 0; dayOffset < 30; dayOffset++) {
            LocalDate flightDate = baseDate.plusDays(dayOffset);

            for (int routeIndex = 0; routeIndex < routeTemplates.size(); routeIndex++) {
                Flight flight = buildFlight(routeTemplates.get(routeIndex), airportByCode, flightDate, dayOffset, routeIndex);
                if (flight == null) {
                    continue;
                }

                flightMapper.insert(flight);
                createdCount++;
            }
        }

        return createdCount;
    }

    private Map<String, Airport> loadAirportMap() {
        Map<String, Airport> airportByCode = new LinkedHashMap<>();
        for (Airport airport : airportMapper.findAll()) {
            airportByCode.put(airport.getCode(), airport);
        }
        return airportByCode;
    }

    private List<RouteTemplate> buildRouteTemplates() {
        List<RouteTemplate> routes = new ArrayList<>();

        routes.add(new RouteTemplate("CA", 1100, "PEK", "PVG", 7, 30, 130, 188, 1280, 35, "B737-800"));
        routes.add(new RouteTemplate("MU", 2100, "PVG", "PEK", 8, 20, 135, 198, 1260, 30, "A320"));
        routes.add(new RouteTemplate("CZ", 3100, "CAN", "PEK", 9, 0, 185, 220, 1760, 45, "A330-200"));
        routes.add(new RouteTemplate("HU", 4100, "SZX", "PVG", 7, 50, 150, 176, 1180, 25, "B737-800"));
        routes.add(new RouteTemplate("3U", 5100, "CTU", "PEK", 6, 55, 175, 164, 1420, 30, "A320"));
        routes.add(new RouteTemplate("CA", 1200, "PEK", "CAN", 12, 40, 190, 200, 1820, 40, "A330-200"));
        routes.add(new RouteTemplate("MU", 2200, "SHA", "SZX", 11, 20, 145, 170, 980, 20, "A320"));
        routes.add(new RouteTemplate("CZ", 3200, "CAN", "CTU", 13, 35, 150, 190, 1120, 25, "B737-800"));
        routes.add(new RouteTemplate("FM", 4200, "SHA", "WUH", 14, 15, 100, 160, 760, 15, "A320"));
        routes.add(new RouteTemplate("9C", 6200, "HGH", "XIY", 15, 10, 150, 186, 820, 18, "A320"));
        routes.add(new RouteTemplate("MU", 2300, "PVG", "KMG", 16, 0, 210, 200, 1390, 25, "A330-200"));
        routes.add(new RouteTemplate("CA", 1300, "PEK", "URC", 9, 10, 255, 210, 2280, 50, "B787-9"));
        routes.add(new RouteTemplate("HU", 4300, "HAK", "PEK", 20, 25, 235, 190, 1680, 35, "A330-200"));
        routes.add(new RouteTemplate("CZ", 3300, "SZX", "NKG", 18, 5, 130, 180, 980, 20, "A320"));
        routes.add(new RouteTemplate("3U", 5300, "CTU", "SYX", 17, 20, 160, 168, 1280, 28, "A320"));
        routes.add(new RouteTemplate("CA", 1400, "PEK", "TAO", 19, 15, 95, 162, 680, 12, "B737-800"));
        routes.add(new RouteTemplate("MU", 2400, "PVG", "CSX", 21, 0, 115, 168, 740, 16, "A320"));
        routes.add(new RouteTemplate("CZ", 3400, "CAN", "HAK", 22, 10, 85, 174, 620, 10, "B737-800"));
        routes.add(new RouteTemplate("FM", 4400, "SHA", "CGO", 8, 40, 125, 158, 690, 14, "A320"));
        routes.add(new RouteTemplate("9C", 6400, "PKX", "KMG", 10, 5, 220, 186, 1160, 24, "A320"));
        routes.add(new RouteTemplate("HU", 4500, "SZX", "CTU", 19, 40, 160, 180, 1080, 22, "B737-800"));
        routes.add(new RouteTemplate("CA", 1500, "PEK", "XIY", 16, 45, 140, 188, 890, 18, "B737-800"));

        routes.add(new RouteTemplate("CA", 7100, "PEK", "NRT", 9, 30, 210, 252, 2380, 55, "A330-200"));
        routes.add(new RouteTemplate("MU", 7200, "PVG", "ICN", 10, 15, 115, 220, 1680, 30, "A320"));
        routes.add(new RouteTemplate("CZ", 7300, "CAN", "SIN", 11, 20, 245, 260, 2280, 50, "A330-200"));
        routes.add(new RouteTemplate("HU", 7400, "SZX", "BKK", 13, 5, 210, 236, 1880, 45, "B787-9"));
        routes.add(new RouteTemplate("CA", 7500, "PEK", "LHR", 14, 10, 660, 320, 5480, 120, "B777-300ER"));
        routes.add(new RouteTemplate("MU", 7600, "PVG", "LAX", 12, 50, 720, 330, 5980, 140, "B787-9"));
        routes.add(new RouteTemplate("CZ", 7700, "CAN", "DXB", 18, 40, 510, 280, 3680, 90, "A350-900"));
        routes.add(new RouteTemplate("CA", 7800, "PEK", "SFO", 15, 25, 690, 320, 5860, 130, "B787-9"));
        routes.add(new RouteTemplate("MU", 7210, "ICN", "PVG", 14, 5, 120, 220, 1580, 28, "A320"));
        routes.add(new RouteTemplate("CA", 7110, "NRT", "PEK", 15, 30, 220, 252, 2280, 50, "A330-200"));
        routes.add(new RouteTemplate("CZ", 7310, "SIN", "CAN", 8, 45, 250, 260, 2180, 48, "A330-200"));
        routes.add(new RouteTemplate("HU", 7410, "BKK", "SZX", 16, 20, 205, 236, 1820, 42, "B787-9"));
        routes.add(new RouteTemplate("CA", 7510, "LHR", "PEK", 12, 20, 590, 320, 5380, 115, "B777-300ER"));
        routes.add(new RouteTemplate("MU", 7610, "LAX", "PVG", 11, 30, 830, 330, 6180, 150, "B787-9"));
        routes.add(new RouteTemplate("CZ", 7710, "DXB", "CAN", 22, 15, 450, 280, 3580, 88, "A350-900"));
        routes.add(new RouteTemplate("CA", 7810, "SFO", "PEK", 13, 40, 760, 320, 5960, 135, "B787-9"));
        routes.add(new RouteTemplate("MU", 7620, "PVG", "SYD", 20, 10, 610, 300, 4880, 100, "A350-900"));
        routes.add(new RouteTemplate("MU", 7630, "SYD", "PVG", 12, 15, 640, 300, 4980, 105, "A350-900"));

        return routes;
    }

    private Flight buildFlight(RouteTemplate route, Map<String, Airport> airportByCode, LocalDate flightDate, int dayOffset, int routeIndex) {
        Airport departureAirport = airportByCode.get(route.departureCode());
        Airport arrivalAirport = airportByCode.get(route.arrivalCode());

        if (departureAirport == null || arrivalAirport == null) {
            System.err.println("Skipping route " + route.departureCode() + " -> " + route.arrivalCode() + " because airport data is missing");
            return null;
        }

        Long airlineId = getAirlineIdByCode(route.airlineCode());
        if (airlineId == null) {
            return null;
        }

        int departureShiftMinutes = Math.floorMod(dayOffset + routeIndex, 4) * 5;
        LocalDateTime departureLocalTime = flightDate.atTime(route.departureHour(), route.departureMinute()).plusMinutes(departureShiftMinutes);
        Instant departureInstant = departureLocalTime.atZone(ZoneId.of(departureAirport.getTimeZone())).toInstant();
        Instant arrivalInstant = departureInstant.plus(Duration.ofMinutes(route.durationMinutes()));

        Flight flight = new Flight(
                buildFlightNumber(route, dayOffset),
                airlineId,
                departureAirport.getId(),
                arrivalAirport.getId(),
                departureInstant,
                arrivalInstant,
                route.totalSeats(),
                BigDecimal.valueOf(route.basePrice())
                        .add(BigDecimal.valueOf((long) Math.floorMod(dayOffset + routeIndex, 6) * route.priceStep()))
        );
        flight.setAircraftType(route.aircraftType());
        return flight;
    }

    private String buildFlightNumber(RouteTemplate route, int dayOffset) {
        return String.format("%s%d%02d", route.airlineCode(), route.numberPrefix(), dayOffset + 1);
    }
    
    private void updateExistingFlightAircraftTypes() {
        System.out.println("Updating existing flights to use aircraft model codes...");

        var existingFlights = flightMapper.findAll();

        java.util.Map<String, String> aircraftTypeMap = new java.util.HashMap<>();
        aircraftTypeMap.put("Boeing 737-800", "B737-800");
        aircraftTypeMap.put("Boeing 777-300", "B777-300ER");
        aircraftTypeMap.put("Boeing 777-300ER", "B777-300ER");
        aircraftTypeMap.put("Airbus A320", "A320");
        aircraftTypeMap.put("Airbus A330", "A330-200");
        aircraftTypeMap.put("Airbus A330-200", "A330-200");
        aircraftTypeMap.put("Boeing 787-9", "B787-9");
        aircraftTypeMap.put("Airbus A350-900", "A350-900");
        aircraftTypeMap.put("波音737-800", "B737-800");
        aircraftTypeMap.put("波音777-300", "B777-300ER");
        aircraftTypeMap.put("空客A320", "A320");
        aircraftTypeMap.put("空客A330", "A330-200");
        aircraftTypeMap.put("波音787-9", "B787-9");
        aircraftTypeMap.put("空客A350-900", "A350-900");
        aircraftTypeMap.put("B737", "B737-800");
        aircraftTypeMap.put("B738", "B737-800");
        aircraftTypeMap.put("B777", "B777-300ER");
        aircraftTypeMap.put("A330", "A330-200");
        aircraftTypeMap.put("A350", "A350-900");
        aircraftTypeMap.put("B787", "B787-9");
        
        int updatedCount = 0;
        
        for (Flight flight : existingFlights) {
            String currentAircraftType = flight.getAircraftType();

            if (currentAircraftType != null && aircraftTypeMap.containsKey(currentAircraftType)) {
                String standardCode = aircraftTypeMap.get(currentAircraftType);
                flight.setAircraftType(standardCode);
                
                try {
                    flightMapper.update(flight);
                    updatedCount++;
                    System.out.println("Updated flight " + flight.getFlightNumber() + 
                                     " aircraft type from '" + currentAircraftType + 
                                     "' to '" + standardCode + "'");
                } catch (Exception e) {
                    System.err.println("Failed to update flight " + flight.getFlightNumber() + ": " + e.getMessage());
                }
            }
        }
        
        System.out.println("Successfully updated " + updatedCount + " flights with standard aircraft model codes.");
    }

    private Long getAirlineIdByCode(String code) {
        com.airticket.model.Airline airline = airlineMapper.findByCode(code);
        if (airline != null) {
            return airline.getId();
        }

        System.err.println("Warning: Airline with code '" + code + "' not found, using default ID 1");
        return 1L;
    }
    

    public void reinitializeData() {
        try {
            User adminUser = userService.findByUsername("admin");
            if (adminUser == null) {
                System.out.println("Creating admin user...");
                userService.createUser("admin", "admin123", "admin@airticket.com", "System Administrator", "ADMIN");
                System.out.println("Admin user created with password: admin123");
            } else {
                System.out.println("Admin user already exists");
                String newPassword = passwordEncoder.encode("admin123");
                System.out.println("Updating admin password. New hash: " + newPassword);

                adminUser.setPassword(newPassword);
                userMapper.update(adminUser);
                System.out.println("Admin password updated in database");
            }

            airportService.initializeCommonAirports();

            airlineService.initializeDefaultAirlines();
            System.out.println("Airlines initialized");

            initializeFlightsForceNew();

            updateExistingFlightAircraftTypes();
            
            System.out.println("Data reinitialization completed successfully");
        } catch (Exception e) {
            System.err.println("Error during data reinitialization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeFlightsForceNew() {
        System.out.println("Initializing comprehensive flight schedule for one month...");

        int createdCount = generateMonthlyFlightSchedule();

        System.out.println("Flight schedule initialization completed, created " + createdCount + " flights");
    }

    private record RouteTemplate(
            String airlineCode,
            int numberPrefix,
            String departureCode,
            String arrivalCode,
            int departureHour,
            int departureMinute,
            int durationMinutes,
            int totalSeats,
            long basePrice,
            long priceStep,
            String aircraftType) {
    }
}
