package com.airticket.model;

public class AircraftModel {
    private String code;
    private String englishName;
    private String chineseName;
    private Integer totalSeats;
    private String description;

    public AircraftModel() {}

    public AircraftModel(String code, String englishName, String chineseName, Integer totalSeats, String description) {
        this.code = code;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.totalSeats = totalSeats;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getChineseName() {
        return chineseName;
    }

    public void setChineseName(String chineseName) {
        this.chineseName = chineseName;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}