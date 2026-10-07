package com.assessment.fraud.rules;

import com.assessment.fraud.domain.RiskLevel;
import com.assessment.fraud.domain.RuleEvaluationResult;
import com.assessment.fraud.domain.TransactionEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class RoundAmountRule implements FraudRule {

    @Override
    public String code() {
        return "ROUND_AMOUNT";
    }

    @Override
    public String name() {
        return "Suspicious round amount";
    }

    @Override
    public RiskLevel defaultRiskLevel() {
        return RiskLevel.LOW;
    }

    @Override
    public String description() {
        return "Flags large round-number amounts (divisible by 100) which can indicate structuring.";
    }

    @Override
    public RuleEvaluationResult evaluate(TransactionEvent event, RuleEvaluationContext context) {
        BigDecimal min = context.properties().getRoundAmountMin();
        BigDecimal amount = event.getAmount();
        if (amount.compareTo(min) >= 0) {
            BigDecimal remainder = amount.remainder(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
            if (remainder.compareTo(BigDecimal.ZERO) == 0) {
                return RuleEvaluationResult.fail(
                        code(),
                        name(),
                        RiskLevel.LOW,
                        "Large round amount detected: %s %s".formatted(amount, event.getCurrency()));
            }
        }
        return RuleEvaluationResult.pass(code(), name());
    }
}
