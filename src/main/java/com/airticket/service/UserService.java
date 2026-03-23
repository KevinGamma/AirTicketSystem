package com.airticket.service;

import com.airticket.dto.UpdateUserRequest;
import com.airticket.dto.UpdateProfileRequest;
import com.airticket.mapper.UserMapper;
import com.airticket.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements UserDetailsService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        return new AuthenticatedUserPrincipal(user);
    }
    
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }
    
    public User findById(Long id) {
        return userMapper.findById(id);
    }
    
    public User createUser(String username, String password, String email, String fullName, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role != null ? role : "CUSTOMER");
        
        userMapper.insert(user);
        return user;
    }
    
    public User createUser(String username, String password, String email, String fullName, String phone, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setRole(role != null ? role : "CUSTOMER");
        
        userMapper.insert(user);
        return user;
    }
    
    public User updateUser(User user) {
        userMapper.update(user);
        return userMapper.findById(user.getId());
    }
    
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
    
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    public List<User> getAllUsers() {
        return userMapper.findAll();
    }
    
    public User updateUserByAdmin(Long id, UpdateUserRequest request) {
        User existingUser = userMapper.findById(id);
        if (existingUser == null) {
            throw new RuntimeException("User not found");
        }
        
        if (request.getEmail() != null) {
            existingUser.setEmail(request.getEmail());
        }
        if (request.getFullName() != null) {
            existingUser.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            existingUser.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            existingUser.setRole(request.getRole());
        }
        
        userMapper.update(existingUser);
        return userMapper.findById(id);
    }
    
    public User updateUserRole(Long id, String role) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        
        user.setRole(role);
        userMapper.update(user);
        return userMapper.findById(id);
    }
    
    public User updateUserProfile(String username, UpdateProfileRequest request) {
        User existingUser = userMapper.findByUsername(username);
        if (existingUser == null) {
            throw new RuntimeException("User not found");
        }
        
        if (request.getEmail() != null) {
            existingUser.setEmail(request.getEmail());
        }
        if (request.getFullName() != null) {
            existingUser.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            existingUser.setPhone(request.getPhone());
        }

        boolean savedPassengerProvided = request.getSavedPassengerName() != null
            || request.getSavedPassengerIdNumber() != null;
        if (savedPassengerProvided) {
            String savedPassengerName = normalizeOptionalValue(request.getSavedPassengerName());
            String savedPassengerIdNumber = normalizeOptionalValue(request.getSavedPassengerIdNumber());

            if ((savedPassengerName == null) != (savedPassengerIdNumber == null)) {
                throw new RuntimeException("Saved passenger name and ID number must be provided together");
            }

            existingUser.setSavedPassengerName(savedPassengerName);
            existingUser.setSavedPassengerIdNumber(savedPassengerIdNumber);
        }
        
        userMapper.updateProfile(existingUser);
        return userMapper.findByUsername(username);
    }
    
    public User updateUserAvatar(String username, String avatarUrl) {
        User existingUser = userMapper.findByUsername(username);
        if (existingUser == null) {
            throw new RuntimeException("User not found");
        }
        
        userMapper.updateAvatar(existingUser.getId(), avatarUrl);
        return userMapper.findByUsername(username);
    }

    private String normalizeOptionalValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
