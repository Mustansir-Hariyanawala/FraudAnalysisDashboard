package com.example.demo.dto;

import com.example.demo.model.Role;
import jakarta.validation.constraints.*;

public record StaffRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 10, message = "Password must be at least 10 characters") String password,
        @NotNull Role role) {}