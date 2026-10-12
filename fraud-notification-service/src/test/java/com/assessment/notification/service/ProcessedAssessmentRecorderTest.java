package com.assessment.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.assessment.notification.domain.NotificationStatus;
import com.assessment.notification.domain.ProcessedAssessment;
import com.assessment.notification.repository.ProcessedAssessmentRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessedAssessmentRecorderTest {

    @Mock
    private ProcessedAssessmentRepository repository;

    private ProcessedAssessmentRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new ProcessedAssessmentRecorder(repository);
    }

    @Test
    void saveNewFlushesEntity() {
        ProcessedAssessment pending = new ProcessedAssessment(UUID.randomUUID(), "tx-1");
        when(repository.saveAndFlush(pending)).thenReturn(pending);

        assertThat(recorder.saveNew(pending)).isSameAs(pending);
        verify(repository).saveAndFlush(pending);
    }

    @Test
    void markSentUpdatesStatusAndClearsError() {
        UUID id = UUID.randomUUID();
        ProcessedAssessment record = new ProcessedAssessment(id, "tx-1");
        record.setStatus(NotificationStatus.PENDING);
        record.setLastError("previous");
        when(repository.findByAssessmentId(id)).thenReturn(Optional.of(record));
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        recorder.markSent(id);

        ArgumentCaptor<ProcessedAssessment> captor = ArgumentCaptor.forClass(ProcessedAssessment.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(captor.getValue().getLastError()).isNull();
    }

    @Test
    void markFailedStoresError() {
        UUID id = UUID.randomUUID();
        ProcessedAssessment record = new ProcessedAssessment(id, "tx-1");
        when(repository.findByAssessmentId(id)).thenReturn(Optional.of(record));
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        recorder.markFailed(id, "smtp down");

        ArgumentCaptor<ProcessedAssessment> captor = ArgumentCaptor.forClass(ProcessedAssessment.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(captor.getValue().getLastError()).isEqualTo("smtp down");
    }

    @Test
    void markPendingRetryResetsFailedRecord() {
        UUID id = UUID.randomUUID();
        ProcessedAssessment record = new ProcessedAssessment(id, "tx-1");
        record.setStatus(NotificationStatus.FAILED);
        record.setLastError("smtp down");
        when(repository.findByAssessmentId(id)).thenReturn(Optional.of(record));
        when(repository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        recorder.markPendingRetry(id);

        ArgumentCaptor<ProcessedAssessment> captor = ArgumentCaptor.forClass(ProcessedAssessment.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(captor.getValue().getLastError()).isNull();
    }

    @Test
    void statusUpdatesNoOpWhenAssessmentMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findByAssessmentId(id)).thenReturn(Optional.empty());

        recorder.markSent(id);
        recorder.markFailed(id, "err");
        recorder.markPendingRetry(id);

        verify(repository, never()).saveAndFlush(any());
    }
}
