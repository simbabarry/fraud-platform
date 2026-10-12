package com.assessment.fraud.dto;

import com.assessment.fraud.domain.enums.EventSource;
import com.assessment.fraud.domain.FraudAssessment;
import com.assessment.fraud.domain.enums.RiskLevel;
import com.assessment.fraud.domain.enums.TransactionCategory;
import com.assessment.fraud.domain.enums.TransactionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudAssessmentResponse(
        UUID assessmentId,
        UUID transactionEventId,
        String externalId,
        String customerId,
        BigDecimal amount,
        String currency,
        TransactionCategory category,
        Instant occurredAt,
        TransactionStatus transactionStatus,
        EventSource source,
        int riskScore,
        RiskLevel overallRiskLevel,
        boolean fraudSuspected,
        int triggeredRuleCount,
        List<FraudFlagResponse> triggeredRules,
        Instant assessedAt) {

    public static FraudAssessmentResponse from(FraudAssessment assessment) {
        var tx = assessment.getTransactionEvent();
        return new FraudAssessmentResponse(
                assessment.getId(),
                tx.getId(),
                tx.getExternalId(),
                assessment.getCustomerId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getCategory(),
                tx.getOccurredAt(),
                tx.getStatus(),
                assessment.getSource(),
                assessment.getRiskScore(),
                assessment.getOverallRiskLevel(),
                assessment.isFraudSuspected(),
                assessment.getTriggeredRuleCount(),
                assessment.getFlags().stream().map(FraudFlagResponse::from).toList(),
                assessment.getCreatedAt());
    }
}
