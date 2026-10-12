package com.assessment.fraud.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.assessment.fraud.domain.enums.EventSource;
import com.assessment.fraud.domain.enums.RiskLevel;
import com.assessment.fraud.domain.enums.TransactionCategory;
import com.assessment.fraud.domain.enums.TransactionStatus;
import com.assessment.fraud.dto.FraudAssessmentResponse;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class KafkaFraudAssessmentPublisherTest {

    @Mock
    private KafkaTemplate<String, FraudAssessmentResponse> kafkaTemplate;

    private KafkaFraudAssessmentPublisher publisher;

    @BeforeEach
    void setUp() throws Exception {
        publisher = new KafkaFraudAssessmentPublisher(kafkaTemplate);
        Field topic = KafkaFraudAssessmentPublisher.class.getDeclaredField("assessmentTopic");
        topic.setAccessible(true);
        topic.set(publisher, "fraud.assessments");
    }

    @Test
    void publishSendsAssessmentKeyedByExternalId() {
        FraudAssessmentResponse assessment = sample();
        when(kafkaTemplate.send(eq("fraud.assessments"), eq("tx-1001"), eq(assessment)))
                .thenReturn(CompletableFuture.completedFuture(successResult(assessment)));

        publisher.publish(assessment);

        verify(kafkaTemplate).send("fraud.assessments", "tx-1001", assessment);
    }

    @Test
    void publishHandlesAsyncFailureWithoutThrowing() {
        FraudAssessmentResponse assessment = sample();
        CompletableFuture<SendResult<String, FraudAssessmentResponse>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));
        when(kafkaTemplate.send(any(), any(), any())).thenReturn(failed);

        publisher.publish(assessment);

        verify(kafkaTemplate).send("fraud.assessments", "tx-1001", assessment);
    }

    private static SendResult<String, FraudAssessmentResponse> successResult(
            FraudAssessmentResponse assessment) {
        ProducerRecord<String, FraudAssessmentResponse> record =
                new ProducerRecord<>("fraud.assessments", "tx-1001", assessment);
        RecordMetadata metadata = new RecordMetadata(new TopicPartition("fraud.assessments", 1), 0, 0, 0L, 0, 0);
        return new SendResult<>(record, metadata);
    }

    private static FraudAssessmentResponse sample() {
        return new FraudAssessmentResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "tx-1001",
                "cust-42",
                new BigDecimal("9200.00"),
                "USD",
                TransactionCategory.ONLINE_PURCHASE,
                Instant.parse("2026-03-15T10:30:00Z"),
                TransactionStatus.ASSESSED,
                EventSource.REST,
                110,
                RiskLevel.CRITICAL,
                true,
                1,
                List.of(),
                Instant.parse("2026-03-15T10:30:01Z"));
    }
}
