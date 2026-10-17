---
name: verify
description: Run the full verification of the Java solution — environment preflight, protected-file check, build, tests, ArchUnit, coverage and code style; with "full" also the Docker image and a live smoke test of both endpoints. Use before committing, before a demo, or when asked to check that everything works.
argument-hint: [full]
---

Mode: $ARGUMENTS (empty = build only, `full` = build and Docker smoke test).

Run from `java/`.

0. **Preflight.**
   - `java -version` is 25 or newer; otherwise use `sdk env` (reads `.sdkmanrc`) or set `JAVA_HOME` to a JDK 25.
   - `docker info` succeeds (Testcontainers needs Docker).
   - `git hash-object src/main/java/org/ikigaidigital/TimeDeposit.java` and `git rev-parse :java/src/main/java/org/ikigaidigital/TimeDeposit.java` (from the repository root) both equal `2952ac14618ad804f435813a8c2a059107294cc7`.
1. **Build.** `./mvnw -B -ntp clean verify` must end with `BUILD SUCCESS`. On failure read `target/surefire-reports` and report the root cause.
2. **Coverage.** Line coverage from `target/site/jacoco/jacoco.csv`.
3. **Docker smoke test** (only in `full` mode):
   - `docker compose --profile app up -d --build`;
   - `curl -s --retry 30 --retry-connrefused --retry-all-errors --retry-delay 2 http://localhost:8080/time-deposits` returns the six demo deposits;
   - `curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:8080/time-deposits/balance-updates` returns `204`;
   - a second GET shows updated balances (deposit 1: 1235595.81, deposit 3: 5012.50, deposit 6: 20083.33; deposits 2, 4 and 5 unchanged);
   - `http://localhost:8080/swagger-ui/index.html` returns `200`, `http://localhost:8080/v3/api-docs` returns `404`.
4. **Always clean up** with `docker compose --profile app down -v`, also after a failure.

Report a short table: step, result, notes.
