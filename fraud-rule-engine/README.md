# Fraud Rule Engine

Ingests categorized transaction events, evaluates pluggable fraud rules, persists assessments, and publishes results to Kafka for the [notification service](../fraud-notification-service/README.md).

Part of the [fraud-platform](../README.md) workspace. Prefer starting the full stack from the parent:

```bash
cd ..
docker compose up -d --build
```

## Capabilities

- REST + Kafka ingestion of categorized transactions
- Strategy-pattern rule engine (`FraudRule` implementations)
- Risk score → `LOW` / `MEDIUM` / `HIGH` / `CRITICAL`
- Records every triggered rule as a fraud flag
- Idempotent processing on `externalId` (safe under concurrency)
- Secured REST API to search/filter assessments
- Audit trail for receive / assess / duplicate / failure
- Publishes `FraudAssessmentResponse` to `fraud.assessments`

## Stack

| Piece | Detail |
|-------|--------|
| Java 21 / Spring Boot 3.3 | Application runtime |
| PostgreSQL (`fraud_db`) | Own database — host `localhost:5433`, container `fraud-postgres:5432` |
| Kafka (KRaft) | Shared broker from parent Compose |
| Flyway | Schema in `src/main/resources/db/migration` |
| Spring Security | HTTP Basic — `INGEST` / `ANALYST` / `ADMIN` |
| Testcontainers | PostgreSQL concurrency IT |
| OpenAPI | http://localhost:8080/swagger-ui.html |

## API quick reference

| User | Password | Access |
|------|----------|--------|
| `ingest` | `ingest` | `POST /api/v1/transactions/**` |
| `analyst` | `analyst` | `GET /api/v1/**` |
| `admin` | `admin` | All roles |

```bash
# Ingest
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

# Search flagged assessments
curl -u analyst:analyst \
  "http://localhost:8080/api/v1/assessments?fraudSuspected=true&riskLevel=HIGH"

# Search fraud flags (paged)
curl -u analyst:analyst \
  "http://localhost:8080/api/v1/fraud-flags?page=0&size=1&sort=createdAt,desc"
```

### Pagination (Swagger / query params)

List endpoints (`/api/v1/assessments`, `/api/v1/fraud-flags`) expose Spring Data `Pageable` as **flat query parameters** (`page`, `size`, `sort`) — not a nested `pageable` JSON object and not a request body. In Swagger, fill:

```json
{
  "page": 0,
  "size": 1,
  "sort": "createdAt,desc"
}
```

| Field | Meaning | Notes |
|-------|---------|--------|
| `page` | Zero-based page index | Default `0` |
| `size` | Page size | Default `20` |
| `sort` | `property,direction` | One string, e.g. `createdAt,desc`. Default when omitted: `createdAt,desc` |

Do **not** use a JSON array for `sort` (e.g. `["createdAt,desc"]` or `["createdAt","desc"]`), and do not leave Swagger’s placeholder value `string`. Those become invalid property names and the API returns an error.

Equivalent query string:

```text
?page=0&size=1&sort=createdAt,desc
```

## Fraud rules

| Code | Behaviour |
|------|-----------|
| `HIGH_AMOUNT` | Amount exceeds configured threshold |
| `VELOCITY` | Too many transactions in a short window |
| `UNUSUAL_COUNTRY` | Merchant country unusual vs customer history |
| `SPENDING_SPIKE` | Amount spike vs recent average |
| `DUPLICATE_TRANSACTION` | Near-duplicate charge in a short window |
| `NIGHT_HIGH_VALUE` | High value during night hours |
| `RISKY_CATEGORY` | Suspicious merchant category |
| `ROUND_AMOUNT` | Large round amounts |

Score contributions: LOW=10, MEDIUM=25, HIGH=50, CRITICAL=80. Overall level is derived from total score and max triggered severity.

Default high-risk countries: `KP`, `SY`, `IR` (override with `FRAUD_HIGH_RISK_COUNTRIES`).

## Kafka

| Topic | Direction | Payload |
|-------|-----------|---------|
| `transactions.categorized` | In | `TransactionEventRequest` |
| `fraud.assessments` | Out | `FraudAssessmentResponse` |

Publishing uses `whenComplete` so async send failures are logged without blocking the assessment transaction. Prefer an outbox for stronger delivery guarantees in production.

## Idempotency & failure handling

- Inserts run in `REQUIRES_NEW`; unique-key conflicts propagate out so Spring rolls back only the short insert transaction
- Callers catch `DataIntegrityViolationException` and await the winner’s assessment
- Assessment failures are recorded in a separate `REQUIRES_NEW` transaction (`FAILED` status + audit)

## Profiles

| Profile | Use |
|---------|-----|
| default | In-memory H2, Kafka off |
| `production` | Postgres `localhost:5433` + Kafka on localhost |
| `docker` | Parent Compose (`fraud-postgres`, `kafka:29092`) |
| `test` | H2, Kafka off, Flyway off |

## Build

```bash
mvn clean test
mvn -DskipTests package
docker build -t fraud-rule-engine:1.0.0 .
```

`ConcurrentIdempotencyPostgresTest` requires Docker (Testcontainers).
