package com.assessment.fraud.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BatchTransactionEventRequest(
        @NotEmpty @Size(max = 100) List<@Valid TransactionEventRequest> events) {}
