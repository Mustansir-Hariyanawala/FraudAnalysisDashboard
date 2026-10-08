package com.example.demo.dto;

import com.example.demo.model.*;
import java.time.LocalDateTime;

public record StaffResponse(Long id, String username, String fullName, String email,
                            Role role, boolean enabled, LocalDateTime lastLoginAt) {
    public static StaffResponse from(StaffUser s) {
        return new StaffResponse(s.getId(), s.getUsername(), s.getFullName(), s.getEmail(),
                s.getRole(), s.isEnabled(), s.getLastLoginAt());
    }
}