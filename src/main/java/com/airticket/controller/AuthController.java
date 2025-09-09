package com.airticket.controller;

import com.airticket.dto.ApiResponse;
import com.airticket.dto.LoginRequest;
import com.airticket.dto.RegisterRequest;
import com.airticket.dto.UpdateProfileRequest;
import com.airticket.model.User;
import com.airticket.service.FileStorageService;
import com.airticket.service.MessageService;
import com.airticket.service.UserService;
import com.airticket.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private MessageService messageService;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            User user = userService.findByUsername(loginRequest.getUsername());
            String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", user);

            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (BadCredentialsException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(messageService.getMessage("auth.login.invalidCredentials")));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            if (userService.findByUsername(request.getUsername()) != null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(messageService.getMessage("auth.register.usernameExists")));
            }

            User user = userService.createUser(
                request.getUsername(), 
                request.getPassword(), 
                request.getEmail(), 
                request.getFullName(), 
                request.getPhone(),
                "CUSTOMER"
            );
            return ResponseEntity.ok(ApiResponse.success(messageService.getMessage("auth.register.success"), user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(messageService.getMessage("auth.register.failed", e.getMessage())));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser() {
        try {
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to get user info"));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        try {
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            User updatedUser = userService.updateUserProfile(username, request);
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update profile: " + e.getMessage()));
        }
    }

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<User>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Please select a file to upload"));
            }

            if (!fileStorageService.isValidImageFile(file)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid file type. Only JPEG, PNG, and GIF images are allowed"));
            }

            if (!fileStorageService.isFileSizeValid(file, 5)) { // 5MB limit
                return ResponseEntity.badRequest().body(ApiResponse.error("File size too large. Maximum size is 5MB"));
            }

            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username);

            if (currentUser.getAvatarUrl() != null) {
                fileStorageService.deleteFile(currentUser.getAvatarUrl());
            }

            String avatarUrl = fileStorageService.storeFile(file, "avatars");

            User updatedUser = userService.updateUserAvatar(username, avatarUrl);
            
            return ResponseEntity.ok(ApiResponse.success("Avatar uploaded successfully", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to upload avatar: " + e.getMessage()));
        }
    }

    @DeleteMapping("/avatar")
    public ResponseEntity<ApiResponse<User>> deleteAvatar() {
        try {
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username);

            if (currentUser.getAvatarUrl() != null) {
                fileStorageService.deleteFile(currentUser.getAvatarUrl());
                User updatedUser = userService.updateUserAvatar(username, null);
                return ResponseEntity.ok(ApiResponse.success("Avatar deleted successfully", updatedUser));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("No avatar to delete"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete avatar: " + e.getMessage()));
        }
    }
}