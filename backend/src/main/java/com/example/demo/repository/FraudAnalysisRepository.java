package com.example.demo.repository;

import com.example.demo.model.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface FraudAnalysisRepository extends JpaRepository<FraudAnalysis, Long>, JpaSpecificationExecutor<FraudAnalysis> {

    @Override
    @EntityGraph(attributePaths = {"transaction.user", "transaction.merchant",
                                   "transaction.beneficiary", "transaction.device"})
    Page<FraudAnalysis> findAll(Specification<FraudAnalysis> spec, Pageable pageable);
    @EntityGraph(attributePaths = {"reasons", "transaction.user", "transaction.merchant",
                                   "transaction.beneficiary", "transaction.device"})
    List<FraudAnalysis> findAllByOrderByTransactionCreatedAtDesc();

    @EntityGraph(attributePaths = {"reasons", "transaction.user", "transaction.merchant",
                                   "transaction.beneficiary", "transaction.device"})
    List<FraudAnalysis> findByRiskLevelInOrderByRiskScoreDesc(Collection<RiskLevel> levels);

    @EntityGraph(attributePaths = {"reasons", "transaction.user", "transaction.merchant",
                                   "transaction.beneficiary", "transaction.device"})
    Optional<FraudAnalysis> findByTransactionId(Long transactionId);

    // dashboard
    long countByRiskLevelIn(Collection<RiskLevel> levels);
    long countByRiskScoreGreaterThanEqual(int score);

    // risk engine: how many past transactions of this customer were flagged
    @Query("select count(a) from FraudAnalysis a " +
           "where a.transaction.user.id = :userId and a.riskLevel in :levels")
    long countFlaggedByUser(@Param("userId") Long userId, @Param("levels") Collection<RiskLevel> levels);
}