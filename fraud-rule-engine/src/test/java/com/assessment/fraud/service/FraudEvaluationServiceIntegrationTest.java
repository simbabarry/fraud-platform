package com.assessment.fraud.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.assessment.fraud.domain.RiskLevel;
import com.assessment.fraud.domain.TransactionCategory;
import com.assessment.fraud.dto.TransactionEventRequest;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FraudEvaluationServiceIntegrationTest {

    @Autowired
    private FraudEvaluationService fraudEvaluationService;

    @Test
    void ingestFlagsHighAmountAndComputesRiskScore() {
        var response = fraudEvaluationService.ingestAndEvaluate(request(
                "ext-high-1",
                "cust-100",
                new BigDecimal("9000.00"),
                TransactionCategory.ONLINE_PURCHASE,
                Instant.parse("2026-03-01T10:00:00Z")));

        var assessment = response.assessment();
        assertThat(assessment.fraudSuspected()).isTrue();
        assertThat(assessment.riskScore()).isGreaterThan(0);
        assertThat(assessment.overallRiskLevel()).isIn(RiskLevel.HIGH, RiskLevel.CRITICAL);
        assertThat(assessment.triggeredRules()).extracting(f -> f.ruleCode()).contains("HIGH_AMOUNT");
    }

    @Test
    void duplicateExternalIdIsIdempotent() {
        var req = request(
                "ext-dup-1",
                "cust-200",
                new BigDecimal("10.00"),
                TransactionCategory.GROCERIES,
                Instant.parse("2026-03-01T11:00:00Z"));
        var first = fraudEvaluationService.ingestAndEvaluate(req);
        var second = fraudEvaluationService.ingestAndEvaluate(req);
        assertThat(second.assessment().assessmentId()).isEqualTo(first.assessment().assessmentId());
    }

    @Test
    void listsRegisteredRules() {
        assertThat(fraudEvaluationService.listRules())
                .extracting(r -> r.ruleCode())
                .contains(
                        "HIGH_AMOUNT",
                        "VELOCITY",
                        "NIGHT_HIGH_VALUE",
                        "UNUSUAL_COUNTRY",
                        "SPENDING_SPIKE",
                        "DUPLICATE_TRANSACTION");
    }

    private static TransactionEventRequest request(
            String externalId,
            String customerId,
            BigDecimal amount,
            TransactionCategory category,
            Instant occurredAt) {
        return new TransactionEventRequest(
                externalId,
                customerId,
                "acc-1",
                amount,
                "USD",
                category,
                "Test Merchant",
                "US",
                occurredAt);
    }
}
