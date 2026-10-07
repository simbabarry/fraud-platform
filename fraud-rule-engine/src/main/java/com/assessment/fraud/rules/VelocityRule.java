package com.assessment.fraud.rules;

import com.assessment.fraud.domain.RiskLevel;
import com.assessment.fraud.domain.RuleEvaluationResult;
import com.assessment.fraud.domain.TransactionEvent;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class VelocityRule implements FraudRule {

    @Override
    public String code() {
        return "VELOCITY";
    }

    @Override
    public String name() {
        return "Transaction velocity";
    }

    @Override
    public RiskLevel defaultRiskLevel() {
        return RiskLevel.MEDIUM;
    }

    @Override
    public String description() {
        return "Flags customers with an unusually high number of transactions in a short time window.";
    }

    @Override
    public RuleEvaluationResult evaluate(TransactionEvent event, RuleEvaluationContext context) {
        int windowMinutes = context.properties().getVelocityWindowMinutes();
        int maxTx = context.properties().getVelocityMaxTransactions();
        var from = event.getOccurredAt().minus(windowMinutes, ChronoUnit.MINUTES);
        // Window ends at current event time — excludes future / out-of-order later events
        long count = context.transactionEventRepository()
                .countByCustomerInWindowExcluding(
                        event.getCustomerId(), from, event.getOccurredAt(), event.getId());

        // +1 for the current event being assessed
        long includingCurrent = count + 1;
        if (includingCurrent > maxTx) {
            return RuleEvaluationResult.fail(
                    code(),
                    name(),
                    RiskLevel.MEDIUM,
                    "Customer %s has %d transactions in the last %d minutes (max %d)"
                            .formatted(event.getCustomerId(), includingCurrent, windowMinutes, maxTx));
        }
        return RuleEvaluationResult.pass(code(), name());
    }
}
