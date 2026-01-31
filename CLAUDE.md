# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Install dependencies and compile
mvn compile

# Run in development mode (hot reload, dev services)
mvn quarkus:dev

# Build and package (creates JAR in target/)
mvn package

# Build native executable (requires GraalVM)
mvn package -Pnative

# Run tests
mvn test

# Clean and rebuild
mvn clean install
```

## Development Setup

```bash
# Generate self-signed TLS certificates for development
./scripts/generate-certs.sh

# Run with dev services (auto-starts MariaDB container)
mvn quarkus:dev
```

## API Endpoints

- `POST /events` - Ingest transaction events (accepts `List<TransactionEvent>`)
- `GET /rules` - List cached rules
- `GET /rules/stats` - Get rule cache statistics
- `POST /rules/refresh` - Force refresh rules from database
- `GET /health` - Health checks
- `GET /health/ready` - Readiness check
- `GET /health/live` - Liveness check

## Project Overview

High-throughput Quarkus 3.x application for transaction event ingestion with Google CEL rule evaluation.

**Architecture:**
- RESTEasy Reactive for non-blocking REST endpoints
- Vert.x Event Bus for async event dispatch
- Google CEL for rule expression evaluation
- Reactive MariaDB client for database access
- Periodic rule cache refresh

**Package Structure:**
- `org.iki.model` - Data models (TransactionEvent, Rule, RuleEvaluationResult)
- `org.iki.rest` - REST endpoints
- `org.iki.verticle` - Vert.x event consumers
- `org.iki.engine` - CEL rule engine
- `org.iki.service` - Business services (RuleCacheService)
- `org.iki.repository` - Database access
- `org.iki.config` - Configuration classes
- `org.iki.codec` - Event bus codecs
- `org.iki.health` - Health checks

**Key Files:**
- `src/main/resources/application.properties` - Application configuration
- `src/main/resources/db/init.sql` - Database schema and sample rules
- `scripts/generate-certs.sh` - TLS certificate generation

## Example Request

```bash
curl -X POST https://localhost:8443/events \
  -H "Content-Type: application/json" \
  -k \
  -d '[
    {
      "debitAccount": "ACC-001",
      "creditAccount": "ACC-002",
      "cin": "CIN-12345",
      "amount": 15000.00,
      "transactedTime": "2024-01-15T10:30:00Z"
    }
  ]'
```

## CEL Rule Examples

Rules are stored in the `rules` table with CEL expressions:

```sql
-- High-value transaction
'amount > 10000.0'

-- Self-transfer detection
'debitAccount == creditAccount'

-- Suspicious account pattern
'debitAccount.startsWith("SUSP-") || creditAccount.startsWith("SUSP-")'

-- Combined conditions
'amount > 25000.0 && debitAccount != creditAccount'
```

Available variables in CEL context:
- `debitAccount` (String)
- `creditAccount` (String)
- `cin` (String)
- `amount` (Double)
- `transactedTimeEpochSeconds` (Long)

## Configuration

Key configuration properties in `application.properties`:

```properties
# Rule refresh interval
app.rules.refresh-interval=60s

# Database connection
quarkus.datasource.reactive.url=mysql://localhost:3306/edios
quarkus.datasource.reactive.max-size=20

# TLS certificates
quarkus.http.ssl.certificate.files=certs/server.crt
quarkus.http.ssl.certificate.key-files=certs/server.key
```

## Testing

JUnit 5 and REST Assured are configured for testing:

```bash
mvn test
```

Dev services automatically provision a MariaDB container for tests.
