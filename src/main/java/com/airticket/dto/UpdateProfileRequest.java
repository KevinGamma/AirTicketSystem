package com.airticket.dto;

import com.airticket.validation.ValidEmail;
import com.airticket.validation.ValidPhone;
import jakarta.validation.constraints.NotBlank;

public class UpdateProfileRequest {
    
    @NotBlank(message = "Email is required")
    @ValidEmail
    private String email;
    
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    @ValidPhone
    private String phone;

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
}