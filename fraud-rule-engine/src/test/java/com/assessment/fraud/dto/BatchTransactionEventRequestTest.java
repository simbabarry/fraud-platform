package com.assessment.fraud.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.assessment.fraud.domain.enums.TransactionCategory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BatchTransactionEventRequestTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsNonEmptyBatchWithinLimit() {
        BatchTransactionEventRequest request = new BatchTransactionEventRequest(List.of(sampleEvent("tx-1")));

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.events()).hasSize(1);
    }

    @Test
    void rejectsEmptyBatch() {
        BatchTransactionEventRequest request = new BatchTransactionEventRequest(Collections.emptyList());

        Set<ConstraintViolation<BatchTransactionEventRequest>> violations = validator.validate(request);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("events"));
    }

    @Test
    void rejectsBatchLargerThan100() {
        List<TransactionEventRequest> events = IntStream.rangeClosed(1, 101)
                .mapToObj(i -> sampleEvent("tx-" + i))
                .toList();

        Set<ConstraintViolation<BatchTransactionEventRequest>> violations =
                validator.validate(new BatchTransactionEventRequest(events));

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().toLowerCase().contains("size")
                || v.getPropertyPath().toString().equals("events"));
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
}
