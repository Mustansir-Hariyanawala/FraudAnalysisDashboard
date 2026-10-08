package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "beneficiaries")
@Getter @Setter @NoArgsConstructor
public class Beneficiary {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) private String accountNumber;
    private String name;
}