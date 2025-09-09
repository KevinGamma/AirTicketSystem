package com.airticket.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;

public class Airport {
    private Long id;
    private String code;
    private String name;
    private String city;
    private String country;
    private String timeZone;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    public Airport() {}

    public Airport(String code, String name, String city, String country, String timeZone) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.country = country;
        this.timeZone = timeZone;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}