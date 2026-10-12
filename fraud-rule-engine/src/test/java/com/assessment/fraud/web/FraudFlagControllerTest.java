package com.assessment.fraud.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.assessment.fraud.domain.enums.RiskLevel;
import com.assessment.fraud.dto.FraudFlagResponse;
import com.assessment.fraud.service.FraudEvaluationService;
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
class FraudFlagControllerTest {

    @Mock
    private FraudEvaluationService fraudEvaluationService;

    private FraudFlagController controller;

    @BeforeEach
    void setUp() {
        controller = new FraudFlagController(fraudEvaluationService);
    }

    @Test
    void searchDelegatesToEvaluationService() {
        var pageable = PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "createdAt"));
        FraudFlagResponse flag = sampleFlag();
        Page<FraudFlagResponse> page = new PageImpl<>(List.of(flag));
        when(fraudEvaluationService.searchFlags("cust-1", "HIGH_AMOUNT", RiskLevel.HIGH, pageable))
                .thenReturn(page);

        Page<FraudFlagResponse> result =
                controller.search("cust-1", "HIGH_AMOUNT", RiskLevel.HIGH, pageable);

        assertThat(result.getContent()).containsExactly(flag);
        verify(fraudEvaluationService).searchFlags("cust-1", "HIGH_AMOUNT", RiskLevel.HIGH, pageable);
    }

    @Test
    void getReturnsFlagById() {
        UUID id = UUID.randomUUID();
        FraudFlagResponse flag = sampleFlag();
        when(fraudEvaluationService.getFlag(id)).thenReturn(flag);

        assertThat(controller.get(id)).isSameAs(flag);
    }

    private static FraudFlagResponse sampleFlag() {
        return new FraudFlagResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "tx-1",
                "cust-1",
                "HIGH_AMOUNT",
                "High amount",
                RiskLevel.HIGH,
                50,
                "too high",
                Instant.now());
    }
}
