# Time Deposit Kata — project rules for Claude Code

The solution lives in `java/`. The other language folders are untouched kata starters.

## Requirements that must never be broken

- `org.ikigaidigital.TimeDeposit` stays byte-identical to the original kata file (it is excluded from Spotless).
- `TimeDepositCalculator.updateBalance(List<TimeDeposit>)` keeps its package, signature and behaviour. `TimeDepositCalculatorTest` holds the characterization tests that pin the behaviour; they must pass unchanged.
- The API has exactly two endpoints: `GET /time-deposits` and `POST /time-deposits/balance-updates`. Never add another one (including Actuator).
- The OpenAPI contract `java/src/main/resources/static/openapi/time-deposit-api.yaml` is the source of truth. Change the contract first, then regenerate (`./mvnw compile`) and implement the generated interface.

## Architecture (hexagonal, enforced by `HexagonalArchitectureTest`)

```
org.ikigaidigital
├── TimeDeposit, TimeDepositCalculator   shared kata classes, stay in place
├── domain/            framework-free: model/, interest/ strategies, port/ (outbound ports and their exceptions)
├── application/       use-case services (transactions are in the persistence adapter's port operations, see ADR 0004)
├── adapter/web/       controller implementing the generated API, MapStruct mapper
├── adapter/persistence/  JPA entities, Spring Data, port implementation, MapStruct mapper
└── config/            Spring wiring of domain objects (@Bean)
```

- The domain never depends on Spring, JPA or adapters. Wire domain objects in `config/`.
- Adapters never depend on each other. Adapter classes are package-private where possible.
- Inside `adapter/persistence/`, one subpackage per store (currently `timedeposit/`). Spring Data interfaces always end with `JpaRepository` so they never clash with the domain ports of the same name.
- Interest strategies take an `InterestContext(balance, days)` and never depend on the root package (no package cycles, enforced by ArchUnit).
- A new interest plan is an `AnnualRatePlan` (or a custom `InterestPlan`) registered in `InterestPlans.defaults()` — the single list used by both the calculator and Spring.
- The OpenAPI YAML is the only published contract: Swagger UI is a static page from the `swagger-ui` webjar; there is no springdoc and no `/v3/api-docs`.

## Code style

- Functional style, small focused methods, no inline comments except a short justification where the kata asks to justify an assumption. Public contracts — extension points (`InterestPlan`), ports and the application service — have short Javadoc; SQL migrations may explain schema decisions.
- Lombok for boilerplate, MapStruct for mapping, records for immutable domain values.
- No static methods in Spring beans (`@Component`, `@Repository`, `@Service`, controllers, configuration); static helpers belong in final utility classes. Methods returning `boolean` start with `is`, `has` or `can`.
- Formatting is Palantir Java Format via Spotless. `./mvnw spotless:apply` fixes it; `verify` fails on violations.
- Database objects use snake_case (`time_deposits`, `plan_type`). Schema changes go through new Flyway migrations.

## Commands (run from `java/`, JDK 25 — see `.sdkmanrc`)

| Task | Command |
|---|---|
| Full check: tests, ArchUnit, coverage ≥ 90 %, style | `./mvnw verify` |
| Format | `./mvnw spotless:apply` |
| Run locally with demo data (Postgres via Docker Compose) | `./mvnw spring-boot:run -Dspring-boot.run.profiles=demo` |
| Run everything in Docker | `docker compose --profile app up --build` |

## Workflow

- Ask clarifying questions when a request is ambiguous before designing a solution.
- Write or extend tests first when changing behaviour; integration tests use Testcontainers (`@IntegrationTest`).
- Keep commits atomic: one logical change per commit, build green at every commit.
- Before finishing a change, run `./mvnw verify` and the `requirements-reviewer` agent.

## Skills and agents

| When | Use |
|---|---|
| Any behaviour change: new interest plan, API or schema change, bug fix | `/change <task>` |
| Check that everything works; `full` adds Docker and a live smoke test | `/verify [full]` |
| Independent review of the whole solution at a milestone (developer asks) | `/review` → `solution-reviewer` + Codex |
| Commit work (developer asks) | `/commit` |
| A change or decision affects README, ADRs or `docs/` | `docs-writer` agent |
| Check a pending change against the kata rules | `requirements-reviewer` agent |
