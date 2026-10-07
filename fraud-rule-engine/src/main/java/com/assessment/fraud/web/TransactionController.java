package com.assessment.fraud.web;

import com.assessment.fraud.dto.AuditEventResponse;
import com.assessment.fraud.dto.BatchTransactionEventRequest;
import com.assessment.fraud.dto.FraudFlagResponse;
import com.assessment.fraud.dto.TransactionEvaluationResponse;
import com.assessment.fraud.dto.TransactionEventRequest;
import com.assessment.fraud.service.AuditService;
import com.assessment.fraud.service.FraudEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions")
@SecurityRequirement(name = "basicAuth")
@RequiredArgsConstructor
public class TransactionController {

    private final FraudEvaluationService fraudEvaluationService;
    private final AuditService auditService;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ingest a categorized transaction event (REST) and evaluate fraud rules")
    public TransactionEvaluationResponse ingest(@Valid @RequestBody TransactionEventRequest request) {
        return fraudEvaluationService.ingestAndEvaluate(request);
    }

    @PostMapping("/events/batch")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ingest a batch of categorized transaction events")
    public List<TransactionEvaluationResponse> ingestBatch(
            @Valid @RequestBody BatchTransactionEventRequest request) {
        return fraudEvaluationService.ingestBatch(request);
    }

    @GetMapping("/{transactionEventId}/fraud-flags")
    @Operation(summary = "List fraud flags for a transaction event")
    public List<FraudFlagResponse> flagsForTransaction(@PathVariable UUID transactionEventId) {
        return fraudEvaluationService.getFlagsForTransaction(transactionEventId);
    }

    @GetMapping("/by-external/{externalId}/audit")
    @Operation(summary = "Audit trail for a transaction external id")
    public List<AuditEventResponse> audit(@PathVariable String externalId) {
        return auditService.history("TransactionEvent", externalId);
    }
}
