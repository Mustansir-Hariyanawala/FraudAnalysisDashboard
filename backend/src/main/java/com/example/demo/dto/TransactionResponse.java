package com.example.demo.dto;

import com.example.demo.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TransactionResponse(
        Long id, String transactionId, String customerId, String customerName,
        BigDecimal amount, String location, String country,
        String merchant, String merchantCategory, String beneficiary, String deviceKey,
        int riskScore, RiskLevel riskLevel, TransactionStatus status, LocalDateTime createdAt,
        List<Reason> reasons) {

    public record Reason(String rule, String description, int points) {}

    public static TransactionResponse from(FraudAnalysis a) {
        Transaction t = a.getTransaction();
        User u = t.getUser();
        return new TransactionResponse(
                t.getId(), t.getTransactionCode(), u.getCustomerId(),
                u.getFirstName() + " " + u.getLastName(),
                t.getAmount(), t.getLocation(), t.getCountry(),
                t.getMerchant().getName(), t.getMerchant().getCategory(),
                t.getBeneficiary() == null ? null : t.getBeneficiary().getName(),
                t.getDevice() == null ? null : t.getDevice().getDeviceKey(),
                a.getRiskScore(), a.getRiskLevel(), t.getStatus(), t.getCreatedAt(),
                a.getReasons().stream()
                        .map(r -> new Reason(r.getRule().name(), r.getRule().getDescription(), r.getPoints()))
                        .toList());
    }
}