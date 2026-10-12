package com.assessment.fraud.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.assessment.fraud.domain.AuditEvent;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditEventResponseTest {

    @Test
    void fromMapsAllAuditFields() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-12T12:00:00Z");
        AuditEvent event = new AuditEvent();
        event.setId(id);
        event.setEntityType("FraudAssessment");
        event.setEntityId("assessment-1");
        event.setAction("ASSESSED");
        event.setDetails("score=110");
        event.setActor("system");
        event.setCreatedAt(createdAt);

        AuditEventResponse response = AuditEventResponse.from(event);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.entityType()).isEqualTo("FraudAssessment");
        assertThat(response.entityId()).isEqualTo("assessment-1");
        assertThat(response.action()).isEqualTo("ASSESSED");
        assertThat(response.details()).isEqualTo("score=110");
        assertThat(response.actor()).isEqualTo("system");
        assertThat(response.createdAt()).isEqualTo(createdAt);
    }
}
