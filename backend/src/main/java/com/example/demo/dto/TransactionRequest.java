package com.example.demo.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TransactionRequest(
        @NotBlank String customerId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String location,
        String country,
        Double latitude,
        Double longitude,
        @NotBlank String merchantName,
        @NotBlank String merchantCategory,
        String beneficiaryAccount,
        String beneficiaryName,
        String deviceKey) {}