package com.airticket.dto;

import com.airticket.validation.ValidEmail;
import com.airticket.validation.ValidPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {
    
    @NotBlank(message = "{validation.username.required}")
    @Size(min = 3, max = 50, message = "{validation.username.length}")
    private String username;
    
    @NotBlank(message = "{validation.password.required}")
    @Size(min = 6, message = "{validation.password.minLength}")
    private String password;
    
    @NotBlank(message = "{validation.email.required}")
    @ValidEmail
    private String email;
    
    @NotBlank(message = "{validation.fullName.required}")
    private String fullName;
    
    @ValidPhone
    private String phone;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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