package com.example.demo.repository;

import com.example.demo.model.StaffUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StaffUserRepository extends JpaRepository<StaffUser, Long> {
    Optional<StaffUser> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
}