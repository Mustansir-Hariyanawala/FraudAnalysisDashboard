package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "risk_reason")
@Getter @Setter @NoArgsConstructor
public class RiskReason {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private FraudAnalysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_code", nullable = false)
    private RiskRule rule;

    private int points;
}