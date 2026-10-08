package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor
public class Transaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String transactionCode;                 // TX001, TX002...

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)              // optional
    private Beneficiary beneficiary;

    @ManyToOne(fetch = FetchType.LAZY)              // optional
    private Device device;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // where THIS transaction happened
    private String location;
    private String country;
    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;               // the decision

    @Column(nullable = false)
    private LocalDateTime createdAt;
}