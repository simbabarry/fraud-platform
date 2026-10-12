package com.assessment.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.assessment.fraud.config.FraudRuleProperties;
import com.assessment.fraud.domain.enums.TransactionCategory;
import com.assessment.fraud.domain.TransactionEvent;
import com.assessment.fraud.repository.TransactionEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnusualCountryRuleTest {

    private UnusualCountryRule rule;
    private TransactionEventRepository repo;
    private RuleEvaluationContext context;

    @BeforeEach
    void setUp() {
        rule = new UnusualCountryRule();
        repo = mock(TransactionEventRepository.class);
        FraudRuleProperties props = new FraudRuleProperties();
        props.setHighRiskCountries("KP,SY");
        props.setUnusualCountryMinHistory(3);
        context = new RuleEvaluationContext(props, repo);
    }

    @Test
    void triggersForHighRiskCountry() {
        var event = event("KP");
        var result = rule.evaluate(event, context);
        assertThat(result.triggered()).isTrue();
        assertThat(result.ruleCode()).isEqualTo("UNUSUAL_COUNTRY");
    }

    @Test
    void triggersWhenCountryNotInCustomerHistory() {
        var event = event("DE");
        when(repo.countByCustomerBeforeExcluding(eq("cust-1"), any(), any())).thenReturn(5L);
        when(repo.findDistinctCountriesByCustomerBeforeExcluding(eq("cust-1"), any(), any()))
                .thenReturn(List.of("US", "ZA"));

        assertThat(rule.evaluate(event, context).triggered()).isTrue();
    }

    @Test
    void passesWhenCountryMatchesHistory() {
        var event = event("US");
        when(repo.countByCustomerBeforeExcluding(eq("cust-1"), any(), any())).thenReturn(5L);
        when(repo.findDistinctCountriesByCustomerBeforeExcluding(eq("cust-1"), any(), any()))
                .thenReturn(List.of("US", "ZA"));

        assertThat(rule.evaluate(event, context).triggered()).isFalse();
    }

    private static TransactionEvent event(String country) {
        TransactionEvent e = new TransactionEvent();
        e.setId(UUID.randomUUID());
        e.setExternalId("tx-country");
        e.setCustomerId("cust-1");
        e.setAccountId("acc-1");
        e.setAmount(new BigDecimal("50"));
        e.setCurrency("USD");
        e.setCategory(TransactionCategory.ONLINE_PURCHASE);
        e.setMerchantName("Shop");
        e.setMerchantCountry(country);
        e.setOccurredAt(Instant.parse("2026-01-15T12:00:00Z"));
        return e;
    }
}
