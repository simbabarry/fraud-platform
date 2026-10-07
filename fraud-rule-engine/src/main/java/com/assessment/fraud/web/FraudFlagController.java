package com.assessment.fraud.web;

import com.assessment.fraud.domain.RiskLevel;
import com.assessment.fraud.dto.FraudFlagResponse;
import com.assessment.fraud.service.FraudEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fraud-flags")
@Tag(name = "Fraud Flags")
@SecurityRequirement(name = "basicAuth")
@RequiredArgsConstructor
public class FraudFlagController {

    private final FraudEvaluationService fraudEvaluationService;

    @GetMapping
    @Operation(summary = "Search fraud flags with optional filters")
    public Page<FraudFlagResponse> search(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(required = false) RiskLevel riskLevel,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        return fraudEvaluationService.searchFlags(customerId, ruleCode, riskLevel, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a fraud flag by id")
    public FraudFlagResponse get(@PathVariable UUID id) {
        return fraudEvaluationService.getFlag(id);
    }
}
