package com.assessment.fraud.dto;

import com.assessment.fraud.domain.TransactionCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionEventRequest(
        @NotBlank @Size(max = 100) String externalId,
        @NotBlank @Size(max = 64) String customerId,
        @NotBlank @Size(max = 64) String accountId,
        @NotNull @DecimalMin(value = "0.01", inclusive = true) BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be ISO-4217 (e.g. USD)")
                String currency,
        @NotNull TransactionCategory category,
        @NotBlank @Size(max = 200) String merchantName,
        @Size(max = 80) String merchantCountry,
        @NotNull Instant occurredAt) {}
