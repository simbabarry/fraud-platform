package com.assessment.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.assessment.notification.config.NotificationProperties;
import com.assessment.notification.dto.FraudAssessmentEvent;
import com.assessment.notification.dto.TriggeredRuleEvent;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

class FraudEmailServiceTest {

    private JavaMailSender mailSender;
    private FraudEmailService emailService;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        NotificationProperties props = new NotificationProperties();
        props.setRecipient("ops@example.com");
        props.setAlertFrom("alerts@example.com");
        emailService = new FraudEmailService(mailSender, props);
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
    }

    @Test
    void subjectUsesOverallRiskLevel() {
        assertThat(emailService.buildSubject(sample(true)))
                .isEqualTo("CRITICAL-risk transaction detected");
    }

    @Test
    void subjectFallsBackToUnknownWhenRiskLevelMissing() {
        FraudAssessmentEvent event = new FraudAssessmentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "tx-1001",
                "cust-42",
                new BigDecimal("10.00"),
                "USD",
                "GROCERIES",
                Instant.now(),
                0,
                null,
                true,
                0,
                List.of(),
                Instant.now());

        assertThat(emailService.buildSubject(event)).isEqualTo("UNKNOWN-risk transaction detected");
    }

    @Test
    void bodyAggregatesAllTriggeredRules() {
        String body = emailService.buildBody(sample(true));
        assertThat(body).contains("Transaction: tx-1001");
        assertThat(body).contains("Customer: cust-42");
        assertThat(body).contains("Amount: USD 9,200");
        assertThat(body).contains("Risk score: 110");
        assertThat(body).contains("Overall risk: CRITICAL");
        assertThat(body).contains("HIGH_AMOUNT: Amount exceeds the configured threshold");
        assertThat(body).contains("UNUSUAL_COUNTRY: Merchant country is unusual for the customer");
        assertThat(body).contains("SPENDING_SPIKE: Amount exceeds the customer's historical average");
    }

    @Test
    void bodyHandlesMissingOptionalFields() {
        FraudAssessmentEvent event = new FraudAssessmentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "   ",
                null,
                null,
                "OTHER",
                Instant.now(),
                0,
                null,
                true,
                0,
                null,
                Instant.now());

        String body = emailService.buildBody(event);

        assertThat(body).contains("Transaction: -");
        assertThat(body).contains("Customer: -");
        assertThat(body).contains("Amount: - -");
        assertThat(body).contains("Overall risk: -");
        assertThat(body).contains("- (none listed)");
    }

    @Test
    void bodyUsesDashForBlankRuleFields() {
        FraudAssessmentEvent event = new FraudAssessmentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "tx-2",
                "cust-2",
                new BigDecimal("1.5"),
                "ZAR",
                "OTHER",
                Instant.now(),
                10,
                "LOW",
                true,
                1,
                List.of(new TriggeredRuleEvent(null, "n", "LOW", "  ", 10)),
                Instant.now());

        assertThat(emailService.buildBody(event)).contains("- -: -");
    }

    @Test
    void sendInvokesMailSender() throws Exception {
        emailService.sendFraudAlert(sample(true));
        verify(mailSender).send(any(MimeMessage.class));
    }

    private static FraudAssessmentEvent sample(boolean fraud) {
        return new FraudAssessmentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "tx-1001",
                "cust-42",
                new BigDecimal("9200.00"),
                "USD",
                "ONLINE_PURCHASE",
                Instant.parse("2026-03-15T10:30:00Z"),
                110,
                "CRITICAL",
                fraud,
                3,
                List.of(
                        new TriggeredRuleEvent(
                                "HIGH_AMOUNT",
                                "High amount",
                                "HIGH",
                                "Amount exceeds the configured threshold",
                                50),
                        new TriggeredRuleEvent(
                                "UNUSUAL_COUNTRY",
                                "Unusual country",
                                "MEDIUM",
                                "Merchant country is unusual for the customer",
                                25),
                        new TriggeredRuleEvent(
                                "SPENDING_SPIKE",
                                "Spending spike",
                                "HIGH",
                                "Amount exceeds the customer's historical average",
                                50)),
                Instant.parse("2026-03-15T10:30:01Z"));
    }
}
