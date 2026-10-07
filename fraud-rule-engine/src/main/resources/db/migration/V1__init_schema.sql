CREATE TABLE transaction_events (
    id UUID PRIMARY KEY,
    external_id VARCHAR(100) NOT NULL UNIQUE,
    customer_id VARCHAR(64) NOT NULL,
    account_id VARCHAR(64) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    category VARCHAR(40) NOT NULL,
    merchant_name VARCHAR(200) NOT NULL,
    merchant_country VARCHAR(80),
    occurred_at TIMESTAMPTZ NOT NULL,
    ingested_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(40) NOT NULL,
    source VARCHAR(40) NOT NULL
);

CREATE INDEX idx_tx_customer ON transaction_events (customer_id);
CREATE INDEX idx_tx_occurred ON transaction_events (occurred_at);
CREATE INDEX idx_tx_status ON transaction_events (status);

CREATE TABLE fraud_assessments (
    id UUID PRIMARY KEY,
    transaction_event_id UUID NOT NULL UNIQUE REFERENCES transaction_events (id),
    customer_id VARCHAR(64) NOT NULL,
    risk_score INTEGER NOT NULL,
    overall_risk_level VARCHAR(20) NOT NULL,
    fraud_suspected BOOLEAN NOT NULL,
    triggered_rule_count INTEGER NOT NULL,
    source VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_assessment_customer ON fraud_assessments (customer_id);
CREATE INDEX idx_assessment_risk ON fraud_assessments (overall_risk_level);
CREATE INDEX idx_assessment_created ON fraud_assessments (created_at);

CREATE TABLE fraud_flags (
    id UUID PRIMARY KEY,
    fraud_assessment_id UUID NOT NULL REFERENCES fraud_assessments (id),
    transaction_event_id UUID NOT NULL REFERENCES transaction_events (id),
    customer_id VARCHAR(64) NOT NULL,
    rule_code VARCHAR(80) NOT NULL,
    rule_name VARCHAR(200) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    score_contribution INTEGER NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_flag_customer ON fraud_flags (customer_id);
CREATE INDEX idx_flag_rule ON fraud_flags (rule_code);
CREATE INDEX idx_flag_assessment ON fraud_flags (fraud_assessment_id);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    action VARCHAR(80) NOT NULL,
    details VARCHAR(2000),
    actor VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_entity ON audit_events (entity_type, entity_id);
CREATE INDEX idx_audit_created ON audit_events (created_at);
