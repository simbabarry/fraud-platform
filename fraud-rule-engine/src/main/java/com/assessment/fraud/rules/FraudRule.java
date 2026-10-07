package com.assessment.fraud.rules;

import com.assessment.fraud.domain.RiskLevel;
import com.assessment.fraud.domain.RuleEvaluationResult;
import com.assessment.fraud.domain.TransactionEvent;

public interface FraudRule {

    String code();

    String name();

    RiskLevel defaultRiskLevel();

    String description();

    RuleEvaluationResult evaluate(TransactionEvent event, RuleEvaluationContext context);
}
