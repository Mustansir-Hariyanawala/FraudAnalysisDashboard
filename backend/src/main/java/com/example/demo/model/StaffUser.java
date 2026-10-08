package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_users")
@Getter @Setter @NoArgsConstructor
public class StaffUser {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) private String username;
    @Column(nullable = false) private String fullName;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String passwordHash;   // BCrypt, never the raw password

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean enabled = true;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
}