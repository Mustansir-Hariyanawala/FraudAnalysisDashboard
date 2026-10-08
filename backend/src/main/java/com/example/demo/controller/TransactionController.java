package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @GetMapping
    public PageResponse<TransactionResponse> search(
            TransactionFilter filter,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(filter, sortBy, sortDir, page, size);
    }
    @GetMapping("/risky")
    public List<TransactionResponse> getRisky() { return service.findRisky(); }

    @GetMapping("/{id}")
    public TransactionResponse getById(@PathVariable Long id) { return service.findById(id); }
}