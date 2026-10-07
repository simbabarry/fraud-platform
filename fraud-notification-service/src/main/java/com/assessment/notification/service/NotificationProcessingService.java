package com.assessment.notification.service;

import com.assessment.notification.domain.NotificationStatus;
import com.assessment.notification.domain.ProcessedAssessment;
import com.assessment.notification.dto.FraudAssessmentEvent;
import com.assessment.notification.repository.ProcessedAssessmentRepository;
import jakarta.mail.MessagingException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessingService {

    private final FraudEmailService emailService;
    private final ProcessedAssessmentRepository processedRepository;
    private final ProcessedAssessmentRecorder recorder;

    /**
     * Idempotent processing keyed by {@code assessmentId}.
     * Status lifecycle: PENDING → SENT | FAILED (FAILED may be retried via Kafka).
     */
    public void process(FraudAssessmentEvent assessment) {
        if (!assessment.fraudSuspected()) {
            log.debug("Ignoring non-fraud assessment {}", assessment.assessmentId());
            return;
        }
        if (assessment.assessmentId() == null) {
            throw new IllegalArgumentException("assessmentId is required");
        }

        Optional<ProcessedAssessment> existing =
                processedRepository.findByAssessmentId(assessment.assessmentId());
        if (existing.isPresent() && existing.get().getStatus() == NotificationStatus.SENT) {
            log.info("Duplicate assessment {} already SENT – skipping email", assessment.assessmentId());
            return;
        }

        try {
            if (existing.isEmpty()) {
                createPending(assessment);
            } else if (existing.get().getStatus() == NotificationStatus.FAILED) {
                recorder.markPendingRetry(assessment.assessmentId());
            }
        } catch (AlreadySentException ex) {
            log.info("Duplicate assessment {} already SENT – skipping email", assessment.assessmentId());
            return;
        }

        // Re-check after create/race
        if (processedRepository.existsByAssessmentIdAndStatus(
                assessment.assessmentId(), NotificationStatus.SENT)) {
            return;
        }

        try {
            emailService.sendFraudAlert(assessment);
            recorder.markSent(assessment.assessmentId());
        } catch (MailException | MessagingException ex) {
            recorder.markFailed(assessment.assessmentId(), trimError(ex));
            // Re-throw so Spring Kafka retries, then dead-letters to fraud.assessments.dlt
            if (ex instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("SMTP send failed for assessment " + assessment.assessmentId(), ex);
        }
    }

    private void createPending(FraudAssessmentEvent assessment) {
        try {
            recorder.saveNew(new ProcessedAssessment(assessment.assessmentId(), assessment.externalId()));
        } catch (DataIntegrityViolationException ex) {
            ProcessedAssessment raced = processedRepository
                    .findByAssessmentId(assessment.assessmentId())
                    .orElseThrow(() -> ex);
            if (raced.getStatus() == NotificationStatus.SENT) {
                throw new AlreadySentException(assessment.assessmentId());
            }
        }
    }

    private static String trimError(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    /** Signals concurrent winner already emailed – swallow in process path. */
    static final class AlreadySentException extends RuntimeException {
        AlreadySentException(java.util.UUID assessmentId) {
            super("Already sent " + assessmentId);
        }
    }
}
