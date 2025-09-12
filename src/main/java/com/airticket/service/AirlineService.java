package com.airticket.service;

import com.airticket.mapper.AirlineMapper;
import com.airticket.model.Airline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

@Service
public class AirlineService {
    
    @Autowired
    private AirlineMapper airlineMapper;

    private static final Map<String, String> AIRLINE_CODE_PATTERNS = new HashMap<>();
    
    static {
        AIRLINE_CODE_PATTERNS.put("CA", "中国国际航空");
        AIRLINE_CODE_PATTERNS.put("MU", "中国东方航空");
        AIRLINE_CODE_PATTERNS.put("CZ", "中国南方航空");
        AIRLINE_CODE_PATTERNS.put("HU", "海南航空");
        AIRLINE_CODE_PATTERNS.put("3U", "四川航空");
        AIRLINE_CODE_PATTERNS.put("MF", "厦门航空");
        AIRLINE_CODE_PATTERNS.put("FM", "上海航空");
        AIRLINE_CODE_PATTERNS.put("9C", "春秋航空");
        AIRLINE_CODE_PATTERNS.put("JD", "首都航空");
        AIRLINE_CODE_PATTERNS.put("G5", "华夏航空");
        AIRLINE_CODE_PATTERNS.put("8L", "祥鹏航空");
        AIRLINE_CODE_PATTERNS.put("EU", "成都航空");
        AIRLINE_CODE_PATTERNS.put("UQ", "乌鲁木齐航空");
        AIRLINE_CODE_PATTERNS.put("GS", "天津航空");
        AIRLINE_CODE_PATTERNS.put("DR", "瑞丽航空");
        AIRLINE_CODE_PATTERNS.put("TV", "西藏航空");
        AIRLINE_CODE_PATTERNS.put("BK", "奥凯航空");
        AIRLINE_CODE_PATTERNS.put("GJ", "长龙航空");
        AIRLINE_CODE_PATTERNS.put("OQ", "青岛航空");
        AIRLINE_CODE_PATTERNS.put("GX", "北部湾航空");
    }

    public List<Airline> getAllAirlines() {
        return airlineMapper.findAll();
    }

    public List<Airline> getAllActiveAirlines() {
        return airlineMapper.findAllActive();
    }

    public Airline getAirlineById(Long id) {
        return airlineMapper.findById(id);
    }

    public Airline getAirlineByCode(String code) {
        return airlineMapper.findByCode(code);
    }

    @Transactional
    public Airline createAirline(Airline airline) {
        if (airline.getCode() == null || !isValidAirlineCode(airline.getCode())) {
            throw new IllegalArgumentException("Invalid airline code format");
        }

        if (airlineMapper.findByCode(airline.getCode()) != null) {
            throw new IllegalArgumentException("Airline code already exists");
        }
        
        airlineMapper.insert(airline);
        return airline;
    }

    @Transactional
    public Airline updateAirline(Airline airline) {
        Airline existing = airlineMapper.findById(airline.getId());
        if (existing == null) {
            throw new IllegalArgumentException("Airline not found");
        }

        if (!isValidAirlineCode(airline.getCode())) {
            throw new IllegalArgumentException("Invalid airline code format");
        }

        if (!existing.getCode().equals(airline.getCode())) {
            if (airlineMapper.findByCode(airline.getCode()) != null) {
                throw new IllegalArgumentException("Airline code already exists");
            }
        }
        
        airlineMapper.update(airline);
        return airlineMapper.findById(airline.getId());
    }

    @Transactional
    public void deleteAirline(Long id) {
        if (airlineMapper.findById(id) == null) {
            throw new IllegalArgumentException("Airline not found");
        }
        airlineMapper.delete(id);
    }

    @Transactional
    public void updateActiveStatus(Long id, Boolean active) {
        if (airlineMapper.findById(id) == null) {
            throw new IllegalArgumentException("Airline not found");
        }
        airlineMapper.updateActiveStatus(id, active);
    }

    public boolean validateFlightNumber(String flightNumber, String airlineCode) {
        if (flightNumber == null || airlineCode == null) {
            return false;
        }

        String pattern = "^" + airlineCode.toUpperCase() + "\\d+$";
        return Pattern.matches(pattern, flightNumber.toUpperCase());
    }

    public String extractAirlineCodeFromFlightNumber(String flightNumber) {
        if (flightNumber == null || flightNumber.length() < 3) {
            return null;
        }

        String twoCharCode = flightNumber.substring(0, 2).toUpperCase();
        if (AIRLINE_CODE_PATTERNS.containsKey(twoCharCode)) {
            return twoCharCode;
        }
        
        return null;
    }
    
    private boolean isValidAirlineCode(String code) {
        if (code == null) return false;
        return Pattern.matches("^[A-Z0-9]{2}$", code.toUpperCase());
    }

    @Transactional
    public void initializeDefaultAirlines() {
        List<Airline> existing = airlineMapper.findAll();
        if (!existing.isEmpty()) {
            return;
        }

        Map<String, String[]> airlines = new HashMap<>();
        airlines.put("CA", new String[]{"中国国际航空", "中国国际航空股份有限公司"});
        airlines.put("MU", new String[]{"中国东方航空", "中国东方航空股份有限公司"});
        airlines.put("CZ", new String[]{"中国南方航空", "中国南方航空股份有限公司"});
        airlines.put("HU", new String[]{"海南航空", "海南航空股份有限公司"});
        airlines.put("3U", new String[]{"四川航空", "四川航空股份有限公司"});
        airlines.put("MF", new String[]{"厦门航空", "厦门航空有限公司"});
        airlines.put("FM", new String[]{"上海航空", "上海航空股份有限公司"});
        airlines.put("9C", new String[]{"春秋航空", "春秋航空股份有限公司"});
        
        for (Map.Entry<String, String[]> entry : airlines.entrySet()) {
            Airline airline = new Airline(entry.getKey(), entry.getValue()[0], entry.getValue()[1]);
            airlineMapper.insert(airline);
        }
    }
}