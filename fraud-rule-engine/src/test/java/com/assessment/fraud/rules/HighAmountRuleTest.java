package com.assessment.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.assessment.fraud.config.FraudRuleProperties;
import com.assessment.fraud.domain.TransactionCategory;
import com.assessment.fraud.domain.TransactionEvent;
import com.assessment.fraud.repository.TransactionEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HighAmountRuleTest {

    private HighAmountRule rule;
    private RuleEvaluationContext context;

    @BeforeEach
    void setUp() {
        rule = new HighAmountRule();
        FraudRuleProperties props = new FraudRuleProperties();
        props.setHighAmountThreshold(new BigDecimal("5000"));
        context = new RuleEvaluationContext(props, mock(TransactionEventRepository.class));
    }

    @Test
    void triggersWhenAmountAtOrAboveThreshold() {
        var result = rule.evaluate(event(new BigDecimal("5000.00")), context);
        assertThat(result.triggered()).isTrue();
        assertThat(result.ruleCode()).isEqualTo("HIGH_AMOUNT");
    }

    @Test
    void passesWhenAmountBelowThreshold() {
        var result = rule.evaluate(event(new BigDecimal("4999.99")), context);
        assertThat(result.triggered()).isFalse();
    }

    private static TransactionEvent event(BigDecimal amount) {
        TransactionEvent e = new TransactionEvent();
        e.setExternalId("tx-1");
        e.setCustomerId("cust-1");
        e.setAccountId("acc-1");
        e.setAmount(amount);
        e.setCurrency("USD");
        e.setCategory(TransactionCategory.ONLINE_PURCHASE);
        e.setMerchantName("Shop");
        e.setOccurredAt(Instant.parse("2026-01-15T12:00:00Z"));
        return e;
    }
}
