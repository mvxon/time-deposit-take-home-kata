---
name: change
description: Implement a behaviour change in the Java solution test-first — clarify, plan, failing test, implementation, ./mvnw verify and requirements-reviewer. Use for new interest plans, OpenAPI contract changes, schema changes and bug fixes, including live extensions during an interview.
argument-hint: <what to change>
---

Task: $ARGUMENTS

Work from `java/` with JDK 25. Follow the steps in order and show the developer the result of each one.

1. **Clarify.** Restate the task in one or two sentences and ask about anything ambiguous before planning. Stop and ask if it conflicts with `ASSIGNMENT.md` or `CLAUDE.md` — for example a new endpoint (exactly two are allowed) or a change to `TimeDeposit` or to the behaviour of `updateBalance` for the existing plan types.
2. **Plan.** List the files to change, the tests to add and whether an ADR in `docs/decisions/` is affected. Wait for the developer's go-ahead.
3. **Failing test first.** Write the test, run it (`./mvnw -B -ntp test -Dtest=<TestClass>`) and show that it fails for the expected reason.
4. **Implement** using the recipe below.
5. **Verify.** `./mvnw -B -ntp verify` must end with `BUILD SUCCESS`; on failure find the root cause in `target/surefire-reports`.
6. **Review.** Run the `requirements-reviewer` agent and fix what it reports.
7. **Report.** Show `git diff --stat` (the real state, not a summary from memory), open questions, whether `docs-writer` should update the documentation, and a draft commit message.

## Recipes

**New interest plan**
- Add an `AnnualRatePlan` to `InterestPlans` and register it in `defaults()`; its rule covers only the plan's own day limits, because the calculator applies the 30-day grace period to every plan before the plan's rule.
- A plan whose interest is not `balance × annual rate / 12` is its own `InterestPlan` implementation reading `InterestContext`.
- Tests in `TimeDepositCalculatorTest` on every day boundary of the new rule. Do not add the new type to `PLAN_TYPES` in `TimeDepositCalculatorEquivalenceTest`: the original calculator treats it as unknown.
- Tell the developer that deposits of this type earned no interest before the change.

**API change**
- Edit `java/src/main/resources/static/openapi/time-deposit-api.yaml` first, then `./mvnw -B -ntp compile` to regenerate `TimeDepositsApi` and the DTOs.
- Implement in `TimeDepositController` and `TimeDepositWebMapper`; update `OpenApiContractTest` when the schema changes.
- A new path or operation is not allowed by the assignment: stop and ask.

**Schema change**
- A new migration `java/src/main/resources/db/migration/V<n>__<description>.sql`; never edit an existing one.
- Update `TimeDepositEntity` / `WithdrawalEntity`, `TimeDepositPersistenceMapper`, the demo data in `db/demo/afterMigrate.sql` and `TimeDepositFixtures`.
- Cover it with an `@IntegrationTest`.

**Bug fix**
- Reproduce the bug with a failing test first, then fix it. If the bug is in the original calculation, stop and ask: its behaviour must be preserved.
