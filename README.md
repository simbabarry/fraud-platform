# Fraud Platform

Parent workspace with **two independent Spring Boot microservices** that integrate over Kafka:

| Service | Role | Port |
|---------|------|------|
| [`fraud-rule-engine`](fraud-rule-engine/README.md) | Ingest events, run strategy rules, persist assessments, publish results | 8080 |
| [`fraud-notification-service`](fraud-notification-service/README.md) | Consume assessments, send one aggregated fraud-alert email | 8081 |

```text
C:\fraud-platform\
├── pom.xml                         # Optional Maven aggregator
├── compose.yaml                    # Shared infrastructure
├── README.md
├── fraud-rule-engine\
└── fraud-notification-service\
```

Each service owns its own `pom.xml`, configuration, Docker image, and database.

## Quick start

```bash
cd C:\fraud-platform
docker compose up -d --build
```

| Endpoint | URL |
|----------|-----|
| Fraud API / Swagger | http://localhost:8080 / http://localhost:8080/swagger-ui.html |
| Notification health | http://localhost:8081/actuator/health |
| Mailpit inbox | http://localhost:8025 |
| Fraud PostgreSQL | `localhost:5433` (`fraud_db` / `fraud_user` / `fraud_password`) |
| Notification PostgreSQL | `localhost:5434` (`notification_db` / `notification_user` / `notification_password`) |
| Kafka | `localhost:9092` (host) · `kafka:29092` (compose network) |

### Demo ingest → email

```bash
curl -u ingest:ingest -X POST http://localhost:8080/api/v1/transactions/events \
  -H "Content-Type: application/json" \
  -d "{
    \"externalId\": \"tx-1001\",
    \"customerId\": \"cust-42\",
    \"accountId\": \"acc-7\",
    \"amount\": 9200.00,
    \"currency\": \"USD\",
    \"category\": \"ONLINE_PURCHASE\",
    \"merchantName\": \"Global Gadgets\",
    \"merchantCountry\": \"US\",
    \"occurredAt\": \"2026-03-15T10:30:00Z\"
  }"
```

Open http://localhost:8025 for the alert email (recipient: `fraud-team@example.com`).

## Build both with Maven

```bash
cd C:\fraud-platform
mvn clean test
```

Services remain independently deployable; the parent POM only aggregates builds.

## Architecture

```text
REST / Kafka events
        │
        ▼
┌─────────────────────┐     fraud.assessments      ┌──────────────────────────┐
│  fraud-rule-engine  │ ─────────────────────────► │ fraud-notification-service│
│  (fraud-postgres)   │                             │ (notification-postgres)   │
└─────────────────────┘                             └────────────┬─────────────┘
                                                                 │ SMTP
                                                                 ▼
                                                              Mailpit
```

- Kafka is the integration boundary (no shared database).
- Notification ignores `fraudSuspected == false`.
- Idempotency key: `assessmentId`.
- Retries + dead-letter topic: `fraud.assessments.dlt`.

## Clean ZIP (exclude `.git` / `target`)

```powershell
$dest = Join-Path $env:USERPROFILE 'Documents\fraud-platform-submission.zip'
if (Test-Path $dest) { Remove-Item $dest }
Get-ChildItem -Force C:\fraud-platform |
  Where-Object { $_.Name -notin '.git','target' } |
  Compress-Archive -DestinationPath $dest -Force
```

(Also ensure nested `**/target` folders are absent — run `mvn clean` first if needed.)
