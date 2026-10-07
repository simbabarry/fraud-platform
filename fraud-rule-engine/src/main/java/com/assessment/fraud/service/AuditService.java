package com.assessment.fraud.service;

import com.assessment.fraud.domain.AuditEvent;
import com.assessment.fraud.dto.AuditEventResponse;
import com.assessment.fraud.repository.AuditEventRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    @Transactional
    public void record(String entityType, String entityId, String action, String details, String actor) {
        AuditEvent event = new AuditEvent();
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setAction(action);
        event.setDetails(details);
        event.setActor(actor == null || actor.isBlank() ? "system" : actor);
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> history(String entityType, String entityId) {
        return auditEventRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId).stream()
                .map(AuditEventResponse::from)
                .toList();
    }
}
