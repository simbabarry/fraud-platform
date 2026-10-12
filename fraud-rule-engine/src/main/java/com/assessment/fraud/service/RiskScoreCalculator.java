package com.assessment.fraud.service;

import com.assessment.fraud.domain.enums.RiskLevel;
import com.assessment.fraud.domain.RuleEvaluationResult;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RiskScoreCalculator {

    public int contribution(RiskLevel level) {
        return switch (level) {
            case LOW -> 10;
            case MEDIUM -> 25;
            case HIGH -> 50;
            case CRITICAL -> 80;
        };
    }

    public int totalScore(List<RuleEvaluationResult> triggered) {
        return triggered.stream().mapToInt(r -> contribution(r.riskLevel())).sum();
    }

    public RiskLevel overallLevel(int score, List<RuleEvaluationResult> triggered) {
        if (triggered.isEmpty() || score <= 0) {
            return RiskLevel.LOW;
        }
        RiskLevel maxTriggered = triggered.stream()
                .map(RuleEvaluationResult::riskLevel)
                .max(Enum::compareTo)
                .orElse(RiskLevel.LOW);
        RiskLevel fromScore;
        if (score >= 100) {
            fromScore = RiskLevel.CRITICAL;
        } else if (score >= 50) {
            fromScore = RiskLevel.HIGH;
        } else if (score >= 25) {
            fromScore = RiskLevel.MEDIUM;
        } else {
            fromScore = RiskLevel.LOW;
        }
        return maxTriggered.ordinal() > fromScore.ordinal() ? maxTriggered : fromScore;
    }
}
