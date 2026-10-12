package com.assessment.fraud.rules;

import com.assessment.fraud.domain.enums.RiskLevel;
import com.assessment.fraud.domain.RuleEvaluationResult;
import com.assessment.fraud.domain.TransactionEvent;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UnusualCountryRule implements FraudRule {

    @Override
    public String code() {
        return "UNUSUAL_COUNTRY";
    }

    @Override
    public String name() {
        return "Unusual origin country";
    }

    @Override
    public RiskLevel defaultRiskLevel() {
        return RiskLevel.HIGH;
    }

    @Override
    public String description() {
        return "Flags transactions from configured high-risk countries, or from a country not seen in the customer's history.";
    }

    @Override
    public RuleEvaluationResult evaluate(TransactionEvent event, RuleEvaluationContext context) {
        String country = event.getMerchantCountry();
        if (country == null || country.isBlank()) {
            return RuleEvaluationResult.pass(code(), name());
        }

        String normalized = country.trim().toUpperCase(Locale.ROOT);
        Set<String> highRisk = context.properties().highRiskCountrySet();
        if (highRisk.contains(normalized)) {
            return RuleEvaluationResult.fail(
                    code(),
                    name(),
                    RiskLevel.HIGH,
                    "Merchant country %s is in the high-risk country list".formatted(normalized));
        }

        var before = event.getOccurredAt();
        long prior = context.transactionEventRepository()
                .countByCustomerBeforeExcluding(event.getCustomerId(), before, event.getId());
        int minHistory = context.properties().getUnusualCountryMinHistory();
        if (prior < minHistory) {
            return RuleEvaluationResult.pass(code(), name());
        }

        Set<String> known = context.transactionEventRepository()
                .findDistinctCountriesByCustomerBeforeExcluding(
                        event.getCustomerId(), before, event.getId())
                .stream()
                .filter(c -> c != null && !c.isBlank())
                .map(c -> c.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        if (!known.isEmpty() && !known.contains(normalized)) {
            return RuleEvaluationResult.fail(
                    code(),
                    name(),
                    RiskLevel.HIGH,
                    "Country %s is unusual for customer %s (known: %s)"
                            .formatted(normalized, event.getCustomerId(), known));
        }

        return RuleEvaluationResult.pass(code(), name());
    }
}
