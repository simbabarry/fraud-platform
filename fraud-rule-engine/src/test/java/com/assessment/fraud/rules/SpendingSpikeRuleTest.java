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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SpendingSpikeRuleTest {

    private SpendingSpikeRule rule;
    private TransactionEventRepository repo;
    private RuleEvaluationContext context;

    @BeforeEach
    void setUp() {
        rule = new SpendingSpikeRule();
        repo = mock(TransactionEventRepository.class);
        FraudRuleProperties props = new FraudRuleProperties();
        props.setSpendingLookbackDays(30);
        props.setSpendingSpikeMultiplier(new BigDecimal("3.0"));
        props.setSpendingSpikeMinHistory(3);
        context = new RuleEvaluationContext(props, repo);
    }

    @Test
    void triggersWhenAmountExceedsAverageTimesMultiplier() {
        var event = event(new BigDecimal("900"));
        when(repo.countByCustomerSinceBeforeExcluding(eq("cust-1"), any(), any(), any())).thenReturn(5L);
        when(repo.averageAmountByCustomerSinceBeforeExcluding(eq("cust-1"), any(), any(), any()))
                .thenReturn(new BigDecimal("100"));

        assertThat(rule.evaluate(event, context).triggered()).isTrue();
    }

    @Test
    void passesWhenInsufficientHistory() {
        var event = event(new BigDecimal("900"));
        when(repo.countByCustomerSinceBeforeExcluding(eq("cust-1"), any(), any(), any())).thenReturn(1L);

        assertThat(rule.evaluate(event, context).triggered()).isFalse();
    }

    private static TransactionEvent event(BigDecimal amount) {
        TransactionEvent e = new TransactionEvent();
        e.setId(UUID.randomUUID());
        e.setExternalId("tx-spike");
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
