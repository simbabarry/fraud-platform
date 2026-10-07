package com.assessment.fraud.rules;

import com.assessment.fraud.domain.RiskLevel;
import com.assessment.fraud.domain.RuleEvaluationResult;
import com.assessment.fraud.domain.TransactionEvent;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class NightHighValueRule implements FraudRule {

    @Override
    public String code() {
        return "NIGHT_HIGH_VALUE";
    }

    @Override
    public String name() {
        return "High-value night transaction";
    }

    @Override
    public RiskLevel defaultRiskLevel() {
        return RiskLevel.MEDIUM;
    }

    @Override
    public String description() {
        return "Flags high-value transactions occurring between 00:00 and 05:59 UTC.";
    }

    @Override
    public RuleEvaluationResult evaluate(TransactionEvent event, RuleEvaluationContext context) {
        int hour = event.getOccurredAt().atZone(ZoneOffset.UTC).getHour();
        boolean night = hour < 6;
        var threshold = context.properties().getNightHighAmountThreshold();
        if (night && event.getAmount().compareTo(threshold) >= 0) {
            return RuleEvaluationResult.fail(
                    code(),
                    name(),
                    RiskLevel.MEDIUM,
                    "Night-time (hour=%d UTC) amount %s exceeds %s"
                            .formatted(hour, event.getAmount(), threshold));
        }
        return RuleEvaluationResult.pass(code(), name());
    }
}
