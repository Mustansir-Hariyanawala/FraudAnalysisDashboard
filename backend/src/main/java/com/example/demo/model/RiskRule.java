package com.example.demo.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RiskRule {
    AMOUNT_ANOMALY("Amount far above customer's normal"),
    VELOCITY("High transaction velocity"),
    NEW_BENEFICIARY("First transfer to this beneficiary"),
    NEW_COUNTRY("Transaction from a new country"),
    FAR_FROM_HOME("Far from customer's home location"),
    IMPOSSIBLE_TRAVEL("Impossible travel since last transaction"),
    NEW_DEVICE("New or unrecognized device"),
    FAILED_AUTH("Repeated failed authentication"),
    PRIOR_FLAGS("Customer previously flagged"),
    HIGH_RISK_MERCHANT("High-risk merchant category"),
    HIGH_RISK_GEOGRAPHY("High-risk country"),
    ROUND_NUMBER("Large round-number transfer"),
    WATCHLIST("Customer on watchlist"),
    WEEKEND_SPIKE("Weekend spike in amount");

    private final String description;
}