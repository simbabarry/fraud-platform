package com.assessment.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.assessment.notification.domain.NotificationStatus;
import com.assessment.notification.domain.ProcessedAssessment;
import com.assessment.notification.dto.FraudAssessmentEvent;
import com.assessment.notification.dto.TriggeredRuleEvent;
import com.assessment.notification.repository.ProcessedAssessmentRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

@ExtendWith(MockitoExtension.class)
class NotificationProcessingServiceTest {

    @Mock
    private FraudEmailService emailService;

    @Mock
    private ProcessedAssessmentRepository repository;

    @Mock
    private ProcessedAssessmentRecorder recorder;

    private NotificationProcessingService service;

    @BeforeEach
    void setUp() {
        service = new NotificationProcessingService(emailService, repository, recorder);
    }

    @Test
    void ignoresNonFraudAssessments() throws Exception {
        service.process(event(false, UUID.randomUUID()));
        verify(emailService, never()).sendFraudAlert(any());
        verify(recorder, never()).saveNew(any());
    }

    @Test
    void skipsWhenAlreadySent() throws Exception {
        UUID id = UUID.randomUUID();
        ProcessedAssessment sent = new ProcessedAssessment(id, "tx-1");
        sent.setStatus(NotificationStatus.SENT);
        when(repository.findByAssessmentId(id)).thenReturn(Optional.of(sent));

        service.process(event(true, id));

        verify(emailService, never()).sendFraudAlert(any());
    }

    @Test
    void sendsAndMarksSentForNewFraudAssessment() throws Exception {
        UUID id = UUID.randomUUID();
        when(repository.findByAssessmentId(id)).thenReturn(Optional.empty());
        when(repository.existsByAssessmentIdAndStatus(id, NotificationStatus.SENT)).thenReturn(false);
        when(recorder.saveNew(any())).thenAnswer(inv -> inv.getArgument(0));

        service.process(event(true, id));

        verify(emailService).sendFraudAlert(any());
        verify(recorder).markSent(id);
        ArgumentCaptor<ProcessedAssessment> captor = ArgumentCaptor.forClass(ProcessedAssessment.class);
        verify(recorder).saveNew(captor.capture());
        assertThat(captor.getValue().getAssessmentId()).isEqualTo(id);
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void marksFailedAndRethrowsOnSmtpError() throws Exception {
        UUID id = UUID.randomUUID();
        when(repository.findByAssessmentId(id)).thenReturn(Optional.empty());
        when(repository.existsByAssessmentIdAndStatus(id, NotificationStatus.SENT)).thenReturn(false);
        when(recorder.saveNew(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new MailSendException("smtp down")).when(emailService).sendFraudAlert(any());

        org.junit.jupiter.api.Assertions.assertThrows(
                MailSendException.class, () -> service.process(event(true, id)));

        verify(recorder).markFailed(id, "smtp down");
        verify(recorder, never()).markSent(id);
    }

    @Test
    void retriesFailedAssessment() throws Exception {
        UUID id = UUID.randomUUID();
        ProcessedAssessment failed = new ProcessedAssessment(id, "tx-1");
        failed.setStatus(NotificationStatus.FAILED);
        when(repository.findByAssessmentId(id)).thenReturn(Optional.of(failed));
        when(repository.existsByAssessmentIdAndStatus(id, NotificationStatus.SENT)).thenReturn(false);

        service.process(event(true, id));

        verify(recorder).markPendingRetry(id);
        verify(emailService, times(1)).sendFraudAlert(any());
        verify(recorder).markSent(id);
    }

    private static FraudAssessmentEvent event(boolean fraud, UUID assessmentId) {
        return new FraudAssessmentEvent(
                assessmentId,
                UUID.randomUUID(),
                "tx-1001",
                "cust-42",
                new BigDecimal("9200.00"),
                "USD",
                "ONLINE_PURCHASE",
                Instant.now(),
                110,
                "CRITICAL",
                fraud,
                1,
                List.of(new TriggeredRuleEvent("HIGH_AMOUNT", "High", "HIGH", "too high", 50)),
                Instant.now());
    }
}
