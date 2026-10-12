package com.assessment.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import jakarta.mail.MessagingException;
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
import org.springframework.dao.DataIntegrityViolationException;
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

    @Test
    void continuesWhenExistingRecordIsStillPending() throws Exception {
        UUID id = UUID.randomUUID();
        ProcessedAssessment pending = new ProcessedAssessment(id, "tx-1");
        pending.setStatus(NotificationStatus.PENDING);
        when(repository.findByAssessmentId(id)).thenReturn(Optional.of(pending));
        when(repository.existsByAssessmentIdAndStatus(id, NotificationStatus.SENT)).thenReturn(false);

        service.process(event(true, id));

        verify(recorder, never()).saveNew(any());
        verify(recorder, never()).markPendingRetry(id);
        verify(emailService).sendFraudAlert(any());
        verify(recorder).markSent(id);
    }

    @Test
    void rejectsMissingAssessmentId() throws Exception {
        assertThatThrownBy(() -> service.process(event(true, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("assessmentId is required");
    }

    @Test
    void skipsWhenConcurrentInsertAlreadySent() throws Exception {
        UUID id = UUID.randomUUID();
        ProcessedAssessment sent = new ProcessedAssessment(id, "tx-1");
        sent.setStatus(NotificationStatus.SENT);
        when(repository.findByAssessmentId(id))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(sent));
        when(recorder.saveNew(any())).thenThrow(new DataIntegrityViolationException("unique"));

        service.process(event(true, id));

        verify(emailService, never()).sendFraudAlert(any());
        verify(recorder, never()).markSent(id);
    }

    @Test
    void continuesWhenConcurrentInsertIsStillPending() throws Exception {
        UUID id = UUID.randomUUID();
        ProcessedAssessment pending = new ProcessedAssessment(id, "tx-1");
        when(repository.findByAssessmentId(id))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(pending));
        when(repository.existsByAssessmentIdAndStatus(id, NotificationStatus.SENT)).thenReturn(false);
        when(recorder.saveNew(any())).thenThrow(new DataIntegrityViolationException("unique"));

        service.process(event(true, id));

        verify(emailService).sendFraudAlert(any());
        verify(recorder).markSent(id);
    }

    @Test
    void rethrowsWhenConcurrentInsertWinnerCannotBeLoaded() throws Exception {
        UUID id = UUID.randomUUID();
        DataIntegrityViolationException conflict = new DataIntegrityViolationException("unique");
        when(repository.findByAssessmentId(id)).thenReturn(Optional.empty());
        when(recorder.saveNew(any())).thenThrow(conflict);

        assertThatThrownBy(() -> service.process(event(true, id))).isSameAs(conflict);

        verify(emailService, never()).sendFraudAlert(any());
    }

    @Test
    void skipsSendWhenRecordBecomesSentAfterCreate() throws Exception {
        UUID id = UUID.randomUUID();
        when(repository.findByAssessmentId(id)).thenReturn(Optional.empty());
        when(recorder.saveNew(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.existsByAssessmentIdAndStatus(id, NotificationStatus.SENT)).thenReturn(true);

        service.process(event(true, id));

        verify(emailService, never()).sendFraudAlert(any());
        verify(recorder, never()).markSent(id);
    }

    @Test
    void wrapsCheckedMessagingExceptionForKafkaRetry() throws Exception {
        UUID id = UUID.randomUUID();
        when(repository.findByAssessmentId(id)).thenReturn(Optional.empty());
        when(repository.existsByAssessmentIdAndStatus(id, NotificationStatus.SENT)).thenReturn(false);
        when(recorder.saveNew(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new MessagingException("mime boom")).when(emailService).sendFraudAlert(any());

        assertThatThrownBy(() -> service.process(event(true, id)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMTP send failed")
                .hasCauseInstanceOf(MessagingException.class);

        verify(recorder).markFailed(eq(id), eq("mime boom"));
        verify(recorder, never()).markSent(id);
    }

    @Test
    void trimsLongSmtpErrorMessages() throws Exception {
        UUID id = UUID.randomUUID();
        String longMessage = "x".repeat(1200);
        when(repository.findByAssessmentId(id)).thenReturn(Optional.empty());
        when(repository.existsByAssessmentIdAndStatus(id, NotificationStatus.SENT)).thenReturn(false);
        when(recorder.saveNew(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new MailSendException(longMessage)).when(emailService).sendFraudAlert(any());

        assertThatThrownBy(() -> service.process(event(true, id))).isInstanceOf(MailSendException.class);

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(recorder).markFailed(eq(id), errorCaptor.capture());
        assertThat(errorCaptor.getValue()).hasSize(1000);
    }

    @Test
    void usesExceptionClassNameWhenSmtpMessageMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(repository.findByAssessmentId(id)).thenReturn(Optional.empty());
        when(repository.existsByAssessmentIdAndStatus(id, NotificationStatus.SENT)).thenReturn(false);
        when(recorder.saveNew(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new MailSendException((String) null)).when(emailService).sendFraudAlert(any());

        assertThatThrownBy(() -> service.process(event(true, id))).isInstanceOf(MailSendException.class);

        verify(recorder).markFailed(eq(id), eq("MailSendException"));
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
