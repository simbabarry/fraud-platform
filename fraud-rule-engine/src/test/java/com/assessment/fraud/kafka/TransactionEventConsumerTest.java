package com.assessment.fraud.kafka;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.assessment.fraud.domain.enums.TransactionCategory;
import com.assessment.fraud.dto.TransactionEventRequest;
import com.assessment.fraud.service.FraudEvaluationService;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TransactionEventConsumerTest {

    @Test
    void onMessageDelegatesToEvaluationService() {
        FraudEvaluationService evaluationService = mock(FraudEvaluationService.class);
        TransactionEventConsumer consumer = new TransactionEventConsumer(evaluationService);
        TransactionEventRequest event = new TransactionEventRequest(
                "tx-kafka-1",
                "cust-1",
                "acc-1",
                new BigDecimal("25.00"),
                "USD",
                TransactionCategory.GROCERIES,
                "Shop",
                "US",
                Instant.parse("2026-03-15T10:00:00Z"));

        consumer.onMessage(event);

        verify(evaluationService).processFromKafka(event);
    }
}
