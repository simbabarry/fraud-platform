package com.assessment.fraud.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fraud_assessments")
public class FraudAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_event_id", nullable = false, unique = true)
    private TransactionEvent transactionEvent;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_risk_level", nullable = false, length = 20)
    private RiskLevel overallRiskLevel;

    @Column(name = "fraud_suspected", nullable = false)
    private boolean fraudSuspected;

    @Column(name = "triggered_rule_count", nullable = false)
    private int triggeredRuleCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventSource source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "fraudAssessment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FraudFlag> flags = new ArrayList<>();
}
