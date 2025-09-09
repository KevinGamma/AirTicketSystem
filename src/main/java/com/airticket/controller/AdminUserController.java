package com.airticket.controller;

import com.airticket.dto.ApiResponse;
import com.airticket.dto.CreateUserRequest;
import com.airticket.dto.UpdateUserRequest;
import com.airticket.model.User;
import com.airticket.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            User user = userService.createUser(
                request.getUsername(), 
                request.getPassword(), 
                request.getEmail(), 
                request.getFullName(),
                request.getPhone(),
                request.getRole()
            );
            return ResponseEntity.ok(ApiResponse.success("User created successfully", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create user: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        try {
            User existingUser = userService.findById(id);
            if (existingUser == null) {
                return ResponseEntity.notFound().build();
            }

            User updatedUser = userService.updateUserByAdmin(id, request);
            return ResponseEntity.ok(ApiResponse.success("User updated successfully", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update user: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            if ("ADMIN".equals(user.getRole())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Cannot delete admin users"));
            }

            userService.deleteUser(id);
            return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete user: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<User>> updateUserRole(@PathVariable Long id, @RequestBody RoleUpdateRequest request) {
        try {
            User user = userService.updateUserRole(id, request.getRole());
            return ResponseEntity.ok(ApiResponse.success("User role updated successfully", user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update user role: " + e.getMessage()));
        }
    }

    public static class RoleUpdateRequest {
        private String role;

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}