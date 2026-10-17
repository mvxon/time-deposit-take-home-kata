# 0001. Preserve the legacy calculation, including its floating-point rounding

## Context

The assignment states that `TimeDepositCalculator.updateBalance` works correctly and its behaviour must not change. The original code computes money in `double`: `balance * rate / 12`, rounded with `new BigDecimal(double).setScale(2, HALF_UP)`. `new BigDecimal(double)` sees the exact binary value, so a mathematically exact half cent is often rounded down: a basic deposit of `18.00` earns `0.01` instead of `0.02` (`BigDecimal.valueOf` would give `0.02`). `TimeDeposit` keeps the balance as `Double` and may not be changed.

## Decision

- Keep the arithmetic and rounding bit for bit. The refactoring only restructures the code (strategies per plan).
- Pin the behaviour with tests: characterization tests for every boundary and for the rounding cases where `BigDecimal.valueOf` would differ, plus an equivalence test that compares the outcome (the resulting balance and the exception, if any) of a verbatim copy of the original calculator with that of the refactored one on 100 000 random deposits, including deposits without a plan type.
- Keep money exact outside the legacy calculation: `numeric` in the database, decimal numbers in the API. The `Double` value is converted with `BigDecimal.valueOf(...).setScale(2, HALF_UP)` when stored, so floating-point noise never accumulates between months.
- Limit money columns to `numeric(15, 2)`: a `double` round-trips any decimal value of up to 15 significant digits exactly, so reading a balance into `TimeDeposit` and writing it back never changes it.
- No deviation from the original, including its failure for a missing plan type: a deposit with a `null` plan type and more than 30 days throws `NullPointerException`, and the deposits before it in the list are already updated while it and the ones after it are not. The class is shared, so other callers may rely on the failure (for example to roll back or alert); earning 0 silently would turn a loud failure into a silent one. Unknown plan types still earn no interest. The grace period is checked before the plan type, as in the original ([ADR 0002](0002-hexagonal-architecture.md)).
- Pin the failure with tests: `TimeDepositCalculatorTest` checks the exception and the partial update (`failsOnDepositWithoutPlanTypeAfterGracePeriodLikeTheOriginal`) and that the balance stays unchanged within the grace period. The `plan_type` column is `not null`, so the API never produces such a deposit.

## Consequences

- Results are identical to the original system, which matters for reconciliation with any existing data.
- Interest can be one cent lower than exact decimal arithmetic in boundary cases. Fixing it is a business decision: in production I would introduce a `BigDecimal`-based `Money` value object, agree on the rounding mode (possibly `HALF_EVEN`), run old and new calculation side by side and recalculate the affected deposits.
- Balances are limited to 9 999 999 999 999.99; interest on a balance near that limit would overflow the column.
- If a deposit without a plan type ever reached the service, the `NullPointerException` is not caught: `TimeDepositPersistenceAdapter.updateBalancesAtomically` (`@Transactional`) catches only `OptimisticLockException`, so the whole balance update rolls back (no balance changes, including pages already flushed) and `ApiExceptionHandler.handleUnexpected` logs the exception and answers `500` as a problem detail. No dedicated handling is added: the schema enforces the invariant where the data enters, the assignment does not require handling invalid input, and a handler for an unreachable case would be dead code; deposits from an untrusted source would be validated at that boundary instead.
