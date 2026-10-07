package com.assessment.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TriggeredRuleEvent(
        String ruleCode, String ruleName, String riskLevel, String reason, int scoreContribution) {}
