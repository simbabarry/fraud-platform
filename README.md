# Fraud Platform

Java 21 / Spring Boot 3 microservices that detect fraudulent transactions and notify operations by email.

Two services share **Kafka** as their only integration boundary. Each owns its own Docker image, configuration, and PostgreSQL database.

| Service | Role | Port |
|---------|------|------|
| [fraud-rule-engine](fraud-rule-engine/README.md) | Ingest events, evaluate strategy rules, score risk, publish assessments | `8080` |
| [fraud-notification-service](fraud-notification-service/README.md) | Consume assessments, send one aggregated fraud-alert email | `8081` |

```text
fraud-platform/
├── pom.xml                         # Maven aggregator (optional)
├── compose.yaml                    # Shared infrastructure
├── README.md
├── fraud-rule-engine/
└── fraud-notification-service/
```

## Architecture

```text
  REST / Kafka (transactions.categorized)
                    │
                    ▼
        ┌───────────────────────┐
        │   fraud-rule-engine   │  DB: fraud_db (:5433)
        │  strategy rules + API │
        └───────────┬───────────┘
                    │  topic: fraud.assessments
                    ▼
        ┌───────────────────────────┐
        │ fraud-notification-service│  DB: notification_db (:5434)
        │  idempotent email alerts  │
        └───────────┬───────────────┘
                    │  SMTP
                    ▼
                 Mailpit (:8025)
```

- Non-fraud assessments (`fraudSuspected == false`) are ignored by the notification service
- Idempotency keys: `externalId` (engine) and `assessmentId` (notifications)
- Temporary SMTP/Kafka failures are retried; exhausted failures go to `fraud.assessments.dlt`

## Prerequisites

- Docker Desktop (Compose v2)
- Java 21 + Maven 3.9+ (for local builds/tests)

## Quick start

```bash
docker compose up -d --build
```

| What | Where |
|------|--------|
| API + Swagger | http://localhost:8080 · http://localhost:8080/swagger-ui.html |
| Notification health | http://localhost:8081/actuator/health |
| Mailpit inbox | http://localhost:8025 |
| Fraud PostgreSQL | `localhost:5433` — `fraud_db` / `fraud_user` / `fraud_password` |
| Notification PostgreSQL | `localhost:5434` — `notification_db` / `notification_user` / `notification_password` |
| Kafka | `localhost:9092` (host) · `kafka:29092` (Compose network) |

```bash
docker compose logs -f fraud-rule-engine fraud-notification-service
docker compose down
```

### Demo: ingest → email

HTTP Basic users: `ingest` / `ingest` (write), `analyst` / `analyst` (read), `admin` / `admin`.

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

Open http://localhost:8025 — you should see one email to `fraud-team@example.com` listing every triggered rule.

```bash
curl -u analyst:analyst "http://localhost:8080/api/v1/assessments?fraudSuspected=true"
curl -u analyst:analyst "http://localhost:8080/api/v1/fraud-flags?page=0&size=1&sort=createdAt,desc"
```

Paged list endpoints take query params (`page`, `size`, `sort=createdAt,desc`) — see [rule engine README](fraud-rule-engine/README.md#pagination-swagger--query-params). In Swagger, `sort` must be the single string `createdAt,desc`, not a JSON array.

## Build & test

```bash
# both modules
mvn clean test

# single module
mvn -pl fraud-rule-engine clean test
mvn -pl fraud-notification-service clean test
```

Each service remains independently buildable and deployable; the parent POM only aggregates.

## Kafka topics

| Topic | Producer | Consumer | Purpose |
|-------|----------|----------|---------|
| `transactions.categorized` | Upstream / demo | Rule engine | Inbound categorized events |
| `fraud.assessments` | Rule engine | Notification service | Assessment results |
| `fraud.assessments.dlt` | Notification (DLT) | Ops / replay | Failed notification payloads |

## Project docs

- [Rule engine README](fraud-rule-engine/README.md) — rules, security, API, idempotency
- [Notification service README](fraud-notification-service/README.md) — email, retries, DLT
