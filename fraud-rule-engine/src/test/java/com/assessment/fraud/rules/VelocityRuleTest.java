package com.assessment.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.assessment.fraud.config.FraudRuleProperties;
import com.assessment.fraud.domain.TransactionCategory;
import com.assessment.fraud.domain.TransactionEvent;
import com.assessment.fraud.repository.TransactionEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VelocityRuleTest {

    private VelocityRule rule;
    private TransactionEventRepository repo;
    private RuleEvaluationContext context;

    @BeforeEach
    void setUp() {
        rule = new VelocityRule();
        repo = mock(TransactionEventRepository.class);
        FraudRuleProperties props = new FraudRuleProperties();
        props.setVelocityWindowMinutes(60);
        props.setVelocityMaxTransactions(5);
        context = new RuleEvaluationContext(props, repo);
    }

    @Test
    void triggersWhenPriorCountPlusCurrentExceedsMax() {
        when(repo.countByCustomerInWindowExcluding(eq("cust-1"), any(), any(), any())).thenReturn(5L);
        assertThat(rule.evaluate(event(), context).triggered()).isTrue();
    }

    @Test
    void passesWhenUnderLimit() {
        when(repo.countByCustomerInWindowExcluding(eq("cust-1"), any(), any(), any())).thenReturn(3L);
        assertThat(rule.evaluate(event(), context).triggered()).isFalse();
    }

    private static TransactionEvent event() {
        TransactionEvent e = new TransactionEvent();
        e.setId(UUID.randomUUID());
        e.setExternalId("tx-vel");
        e.setCustomerId("cust-1");
        e.setAccountId("acc-1");
        e.setAmount(new BigDecimal("10"));
        e.setCurrency("USD");
        e.setCategory(TransactionCategory.GROCERIES);
        e.setMerchantName("Shop");
        e.setOccurredAt(Instant.parse("2026-01-15T12:00:00Z"));
        return e;
    }
}
