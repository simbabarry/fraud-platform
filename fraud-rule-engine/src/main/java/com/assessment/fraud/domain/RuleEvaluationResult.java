package com.assessment.fraud.domain;

/**
 * Immutable outcome of a single fraud rule evaluation.
 */
public record RuleEvaluationResult(
        String ruleCode,
        String ruleName,
        RiskLevel riskLevel,
        String reason,
        boolean triggered) {

    public static RuleEvaluationResult pass(String ruleCode, String ruleName) {
        return new RuleEvaluationResult(ruleCode, ruleName, RiskLevel.LOW, "Rule not triggered", false);
    }

    public static RuleEvaluationResult fail(
            String ruleCode, String ruleName, RiskLevel riskLevel, String reason) {
        return new RuleEvaluationResult(ruleCode, ruleName, riskLevel, reason, true);
    }
}
