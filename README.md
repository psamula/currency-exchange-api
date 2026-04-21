# Currency Exchange API

Small REST service for opening a currency account and exchanging funds between
PLN and USD at the current NBP (National Bank of Poland) rate.

## Requirements

- **Java 21**
- Internet access (rates fetched from `https://api.nbp.pl`)
- Maven not required - use the shipped `mvnw` / `mvnw.cmd`

## Running

Run from the **project root** (the H2 file is created relative to the working
directory):

```bash
./mvnw spring-boot:run      # Linux/macOS
mvnw.cmd spring-boot:run    # Windows
```

The service starts on port `8080` and stores its H2 file under `./data/`.

## Running tests

```bash
./mvnw test
```

Unit tests for the domain plus full-stack MockMvc integration tests using
WireMock (NBP stub) and an in-memory H2.

## API

All endpoints consume and produce `application/json`. Errors follow
[RFC 9457 Problem Details](https://datatracker.ietf.org/doc/html/rfc9457).

### 1. Open an account - `POST /api/accounts`

```json
{ "firstName": "Jan", "lastName": "Kowalski", "initialBalancePln": 1000.00 }
```

`201 Created` (+ `Location` header):

```json
{ "id": "2b2a3f3e-...", "firstName": "Jan", "lastName": "Kowalski",
  "balancePln": 1000.00, "balanceUsd": 0.00 }
```

### 2. Get account state - `GET /api/accounts/{accountId}`

```json
{ "id": "2b2a3f3e-...", "firstName": "Jan", "lastName": "Kowalski",
  "balancePln": 900.00, "balanceUsd": 25.65 }
```

### 3. Exchange currencies - `POST /api/accounts/{accountId}/exchanges`

```json
{ "fromCurrency": "PLN", "toCurrency": "USD", "amount": 100.00 }
```

`200 OK`:

```json
{
  "accountId": "2b2a3f3e-...",
  "fromCurrency": "PLN", "amountDebited": 100.00,
  "toCurrency": "USD",   "amountCredited": 25.65,
  "rateApplied": 3.8982, "rateEffectiveDate": "2026-04-16",
  "balancePln": 900.00,  "balanceUsd": 25.65
}
```

### Error codes

| Status | When |
|--------|------|
| `400` | validation failed / same source and target currency / malformed body / invalid path parameter |
| `404` | account with the given `id` does not exist |
| `405` / `415` | wrong HTTP method or `Content-Type` |
| `409` | concurrent modification of the same account (optimistic locking) |
| `422` | insufficient balance / exchange amount too small to produce a non-zero credited amount |
| `503` | NBP API unreachable or returned an error |

## Assumptions

Choices made where the task statement left room for judgement:

- **NBP table C** (bid/ask), not table A (mid). PLN→USD applies `ask`,
  USD→PLN applies `bid`. Using mid would skip the spread.
- **Amounts** are `BigDecimal` with scale 2 (grosz / cent).
- **Account state** is not dynamically recomputed: USD acquired once stays
  as USD at the historical rate, never reconverted on read.
- **Retries** on `409 Conflict` or `503 Service Unavailable` are the
  client's responsibility - the server does not retry.
- **Persistence** is H2 file-based for zero-setup onboarding; Flyway owns
  the schema so switching to Postgres/MSSQL is a properties change.

## Design decisions

### Money - asymmetric rounding

Balance arithmetic uses **`HALF_EVEN`** (banker's rounding, standard for
aggregated math). The credited amount of an exchange is rounded **`DOWN`**
(towards zero).

**Why asymmetric?** `HALF_EVEN` in a client-controlled single transaction
enables *dust arbitrage*: `0.02 PLN / 3.6271 = 0.005514 USD` rounds up to
`0.01 USD` (worth ~`0.035 PLN` at the bid rate), which can be looped for
profit. Rounding the credited side down makes the sub-grosz remainder
accrue to the bank - the industry convention.

When the credited amount would round down to zero, the request is rejected
with `422 Exchange amount too small`.

### Account model - two columns, not a generic balances table

An account holds two independent balances: `balancePln` and `balanceUsd`.
With a fixed two-currency set, typed columns are self-documenting, need
no joins, and avoid nullable `Map` lookups. Going generic now would be
speculative.

**Adding a third currency** is a deliberate refactor: introduce an
`account_balances (account_id, currency, amount)` table, replace the two
columns with a `Map<Currency, BigDecimal>`, backfill via Flyway. Done when
the business confirms the need.

### Concurrency - optimistic locking

`Account` carries a JPA `@Version` field. Concurrent exchanges on the same
account surface as `409 Conflict`. Chosen over pessimistic locking because
collisions are rare, there is no deadlock risk, and conflicts are made
visible to the client. Server-side retry is intentionally out of scope.

### Transaction boundary - split orchestration from mutation

- `ExchangeService` - orchestrates (validate, fetch NBP rate, compute).
  **No `@Transactional`** - the HTTP call to NBP must not run inside a DB
  transaction.
- `AccountExchangeOperation` - package-private, owns the `@Transactional`
  boundary, mutates balances.

Avoids holding a DB connection across a network call and side-steps the
self-invocation AOP pitfall.

### NBP rate cache - Caffeine, 5-minute TTL

NBP publishes new rates once a day. A 5-minute in-process cache eliminates
almost all redundant HTTP calls while recovering quickly from short
upstream outages.

### Account identifier - UUID

Avoids leaking the number of registered accounts and lets the app generate
identifiers without a DB round-trip.

### Package layout - by feature

```
org.example.currency
├── account    - entity, repository, DTOs, service, controller
├── exchange   - request/response, calculation, service, controller
├── nbp        - HTTP client, caching, response mapping
└── common     - domain exceptions, global exception handler
```

### Exception handling - `ResponseEntityExceptionHandler`

`GlobalExceptionHandler` extends Spring's `ResponseEntityExceptionHandler`,
so standard MVC errors (`405`, `415`, `400`, `404`) map to RFC 9457
responses for free. The handler only overrides mappings where the domain
wants a custom title and adds handlers for domain-specific exceptions.

## Out of scope (would add for production)

- **Authentication** - not in the task statement.
- **Metrics & tracing** - Micrometer + Prometheus / OpenTelemetry.
- **Retry + circuit breaker on NBP** - Resilience4j.
- **Idempotency key on exchanges** - so a client retry on a flaky network
  does not double-exchange.
- **Audit history** - an `exchange_history` table for regulatory needs.
- **Rate limiting** on the exchange endpoint.
- **OpenAPI / Swagger UI** - `springdoc-openapi-starter`.
- **Move off H2** - Postgres / MSSQL for production concurrency & backups.

## Tech stack

Java 21, Spring Boot 3.3 (Web, Validation, Data JPA, Cache), H2 (file),
Flyway, Caffeine, Lombok. 

Tests: JUnit 5, AssertJ, MockMvc, WireMock.
