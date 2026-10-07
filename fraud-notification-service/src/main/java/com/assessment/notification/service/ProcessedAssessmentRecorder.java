package com.assessment.notification.service;

import com.assessment.notification.domain.NotificationStatus;
import com.assessment.notification.domain.ProcessedAssessment;
import com.assessment.notification.repository.ProcessedAssessmentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists notification status in independent transactions so Kafka-retry
 * exceptions do not roll back SENT/FAILED markers.
 */
@Service
@RequiredArgsConstructor
public class ProcessedAssessmentRecorder {

    private final ProcessedAssessmentRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessedAssessment saveNew(ProcessedAssessment assessment) {
        return repository.saveAndFlush(assessment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(UUID assessmentId) {
        repository.findByAssessmentId(assessmentId).ifPresent(record -> {
            record.setStatus(NotificationStatus.SENT);
            record.setLastError(null);
            repository.saveAndFlush(record);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID assessmentId, String error) {
        repository.findByAssessmentId(assessmentId).ifPresent(record -> {
            record.setStatus(NotificationStatus.FAILED);
            record.setLastError(error);
            repository.saveAndFlush(record);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPendingRetry(UUID assessmentId) {
        repository.findByAssessmentId(assessmentId).ifPresent(record -> {
            record.setStatus(NotificationStatus.PENDING);
            record.setLastError(null);
            repository.saveAndFlush(record);
        });
    }
}
