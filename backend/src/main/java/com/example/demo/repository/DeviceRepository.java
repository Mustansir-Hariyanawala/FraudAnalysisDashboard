package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.*;
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Optional<Device> findByDeviceKey(String deviceKey);
}
