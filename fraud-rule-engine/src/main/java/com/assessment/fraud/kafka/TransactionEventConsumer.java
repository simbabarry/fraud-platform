package com.assessment.fraud.kafka;

import com.assessment.fraud.dto.TransactionEventRequest;
import com.assessment.fraud.service.FraudEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fraud.kafka.enabled", havingValue = "true")
public class TransactionEventConsumer {

    private final FraudEvaluationService fraudEvaluationService;

    @KafkaListener(
            topics = "${fraud.kafka.transaction-topic:transactions.categorized}",
            groupId = "${spring.kafka.consumer.group-id:fraud-rule-engine}")
    public void onMessage(@Payload TransactionEventRequest event) {
        log.info("Kafka event received externalId={}", event.externalId());
        fraudEvaluationService.processFromKafka(event);
    }
}
