package com.assessment.notification.consumer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.assessment.notification.dto.FraudAssessmentEvent;
import com.assessment.notification.dto.TriggeredRuleEvent;
import com.assessment.notification.service.NotificationProcessingService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FraudAssessmentConsumerTest {

    @Test
    void consumeDelegatesToProcessingService() {
        NotificationProcessingService processingService = mock(NotificationProcessingService.class);
        FraudAssessmentConsumer consumer = new FraudAssessmentConsumer(processingService);
        FraudAssessmentEvent event = sample();

        consumer.consume(event);

        verify(processingService).process(event);
    }

    private static FraudAssessmentEvent sample() {
        return new FraudAssessmentEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "tx-1",
                "cust-1",
                new BigDecimal("100.00"),
                "USD",
                "ONLINE_PURCHASE",
                Instant.now(),
                50,
                "HIGH",
                true,
                1,
                List.of(new TriggeredRuleEvent("HIGH_AMOUNT", "High", "HIGH", "too high", 50)),
                Instant.now());
    }
}
