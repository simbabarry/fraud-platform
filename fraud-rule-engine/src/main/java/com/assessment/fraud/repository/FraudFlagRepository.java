package com.assessment.fraud.repository;

import com.assessment.fraud.domain.FraudFlag;
import com.assessment.fraud.domain.enums.RiskLevel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FraudFlagRepository extends JpaRepository<FraudFlag, UUID> {

    @EntityGraph(attributePaths = {"transactionEvent", "fraudAssessment"})
    List<FraudFlag> findByTransactionEventIdOrderByCreatedAtDesc(UUID transactionEventId);

    @EntityGraph(attributePaths = {"transactionEvent", "fraudAssessment"})
    @Query(
            """
            SELECT f FROM FraudFlag f
            WHERE (:customerId IS NULL OR f.customerId = :customerId)
              AND (:ruleCode IS NULL OR f.ruleCode = :ruleCode)
              AND (:riskLevel IS NULL OR f.riskLevel = :riskLevel)
            """)
    Page<FraudFlag> search(
            @Param("customerId") String customerId,
            @Param("ruleCode") String ruleCode,
            @Param("riskLevel") RiskLevel riskLevel,
            Pageable pageable);
}
