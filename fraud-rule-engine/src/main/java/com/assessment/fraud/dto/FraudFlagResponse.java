package com.assessment.fraud.dto;

import com.assessment.fraud.domain.FraudFlag;
import com.assessment.fraud.domain.RiskLevel;
import java.time.Instant;
import java.util.UUID;

public record FraudFlagResponse(
        UUID id,
        UUID assessmentId,
        UUID transactionEventId,
        String externalTransactionId,
        String customerId,
        String ruleCode,
        String ruleName,
        RiskLevel riskLevel,
        int scoreContribution,
        String reason,
        Instant createdAt) {

    public static FraudFlagResponse from(FraudFlag flag) {
        return new FraudFlagResponse(
                flag.getId(),
                flag.getFraudAssessment().getId(),
                flag.getTransactionEvent().getId(),
                flag.getTransactionEvent().getExternalId(),
                flag.getCustomerId(),
                flag.getRuleCode(),
                flag.getRuleName(),
                flag.getRiskLevel(),
                flag.getScoreContribution(),
                flag.getReason(),
                flag.getCreatedAt());
    }
}
