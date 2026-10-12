package com.assessment.fraud.rules;

import com.assessment.fraud.domain.enums.RiskLevel;
import com.assessment.fraud.domain.RuleEvaluationResult;
import com.assessment.fraud.domain.enums.TransactionCategory;
import com.assessment.fraud.domain.TransactionEvent;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RiskyCategoryRule implements FraudRule {

    private static final Set<TransactionCategory> RISKY =
            EnumSet.of(TransactionCategory.ATM_WITHDRAWAL, TransactionCategory.TRANSFER);
    private static final BigDecimal THRESHOLD = new BigDecimal("2000");

    @Override
    public String code() {
        return "RISKY_CATEGORY";
    }

    @Override
    public String name() {
        return "High-value risky category";
    }

    @Override
    public RiskLevel defaultRiskLevel() {
        return RiskLevel.HIGH;
    }

    @Override
    public String description() {
        return "Flags high-value ATM withdrawals and transfers, which are common fraud cash-out paths.";
    }

    @Override
    public RuleEvaluationResult evaluate(TransactionEvent event, RuleEvaluationContext context) {
        if (RISKY.contains(event.getCategory()) && event.getAmount().compareTo(THRESHOLD) >= 0) {
            return RuleEvaluationResult.fail(
                    code(),
                    name(),
                    RiskLevel.HIGH,
                    "Category %s with amount %s is considered high risk"
                            .formatted(event.getCategory(), event.getAmount()));
        }
        return RuleEvaluationResult.pass(code(), name());
    }
}
