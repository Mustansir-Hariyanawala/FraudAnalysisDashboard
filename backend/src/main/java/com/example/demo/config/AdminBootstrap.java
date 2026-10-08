package com.example.demo.config;

import com.example.demo.model.*;
import com.example.demo.repository.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final StaffUserRepository repo;
    private final PasswordEncoder encoder;

    @Value("${app.admin.username}") private String username;
    @Value("${app.admin.email}") private String email;
    @Value("${app.admin.password}") private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (repo.count() > 0) return;
        StaffUser admin = new StaffUser();
        admin.setUsername(username);
        admin.setFullName("System Admin");
        admin.setEmail(email);
        admin.setPasswordHash(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        repo.save(admin);
    }
}