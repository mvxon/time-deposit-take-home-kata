# 0002. Lightweight hexagonal architecture

## Context

The assignment prefers hexagonal architecture. The service has two use cases and one storage.

## Decision

- `domain/` is framework-free: `model/` (the deposit with its withdrawals, money rounding), `interest/` (the strategies) and `port/` (the outbound port `TimeDepositRepository` with the exception of its contract). It does not depend on Spring, JPA or adapters.
- `application/` holds the use-case service. `adapter/web` and `adapter/persistence` are the inbound and outbound adapters; their classes are package-private. `config/` wires domain objects as Spring beans.
- No inbound port interfaces: the controller already depends on the core, so an interface with a single implementation would add indirection without inverting any dependency. A second entry point (for example a scheduler) would call the same service.
- `TimeDeposit` and `TimeDepositCalculator` stay in the root package `org.ikigaidigital` because they are shared kata classes and their package is part of their contract. `TimeDepositApplication` stays there too: moving it would require configuring component, entity and repository scanning explicitly.
- Interest strategies receive an `InterestContext` instead of `TimeDeposit`, so `domain.interest` does not depend on the root package and there is no package cycle.
- Extending interest rules: `InterestPlan` receives an `InterestContext` record (`balance`, `days`). A rule that needs more data (for example withdrawals) adds a field to `InterestContext` and the calculator fills it, so existing plans do not change. The kata sets a limit: `updateBalance(List<TimeDeposit>)` only receives `TimeDeposit`, so data that `TimeDeposit` does not carry would have to be supplied to the calculator separately.
- Grace period: no plan earns interest during the first 30 days, so `TimeDepositCalculator` checks `GracePeriod.isOver(days)` before it looks up the plan, as the original does. It is the literal reading of "No interest is applied for the first 30 days for any existing plans". `InterestPlan.monthlyInterest` is called only after the grace period, and a plan's rule states only its own day limits (`days <= 365` for student, `days > 45` for premium). Checking the grace period first also keeps the failure for a missing plan type exactly where the original has it ([ADR 0001](0001-preserve-legacy-calculation.md)).
- Registering interest plans: plans are listed explicitly in `InterestPlans.defaults()`, the composition root of the interest rules. `new TimeDepositCalculator()` and the Spring bean in `InterestConfiguration` (which calls that constructor) use the same list, so the API and the shared calculator always apply the same plans. A new plan is a new `InterestPlan` (or an `AnnualRatePlan`) plus one entry in the list; it needs no grace-period handling of its own, and `TimeDepositCalculator` and the existing plans do not change. Changing the list is registration, not modification, so the calculator stays closed for modification. Plans are deliberately not Spring beans. `new TimeDepositCalculator()` is the original kata entry point and must work without Spring, so it needs its own list; plans declared as beans would create a second list, and the API would apply a plan that a calculator created with `new` ignores. A `@Bean` method for the list would only move the registry into `InterestConfiguration`. Scanning `InterestPlan` implementations would avoid editing any list, but makes registration implicit (any class on the classpath silently changes how interest is calculated) and turns every rate plan into its own class; an explicit list of money rules is easier to audit.
- Concurrent updates surface as the domain exception `org.ikigaidigital.domain.port.ConcurrentUpdateException`, declared next to the port whose contract it belongs to. The persistence adapter translates the JPA `OptimisticLockException` into it and the web adapter maps it to `409`, so the port contract and the web adapter do not depend on Spring or JPA exceptions.
- ArchUnit enforces the rules: inward-only dependencies, a framework-free domain, no package cycles, adapters used only by themselves.

## Consequences

- The domain and the calculation are tested without Spring; the storage can be replaced without touching the service.
- A new plan always needs a code change and a release. If plans start to change often, the next step is data-driven rate plans: rate and day limits stored as data and an `AnnualRatePlan` built from them, so a new rate plan is a data change; classes remain only for non-linear rules.
- The grace period belongs to the calculator, not to the plans: a future plan without a grace period would need a change in `TimeDepositCalculator`. Accepted, as no such plan is needed now.
- `TimeDepositDetails` (a deposit with its withdrawals) wraps the mutable legacy `TimeDeposit`, so it is immutable only shallowly — a consequence of not changing that class.
