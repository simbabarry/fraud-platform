package com.assessment.fraud.service;

import com.assessment.fraud.dto.FraudAssessmentResponse;

public interface FraudAssessmentPublisher {

    void publish(FraudAssessmentResponse assessment);
}
