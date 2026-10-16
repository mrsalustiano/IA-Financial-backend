# IA-Financial

Personal financial control system with AI, built with **Java 21**, **Spring Boot 3.5.6** and
**Hexagonal Architecture (Ports and Adapters)**.

Current scope: **FASE 1 — Accounts (CRUD)** and **Incomes (CRUD)**, plus the cross-cutting
foundation (error handling, response envelope, audit trail, soft delete, MongoDB schema
validators, OpenAPI, Ollama adapter for upcoming AI features).

## How to run

```bash
# start a local MongoDB on localhost:27017, then:
mvn spring-boot:run
# Swagger UI: http://localhost:8080/swagger-ui.html
# Actuator:   http://localhost:8080/actuator/health
```

```bash
mvn clean test        # runs all tests + JaCoCo (fails under 90% line coverage)
mvn clean verify      # same, plus packaging
```

Tests bootstrap an **embedded MongoDB** (flapdoodle 4.16.2, MongoDB 7.0) — no Docker needed.
The Ollama adapter is integration-tested against WireMock.

## Architecture

```
com.finance
├── domain          # entities, enums, events, domain services, ports (in/out)
├── application     # use cases (CQRS-light), commands, queries, DTOs, MapStruct mappers, validators
├── adapters.in.rest.v1      # REST controllers (/api/v1)
├── adapters.out.mongodb     # documents, Spring Data repositories, persistence mappers, port adapters
├── adapters.out.audit       # AuditPort -> audit_logs collection (old/new JSON values)
├── adapters.out.events      # EventPublisherPort -> Spring ApplicationEventPublisher
├── adapters.out.ollama      # AiProviderPort -> Ollama HTTP client (strategy default)
├── config          # MongoConfig (Decimal128 money), MongoSchemaInitializer ($jsonSchema + indexes)
├── documentation   # OpenAPI
└── shared          # exceptions, GlobalExceptionHandler, response envelope, constants, utils
```

Dependencies point inward: `application` depends only on `domain`; adapters depend on both;
nothing in the core depends on Spring Web/Data types except the `Pageable` query records.

## APIs (v1)

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| POST | `/api/v1/accounts` | Create account | 201 + envelope |
| GET | `/api/v1/accounts/{id}` | Get account | 200 |
| GET | `/api/v1/accounts?type=&active=&page=&size=&sort=` | List/filter/paginate | 200 |
| PUT | `/api/v1/accounts/{id}` | Update account | 200 |
| DELETE | `/api/v1/accounts/{id}` | Soft delete | 204 |
| POST | `/api/v1/incomes` | Register income | 201 |
| GET | `/api/v1/incomes/{id}` | Get income | 200 |
| GET | `/api/v1/incomes?accountId=&startDate=&endDate=&page=&size=&sort=` | List/filter by period | 200 |
| PUT | `/api/v1/incomes/{id}` | Update income | 200 |
| DELETE | `/api/v1/incomes/{id}` | Soft delete | 204 |

All success responses use the envelope `{"data": ..., "message": "Success", "timestamp": ...}`.
Errors use `{"timestamp", "status", "error", "message", "details"}` with the codes below.

## Error semantics

| Situation | HTTP |
|-----------|------|
| Bean Validation failure, malformed body, amount ≤ 0, future `receivedAt`, unknown enum | 400 |
| Missing auth (reserved) / forbidden (reserved) | 401 / 403 |
| Account or income not found (or already soft-deleted) | 404 |
| Duplicate account name (case-insensitive) | 409 |
| Business rule violated (e.g. income on inactive account) | 422 |
| Database failure | 500 |
| Ollama unreachable / provider error | 503 |

## Data model (MongoDB)

- `accounts`: `_id, userId, name (indexed), description, type, active, deleted, deletedAt, createdAt, updatedAt` — `$jsonSchema` validator, unique-by-name handled in the use case, indexes on `userId+name`, `type`, `active`, `deleted`.
- `incomes`: `_id, userId, accountId (indexed), description, amount (Decimal128), receivedAt (date), deleted, deletedAt, createdAt, updatedAt` — validator + indexes for `userId+receivedAt` and `accountId+receivedAt`.
- `audit_logs`: `entityType, entityId, action (CREATE/UPDATE/DELETE), oldValue, newValue (JSON strings), createdAt` — written on every use case mutation.

Money is stored as BSON `Decimal128` via custom converters, and `LocalDate` as BSON date (UTC) —
both chosen so future aggregations (monthly balances) work numerically. Soft deletes never remove
documents; all reads filter `deleted: false`.

## Acceptance criteria — FASE 1 (Gherkin)

```gherkin
Feature: Account management
  Scenario: Create account
    When I POST /api/v1/accounts with a valid name and type
    Then the response is 201 with the account envelope
    And an audit_logs entry exists with action CREATE and newValue filled
    And an AccountCreatedEvent was published

  Scenario: Duplicate name
    Given an active account named "Broker"
    When I POST an account named "broker"
    Then the response is 409

  Scenario: Soft delete
    When I DELETE /api/v1/accounts/{id}
    Then the response is 204
    And GET /api/v1/accounts/{id} returns 404
    And the document still exists with deleted=true and deletedAt set

Feature: Income management
  Scenario: Register income
    Given an active account
    When I POST /api/v1/incomes with amount > 0 and receivedAt <= today
    Then the response is 201 with the income envelope
    And an audit_logs entry exists with action CREATE

  Scenario: Invalid amount
    When I POST an income with amount 0 or negative
    Then the response is 400

  Scenario: Unknown or inactive account
    When I POST an income for a nonexistent account
    Then the response is 404
    When I POST an income for an inactive account
    Then the response is 422

  Scenario: Filter by period
    Given incomes in September and October 2026
    When I GET /api/v1/incomes?accountId=...&startDate=2026-09-01&endDate=2026-09-30
    Then only September incomes are returned with correct pagination totals
```

## Definition of Done

- [x] Hexagonal layers respected (domain ← application ← adapters)
- [x] FASE 1 use cases: accounts CRUD + incomes CRUD
- [x] Mandatory exceptions + GlobalExceptionHandler with the required HTTP codes
- [x] Standard response envelope on all endpoints
- [x] DTOs as records, MapStruct for all mapping, Bean Validation on inputs
- [x] Soft delete + audit trail (old/new values) + domain events
- [x] Multi-user-ready `userId` field (single `LOCAL_USER` for now)
- [x] MongoDB with `$jsonSchema` validators, indexes, Decimal128 money, BSON dates
- [x] Pagination, filtering and sorting on list endpoints
- [x] Swagger UI (`/swagger-ui.html`) + actuator health/info/metrics
- [x] Ollama adapter behind `AiProviderPort` (tested with WireMock)
- [x] 106 tests (unit + API integration on embedded MongoDB), **92.6% line coverage** (JaCoCo gate ≥ 90%)
- [ ] JWT auth (FASE 7), categorization/AI (FASE 5), Docker deployment (FASE 8) — future phases

## Test tooling notes

This repo was built on a machine without Maven preinstalled; `.tools/` (git-ignored) contains
Apache Maven 3.9.9 plus small single-file Java launchers (`Maven.java`, `Downloader.java`,
`Unzip.java`). To run Maven: `java .tools/Maven.java .tools/apache-maven-3.9.9 . test`.
