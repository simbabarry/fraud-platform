package com.assessment.fraud.service;

import com.assessment.fraud.domain.TransactionStatus;
import com.assessment.fraud.repository.TransactionEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists FAILED status + audit in a separate transaction so they survive
 * when the main assessment transaction rolls back.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentFailureRecorder {

    private final TransactionEventRepository transactionEventRepository;
    private final AuditService auditService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String externalId, String message, String actor) {
        transactionEventRepository
                .findByExternalId(externalId)
                .ifPresent(event -> {
                    event.setStatus(TransactionStatus.FAILED);
                    transactionEventRepository.save(event);
                });
        auditService.record(
                "TransactionEvent",
                externalId,
                "FAILED",
                message == null ? "Processing failed" : message,
                actor);
        log.warn("Recorded FAILED status for externalId={}: {}", externalId, message);
    }
}
