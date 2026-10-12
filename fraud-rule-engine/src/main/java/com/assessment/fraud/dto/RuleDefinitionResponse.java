package com.assessment.fraud.dto;

import com.assessment.fraud.domain.enums.RiskLevel;

public record RuleDefinitionResponse(
        String ruleCode, String ruleName, RiskLevel defaultRiskLevel, String description) {}
