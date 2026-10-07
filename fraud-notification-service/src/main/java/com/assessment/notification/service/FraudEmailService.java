package com.assessment.notification.service;

import com.assessment.notification.config.NotificationProperties;
import com.assessment.notification.dto.FraudAssessmentEvent;
import com.assessment.notification.dto.TriggeredRuleEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudEmailService {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    /**
     * Sends one email per assessment that lists every triggered rule.
     * Transient SMTP failures are retried a few times before bubbling up to Kafka.
     */
    @Retryable(
            retryFor = {MailException.class, MessagingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 500))
    public void sendFraudAlert(FraudAssessmentEvent assessment) throws MessagingException {
        String subject = buildSubject(assessment);
        String body = buildBody(assessment);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
        helper.setFrom(properties.getAlertFrom());
        helper.setTo(properties.getRecipient());
        helper.setSubject(subject);
        helper.setText(body, false);

        mailSender.send(message);
        log.info(
                "Sent fraud alert email assessmentId={} externalId={} rules={}",
                assessment.assessmentId(),
                assessment.externalId(),
                assessment.triggeredRuleCount());
    }

    String buildSubject(FraudAssessmentEvent assessment) {
        String level = assessment.overallRiskLevel() == null ? "UNKNOWN" : assessment.overallRiskLevel();
        return level + "-risk transaction detected";
    }

    String buildBody(FraudAssessmentEvent assessment) {
        StringBuilder sb = new StringBuilder();
        sb.append("Transaction: ").append(nullToDash(assessment.externalId())).append('\n');
        sb.append("Customer: ").append(nullToDash(assessment.customerId())).append('\n');
        sb.append("Amount: ")
                .append(nullToDash(assessment.currency()))
                .append(' ')
                .append(formatAmount(assessment.amount()))
                .append('\n');
        sb.append("Risk score: ").append(assessment.riskScore()).append('\n');
        sb.append("Overall risk: ").append(nullToDash(assessment.overallRiskLevel())).append('\n');
        sb.append('\n');
        sb.append("Triggered rules:\n");

        List<TriggeredRuleEvent> rules =
                assessment.triggeredRules() == null ? List.of() : assessment.triggeredRules();
        if (rules.isEmpty()) {
            sb.append("- (none listed)\n");
        } else {
            for (TriggeredRuleEvent rule : rules) {
                sb.append("- ")
                        .append(nullToDash(rule.ruleCode()))
                        .append(": ")
                        .append(nullToDash(rule.reason()))
                        .append('\n');
            }
        }
        return sb.toString();
    }

    private static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        return NumberFormat.getNumberInstance(Locale.US).format(amount);
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
