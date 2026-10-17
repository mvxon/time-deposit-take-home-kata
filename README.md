# XA Bank Time Deposit — Java solution

Solution of the [Time Deposit Refactoring Kata](ASSIGNMENT.md) in Java: the legacy interest calculation is refactored without changing its behaviour and exposed through a REST API backed by PostgreSQL.

## Quick start

Requires only Docker.

```bash
cd java
docker compose --profile app up --build
```

This builds the application image, starts PostgreSQL and the application with demo data, and exposes the API on <http://localhost:8080>. Stop and remove everything with `docker compose --profile app down -v`.

## Calling the endpoints through Swagger

The API contract is [`time-deposit-api.yaml`](java/src/main/resources/static/openapi/time-deposit-api.yaml); Swagger UI renders exactly this file.

1. Open <http://localhost:8080/swagger-ui/index.html>.
2. **GET `/time-deposits`** → *Try it out* → *Execute*. Six demo deposits are returned with their withdrawals.
3. **POST `/time-deposits/balance-updates`** → *Try it out* → *Execute*. The response is `204 No Content`.
4. Run the GET again: deposits 1, 3 and 6 earned interest (`1235595.81`, `5012.50`, `20083.33`); deposit 2 is in its 30-day grace period, deposit 4 is a student deposit older than a year and deposit 5 is a premium deposit younger than 45 days, so their balances are unchanged.

Every POST applies one more month of interest. The same calls with curl:

```bash
curl http://localhost:8080/time-deposits
curl -X POST http://localhost:8080/time-deposits/balance-updates
```

## Solution overview

- **Stack:** Java 25, Spring Boot 4.1, PostgreSQL 18, Flyway, Spring Data JPA, MapStruct, Lombok, OpenAPI Generator.
- **Contract first:** the OpenAPI YAML is the single source of the API. The controller implements the generated interface; there are exactly two operations, verified by `OpenApiContractTest`.
- **Hexagonal architecture**, enforced by ArchUnit (`HexagonalArchitectureTest`):

  ```
  org.ikigaidigital
  ├── TimeDeposit, TimeDepositCalculator   shared kata classes, kept in place
  ├── domain/            framework-free: model/, interest/ strategies, port/ (outbound ports and their exceptions)
  ├── application/       use-case service
  ├── adapter/web/       controller implementing the generated API
  ├── adapter/persistence/  JPA entities, Spring Data, port implementation
  └── config/            Spring wiring of the domain
  ```

- **Extensible interest rules:** each plan is an `InterestPlan` strategy registered in `InterestPlans.defaults()`; a new plan with an annual rate is one line, a non-linear one is a new class. The calculator applies the 30-day grace period to every plan before the plan's own rule. `TimeDeposit` is unchanged byte for byte and `updateBalance` keeps its signature.
- **Behaviour preserved:** characterization tests pin every boundary (30/31, 45/46, 365/366 days), the legacy rounding and the failure for a deposit without a plan type; `TimeDepositCalculatorEquivalenceTest` compares the outcome (resulting balance and exception, if any) of the refactored calculator with that of a verbatim copy of the original on 100 000 random deposits, including `null` plan types.
- **Balance update** runs page by page in a single transaction with optimistic locking: either every deposit gets its interest or none does, and a concurrent run is rejected with `409`.

Details and trade-offs of each decision are in [docs/decisions](docs/decisions).

## Assumptions

The requirements leave several points open; these are the interpretations taken (each is also justified in the code where it applies).

- **Plan rates are annual**, as deposit rates are conventionally quoted: monthly interest is `rate / 12`, exactly as in the original calculator.
- **Every POST applies one month of interest and does not change `days`.** `TimeDeposit` has no setter for `days`, so it is treated as maintained outside this service. Calling the operation once per accrual period is the caller's responsibility — see [ADR 0003](docs/decisions/0003-accrual-responsibility-and-idempotency.md).
- **"No interest after 1 year"** means after day 365, matching the original `days < 366` check.
- **Unknown plan types earn no interest**, as in the original. A deposit without a plan type (`null`) fails with `NullPointerException` after the grace period, exactly as in the original; the column is `not null`, so such rows cannot exist. See [ADR 0001](docs/decisions/0001-preserve-legacy-calculation.md).
- **Table and column names** are the ones from the requirements in PostgreSQL's snake_case: `time_deposits`, `withdrawals`, `plan_type`, `time_deposit_id`.
- **Technical additions to the schema:** `time_deposits.version` for optimistic locking; money columns are `numeric(15, 2)` because the legacy `Double` balance round-trips exactly only up to 15 significant digits — see [ADR 0001](docs/decisions/0001-preserve-legacy-calculation.md).
- **Demo data** is loaded only with the `demo` Spring profile (enabled in Docker Compose), because the API has no endpoint to create deposits.

## Development

Requires JDK 25 (`java/.sdkmanrc`) and Docker for Testcontainers.

Java 25 is the current LTS release and Spring Boot 4.1 the current generation of the framework, a sensible baseline for a new service. A reviewer without JDK 25 can still run everything with Docker (see Quick start), and CI runs the tests.

```bash
cd java
./mvnw verify                                              # build, tests, ArchUnit, coverage ≥ 90 %, code style
./mvnw spotless:apply                                      # format the code
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo     # run locally; PostgreSQL starts via Docker Compose
```

`verify` runs 56 tests: unit and characterization tests, the equivalence test against the original calculator, Testcontainers integration tests against PostgreSQL, contract and architecture tests. Line coverage is 92.8 %. GitHub Actions (`.github/workflows/java-ci.yml`) runs on pushes to every branch and on pull requests, but only when files under `java/` or the workflow itself change, and can be started manually. The job runs `./mvnw verify`, publishes a line coverage summary, uploads the JaCoCo report and builds the Docker image.

The history is organised into atomic commits, each with its own tests; every commit builds and passes its tests. The first commit adds characterization tests on the unchanged legacy calculator.

## Documentation

- [Design decisions](docs/decisions) — the reasoning and trade-offs behind the solution.
- [AI-assisted development](docs/ai-assisted-development.md) — tools, agents, skills and hooks, the independent review loop and how AI was used.
- [Assignment](ASSIGNMENT.md) — the original task description.
