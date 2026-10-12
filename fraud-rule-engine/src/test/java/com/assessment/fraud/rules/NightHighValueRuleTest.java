package com.assessment.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.assessment.fraud.config.FraudRuleProperties;
import com.assessment.fraud.domain.enums.TransactionCategory;
import com.assessment.fraud.domain.TransactionEvent;
import com.assessment.fraud.repository.TransactionEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NightHighValueRuleTest {

    private NightHighValueRule rule;
    private RuleEvaluationContext context;

    @BeforeEach
    void setUp() {
        rule = new NightHighValueRule();
        FraudRuleProperties props = new FraudRuleProperties();
        props.setNightHighAmountThreshold(new BigDecimal("1000"));
        context = new RuleEvaluationContext(props, mock(TransactionEventRepository.class));
    }

    @Test
    void triggersForNightHighValue() {
        var result = rule.evaluate(event("2026-01-15T02:30:00Z", "1500"), context);
        assertThat(result.triggered()).isTrue();
    }

    @Test
    void passesDuringDay() {
        var result = rule.evaluate(event("2026-01-15T14:00:00Z", "1500"), context);
        assertThat(result.triggered()).isFalse();
    }

    private static TransactionEvent event(String occurredAt, String amount) {
        TransactionEvent e = new TransactionEvent();
        e.setExternalId("tx-night");
        e.setCustomerId("cust-1");
        e.setAccountId("acc-1");
        e.setAmount(new BigDecimal(amount));
        e.setCurrency("USD");
        e.setCategory(TransactionCategory.ONLINE_PURCHASE);
        e.setMerchantName("NightMart");
        e.setOccurredAt(Instant.parse(occurredAt));
        return e;
    }
}
