package com.assessment.fraud.rules;

import com.assessment.fraud.domain.enums.RiskLevel;
import com.assessment.fraud.domain.RuleEvaluationResult;
import com.assessment.fraud.domain.TransactionEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class SpendingSpikeRule implements FraudRule {

    @Override
    public String code() {
        return "SPENDING_SPIKE";
    }

    @Override
    public String name() {
        return "Sudden spending spike";
    }

    @Override
    public RiskLevel defaultRiskLevel() {
        return RiskLevel.HIGH;
    }

    @Override
    public String description() {
        return "Flags when the amount significantly exceeds the customer's average spend over a lookback window.";
    }

    @Override
    public RuleEvaluationResult evaluate(TransactionEvent event, RuleEvaluationContext context) {
        int lookbackDays = context.properties().getSpendingLookbackDays();
        int minHistory = context.properties().getSpendingSpikeMinHistory();
        BigDecimal multiplier = context.properties().getSpendingSpikeMultiplier();

        var from = event.getOccurredAt().minus(lookbackDays, ChronoUnit.DAYS);
        var before = event.getOccurredAt();
        long prior = context.transactionEventRepository()
                .countByCustomerSinceBeforeExcluding(
                        event.getCustomerId(), from, before, event.getId());
        if (prior < minHistory) {
            return RuleEvaluationResult.pass(code(), name());
        }

        BigDecimal average = context.transactionEventRepository()
                .averageAmountByCustomerSinceBeforeExcluding(
                        event.getCustomerId(), from, before, event.getId());
        if (average == null || average.compareTo(BigDecimal.ZERO) <= 0) {
            return RuleEvaluationResult.pass(code(), name());
        }

        BigDecimal threshold = average.multiply(multiplier).setScale(4, RoundingMode.HALF_UP);
        if (event.getAmount().compareTo(threshold) >= 0) {
            return RuleEvaluationResult.fail(
                    code(),
                    name(),
                    RiskLevel.HIGH,
                    "Amount %s exceeds %sx customer average %s (threshold %s) over last %d days"
                            .formatted(
                                    event.getAmount(),
                                    multiplier,
                                    average.setScale(2, RoundingMode.HALF_UP),
                                    threshold.setScale(2, RoundingMode.HALF_UP),
                                    lookbackDays));
        }

        return RuleEvaluationResult.pass(code(), name());
    }
}
