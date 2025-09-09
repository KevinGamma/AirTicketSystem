package com.airticket.dto;

import com.airticket.validation.ValidEmail;
import com.airticket.validation.ValidPhone;

public class UpdateUserRequest {
    
    @ValidEmail
    private String email;
    
    private String fullName;
    
    @ValidPhone
    private String phone;
    
    private String role;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}