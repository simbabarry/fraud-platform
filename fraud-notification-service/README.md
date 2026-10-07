# Fraud Notification Service

Independent microservice in the [fraud-platform](../README.md) workspace.

Consumes `FraudAssessmentResponse` events from Kafka topic `fraud.assessments` and sends **one email per fraudulent assessment**, aggregating every entry in `triggeredRules`.

## Responsibilities

- Ignore assessments where `fraudSuspected == false`
- Idempotency key: `assessmentId` (statuses `PENDING` / `SENT` / `FAILED`)
- One email containing **all** triggered rules (not one email per rule)
- SMTP retries (`@Retryable`) + Kafka retries → DLT `fraud.assessments.dlt`
- Own database: `notification_db` on `notification-postgres`
- Alert recipient: `FRAUD_ALERT_RECIPIENT` / `fraud.notification.recipient` (default `fraud-team@example.com`)

## Stack

- Java 21 / Spring Boot 3.3
- Spring Kafka (consumer + dead-letter recoverer)
- Spring Mail → Mailpit
- PostgreSQL + Flyway (`processed_assessments`)
- Port **8081**

## Configuration

| Property | Default |
|----------|---------|
| `fraud.kafka.assessment-topic` | `fraud.assessments` |
| `fraud.kafka.assessment-dlt-topic` | `fraud.assessments.dlt` |
| `fraud.notification.recipient` | `fraud-team@example.com` |
| Datasource (local) | `jdbc:postgresql://localhost:5434/notification_db` |

## Run with the platform

```bash
cd C:\fraud-platform
docker compose up -d --build
```

| URL | Purpose |
|-----|---------|
| http://localhost:8081/actuator/health | Health |
| http://localhost:8025 | Mailpit UI |
| `localhost:5434` | Notification Postgres |

## Local Maven (infra via parent Compose)

```bash
cd C:\fraud-platform
docker compose up -d notification-postgres kafka mailpit
cd fraud-notification-service
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

## Tests

```bash
mvn test
```

## Example email body

```text
Subject: HIGH-risk transaction detected

Transaction: tx-1001
Customer: cust-42
Amount: USD 9,200
Risk score: 60
Overall risk: HIGH

Triggered rules:
- HIGH_AMOUNT: Amount exceeds the configured threshold
- ROUND_AMOUNT: Large round amount detected
```
