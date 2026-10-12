package com.assessment.fraud.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.assessment.fraud.domain.enums.TransactionCategory;
import com.assessment.fraud.dto.FraudAssessmentResponse;
import com.assessment.fraud.dto.TransactionEventRequest;
import com.assessment.fraud.repository.FraudAssessmentRepository;
import com.assessment.fraud.repository.TransactionEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PostgreSQL Testcontainers IT: concurrent duplicate externalId must yield one
 * transaction row and one assessment (idempotent).
 */
@SpringBootTest
@Testcontainers
class ConcurrentIdempotencyPostgresTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("fraud_db")
            .withUsername("fraud_user")
            .withPassword("fraud_password");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("fraud.kafka.enabled", () -> "false");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @Autowired
    private FraudEvaluationService fraudEvaluationService;

    @Autowired
    private TransactionEventRepository transactionEventRepository;

    @Autowired
    private FraudAssessmentRepository fraudAssessmentRepository;

    @Test
    void concurrentSameExternalIdProducesSingleAssessment() throws Exception {
        String externalId = "concurrent-" + UUID.randomUUID();
        TransactionEventRequest request = new TransactionEventRequest(
                externalId,
                "cust-concurrent",
                "acc-1",
                new BigDecimal("42.00"),
                "USD",
                TransactionCategory.GROCERIES,
                "Corner Shop",
                "ZA",
                Instant.parse("2026-06-01T10:00:00Z"));

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<FraudAssessmentResponse>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                if (!start.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for start signal");
                }
                return fraudEvaluationService.ingestAndEvaluate(request).assessment();
            }));
        }

        assertThat(ready.await(15, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<FraudAssessmentResponse> results = new ArrayList<>();
        for (Future<FraudAssessmentResponse> future : futures) {
            results.add(future.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        UUID assessmentId = results.getFirst().assessmentId();
        assertThat(results).allSatisfy(r -> assertThat(r.assessmentId()).isEqualTo(assessmentId));
        assertThat(results).allSatisfy(r -> assertThat(r.externalId()).isEqualTo(externalId));

        assertThat(transactionEventRepository.findByExternalId(externalId)).isPresent();
        assertThat(fraudAssessmentRepository.findByTransactionEvent_ExternalId(externalId)).isPresent();
        assertThat(transactionEventRepository.count()).isEqualTo(1);
        assertThat(fraudAssessmentRepository.count()).isEqualTo(1);
    }
}
