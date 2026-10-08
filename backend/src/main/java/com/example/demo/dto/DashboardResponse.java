package com.example.demo.dto;

import java.math.BigDecimal;

public record DashboardResponse(
        BigDecimal totalAmount,
        long totalTransactions,
        long suspiciousTransactions,
        long highRisk,
        long blocked) {}