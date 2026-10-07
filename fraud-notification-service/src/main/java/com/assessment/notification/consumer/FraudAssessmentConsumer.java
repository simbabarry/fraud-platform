package com.assessment.notification.consumer;

import com.assessment.notification.dto.FraudAssessmentEvent;
import com.assessment.notification.service.NotificationProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FraudAssessmentConsumer {

    private final NotificationProcessingService processingService;

    @KafkaListener(
            topics = "${fraud.kafka.assessment-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(FraudAssessmentEvent assessment) {
        log.debug(
                "Received assessmentId={} fraudSuspected={} rules={}",
                assessment.assessmentId(),
                assessment.fraudSuspected(),
                assessment.triggeredRuleCount());
        processingService.process(assessment);
    }
}
