package com.assessment.fraud.dto;

/** Kept for backward-compatible REST ingest responses. */
public record TransactionEvaluationResponse(FraudAssessmentResponse assessment) {

    public static TransactionEvaluationResponse of(FraudAssessmentResponse assessment) {
        return new TransactionEvaluationResponse(assessment);
    }
}
