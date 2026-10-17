# 0003. The caller controls accrual periods; no idempotency in the service

## Context

`POST /time-deposits/balance-updates` applies one month of interest. Calling it twice applies two months. `days` is only read: the original calculator never changes it and `TimeDeposit` has no setter for it. The requirements do not define accrual periods, missed months or retries.

## Decision

The service is responsible for applying interest correctly and atomically; the caller (for example a monthly scheduler) is responsible for calling it once per accrual period and for not retrying blindly after a timeout. A concurrent run is detected by optimistic locking and rejected with `409`; nothing is applied in that case.

No idempotency is implemented, because every option changes the observable behaviour of the operation and needs business rules that the requirements do not give:

- **`Idempotency-Key` header** protects against a retried request only, not against two different calls for the same month — and that is the real business risk.
- **Period-based guard** ("interest for October is already applied") needs a definition of the period (clock or request parameter), a rule for missed months and a way to reconcile calendar months with `days`.

## Possible design for production

If `days` is the age of the deposit and another system advances it daily, `days` itself can act as the clock:

- store `last_accrued_day`, the value of `days` at the last accrual;
- accrue only when a new 30-day period has completed: `days / 30 > last_accrued_day / 30`, then set `last_accrued_day = days`;
- missed periods become explicit: accrue `days / 30 - last_accrued_day / 30` months or only one.

This makes accrual idempotent without a calendar, gives `days` a clear role and allows processing in independent chunks (see [0004](0004-paged-single-transaction-update.md)). An `Idempotency-Key` would be added on top if callers cannot guarantee a single call.

## Consequences

- The operation is simple and predictable, and the demo in Swagger can be repeated.
- The caller has to know whether a call succeeded; after a timeout it has to check the balances before retrying.
