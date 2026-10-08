package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.example.demo.model.RiskRule.*;

@Service
@RequiredArgsConstructor
public class RiskScoringService {

    // Placeholders: next step is moving these into DB tables
    private static final Set<String> HIGH_RISK_CATEGORIES =
            Set.of("crypto", "gambling", "wire transfer", "money exchange");
    private static final Set<String> HIGH_RISK_COUNTRIES =
            Set.of("north korea", "iran", "myanmar", "syria");

    private static final BigDecimal NEW_CUSTOMER_LARGE_AMOUNT = new BigDecimal("100000");
    private static final BigDecimal ROUND_MIN_AMOUNT = new BigDecimal("50000");
    private static final BigDecimal ROUND_MULTIPLE = new BigDecimal("10000");
    private static final double MAX_TRAVEL_SPEED_KMH = 900;
    private static final double MIN_TRAVEL_DISTANCE_KM = 100;
    private static final double FAR_FROM_HOME_KM = 500;

    private final TransactionRepository txRepo;
    private final FraudAnalysisRepository analysisRepo;

    public record Hit(RiskRule rule, int points) {}
    public record RiskAssessment(int score, RiskLevel level, TransactionStatus status, List<Hit> hits) {}

    /** Call BEFORE saving the transaction, with user/merchant/etc. and createdAt already set. */
    public RiskAssessment assess(Transaction t) {
        User u = t.getUser();
        boolean hasHistory = txRepo.existsByUserId(u.getId());
        Double normal = txRepo.findAverageAmount(u.getId(), TransactionStatus.APPROVED);

        List<Hit> hits = new ArrayList<>();
        add(hits, AMOUNT_ANOMALY,      amountAnomaly(t, normal));
        add(hits, VELOCITY,            velocity(t));
        add(hits, NEW_BENEFICIARY,     newBeneficiary(t, hasHistory));
        add(hits, NEW_COUNTRY,         geographicAnomaly(t));
        add(hits, FAR_FROM_HOME,       farFromHome(t));
        add(hits, IMPOSSIBLE_TRAVEL,   impossibleTravel(t));
        add(hits, NEW_DEVICE,          deviceAnomaly(t, hasHistory));
        add(hits, FAILED_AUTH,         failedAuthentication(t));
        add(hits, PRIOR_FLAGS,         historicalRisk(t));
        add(hits, HIGH_RISK_MERCHANT,  highRiskCategory(t));
        add(hits, HIGH_RISK_GEOGRAPHY, highRiskGeography(t));
        add(hits, ROUND_NUMBER,        roundNumber(t));
        add(hits, WATCHLIST,           watchlist(t));
        add(hits, WEEKEND_SPIKE,       weekendActivity(t, normal));

        int score = Math.min(hits.stream().mapToInt(Hit::points).sum(), 100);
        RiskLevel level = level(score);
        return new RiskAssessment(score, level, status(level), hits);
    }

    // ===================== individual signals =====================

    int amountAnomaly(Transaction t, Double normal) {
        if (normal == null || normal <= 0)
            return t.getAmount().compareTo(NEW_CUSTOMER_LARGE_AMOUNT) >= 0 ? 20 : 0;
        double ratio = t.getAmount().doubleValue() / normal;
        if (ratio > 10) return 30;
        if (ratio > 5) return 20;
        return 0;
    }

    int velocity(Transaction t) {
        long count = txRepo.countByUserIdAndCreatedAtAfter(
                t.getUser().getId(), t.getCreatedAt().minusMinutes(10)) + 1;
        if (count > 10) return 25;
        if (count > 5) return 15;
        return 0;
    }

    int newBeneficiary(Transaction t, boolean hasHistory) {
        if (!hasHistory || t.getBeneficiary() == null) return 0;
        return txRepo.existsByUserIdAndBeneficiaryId(
                t.getUser().getId(), t.getBeneficiary().getId()) ? 0 : 10;
    }

    /** Country that is neither the customer's home country nor one they've transacted in. */
    int geographicAnomaly(Transaction t) {
        String country = t.getCountry();
        if (country == null) return 0;
        if (country.equalsIgnoreCase(t.getUser().getCountry())) return 0;
        return txRepo.existsByUserIdAndCountryIgnoreCase(t.getUser().getId(), country) ? 0 : 15;
    }

    /** Transaction location far from the customer's registered home location. */
    int farFromHome(Transaction t) {
        User u = t.getUser();
        if (u.getLatitude() == null || u.getLongitude() == null
                || t.getLatitude() == null || t.getLongitude() == null) return 0;
        double km = distanceKm(u.getLatitude(), u.getLongitude(), t.getLatitude(), t.getLongitude());
        return km > FAR_FROM_HOME_KM ? 10 : 0;
    }

    int impossibleTravel(Transaction t) {
        if (t.getLatitude() == null || t.getLongitude() == null) return 0;
        return txRepo.findFirstByUserIdAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByCreatedAtDesc(
                        t.getUser().getId())
                .map(prev -> {
                    double km = distanceKm(prev.getLatitude(), prev.getLongitude(),
                                           t.getLatitude(), t.getLongitude());
                    if (km < MIN_TRAVEL_DISTANCE_KM) return 0;
                    double hours = Math.max(
                            Duration.between(prev.getCreatedAt(), t.getCreatedAt()).toSeconds() / 3600.0,
                            1.0 / 60);
                    return km / hours > MAX_TRAVEL_SPEED_KMH ? 25 : 0;
                })
                .orElse(0);
    }

    int deviceAnomaly(Transaction t, boolean hasHistory) {
        if (!hasHistory || t.getDevice() == null) return 0;
        return txRepo.existsByUserIdAndDeviceId(t.getUser().getId(), t.getDevice().getId()) ? 0 : 15;
    }

    int failedAuthentication(Transaction t) {
        return t.getUser().getFailedAuthAttempts() >= 3 ? 15 : 0;
    }

    int historicalRisk(Transaction t) {
        long flagged = analysisRepo.countFlaggedByUser(
                t.getUser().getId(), List.of(RiskLevel.HIGH, RiskLevel.CRITICAL));
        if (flagged >= 3) return 20;
        if (flagged >= 1) return 10;
        return 0;
    }

    int highRiskCategory(Transaction t) {
        return inSet(HIGH_RISK_CATEGORIES, t.getMerchant().getCategory()) ? 15 : 0;
    }

    int highRiskGeography(Transaction t) {
        return inSet(HIGH_RISK_COUNTRIES, t.getCountry()) ? 15 : 0;
    }

    int roundNumber(Transaction t) {
        boolean large = t.getAmount().compareTo(ROUND_MIN_AMOUNT) >= 0;
        boolean round = t.getAmount().remainder(ROUND_MULTIPLE).signum() == 0;
        return large && round ? 5 : 0;
    }

    int watchlist(Transaction t) {
        return t.getUser().isWatchlisted() ? 25 : 0;
    }

    int weekendActivity(Transaction t, Double normal) {
        DayOfWeek d = t.getCreatedAt().getDayOfWeek();
        boolean weekend = d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY;
        boolean spike = normal != null && t.getAmount().doubleValue() > 2 * normal;
        return weekend && spike ? 5 : 0;
    }

    // ===================== ranking =====================

    /** LOW 0-24, MEDIUM 25-49, HIGH 50-74, CRITICAL 75-100 */
    public RiskLevel level(int score) {
        if (score >= 75) return RiskLevel.CRITICAL;
        if (score >= 50) return RiskLevel.HIGH;
        if (score >= 25) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    public TransactionStatus status(RiskLevel level) {
        return switch (level) {
            case CRITICAL -> TransactionStatus.BLOCKED;
            case HIGH -> TransactionStatus.REVIEW;
            default -> TransactionStatus.APPROVED;
        };
    }

    // ===================== helpers =====================

    private void add(List<Hit> hits, RiskRule rule, int points) {
        if (points > 0) hits.add(new Hit(rule, points));
    }

    private boolean inSet(Set<String> set, String value) {
        return value != null && set.contains(value.toLowerCase());
    }

    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}