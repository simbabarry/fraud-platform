package com.assessment.fraud.repository;

import com.assessment.fraud.domain.TransactionEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionEventRepository extends JpaRepository<TransactionEvent, UUID> {

    Optional<TransactionEvent> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    @Query(
            """
            SELECT COUNT(t) FROM TransactionEvent t
            WHERE t.customerId = :customerId
              AND t.occurredAt >= :from
              AND t.occurredAt <= :to
              AND t.id <> :excludeId
            """)
    long countByCustomerInWindowExcluding(
            @Param("customerId") String customerId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("excludeId") UUID excludeId);

    @Query(
            """
            SELECT COUNT(t) FROM TransactionEvent t
            WHERE t.customerId = :customerId
              AND t.id <> :excludeId
              AND t.occurredAt < :before
            """)
    long countByCustomerBeforeExcluding(
            @Param("customerId") String customerId,
            @Param("before") Instant before,
            @Param("excludeId") UUID excludeId);

    @Query(
            """
            SELECT DISTINCT t.merchantCountry FROM TransactionEvent t
            WHERE t.customerId = :customerId
              AND t.id <> :excludeId
              AND t.occurredAt < :before
              AND t.merchantCountry IS NOT NULL
            """)
    List<String> findDistinctCountriesByCustomerBeforeExcluding(
            @Param("customerId") String customerId,
            @Param("before") Instant before,
            @Param("excludeId") UUID excludeId);

    @Query(
            """
            SELECT COUNT(t) FROM TransactionEvent t
            WHERE t.customerId = :customerId
              AND t.id <> :excludeId
              AND t.occurredAt >= :from
              AND t.occurredAt < :before
            """)
    long countByCustomerSinceBeforeExcluding(
            @Param("customerId") String customerId,
            @Param("from") Instant from,
            @Param("before") Instant before,
            @Param("excludeId") UUID excludeId);

    @Query(
            """
            SELECT AVG(t.amount) FROM TransactionEvent t
            WHERE t.customerId = :customerId
              AND t.id <> :excludeId
              AND t.occurredAt >= :from
              AND t.occurredAt < :before
            """)
    BigDecimal averageAmountByCustomerSinceBeforeExcluding(
            @Param("customerId") String customerId,
            @Param("from") Instant from,
            @Param("before") Instant before,
            @Param("excludeId") UUID excludeId);

    @Query(
            """
            SELECT COUNT(t) FROM TransactionEvent t
            WHERE t.customerId = :customerId
              AND t.id <> :excludeId
              AND t.amount = :amount
              AND LOWER(t.merchantName) = LOWER(:merchantName)
              AND t.occurredAt >= :from
              AND t.occurredAt <= :to
            """)
    long countDuplicateCandidates(
            @Param("customerId") String customerId,
            @Param("amount") BigDecimal amount,
            @Param("merchantName") String merchantName,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("excludeId") UUID excludeId);
}
