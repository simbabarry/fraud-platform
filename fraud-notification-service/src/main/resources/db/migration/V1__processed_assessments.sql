CREATE TABLE processed_assessments (
    id UUID PRIMARY KEY,
    assessment_id UUID NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    external_id VARCHAR(100),
    last_error VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_processed_status ON processed_assessments (status);
CREATE INDEX idx_processed_external ON processed_assessments (external_id);
