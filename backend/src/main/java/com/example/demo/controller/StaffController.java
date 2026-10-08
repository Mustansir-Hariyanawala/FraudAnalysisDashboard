package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.StaffUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffUserService service;

    @PostMapping
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody StaffRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public List<StaffResponse> all() { return service.findAll(); }

    @PatchMapping("/{id}/enabled")
    public StaffResponse setEnabled(@PathVariable Long id, @RequestParam boolean value, Authentication auth) {
        return service.setEnabled(id, value, auth.getName());
    }
}