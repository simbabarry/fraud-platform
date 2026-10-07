package com.assessment.fraud.service;

import com.assessment.fraud.dto.FraudAssessmentResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "fraud.kafka.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpFraudAssessmentPublisher implements FraudAssessmentPublisher {

    @Override
    public void publish(FraudAssessmentResponse assessment) {
        // Kafka disabled (local/test profiles)
    }
}
