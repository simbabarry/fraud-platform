package com.assessment.fraud.dto;

import com.assessment.fraud.domain.AuditEvent;
import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id, String entityType, String entityId, String action, String details, String actor, Instant createdAt) {

    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEntityType(),
                event.getEntityId(),
                event.getAction(),
                event.getDetails(),
                event.getActor(),
                event.getCreatedAt());
    }
}
