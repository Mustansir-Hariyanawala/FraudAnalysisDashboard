package com.example.demo.dto;

import com.example.demo.model.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TransactionFilter(
        List<String> customerId,
        List<String> merchant,
        List<String> beneficiary,
        List<String> country,
        List<RiskLevel> riskLevel,
        List<TransactionStatus> status,
        BigDecimal minAmount,
        BigDecimal maxAmount,
        Integer minScore,
        Integer maxScore,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {}