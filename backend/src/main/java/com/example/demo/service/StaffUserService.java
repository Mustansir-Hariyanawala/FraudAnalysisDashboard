package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.model.StaffUser;
import com.example.demo.repository.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffUserService {

    private final StaffUserRepository repo;
    private final PasswordEncoder encoder;

    public StaffResponse create(StaffRequest r) {
        if (repo.existsByUsernameIgnoreCase(r.username()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        if (repo.existsByEmailIgnoreCase(r.email()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");

        StaffUser s = new StaffUser();
        s.setUsername(r.username().trim());
        s.setFullName(r.fullName());
        s.setEmail(r.email());
        s.setPasswordHash(encoder.encode(r.password()));
        s.setRole(r.role());
        return StaffResponse.from(repo.save(s));
    }

    public List<StaffResponse> findAll() {
        return repo.findAll().stream().map(StaffResponse::from).toList();
    }

    public StaffResponse findByUsername(String username) {
        return StaffResponse.from(repo.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found")));
    }

    @Transactional
    public void recordLogin(String username) {
        repo.findByUsernameIgnoreCase(username).ifPresent(s -> s.setLastLoginAt(LocalDateTime.now()));
    }

    @Transactional
    public StaffResponse setEnabled(Long id, boolean enabled, String currentUsername) {
        StaffUser s = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found"));
        if (!enabled && s.getUsername().equalsIgnoreCase(currentUsername))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot disable your own account");
        s.setEnabled(enabled);
        return StaffResponse.from(s);
    }
}