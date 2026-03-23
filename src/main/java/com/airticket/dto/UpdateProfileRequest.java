package com.airticket.dto;

import com.airticket.validation.ValidEmail;
import com.airticket.validation.ValidPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateProfileRequest {
    
    @NotBlank(message = "Email is required")
    @ValidEmail
    private String email;
    
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    @ValidPhone
    private String phone;

    private String savedPassengerName;

    @Pattern(regexp = "^$|^\\d{17}[\\dXx]$", message = "Invalid passenger ID number")
    private String savedPassengerIdNumber;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSavedPassengerName() {
        return savedPassengerName;
    }

    public void setSavedPassengerName(String savedPassengerName) {
        this.savedPassengerName = savedPassengerName;
    }

    public String getSavedPassengerIdNumber() {
        return savedPassengerIdNumber;
    }

    public void setSavedPassengerIdNumber(String savedPassengerIdNumber) {
        this.savedPassengerIdNumber = savedPassengerIdNumber;
    }
}
