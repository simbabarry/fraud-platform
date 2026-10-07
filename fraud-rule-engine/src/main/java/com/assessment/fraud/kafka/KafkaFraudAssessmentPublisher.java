package com.assessment.fraud.kafka;

import com.assessment.fraud.dto.FraudAssessmentResponse;
import com.assessment.fraud.service.FraudAssessmentPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fraud.kafka.enabled", havingValue = "true")
public class KafkaFraudAssessmentPublisher implements FraudAssessmentPublisher {

    private final KafkaTemplate<String, FraudAssessmentResponse> kafkaTemplate;

    @Value("${fraud.kafka.assessment-topic:fraud.assessments}")
    private String assessmentTopic;

    @Override
    public void publish(FraudAssessmentResponse assessment) {
        kafkaTemplate
                .send(assessmentTopic, assessment.externalId(), assessment)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error(
                                "Publishing assessment failed topic={} key={}",
                                assessmentTopic,
                                assessment.externalId(),
                                exception);
                    } else {
                        log.debug(
                                "Published assessment to {} key={} partition={}",
                                assessmentTopic,
                                assessment.externalId(),
                                result.getRecordMetadata().partition());
                    }
                });
        // Note: for stronger delivery guarantees, prefer a transactional outbox.
    }
}
