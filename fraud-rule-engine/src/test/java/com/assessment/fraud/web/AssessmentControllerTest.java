package com.assessment.fraud.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.assessment.fraud.domain.enums.EventSource;
import com.assessment.fraud.domain.enums.RiskLevel;
import com.assessment.fraud.domain.enums.TransactionCategory;
import com.assessment.fraud.domain.enums.TransactionStatus;
import com.assessment.fraud.dto.AuditEventResponse;
import com.assessment.fraud.dto.FraudAssessmentResponse;
import com.assessment.fraud.service.AuditService;
import com.assessment.fraud.service.FraudEvaluationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AssessmentControllerTest {

    @Mock
    private FraudEvaluationService fraudEvaluationService;

    @Mock
    private AuditService auditService;

    private AssessmentController controller;

    @BeforeEach
    void setUp() {
        controller = new AssessmentController(fraudEvaluationService, auditService);
    }

    @Test
    void searchDelegatesToEvaluationService() {
        var pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FraudAssessmentResponse> page = new PageImpl<>(List.of(sampleAssessment()));
        when(fraudEvaluationService.searchAssessments("cust-1", RiskLevel.HIGH, true, pageable))
                .thenReturn(page);

        Page<FraudAssessmentResponse> result =
                controller.search("cust-1", RiskLevel.HIGH, true, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(fraudEvaluationService).searchAssessments("cust-1", RiskLevel.HIGH, true, pageable);
    }

    @Test
    void getReturnsAssessmentById() {
        UUID id = UUID.randomUUID();
        FraudAssessmentResponse assessment = sampleAssessment();
        when(fraudEvaluationService.getAssessment(id)).thenReturn(assessment);

        assertThat(controller.get(id)).isSameAs(assessment);
    }

    @Test
    void auditEnsuresAssessmentExistsThenReturnsHistory() {
        UUID id = UUID.randomUUID();
        List<AuditEventResponse> history = List.of(new AuditEventResponse(
                UUID.randomUUID(),
                "FraudAssessment",
                id.toString(),
                "ASSESSED",
                "ok",
                "system",
                Instant.now()));
        when(fraudEvaluationService.getAssessment(id)).thenReturn(sampleAssessment());
        when(auditService.history("FraudAssessment", id.toString())).thenReturn(history);

        assertThat(controller.audit(id)).isEqualTo(history);
        verify(fraudEvaluationService).getAssessment(id);
        verify(auditService).history("FraudAssessment", id.toString());
    }

    private static FraudAssessmentResponse sampleAssessment() {
        return new FraudAssessmentResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "tx-1",
                "cust-1",
                new BigDecimal("10.00"),
                "USD",
                TransactionCategory.GROCERIES,
                Instant.now(),
                TransactionStatus.ASSESSED,
                EventSource.REST,
                0,
                RiskLevel.LOW,
                false,
                0,
                List.of(),
                Instant.now());
    }
}
