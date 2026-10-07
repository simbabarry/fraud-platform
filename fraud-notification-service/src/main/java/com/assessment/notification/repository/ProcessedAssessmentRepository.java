package com.assessment.notification.repository;

import com.assessment.notification.domain.ProcessedAssessment;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedAssessmentRepository extends JpaRepository<ProcessedAssessment, UUID> {

    boolean existsByAssessmentIdAndStatus(
            UUID assessmentId, com.assessment.notification.domain.NotificationStatus status);

    Optional<ProcessedAssessment> findByAssessmentId(UUID assessmentId);
}
