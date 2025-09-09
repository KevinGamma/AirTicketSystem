package com.airticket.mapper;

import com.airticket.model.Airport;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AirportMapper {
    
    @Select("SELECT * FROM airports ORDER BY created_at DESC")
    List<Airport> findAll();
    
    @Select("SELECT * FROM airports WHERE id = #{id}")
    Airport findById(Long id);
    
    @Select("SELECT * FROM airports WHERE code = #{code}")
    Airport findByCode(String code);
    
    @Select("SELECT * FROM airports WHERE city = #{city}")
    List<Airport> findByCity(String city);
    
    @Insert("INSERT INTO airports (code, name, city, country, timezone, created_at) " +
            "VALUES (#{code}, #{name}, #{city}, #{country}, #{timeZone}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Airport airport);
    
    @Update("UPDATE airports SET code = #{code}, name = #{name}, city = #{city}, country = #{country}, " +
            "timezone = #{timeZone} WHERE id = #{id}")
    int update(Airport airport);
    
    @Select("SELECT * FROM airports WHERE country = #{country}")
    List<Airport> findByCountry(String country);
    
    @Select("SELECT DISTINCT country FROM airports ORDER BY country")
    List<String> findAllCountries();
    
    @Select("SELECT DISTINCT city FROM airports WHERE country = #{country} ORDER BY city")
    List<String> findCitiesByCountry(String country);
    
    @Delete("DELETE FROM airports")
    int deleteAll();
    
    @Delete("DELETE FROM airports WHERE id = #{id}")
    int deleteById(Long id);
}