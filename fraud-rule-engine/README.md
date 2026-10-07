# Fraud Rule Engine Service

Independent microservice in the [fraud-platform](../README.md) workspace.

- Receives **categorized transaction events** via **REST** and **Kafka**
- Runs every applicable **Strategy-pattern** fraud rule
- Calculates a **risk score** and assigns **LOW / MEDIUM / HIGH / CRITICAL**
- Records **which rules were triggered**
- Prevents **duplicate event processing** (idempotent `externalId`)
- Exposes a secured REST API to **retrieve / filter assessments**
- Maintains an **audit trail**
- Publishes assessments to Kafka for **[fraud-notification-service](../fraud-notification-service/README.md)**

## Stack checklist

| Requirement | Implementation |
|-------------|----------------|
| Java 21 + Spring Boot 3 | ✅ |
| Own PostgreSQL database | ✅ `fraud_db` (compose service `fraud-postgres`) |
| Kafka (KRaft) | ✅ shared broker in parent `compose.yaml` |
| Strategy pattern for rules | ✅ `FraudRule` + `@Component` implementations |
| REST API for flagged assessments | ✅ `/api/v1/assessments`, `/api/v1/fraud-flags` |
| Flyway migrations | ✅ `db/migration/V1__init_schema.sql` |
| Spring Security (roles) | ✅ `INGEST` / `ANALYST` / `ADMIN` (HTTP Basic) |
| Testcontainers / JUnit 5 / Mockito | ✅ unit/IT + PostgreSQL concurrency IT |
| Docker image | ✅ `Dockerfile` |
| OpenAPI / Swagger | ✅ `/swagger-ui.html` |

## Quick start

Infrastructure lives in the **parent** workspace:

```bash
cd C:\fraud-platform
docker compose up -d --build
```

| Service | URL / port |
|---------|------------|
| API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| Fraud Postgres (host) | `localhost:5433` → `fraud-postgres:5432` |
| Kafka | `localhost:9092` · `kafka:29092` |

### Demo users (HTTP Basic)

| User | Password | Roles |
|------|----------|-------|
| `ingest` | `ingest` | INGEST (POST events) |
| `analyst` | `analyst` | ANALYST (GET assessments) |
| `admin` | `admin` | ADMIN + all |

### Example ingest

```bash
curl -u ingest:ingest -X POST http://localhost:8080/api/v1/transactions/events \
  -H "Content-Type: application/json" \
  -d '{
    "externalId": "tx-1001",
    "customerId": "cust-42",
    "accountId": "acc-7",
    "amount": 9200.00,
    "currency": "USD",
    "category": "ONLINE_PURCHASE",
    "merchantName": "Global Gadgets",
    "merchantCountry": "US",
    "occurredAt": "2026-03-15T10:30:00Z"
  }'
```

### Example search assessments

```bash
curl -u analyst:analyst "http://localhost:8080/api/v1/assessments?fraudSuspected=true&riskLevel=HIGH"
```

## Kafka topics

| Topic | Direction | Payload |
|-------|-----------|---------|
| `transactions.categorized` | In | `TransactionEventRequest` JSON |
| `fraud.assessments` | Out → notification service | `FraudAssessmentResponse` JSON |

## Fraud rules (Strategy)

| Code | Maps to requirement |
|------|---------------------|
| `HIGH_AMOUNT` | Exceeds configurable amount |
| `VELOCITY` | Several txs in a short period |
| `UNUSUAL_COUNTRY` | Unusual country |
| `SPENDING_SPIKE` | Sudden spend vs normal pattern |
| `DUPLICATE_TRANSACTION` | Duplicate / near-duplicate charge |
| `NIGHT_HIGH_VALUE` + `RISKY_CATEGORY` | Suspicious time / merchant category |
| `ROUND_AMOUNT` | Extra heuristic |

Risk score contributions: LOW=10, MEDIUM=25, HIGH=50, CRITICAL=80.

## Profiles

| Profile | Use |
|---------|-----|
| default | In-memory H2, Kafka off |
| `production` | Postgres `localhost:5433` + Kafka |
| `docker` | Inside parent compose (`fraud-postgres`) |
| `test` | H2, Kafka off, Flyway off |

## Build / test / image

```bash
# from this module
mvn clean test
mvn -DskipTests package
docker build -t fraud-rule-engine:1.0.0 .

# or from parent aggregator
cd C:\fraud-platform
mvn clean test
```

PostgreSQL Testcontainers IT (`ConcurrentIdempotencyPostgresTest`) requires Docker.
