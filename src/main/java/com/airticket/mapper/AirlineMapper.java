package com.airticket.mapper;

import com.airticket.model.Airline;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AirlineMapper {

    @Select("SELECT * FROM airlines WHERE active = true ORDER BY code")
    List<Airline> findAllActive();

    @Select("SELECT * FROM airlines ORDER BY code")
    List<Airline> findAll();

    @Select("SELECT * FROM airlines WHERE id = #{id}")
    Airline findById(Long id);

    @Select("SELECT * FROM airlines WHERE code = #{code}")
    Airline findByCode(String code);

    @Insert("INSERT INTO airlines (code, name, full_name, logo_url, description, active, created_at, updated_at) " +
            "VALUES (#{code}, #{name}, #{fullName}, #{logoUrl}, #{description}, #{active}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Airline airline);

    @Update("UPDATE airlines SET code = #{code}, name = #{name}, full_name = #{fullName}, " +
            "logo_url = #{logoUrl}, description = #{description}, active = #{active}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(Airline airline);

    @Delete("DELETE FROM airlines WHERE id = #{id}")
    int delete(Long id);

    @Update("UPDATE airlines SET active = #{active}, updated_at = NOW() WHERE id = #{id}")
    int updateActiveStatus(@Param("id") Long id, @Param("active") Boolean active);
    
    @Delete("DELETE FROM airlines")
    int deleteAll();
}