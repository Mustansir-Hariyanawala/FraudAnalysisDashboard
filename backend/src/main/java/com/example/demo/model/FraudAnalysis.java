package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "fraud_analysis")
@Getter @Setter @NoArgsConstructor
public class FraudAnalysis {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(unique = true)
    private Transaction transaction;

    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;

    private LocalDateTime analyzedAt;

    // empty for clean transactions: no nulls, just no rows
    @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RiskReason> reasons = new ArrayList<>();

    public void addReason(RiskRule rule, int points) {
        RiskReason r = new RiskReason();
        r.setAnalysis(this);
        r.setRule(rule);
        r.setPoints(points);
        reasons.add(r);
    }
}