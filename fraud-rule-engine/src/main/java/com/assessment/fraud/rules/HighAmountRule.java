package com.assessment.fraud.rules;

import com.assessment.fraud.domain.enums.RiskLevel;
import com.assessment.fraud.domain.RuleEvaluationResult;
import com.assessment.fraud.domain.TransactionEvent;
import org.springframework.stereotype.Component;

@Component
public class HighAmountRule implements FraudRule {

    @Override
    public String code() {
        return "HIGH_AMOUNT";
    }

    @Override
    public String name() {
        return "High transaction amount";
    }

    @Override
    public RiskLevel defaultRiskLevel() {
        return RiskLevel.HIGH;
    }

    @Override
    public String description() {
        return "Flags transactions whose amount exceeds the configured high-amount threshold.";
    }

    @Override
    public RuleEvaluationResult evaluate(TransactionEvent event, RuleEvaluationContext context) {
        var threshold = context.properties().getHighAmountThreshold();
        if (event.getAmount().compareTo(threshold) >= 0) {
            return RuleEvaluationResult.fail(
                    code(),
                    name(),
                    RiskLevel.HIGH,
                    "Amount %s %s exceeds threshold %s"
                            .formatted(event.getAmount(), event.getCurrency(), threshold));
        }
        return RuleEvaluationResult.pass(code(), name());
    }
}
