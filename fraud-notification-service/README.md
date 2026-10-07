# Fraud Notification Service

Consumes fraud assessment events from Kafka and sends **one email per fraudulent assessment**, aggregating every triggered rule into a single alert.

Part of the [fraud-platform](../README.md) workspace. Prefer starting the full stack from the parent:

```bash
cd ..
docker compose up -d --build
```

Then open Mailpit at http://localhost:8025.

## Responsibilities

| Concern | Behaviour |
|---------|-----------|
| Filtering | Ignore events where `fraudSuspected == false` |
| Aggregation | One email listing all `triggeredRules` |
| Idempotency | Keyed by `assessmentId` — statuses `PENDING` → `SENT` / `FAILED` |
| SMTP retries | Spring `@Retryable` for transient mail failures |
| Kafka retries | `DefaultErrorHandler` + backoff, then DLT |
| Dead letter | Topic `fraud.assessments.dlt` |
| Recipient | Single ops address — no customer emails required |

## Stack

| Piece | Detail |
|-------|--------|
| Java 21 / Spring Boot 3.3 | Application runtime |
| PostgreSQL (`notification_db`) | Own database — host `localhost:5434`, container `notification-postgres:5432` |
| Spring Kafka | Consumer group `fraud-notification-service` |
| Spring Mail | SMTP to Mailpit in Compose |
| Flyway | `processed_assessments` table |
| Port | `8081` |

## Configuration

| Property / env | Default |
|----------------|---------|
| `fraud.kafka.assessment-topic` | `fraud.assessments` |
| `fraud.kafka.assessment-dlt-topic` | `fraud.assessments.dlt` |
| `fraud.notification.recipient` / `FRAUD_ALERT_RECIPIENT` | `fraud-team@example.com` |
| `fraud.notification.alert-from` / `FRAUD_ALERT_FROM` | `fraud-alerts@example.com` |
| Datasource (local defaults) | `jdbc:postgresql://localhost:5434/notification_db` |

Override the recipient in Compose or the environment:

```bash
FRAUD_ALERT_RECIPIENT=fraud-team@example.com
```

## Flow

1. Rule engine publishes `FraudAssessmentResponse` to `fraud.assessments`
2. This service deserializes the event (unknown fields ignored)
3. Non-fraud events return immediately
4. If `assessmentId` is already `SENT`, skip
5. Send one MIME email; mark `SENT` (or `FAILED` and rethrow for Kafka retry)
6. After Kafka retries are exhausted, the record is published to `fraud.assessments.dlt`

## Example email

```text
Subject: HIGH-risk transaction detected

Transaction: tx-1001
Customer: cust-42
Amount: USD 9,200
Risk score: 60
Overall risk: HIGH

Triggered rules:
- HIGH_AMOUNT: Amount 9200.0000 USD exceeds threshold 5000
- ROUND_AMOUNT: Large round amount detected: 9200.0000 USD
```

## Local development

```bash
# infrastructure only
cd ..
docker compose up -d notification-postgres kafka mailpit

cd fraud-notification-service
mvn spring-boot:run -Dspring-boot.run.profiles=docker
```

| URL | Purpose |
|-----|---------|
| http://localhost:8081/actuator/health | Health |
| http://localhost:8025 | Mailpit UI |
| `localhost:5434` | Notification Postgres |

## Build & test

```bash
mvn clean test
mvn -DskipTests package
docker build -t fraud-notification-service:1.0.0 .
```
