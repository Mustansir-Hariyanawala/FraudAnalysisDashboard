package com.example.demo.dto;

import jakarta.validation.constraints.*;

public record UserRequest(
        @NotBlank String customerId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String emailId,
        String location,
        String country,
        Double latitude,
        Double longitude,
        Integer failedAuthAttempts) {}