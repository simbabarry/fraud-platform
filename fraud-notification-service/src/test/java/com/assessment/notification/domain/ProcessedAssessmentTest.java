package com.assessment.notification.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessedAssessmentTest {

    @Test
    void constructorInitializesPendingRecord() {
        UUID assessmentId = UUID.randomUUID();
        ProcessedAssessment record = new ProcessedAssessment(assessmentId, "tx-9");

        assertThat(record.getId()).isNotNull();
        assertThat(record.getAssessmentId()).isEqualTo(assessmentId);
        assertThat(record.getExternalId()).isEqualTo("tx-9");
        assertThat(record.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void onCreateFillsTimestampsAndDefaults() {
        ProcessedAssessment record = new ProcessedAssessment();
        record.setAssessmentId(UUID.randomUUID());

        record.onCreate();

        assertThat(record.getId()).isNotNull();
        assertThat(record.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(record.getCreatedAt()).isNotNull();
        assertThat(record.getUpdatedAt()).isEqualTo(record.getCreatedAt());
    }

    @Test
    void onCreateKeepsExistingIdAndStatus() {
        UUID id = UUID.randomUUID();
        ProcessedAssessment record = new ProcessedAssessment();
        record.setId(id);
        record.setAssessmentId(UUID.randomUUID());
        record.setStatus(NotificationStatus.FAILED);

        record.onCreate();

        assertThat(record.getId()).isEqualTo(id);
        assertThat(record.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(record.getCreatedAt()).isNotNull();
    }

    @Test
    void onUpdateRefreshesUpdatedAt() {
        ProcessedAssessment record = new ProcessedAssessment(UUID.randomUUID(), "tx-1");
        record.onCreate();
        var createdAt = record.getCreatedAt();

        record.onUpdate();

        assertThat(record.getUpdatedAt()).isAfterOrEqualTo(createdAt);
    }
}
