package com.airticket.service;

import com.airticket.mapper.AirportMapper;
import com.airticket.model.Airport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AirportService {
    
    @Autowired
    private AirportMapper airportMapper;
    
    public List<Airport> findAll() {
        return airportMapper.findAll();
    }
    
    public Airport findById(Long id) {
        return airportMapper.findById(id);
    }
    
    public Airport findByCode(String code) {
        return airportMapper.findByCode(code);
    }
    
    public List<Airport> findByCity(String city) {
        return airportMapper.findByCity(city);
    }
    
    public List<Airport> findByCountry(String country) {
        return airportMapper.findByCountry(country);
    }
    
    public List<String> findAllCountries() {
        return airportMapper.findAllCountries();
    }
    
    public List<String> findCitiesByCountry(String country) {
        return airportMapper.findCitiesByCountry(country);
    }
    
    @Transactional
    public Airport createAirport(Airport airport) {
        // Validate required fields
        if (airport.getCode() == null || airport.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Airport code is required");
        }
        if (airport.getName() == null || airport.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Airport name is required");
        }
        if (airport.getCity() == null || airport.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }
        if (airport.getCountry() == null || airport.getCountry().trim().isEmpty()) {
            throw new IllegalArgumentException("Country is required");
        }

        airport.setCode(airport.getCode().toUpperCase());

        Airport existing = airportMapper.findByCode(airport.getCode());
        if (existing != null) {
            throw new IllegalArgumentException("Airport code " + airport.getCode() + " already exists");
        }

        airport.setCreatedAt(Instant.now());
        
        airportMapper.insert(airport);
        return airport;
    }
    
    @Transactional
    public Airport updateAirport(Long id, Airport airport) {
        Airport existingAirport = airportMapper.findById(id);
        if (existingAirport == null) {
            throw new IllegalArgumentException("Airport with id " + id + " not found");
        }

        if (airport.getCode() == null || airport.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Airport code is required");
        }
        if (airport.getName() == null || airport.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Airport name is required");
        }
        if (airport.getCity() == null || airport.getCity().trim().isEmpty()) {
            throw new IllegalArgumentException("City is required");
        }
        if (airport.getCountry() == null || airport.getCountry().trim().isEmpty()) {
            throw new IllegalArgumentException("Country is required");
        }

        airport.setCode(airport.getCode().toUpperCase());

        Airport existingByCode = airportMapper.findByCode(airport.getCode());
        if (existingByCode != null && !existingByCode.getId().equals(id)) {
            throw new IllegalArgumentException("Airport code " + airport.getCode() + " already exists");
        }

        airport.setId(id);
        airport.setCreatedAt(existingAirport.getCreatedAt());
        
        airportMapper.update(airport);
        return airport;
    }
    
    @Transactional
    public boolean deleteAirport(Long id) {
        Airport existingAirport = airportMapper.findById(id);
        if (existingAirport == null) {
            throw new IllegalArgumentException("Airport with id " + id + " not found");
        }

        return airportMapper.deleteById(id) > 0;
    }
    
    @Transactional
    public void initializeCommonAirports() {
        List<Airport> existing = airportMapper.findAll();
        if (!existing.isEmpty()) {
            return;
        }

        Airport[] domesticAirports = {
            new Airport("PEK", "北京首都国际机场", "北京", "中国", "Asia/Shanghai"),
            new Airport("PKX", "北京大兴国际机场", "北京", "中国", "Asia/Shanghai"),
            new Airport("PVG", "上海浦东国际机场", "上海", "中国", "Asia/Shanghai"),
            new Airport("SHA", "上海虹桥国际机场", "上海", "中国", "Asia/Shanghai"),
            new Airport("CAN", "广州白云国际机场", "广州", "中国", "Asia/Shanghai"),
            new Airport("SZX", "深圳宝安国际机场", "深圳", "中国", "Asia/Shanghai"),
            new Airport("CTU", "成都双流国际机场", "成都", "中国", "Asia/Shanghai"),
            new Airport("KMG", "昆明长水国际机场", "昆明", "中国", "Asia/Shanghai"),
            new Airport("XIY", "西安咸阳国际机场", "西安", "中国", "Asia/Shanghai"),
            new Airport("HGH", "杭州萧山国际机场", "杭州", "中国", "Asia/Shanghai"),
            new Airport("NKG", "南京禄口国际机场", "南京", "中国", "Asia/Shanghai"),
            new Airport("WUH", "武汉天河国际机场", "武汉", "中国", "Asia/Shanghai"),
            new Airport("CSX", "长沙黄花国际机场", "长沙", "中国", "Asia/Shanghai"),
            new Airport("CGO", "郑州新郑国际机场", "郑州", "中国", "Asia/Shanghai"),
            new Airport("TAO", "青岛胶东国际机场", "青岛", "中国", "Asia/Shanghai"),
            new Airport("DLC", "大连周水子国际机场", "大连", "中国", "Asia/Shanghai"),
            new Airport("SYX", "三亚凤凰国际机场", "三亚", "中国", "Asia/Shanghai"),
            new Airport("HAK", "海口美兰国际机场", "海口", "中国", "Asia/Shanghai"),
            new Airport("URC", "乌鲁木齐地窝堡国际机场", "乌鲁木齐", "中国", "Asia/Urumqi"),
            new Airport("LHW", "兰州中川国际机场", "兰州", "中国", "Asia/Shanghai"),
            new Airport("NNG", "南宁吴圩国际机场", "南宁", "中国", "Asia/Shanghai")
        };

        Airport[] internationalAirports = {
            new Airport("NRT", "成田国际机场", "东京", "日本", "Asia/Tokyo"),
            new Airport("HND", "羽田机场", "东京", "日本", "Asia/Tokyo"),
            new Airport("ICN", "仁川国际机场", "首尔", "韩国", "Asia/Seoul"),
            new Airport("SIN", "新加坡樟宜机场", "新加坡", "新加坡", "Asia/Singapore"),
            new Airport("BKK", "素万那普机场", "曼谷", "泰国", "Asia/Bangkok"),
            new Airport("KUL", "吉隆坡国际机场", "吉隆坡", "马来西亚", "Asia/Kuala_Lumpur"),
            new Airport("MNL", "尼诺伊·阿基诺国际机场", "马尼拉", "菲律宾", "Asia/Manila"),
            new Airport("CGK", "苏加诺-哈达国际机场", "雅加达", "印度尼西亚", "Asia/Jakarta"),
            new Airport("LAX", "洛杉矶国际机场", "洛杉矶", "美国", "America/Los_Angeles"),
            new Airport("SFO", "旧金山国际机场", "旧金山", "美国", "America/Los_Angeles"),
            new Airport("JFK", "约翰·肯尼迪国际机场", "纽约", "美国", "America/New_York"),
            new Airport("LHR", "伦敦希思罗机场", "伦敦", "英国", "Europe/London"),
            new Airport("CDG", "戴高乐机场", "巴黎", "法国", "Europe/Paris"),
            new Airport("FRA", "法兰克福机场", "法兰克福", "德国", "Europe/Berlin"),
            new Airport("AMS", "阿姆斯特丹史基浦机场", "阿姆斯特丹", "荷兰", "Europe/Amsterdam"),
            new Airport("SYD", "悉尼金斯福德·史密斯机场", "悉尼", "澳大利亚", "Australia/Sydney"),
            new Airport("MEL", "墨尔本机场", "墨尔本", "澳大利亚", "Australia/Melbourne"),
            new Airport("YVR", "温哥华国际机场", "温哥华", "加拿大", "America/Vancouver"),
            new Airport("YYZ", "多伦多皮尔逊国际机场", "多伦多", "加拿大", "America/Toronto"),
            new Airport("DXB", "迪拜国际机场", "迪拜", "阿联酋", "Asia/Dubai")
        };

        for (Airport airport : domesticAirports) {
            airport.setCreatedAt(Instant.now());
            airportMapper.insert(airport);
        }

        for (Airport airport : internationalAirports) {
            airport.setCreatedAt(Instant.now());
            airportMapper.insert(airport);
        }
    }
}