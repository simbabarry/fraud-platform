package com.assessment.fraud.rules;

import com.assessment.fraud.domain.enums.RiskLevel;
import com.assessment.fraud.domain.RuleEvaluationResult;
import com.assessment.fraud.domain.TransactionEvent;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Detects semantic duplicates (same customer, amount, merchant within a short window).
 * Exact {@code externalId} collisions remain API-level 409 Conflict for idempotency.
 */
@Component
public class DuplicateTransactionRule implements FraudRule {

    @Override
    public String code() {
        return "DUPLICATE_TRANSACTION";
    }

    @Override
    public String name() {
        return "Duplicate transaction";
    }

    @Override
    public RiskLevel defaultRiskLevel() {
        return RiskLevel.MEDIUM;
    }

    @Override
    public String description() {
        return "Flags near-duplicate charges: same customer, amount, and merchant within a configurable time window.";
    }

    @Override
    public RuleEvaluationResult evaluate(TransactionEvent event, RuleEvaluationContext context) {
        int windowMinutes = context.properties().getDuplicateWindowMinutes();
        var from = event.getOccurredAt().minus(windowMinutes, ChronoUnit.MINUTES);
        var to = event.getOccurredAt().plus(windowMinutes, ChronoUnit.MINUTES);

        long duplicates = context.transactionEventRepository()
                .countDuplicateCandidates(
                        event.getCustomerId(),
                        event.getAmount(),
                        event.getMerchantName(),
                        from,
                        to,
                        event.getId());

        if (duplicates > 0) {
            return RuleEvaluationResult.fail(
                    code(),
                    name(),
                    RiskLevel.MEDIUM,
                    "Possible duplicate: customer %s charged %s at '%s' within ±%d minutes (%d similar)"
                            .formatted(
                                    event.getCustomerId(),
                                    event.getAmount(),
                                    event.getMerchantName(),
                                    windowMinutes,
                                    duplicates));
        }

        return RuleEvaluationResult.pass(code(), name());
    }
}
