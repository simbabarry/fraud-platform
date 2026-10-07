package com.assessment.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consumer-side projection of {@code FraudAssessmentResponse} published by the rule engine.
 * Unknown fields are ignored so the services can evolve independently.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FraudAssessmentEvent(
        UUID assessmentId,
        UUID transactionEventId,
        String externalId,
        String customerId,
        BigDecimal amount,
        String currency,
        String category,
        Instant occurredAt,
        int riskScore,
        String overallRiskLevel,
        boolean fraudSuspected,
        int triggeredRuleCount,
        List<TriggeredRuleEvent> triggeredRules,
        Instant assessedAt) {}
