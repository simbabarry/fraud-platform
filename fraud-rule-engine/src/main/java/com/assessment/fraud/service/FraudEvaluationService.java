package com.assessment.fraud.service;

import com.assessment.fraud.config.FraudRuleProperties;
import com.assessment.fraud.domain.enums.EventSource;
import com.assessment.fraud.domain.FraudAssessment;
import com.assessment.fraud.domain.FraudFlag;
import com.assessment.fraud.domain.enums.RiskLevel;
import com.assessment.fraud.domain.RuleEvaluationResult;
import com.assessment.fraud.domain.TransactionEvent;
import com.assessment.fraud.domain.enums.TransactionStatus;
import com.assessment.fraud.dto.BatchTransactionEventRequest;
import com.assessment.fraud.dto.FraudAssessmentResponse;
import com.assessment.fraud.dto.FraudFlagResponse;
import com.assessment.fraud.dto.RuleDefinitionResponse;
import com.assessment.fraud.dto.TransactionEvaluationResponse;
import com.assessment.fraud.dto.TransactionEventRequest;
import com.assessment.fraud.repository.FraudAssessmentRepository;
import com.assessment.fraud.repository.FraudFlagRepository;
import com.assessment.fraud.repository.TransactionEventRepository;
import com.assessment.fraud.rules.FraudRule;
import com.assessment.fraud.rules.RuleEvaluationContext;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudEvaluationService {

    private static final int CONCURRENT_ASSESSMENT_WAIT_ATTEMPTS = 20;
    private static final long CONCURRENT_ASSESSMENT_WAIT_MS = 50L;

    private final TransactionEventRepository transactionEventRepository;
    private final FraudAssessmentRepository fraudAssessmentRepository;
    private final FraudFlagRepository fraudFlagRepository;
    private final List<FraudRule> fraudRules;
    private final FraudRuleProperties properties;
    private final RiskScoreCalculator riskScoreCalculator;
    private final AuditService auditService;
    private final FraudAssessmentPublisher assessmentPublisher;
    private final IdempotentEventInserter idempotentEventInserter;
    private final AssessmentFailureRecorder assessmentFailureRecorder;

    @Transactional
    public TransactionEvaluationResponse ingestAndEvaluate(TransactionEventRequest request) {
        return TransactionEvaluationResponse.of(process(request, EventSource.REST, "rest-api"));
    }

    @Transactional
    public FraudAssessmentResponse processFromKafka(TransactionEventRequest request) {
        return process(request, EventSource.KAFKA, "kafka-consumer");
    }

    @Transactional
    public List<TransactionEvaluationResponse> ingestBatch(BatchTransactionEventRequest request) {
        return request.events().stream().map(this::ingestAndEvaluate).toList();
    }

    /**
     * Idempotent processing: same externalId returns the existing assessment (no re-scoring).
     * Concurrent inserts use a separate transaction so PostgreSQL unique violations do not
     * mark this outer transaction rollback-only.
     */
    private FraudAssessmentResponse process(TransactionEventRequest request, EventSource source, String actor) {
        Optional<FraudAssessment> existing =
                fraudAssessmentRepository.findByTransactionEvent_ExternalId(request.externalId());
        if (existing.isPresent()) {
            auditService.record(
                    "TransactionEvent",
                    request.externalId(),
                    "DUPLICATE_SKIPPED",
                    "Event already assessed – idempotent return",
                    actor);
            return FraudAssessmentResponse.from(existing.get());
        }

        TransactionEvent candidate = toEntity(request, source);
        candidate.setStatus(TransactionStatus.PROCESSING);

        // Catch unique conflicts OUTSIDE the REQUIRES_NEW method so Spring can
        // roll back the short inner transaction before control returns here.
        Optional<TransactionEvent> inserted;
        try {
            inserted = idempotentEventInserter.insertIfAbsent(candidate);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Concurrent insert lost for externalId={}", request.externalId());
            inserted = Optional.empty();
        }
        if (inserted.isEmpty()) {
            return awaitExistingAssessment(request.externalId(), actor);
        }

        TransactionEvent event = inserted.get();
        // Re-attach in this transaction (inserted in REQUIRES_NEW)
        event = transactionEventRepository
                .findById(event.getId())
                .orElseThrow(() -> new IllegalStateException("Inserted event not found"));

        auditService.record(
                "TransactionEvent",
                event.getExternalId(),
                "RECEIVED",
                "Source=" + source + ", status=PROCESSING",
                actor);

        try {
            FraudAssessment assessment = evaluateAndPersist(event, source);
            event.setStatus(TransactionStatus.ASSESSED);
            transactionEventRepository.save(event);

            auditService.record(
                    "FraudAssessment",
                    assessment.getId().toString(),
                    "ASSESSED",
                    "score=%d level=%s rules=%d"
                            .formatted(
                                    assessment.getRiskScore(),
                                    assessment.getOverallRiskLevel(),
                                    assessment.getTriggeredRuleCount()),
                    actor);

            FraudAssessmentResponse response = FraudAssessmentResponse.from(assessment);
            assessmentPublisher.publish(response);
            log.info(
                    "Assessed externalId={} score={} level={} rules={}",
                    event.getExternalId(),
                    assessment.getRiskScore(),
                    assessment.getOverallRiskLevel(),
                    assessment.getTriggeredRuleCount());
            return response;
        } catch (RuntimeException ex) {
            assessmentFailureRecorder.recordFailure(event.getExternalId(), ex.getMessage(), actor);
            throw ex;
        }
    }

    /**
     * Another writer won the insert race — wait briefly for their assessment to commit.
     */
    private FraudAssessmentResponse awaitExistingAssessment(String externalId, String actor) {
        for (int attempt = 0; attempt < CONCURRENT_ASSESSMENT_WAIT_ATTEMPTS; attempt++) {
            Optional<FraudAssessment> assessment =
                    fraudAssessmentRepository.findByTransactionEvent_ExternalId(externalId);
            if (assessment.isPresent()) {
                auditService.record(
                        "TransactionEvent",
                        externalId,
                        "DUPLICATE_SKIPPED",
                        "Concurrent duplicate insert resolved to existing assessment",
                        actor);
                return FraudAssessmentResponse.from(assessment.get());
            }
            try {
                Thread.sleep(CONCURRENT_ASSESSMENT_WAIT_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Transaction already accepted but assessment is not ready yet: " + externalId);
    }

    @Transactional(readOnly = true)
    public Page<FraudAssessmentResponse> searchAssessments(
            String customerId, RiskLevel riskLevel, Boolean fraudSuspected, Pageable pageable) {
        return fraudAssessmentRepository
                .search(emptyToNull(customerId), riskLevel, fraudSuspected, pageable)
                .map(FraudAssessmentResponse::from);
    }

    @Transactional(readOnly = true)
    public FraudAssessmentResponse getAssessment(UUID id) {
        return fraudAssessmentRepository
                .findDetailedById(id)
                .map(FraudAssessmentResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assessment not found"));
    }

    @Transactional(readOnly = true)
    public Page<FraudFlagResponse> searchFlags(
            String customerId, String ruleCode, RiskLevel riskLevel, Pageable pageable) {
        return fraudFlagRepository
                .search(emptyToNull(customerId), emptyToNull(ruleCode), riskLevel, pageable)
                .map(FraudFlagResponse::from);
    }

    @Transactional(readOnly = true)
    public FraudFlagResponse getFlag(UUID id) {
        return fraudFlagRepository
                .findById(id)
                .map(FraudFlagResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fraud flag not found"));
    }

    @Transactional(readOnly = true)
    public List<FraudFlagResponse> getFlagsForTransaction(UUID transactionEventId) {
        if (!transactionEventRepository.existsById(transactionEventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction event not found");
        }
        return fraudFlagRepository.findByTransactionEventIdOrderByCreatedAtDesc(transactionEventId).stream()
                .map(FraudFlagResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RuleDefinitionResponse> listRules() {
        return fraudRules.stream()
                .sorted(Comparator.comparing(FraudRule::code))
                .map(r -> new RuleDefinitionResponse(
                        r.code(), r.name(), r.defaultRiskLevel(), r.description()))
                .toList();
    }

    private FraudAssessment evaluateAndPersist(TransactionEvent event, EventSource source) {
        var context = new RuleEvaluationContext(properties, transactionEventRepository);
        List<RuleEvaluationResult> triggered = new ArrayList<>();
        for (FraudRule rule : fraudRules) {
            RuleEvaluationResult result = rule.evaluate(event, context);
            if (result.triggered()) {
                triggered.add(result);
            }
        }

        int score = riskScoreCalculator.totalScore(triggered);
        RiskLevel overall = riskScoreCalculator.overallLevel(score, triggered);

        FraudAssessment assessment = new FraudAssessment();
        assessment.setTransactionEvent(event);
        assessment.setCustomerId(event.getCustomerId());
        assessment.setRiskScore(score);
        assessment.setOverallRiskLevel(overall);
        assessment.setFraudSuspected(!triggered.isEmpty());
        assessment.setTriggeredRuleCount(triggered.size());
        assessment.setSource(source);
        assessment = fraudAssessmentRepository.save(assessment);

        for (RuleEvaluationResult result : triggered) {
            FraudFlag flag = new FraudFlag();
            flag.setFraudAssessment(assessment);
            flag.setTransactionEvent(event);
            flag.setCustomerId(event.getCustomerId());
            flag.setRuleCode(result.ruleCode());
            flag.setRuleName(result.ruleName());
            flag.setRiskLevel(result.riskLevel());
            flag.setScoreContribution(riskScoreCalculator.contribution(result.riskLevel()));
            flag.setReason(result.reason());
            assessment.getFlags().add(fraudFlagRepository.save(flag));
        }
        return assessment;
    }

    private static TransactionEvent toEntity(TransactionEventRequest request, EventSource source) {
        TransactionEvent event = new TransactionEvent();
        event.setExternalId(request.externalId());
        event.setCustomerId(request.customerId());
        event.setAccountId(request.accountId());
        event.setAmount(request.amount());
        event.setCurrency(request.currency().toUpperCase());
        event.setCategory(request.category());
        event.setMerchantName(request.merchantName());
        event.setMerchantCountry(request.merchantCountry());
        event.setOccurredAt(request.occurredAt());
        event.setSource(source);
        event.setStatus(TransactionStatus.RECEIVED);
        return event;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
