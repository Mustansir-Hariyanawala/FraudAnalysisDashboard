package com.example.demo.dto;

import com.example.demo.model.User;
import java.time.LocalDateTime;

public record UserResponse(String customerId, String firstName, String lastName, String emailId,
                           String location, String country, Double latitude, Double longitude,
                           boolean watchlisted, int failedAuthAttempts, LocalDateTime createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getCustomerId(), u.getFirstName(), u.getLastName(), u.getEmailId(),
                u.getLocation(), u.getCountry(), u.getLatitude(), u.getLongitude(),
                u.isWatchlisted(), u.getFailedAuthAttempts(), u.getCreatedAt());
    }
}