package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository txRepo;
    private final FraudAnalysisRepository analysisRepo;
    private final UserRepository userRepo;
    private final MerchantRepository merchantRepo;
    private final BeneficiaryRepository beneficiaryRepo;
    private final DeviceRepository deviceRepo;
    private final RiskScoringService risk;

    @Transactional
    public TransactionResponse create(TransactionRequest r) {
        User user = userRepo.findByCustomerId(r.customerId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Unknown customer: " + r.customerId()));

        Transaction t = new Transaction();
        t.setUser(user);
        t.setMerchant(resolveMerchant(r));
        t.setBeneficiary(resolveBeneficiary(r));
        t.setDevice(resolveDevice(r));
        t.setAmount(r.amount());
        t.setLocation(r.location());
        t.setCountry(r.country());
        t.setLatitude(r.latitude());
        t.setLongitude(r.longitude());
        t.setCreatedAt(LocalDateTime.now());              // before assess()

        RiskScoringService.RiskAssessment result = risk.assess(t);   // before saving
        t.setStatus(result.status());

        t = txRepo.save(t);
        t.setTransactionCode("TX%03d".formatted(t.getId()));

        FraudAnalysis a = new FraudAnalysis();
        a.setTransaction(t);
        a.setRiskScore(result.score());
        a.setRiskLevel(result.level());
        a.setAnalyzedAt(LocalDateTime.now());
        result.hits().forEach(h -> a.addReason(h.rule(), h.points()));
        analysisRepo.save(a);

        return TransactionResponse.from(a);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(TransactionFilter filter, String sortBy,
                                                    String sortDir, int page, int size) {
        if (!FraudAnalysisSpecs.SORT_KEYS.contains(sortBy))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sortBy must be one of " + FraudAnalysisSpecs.SORT_KEYS);

        boolean asc = "asc".equalsIgnoreCase(sortDir);
        // PageRequest has no Sort here on purpose: ordering is set inside the specification
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));

        return PageResponse.of(
                analysisRepo.findAll(FraudAnalysisSpecs.build(filter, sortBy, asc), pageable)
                            .map(TransactionResponse::from));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findRisky() {
        return analysisRepo.findByRiskLevelInOrderByRiskScoreDesc(
                        List.of(RiskLevel.HIGH, RiskLevel.CRITICAL))
                .stream().map(TransactionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id) {
        return analysisRepo.findByTransactionId(id).map(TransactionResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    public DashboardResponse dashboard() {
        return new DashboardResponse(
                txRepo.sumAllAmounts(),
                txRepo.count(),
                analysisRepo.countByRiskScoreGreaterThanEqual(25),                         // MEDIUM and above
                analysisRepo.countByRiskLevelIn(List.of(RiskLevel.HIGH, RiskLevel.CRITICAL)),
                txRepo.countByStatus(TransactionStatus.BLOCKED));
    }

    // ---------- find-or-create helpers ----------

    private Merchant resolveMerchant(TransactionRequest r) {
        return merchantRepo.findByNameIgnoreCase(r.merchantName()).orElseGet(() -> {
            Merchant m = new Merchant();
            m.setName(r.merchantName());
            m.setCategory(r.merchantCategory());
            return merchantRepo.save(m);
        });
    }

    private Beneficiary resolveBeneficiary(TransactionRequest r) {
        if (r.beneficiaryAccount() == null || r.beneficiaryAccount().isBlank()) return null;
        return beneficiaryRepo.findByAccountNumber(r.beneficiaryAccount()).orElseGet(() -> {
            Beneficiary b = new Beneficiary();
            b.setAccountNumber(r.beneficiaryAccount());
            b.setName(r.beneficiaryName());
            return beneficiaryRepo.save(b);
        });
    }

    private Device resolveDevice(TransactionRequest r) {
        if (r.deviceKey() == null || r.deviceKey().isBlank()) return null;
        return deviceRepo.findByDeviceKey(r.deviceKey()).orElseGet(() -> {
            Device d = new Device();
            d.setDeviceKey(r.deviceKey());
            return deviceRepo.save(d);
        });
    }
}