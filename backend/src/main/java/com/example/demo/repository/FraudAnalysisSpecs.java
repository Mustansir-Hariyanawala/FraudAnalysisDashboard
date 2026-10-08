package com.example.demo.repository;

import com.example.demo.dto.TransactionFilter;
import com.example.demo.model.*;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public final class FraudAnalysisSpecs {

    public static final Set<String> SORT_KEYS =
            Set.of("date", "amount", "risk", "status", "customer", "merchant");

    private FraudAnalysisSpecs() {}

    public static Specification<FraudAnalysis> build(TransactionFilter f, String sortBy, boolean asc) {
        List<String> customers = clean(f.customerId());
        List<String> merchants = clean(f.merchant());
        List<String> beneficiaries = clean(f.beneficiary());
        List<String> countries = clean(f.country());

        return (root, query, cb) -> {
            Join<FraudAnalysis, Transaction> tx = root.join("transaction");
            Join<Transaction, User> user = tx.join("user");
            Join<Transaction, Merchant> merchant = tx.join("merchant");
            Join<Transaction, Beneficiary> bene = tx.join("beneficiary", JoinType.LEFT);

            List<Predicate> p = new ArrayList<>();

            if (!customers.isEmpty())
                p.add(cb.upper(user.<String>get("customerId")).in(customers.stream().map(String::toUpperCase).toList()));
            if (!merchants.isEmpty())
                p.add(anyLike(cb, merchant.get("name"), merchants));
            if (!beneficiaries.isEmpty())
                p.add(cb.or(anyLike(cb, bene.get("name"), beneficiaries),
                            anyLike(cb, bene.get("accountNumber"), beneficiaries)));
            if (!countries.isEmpty())
                p.add(cb.lower(tx.<String>get("country")).in(countries.stream().map(String::toLowerCase).toList()));

            if (f.riskLevel() != null && !f.riskLevel().isEmpty())
                p.add(root.get("riskLevel").in(f.riskLevel()));
            if (f.status() != null && !f.status().isEmpty())
                p.add(tx.get("status").in(f.status()));

            if (f.minAmount() != null) p.add(cb.greaterThanOrEqualTo(tx.<BigDecimal>get("amount"), f.minAmount()));
            if (f.maxAmount() != null) p.add(cb.lessThanOrEqualTo(tx.<BigDecimal>get("amount"), f.maxAmount()));
            if (f.minScore() != null)  p.add(cb.greaterThanOrEqualTo(root.<Integer>get("riskScore"), f.minScore()));
            if (f.maxScore() != null)  p.add(cb.lessThanOrEqualTo(root.<Integer>get("riskScore"), f.maxScore()));
            if (f.from() != null) p.add(cb.greaterThanOrEqualTo(tx.<LocalDateTime>get("createdAt"), f.from().atStartOfDay()));
            if (f.to() != null)   p.add(cb.lessThan(tx.<LocalDateTime>get("createdAt"), f.to().plusDays(1).atStartOfDay()));

            // Spring Data reuses this spec for the count query, which must not be ordered
            if (!Long.class.equals(query.getResultType())) {
                Expression<?> key = switch (sortBy) {
                    case "amount"   -> tx.get("amount");
                    case "risk"     -> root.get("riskScore");          // score order == level order
                    case "status"   -> cb.<Integer>selectCase()        // severity, not alphabetical
                            .when(cb.equal(tx.get("status"), TransactionStatus.BLOCKED), 2)
                            .when(cb.equal(tx.get("status"), TransactionStatus.REVIEW), 1)
                            .otherwise(0);
                    case "customer" -> user.get("customerId");
                    case "merchant" -> merchant.get("name");
                    default         -> tx.get("createdAt");
                };
                query.orderBy(asc ? cb.asc(key) : cb.desc(key),
                              cb.desc(tx.get("createdAt")),            // stable tie-breakers
                              cb.desc(root.get("id")));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private static Predicate anyLike(CriteriaBuilder cb, Expression<String> path, List<String> values) {
        return cb.or(values.stream()
                .map(v -> cb.like(cb.lower(path), "%" + escape(v.toLowerCase()) + "%", '\\'))
                .toArray(Predicate[]::new));
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static List<String> clean(List<String> in) {
        if (in == null) return List.of();
        return in.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}