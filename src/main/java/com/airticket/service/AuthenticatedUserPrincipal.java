package com.airticket.service;

import com.airticket.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AuthenticatedUserPrincipal implements UserDetails {

    private final User user;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUserPrincipal(User user) {
        this.user = user;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
    }

    public User getUser() {
        return user;
    }

    public User toSafeUser() {
        User safeUser = new User();
        safeUser.setId(user.getId());
        safeUser.setUsername(user.getUsername());
        safeUser.setEmail(user.getEmail());
        safeUser.setFullName(user.getFullName());
        safeUser.setPhone(user.getPhone());
        safeUser.setRole(user.getRole());
        safeUser.setAvatarUrl(user.getAvatarUrl());
        safeUser.setBalance(user.getBalance());
        safeUser.setCreatedAt(user.getCreatedAt());
        safeUser.setUpdatedAt(user.getUpdatedAt());
        return safeUser;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
