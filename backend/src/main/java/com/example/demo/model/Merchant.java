package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "merchants")
@Getter @Setter @NoArgsConstructor
public class Merchant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) private String name;
    @Column(nullable = false) private String category;
}