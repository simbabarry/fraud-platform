package com.assessment.fraud.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.assessment.fraud.domain.enums.EventSource;
import com.assessment.fraud.domain.enums.RiskLevel;
import com.assessment.fraud.domain.enums.TransactionCategory;
import com.assessment.fraud.domain.enums.TransactionStatus;
import com.assessment.fraud.dto.AuditEventResponse;
import com.assessment.fraud.dto.BatchTransactionEventRequest;
import com.assessment.fraud.dto.FraudAssessmentResponse;
import com.assessment.fraud.dto.FraudFlagResponse;
import com.assessment.fraud.dto.TransactionEvaluationResponse;
import com.assessment.fraud.dto.TransactionEventRequest;
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

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private FraudEvaluationService fraudEvaluationService;

    @Mock
    private AuditService auditService;

    private TransactionController controller;

    @BeforeEach
    void setUp() {
        controller = new TransactionController(fraudEvaluationService, auditService);
    }

    @Test
    void ingestDelegatesToEvaluationService() {
        TransactionEventRequest request = sampleEvent("tx-1");
        TransactionEvaluationResponse response = TransactionEvaluationResponse.of(sampleAssessment());
        when(fraudEvaluationService.ingestAndEvaluate(request)).thenReturn(response);

        assertThat(controller.ingest(request)).isSameAs(response);
        verify(fraudEvaluationService).ingestAndEvaluate(request);
    }

    @Test
    void ingestBatchDelegatesToEvaluationService() {
        BatchTransactionEventRequest batch = new BatchTransactionEventRequest(List.of(sampleEvent("tx-1")));
        List<TransactionEvaluationResponse> responses =
                List.of(TransactionEvaluationResponse.of(sampleAssessment()));
        when(fraudEvaluationService.ingestBatch(batch)).thenReturn(responses);

        assertThat(controller.ingestBatch(batch)).isEqualTo(responses);
        verify(fraudEvaluationService).ingestBatch(batch);
    }

    @Test
    void flagsForTransactionDelegatesToEvaluationService() {
        UUID transactionId = UUID.randomUUID();
        List<FraudFlagResponse> flags = List.of(new FraudFlagResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                transactionId,
                "tx-1",
                "cust-1",
                "HIGH_AMOUNT",
                "High amount",
                RiskLevel.HIGH,
                50,
                "too high",
                Instant.now()));
        when(fraudEvaluationService.getFlagsForTransaction(transactionId)).thenReturn(flags);

        assertThat(controller.flagsForTransaction(transactionId)).isEqualTo(flags);
    }

    @Test
    void auditUsesTransactionExternalId() {
        List<AuditEventResponse> history = List.of(new AuditEventResponse(
                UUID.randomUUID(),
                "TransactionEvent",
                "tx-1",
                "RECEIVED",
                "ok",
                "ingest",
                Instant.now()));
        when(auditService.history("TransactionEvent", "tx-1")).thenReturn(history);

        assertThat(controller.audit("tx-1")).isEqualTo(history);
        verify(auditService).history("TransactionEvent", "tx-1");
    }

    private static TransactionEventRequest sampleEvent(String externalId) {
        return new TransactionEventRequest(
                externalId,
                "cust-1",
                "acc-1",
                new BigDecimal("10.00"),
                "USD",
                TransactionCategory.GROCERIES,
                "Shop",
                "US",
                Instant.parse("2026-03-15T10:00:00Z"));
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
