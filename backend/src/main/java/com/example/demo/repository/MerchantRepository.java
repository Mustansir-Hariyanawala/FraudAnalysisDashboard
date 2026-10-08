package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.*;
public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    Optional<Merchant> findByNameIgnoreCase(String name);
}
