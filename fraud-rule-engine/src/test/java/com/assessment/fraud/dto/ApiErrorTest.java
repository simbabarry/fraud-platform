package com.assessment.fraud.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiErrorTest {

    @Test
    void recordExposesAllFields() {
        Instant now = Instant.parse("2026-10-12T12:00:00Z");
        ApiError error = new ApiError(
                now, 400, "Bad Request", "Validation failed", "/api/v1/transactions/events", List.of("amount: must be positive"));

        assertThat(error.timestamp()).isEqualTo(now);
        assertThat(error.status()).isEqualTo(400);
        assertThat(error.error()).isEqualTo("Bad Request");
        assertThat(error.message()).isEqualTo("Validation failed");
        assertThat(error.path()).isEqualTo("/api/v1/transactions/events");
        assertThat(error.details()).containsExactly("amount: must be positive");
    }
}
