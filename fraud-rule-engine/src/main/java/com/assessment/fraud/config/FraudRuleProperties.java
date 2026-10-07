package com.assessment.fraud.config;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class FraudRuleProperties {

    @Value("${fraud.rules.high-amount-threshold}")
    private BigDecimal highAmountThreshold;

    @Value("${fraud.rules.velocity-window-minutes}")
    private int velocityWindowMinutes;

    @Value("${fraud.rules.velocity-max-transactions}")
    private int velocityMaxTransactions;

    @Value("${fraud.rules.night-high-amount-threshold}")
    private BigDecimal nightHighAmountThreshold;

    @Value("${fraud.rules.round-amount-min}")
    private BigDecimal roundAmountMin;

    /** Comma-separated ISO country codes treated as high-risk (e.g. XX,YY). */
    @Value("${fraud.rules.high-risk-countries:KP,SY,IR}")
    private String highRiskCountries;

    /** Min prior txs before "unusual country vs history" can fire. */
    @Value("${fraud.rules.unusual-country-min-history}")
    private int unusualCountryMinHistory;

    @Value("${fraud.rules.spending-lookback-days}")
    private int spendingLookbackDays;

    @Value("${fraud.rules.spending-spike-multiplier}")
    private BigDecimal spendingSpikeMultiplier;

    @Value("${fraud.rules.spending-spike-min-history}")
    private int spendingSpikeMinHistory;

    @Value("${fraud.rules.duplicate-window-minutes}")
    private int duplicateWindowMinutes;

    public Set<String> highRiskCountrySet() {
        if (highRiskCountries == null || highRiskCountries.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(highRiskCountries.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
