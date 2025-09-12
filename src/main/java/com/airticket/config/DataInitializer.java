package com.airticket.config;

import com.airticket.mapper.UserMapper;
import com.airticket.mapper.AirportMapper;
import com.airticket.mapper.FlightMapper;
import com.airticket.model.User;
import com.airticket.model.Airport;
import com.airticket.model.Flight;
import com.airticket.service.UserService;
import com.airticket.service.AirlineService;
import com.airticket.service.AirportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;

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
        };
    }
    
    private void initializeAirports() {
        updateExistingAirportsToChinese();
        addAdditionalChineseAirports();
    }
    
    private void updateExistingAirportsToChinese() {
        System.out.println("Updating existing airports to Chinese names...");
        
        // Update existing airports with Chinese city names
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
            System.out.println("Initializing comprehensive flight schedule for one month...");
            
            LocalDateTime baseDate = LocalDateTime.now().plusDays(1);

            for (int day = 0; day < 30; day++) {
                LocalDateTime currentDate = baseDate.plusDays(day);
                createDailyFlights(currentDate, "morning", day);
                createDailyFlights(currentDate, "afternoon", day);
                createDailyFlights(currentDate, "evening", day);
            }
        } else {
            System.out.println("Flights already exist");
        }
    }
    
    private void createDailyFlights(LocalDateTime date, String timeSlot, int dayOffset) {
        Flight[] dailyFlights = null;
        
        switch (timeSlot) {
            case "morning":
                dailyFlights = getMorningFlights(date, dayOffset);
                break;
            case "afternoon":
                dailyFlights = getAfternoonFlights(date, dayOffset);
                break;
            case "evening":
                dailyFlights = getEveningFlights(date, dayOffset);
                break;
        }
        
        if (dailyFlights != null) {
            for (Flight flight : dailyFlights) {
                setRandomAircraftType(flight, dayOffset);
                flightMapper.insert(flight);
                System.out.println("Created flight: " + flight.getFlightNumber() + " on " + date.toLocalDate());
            }
        }
    }
    
    private Instant convertToUtc(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.of("Asia/Shanghai")).toInstant();
    }
    
    private Flight[] getMorningFlights(LocalDateTime date, int dayOffset) {
        String dayStr = String.format("%02d", (dayOffset % 30) + 1);
        
        return new Flight[]{
            new Flight("CA12" + dayStr, getAirlineIdByCode("CA"), 1L, 2L,
                convertToUtc(date.withHour(6).withMinute(30)),
                convertToUtc(date.withHour(9).withMinute(0)),
                180, new BigDecimal("1280.00")),

            new Flight("MU56" + dayStr, getAirlineIdByCode("MU"), 2L, 1L,
                convertToUtc(date.withHour(7).withMinute(15)),
                convertToUtc(date.withHour(9).withMinute(45)),
                200, new BigDecimal("1290.00")),

            new Flight("CZ34" + dayStr, getAirlineIdByCode("CZ"), 1L, 3L,
                convertToUtc(date.withHour(8).withMinute(0)),
                convertToUtc(date.withHour(11).withMinute(45)),
                220, new BigDecimal("1890.00")),

            new Flight("CZ45" + dayStr, getAirlineIdByCode("CZ"), 3L, 2L,
                convertToUtc(date.withHour(9).withMinute(30)),
                convertToUtc(date.withHour(12).withMinute(15)),
                160, new BigDecimal("1580.00")),

            new Flight("MU78" + dayStr, getAirlineIdByCode("MU"), 2L, 3L,
                convertToUtc(date.withHour(10).withMinute(45)),
                convertToUtc(date.withHour(13).withMinute(30)),
                180, new BigDecimal("1620.00")),
        };
    }
    
    private Flight[] getAfternoonFlights(LocalDateTime date, int dayOffset) {
        String dayStr = String.format("%02d", (dayOffset % 30) + 1);
        
        return new Flight[]{
            new Flight("CA21" + dayStr, getAirlineIdByCode("CA"), 1L, 2L,
                convertToUtc(date.withHour(13).withMinute(20)),
                convertToUtc(date.withHour(15).withMinute(50)),
                180, new BigDecimal("1350.00")),

            new Flight("MU65" + dayStr, getAirlineIdByCode("MU"), 2L, 1L,
                convertToUtc(date.withHour(14).withMinute(30)),
                convertToUtc(date.withHour(17).withMinute(0)),
                200, new BigDecimal("1380.00")),

            new Flight("CZ43" + dayStr, getAirlineIdByCode("CZ"), 3L, 1L,
                convertToUtc(date.withHour(15).withMinute(15)),
                convertToUtc(date.withHour(18).withMinute(45)),
                220, new BigDecimal("1920.00")),

            new Flight("CA18" + dayStr, getAirlineIdByCode("CA"), 1L, 6L,
                convertToUtc(date.withHour(16).withMinute(0)),
                convertToUtc(date.withHour(20).withMinute(20)),
                200, new BigDecimal("2800.00")),

            new Flight("MU50" + dayStr, getAirlineIdByCode("MU"), 2L, 7L,
                convertToUtc(date.withHour(17).withMinute(40)),
                convertToUtc(date.withHour(20).withMinute(50)),
                180, new BigDecimal("1600.00")),
        };
    }
    
    private Flight[] getEveningFlights(LocalDateTime date, int dayOffset) {
        String dayStr = String.format("%02d", (dayOffset % 30) + 1);
        
        return new Flight[]{
            new Flight("CA31" + dayStr, getAirlineIdByCode("CA"), 1L, 2L,
                convertToUtc(date.withHour(19).withMinute(30)),
                convertToUtc(date.withHour(22).withMinute(0)),
                180, new BigDecimal("1420.00")),

            new Flight("MU75" + dayStr, getAirlineIdByCode("MU"), 2L, 1L,
                convertToUtc(date.withHour(20).withMinute(15)),
                convertToUtc(date.withHour(22).withMinute(45)),
                200, new BigDecimal("1450.00")),

            new Flight("CZ35" + dayStr, getAirlineIdByCode("CZ"), 3L, 8L,
                convertToUtc(date.withHour(21).withMinute(0)),
                convertToUtc(date.plusDays(1).withHour(1).withMinute(15)),
                220, new BigDecimal("1200.00")),

            new Flight("MU58" + dayStr, getAirlineIdByCode("MU"), 2L, 4L,
                convertToUtc(date.withHour(22).withMinute(30)),
                convertToUtc(date.plusDays(1).withHour(18).withMinute(40)), // 跨日期
                300, new BigDecimal("4500.00")),

            new Flight("CA98" + dayStr, getAirlineIdByCode("CA"), 1L, 5L,
                convertToUtc(date.withHour(23).withMinute(15)),
                convertToUtc(date.plusDays(1).withHour(19).withMinute(30)), // 跨日期
                350, new BigDecimal("5200.00")),
        };
    }
    
    private void setRandomAircraftType(Flight flight, int dayOffset) {
        String[] aircraftTypes = {
            "B737-800",
            "B777-300ER", 
            "A320",
            "A330-200",
            "B787-9",
            "A350-900"
        };

        if (flight.getDepartureAirportId() <= 3L && flight.getArrivalAirportId() <= 3L) {
            flight.setAircraftType(aircraftTypes[dayOffset % 3]);
        } else {
            flight.setAircraftType(aircraftTypes[3 + (dayOffset % 3)]);
        }
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
        
        LocalDateTime baseDate = LocalDateTime.now().plusDays(1);

        for (int day = 0; day < 30; day++) {
            LocalDateTime currentDate = baseDate.plusDays(day);

            createDailyFlights(currentDate, "morning", day);

            createDailyFlights(currentDate, "afternoon", day);

            createDailyFlights(currentDate, "evening", day);
        }
        
        System.out.println("Flight schedule initialization completed");
    }
}