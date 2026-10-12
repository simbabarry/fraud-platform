package com.assessment.fraud.web;

import com.assessment.fraud.domain.enums.RiskLevel;
import com.assessment.fraud.dto.AuditEventResponse;
import com.assessment.fraud.dto.FraudAssessmentResponse;
import com.assessment.fraud.service.AuditService;
import com.assessment.fraud.service.FraudEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
@RequestMapping("/api/v1/assessments")
@Tag(name = "Fraud Assessments")
@SecurityRequirement(name = "basicAuth")
@RequiredArgsConstructor
public class AssessmentController {

    private final FraudEvaluationService fraudEvaluationService;
    private final AuditService auditService;

    @GetMapping
    @Operation(
            summary = "Search fraud assessments with filters",
            description =
                    "Pagination uses query params page, size, and sort. "
                            + "Example: page=0&size=1&sort=createdAt,desc. "
                            + "In Swagger, set sort to the single string createdAt,desc (not a JSON array).")
    public Page<FraudAssessmentResponse> search(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) Boolean fraudSuspected,
            @ParameterObject
                    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
                    @Parameter(description = "page / size / sort=createdAt,desc")
                    Pageable pageable) {
        return fraudEvaluationService.searchAssessments(customerId, riskLevel, fraudSuspected, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get assessment by id")
    public FraudAssessmentResponse get(@PathVariable UUID id) {
        return fraudEvaluationService.getAssessment(id);
    }

    @GetMapping("/{id}/audit")
    @Operation(summary = "Audit trail for an assessment")
    public List<AuditEventResponse> audit(@PathVariable UUID id) {
        fraudEvaluationService.getAssessment(id); // ensure exists
        return auditService.history("FraudAssessment", id.toString());
    }
}
