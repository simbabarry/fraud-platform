package com.assessment.fraud.web;

import com.assessment.fraud.dto.RuleDefinitionResponse;
import com.assessment.fraud.service.FraudEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules")
@Tag(name = "Rules")
@SecurityRequirement(name = "basicAuth")
@RequiredArgsConstructor
public class RuleController {

    private final FraudEvaluationService fraudEvaluationService;

    @GetMapping
    @Operation(summary = "List registered fraud rules (Strategy pattern)")
    public List<RuleDefinitionResponse> list() {
        return fraudEvaluationService.listRules();
    }
}
