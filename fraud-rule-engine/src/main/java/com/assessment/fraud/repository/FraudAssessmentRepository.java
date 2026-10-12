package com.assessment.fraud.repository;

import com.assessment.fraud.domain.FraudAssessment;
import com.assessment.fraud.domain.enums.RiskLevel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FraudAssessmentRepository extends JpaRepository<FraudAssessment, UUID> {

    @EntityGraph(attributePaths = {"transactionEvent", "flags"})
    Optional<FraudAssessment> findByTransactionEvent_ExternalId(String externalId);

    @EntityGraph(attributePaths = {"transactionEvent", "flags"})
    @Query("SELECT a FROM FraudAssessment a WHERE a.id = :id")
    Optional<FraudAssessment> findDetailedById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"transactionEvent", "flags"})
    @Query(
            """
            SELECT a FROM FraudAssessment a
            WHERE (:customerId IS NULL OR a.customerId = :customerId)
              AND (:riskLevel IS NULL OR a.overallRiskLevel = :riskLevel)
              AND (:fraudSuspected IS NULL OR a.fraudSuspected = :fraudSuspected)
            """)
    Page<FraudAssessment> search(
            @Param("customerId") String customerId,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("fraudSuspected") Boolean fraudSuspected,
            Pageable pageable);
}
