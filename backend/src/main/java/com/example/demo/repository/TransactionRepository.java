package com.example.demo.repository;

import com.example.demo.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // dashboard
    long countByStatus(TransactionStatus status);

    @Query("select coalesce(sum(t.amount), 0) from Transaction t")
    BigDecimal sumAllAmounts();

    // risk engine
    boolean existsByUserId(Long userId);
    boolean existsByUserIdAndBeneficiaryId(Long userId, Long beneficiaryId);
    boolean existsByUserIdAndDeviceId(Long userId, Long deviceId);
    boolean existsByUserIdAndCountryIgnoreCase(Long userId, String country);
    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime since);

    Optional<Transaction> findFirstByUserIdAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByCreatedAtDesc(Long userId);

    /** Customer's "normal" amount = average of their APPROVED transactions. Null if none. */
    @Query("select avg(t.amount) from Transaction t where t.user.id = :userId and t.status = :status")
    Double findAverageAmount(@Param("userId") Long userId, @Param("status") TransactionStatus status);
}