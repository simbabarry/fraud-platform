package com.assessment.fraud.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.assessment.fraud.domain.RiskLevel;
import com.assessment.fraud.domain.RuleEvaluationResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiskScoreCalculatorTest {

    private final RiskScoreCalculator calculator = new RiskScoreCalculator();

    @Test
    void sumsContributionsAndMapsOverallLevel() {
        var triggered = List.of(
                RuleEvaluationResult.fail("A", "A", RiskLevel.MEDIUM, "m"),
                RuleEvaluationResult.fail("B", "B", RiskLevel.HIGH, "h"));
        int score = calculator.totalScore(triggered);
        assertThat(score).isEqualTo(75);
        assertThat(calculator.overallLevel(score, triggered)).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void emptyTriggeredIsLow() {
        assertThat(calculator.overallLevel(0, List.of())).isEqualTo(RiskLevel.LOW);
    }
}
