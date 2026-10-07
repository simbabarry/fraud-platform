package com.assessment.fraud.rules;

import com.assessment.fraud.config.FraudRuleProperties;
import com.assessment.fraud.repository.TransactionEventRepository;

public record RuleEvaluationContext(
        FraudRuleProperties properties, TransactionEventRepository transactionEventRepository) {}
