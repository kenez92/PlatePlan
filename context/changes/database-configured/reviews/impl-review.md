<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Configure the Supabase Database

- **Plan**: context/changes/database-configured/plan.md
- **Scope**: Full plan (Phases 1-4 of 4)
- **Reviewed phases**: 1, 2, 3, 4
- **Date**: 2026-09-30
- **Verdict**: NEEDS ATTENTION
- **Findings**: 0 critical, 3 warnings, 4 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | PASS |
| Scope Discipline | PASS |
| Safety & Quality | WARNING |
| Architecture | PASS |
| Pattern Consistency | WARNING |
| Success Criteria | PASS |

Notes: the four user-approved deviations (LiquibaseConfiguration as a SpringLiquibase subclass, `includeAll` with `changes/001-test.xml` holding an inline `SELECT 1`, 4-space style, `getRequiredProperty` on the three datasource keys) were checked against the code and are consistent. All automated criteria were re-run (full suite with `--rerun-tasks`: 6 tests, 0 failures; rg/git grep checks clean). All manual items are ticked with evidence (user confirmation on Fly and Supabase; 404 on `/actuator/heapdump` and masked `/actuator/env` also checked against production).

## Findings

### F1 — `/actuator/loggers` is writable and unauthenticated

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/resources/application.properties:15
- **Detail**: `management.endpoints.web.exposure.include=*` with no Spring Security leaves `loggers` at Boot's default unrestricted access, so an unauthenticated `POST /actuator/loggers/{name}` can raise log levels (for example for Hikari or the PostgreSQL driver). Now that a database URL and password live in the process, more verbose logging can push connection details into Fly logs. The plan left the other endpoints to F-02 ("Other Actuator endpoints stay as they are"), so this is a gap in the plan, not in the implementation.
- **Fix A ⭐ Recommended**: Add `management.endpoint.loggers.access=read-only` now, and leave the rest to F-02.
  - Strength: One line, same mechanism already used for heapdump and shutdown; removes the only writable endpoint.
  - Tradeoff: Changes behaviour outside the written plan scope; read access to logger levels stays public.
  - Confidence: HIGH — `access=read-only` is a documented value for operation-level endpoints.
  - Blind spot: Other read endpoints (`threaddump`, `logfile`, `metrics`, `beans`) still leak internals until F-02.
- **Fix B**: Replace `include=*` with an explicit list (`health,info`) until F-02.
  - Strength: Closes every other endpoint too.
  - Tradeoff: Breaks `ActuatorEndpointsTest` (which expects `beans`) and the plan's statement that the other endpoints stay exposed.
  - Confidence: MEDIUM — depends on whether anything uses `beans` or `metrics` today.
  - Blind spot: Have not checked whether the Fly setup or CI relies on any of the other endpoints.
- **Decision**: FIXED via Fix A (loggers access=read-only)

### F2 — Liquibase catch-all and the 5-minute lock wait

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/config/LiquibaseConfiguration.java:46-53
- **Detail**: `catch (Exception)` swallows every failure, as the plan asks. Two consequences the plan does not mention. (1) If a crash leaves `DATABASECHANGELOGLOCK` set, Liquibase waits for its default 5 minutes at start, inside context refresh, before the web server starts; a deploy can then look unhealthy, and the next start repeats it until the lock row is cleared by hand. (2) A broken changelog or a checksum error is swallowed the same way as "database unreachable", so the application can run against a stale schema with only a WARN line.
- **Fix A ⭐ Recommended**: Lower the lock wait (`spring.liquibase.changelog-lock-wait-time-in-minutes`-style setting applied to `SpringLiquibase`) and log a distinct message for lock and validation errors, keeping the swallow.
  - Strength: Bounded start delay; keeps the plan's "the application always starts" contract.
  - Tradeoff: Two more settings and a little more code.
  - Confidence: MEDIUM — the exact property name in `LiquibaseProperties` for Boot 4.1.1 has not been checked.
  - Blind spot: Not reproduced against a real stuck lock.
- **Fix B**: Accept as is and document the manual lock release (`UPDATE DATABASECHANGELOGLOCK SET locked = false`) in `infrastructure.md`.
  - Strength: No code change.
  - Tradeoff: A 5-minute start stall remains possible.
  - Confidence: HIGH — documentation only.
  - Blind spot: Does not address silent schema drift from swallowed validation errors.
- **Decision**: SKIPPED

### F3 — Hikari pool sized for a large database, not a Supabase pooler

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: src/main/resources/application.properties:6
- **Detail**: Only `connection-timeout=5000` is set. Hikari defaults to `maximumPoolSize=10` and `minimumIdle=10`, so the first `getConnection()` opens ten backend connections. On one Fly Machine with a Supabase free-tier session pooler (the documented fallback) this wastes scarce connection slots and can trigger "max clients reached".
- **Fix**: Set `spring.datasource.hikari.maximum-pool-size=3` and `spring.datasource.hikari.minimum-idle=1`, and note the choice next to the pooler fallback in `infrastructure.md`.
- **Decision**: FIXED (maximum-pool-size=3, minimum-idle=1, noted in infrastructure.md)

### F4 — A changelog file was renamed after the first live apply

- **Severity**: 🔍 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: src/main/resources/db/changelog/changes/001-test.xml (history: `177094c` after live verification in `2fd2428`)
- **Detail**: Liquibase identifies a changeSet by id, author and file path. If an earlier image applied `test.xml` (id `test`) or `001-select-1.xml`, `DATABASECHANGELOG` holds a stale row and `001-test.xml` runs once more. Harmless for `SELECT 1`; the same rename on real DDL would be dangerous.
- **Fix**: In Supabase, check the `FILENAME` column of `DATABASECHANGELOG`; delete a stale row by hand if you want a clean table. Do not rename applied files again.
- **Decision**: ACCEPTED - SELECT 1 is harmless; check FILENAME in DATABASECHANGELOG manually

### F5 — The health-check log line carries the database user name

- **Severity**: 🔍 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: logger `org.springframework.boot.jdbc.health.DataSourceHealthIndicator`
- **Detail**: When the database rejects a login, the driver message (`tenant/user postgres.<project-ref> not found`) is logged at WARN with a full stack trace each time `/actuator/health` is called. The password and full JDBC URL are not logged, so the plan criterion holds, but the Supabase project reference ends up in Fly logs.
- **Fix**: Either accept it, or set `logging.level.org.springframework.boot.jdbc.health.DataSourceHealthIndicator=ERROR` to hide the warning (this also hides the cause).
- **Decision**: ACCEPTED - the cause of the error in the log is more useful than hiding the user name

### F6 — Indentation mix in the files of this change

- **Severity**: 🔍 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Pattern Consistency
- **Location**: src/main/java/com/kenez92/plateplan/config/DataSourceConfiguration.java, src/test/java/com/kenez92/plateplan/ApplicationTest.java, src/test/java/com/kenez92/plateplan/config/LiquibaseConfigurationTest.java
- **Detail**: `LiquibaseConfiguration.java` and the XML changelogs use four spaces (the style you asked for); these three files written in this change still use tabs.
- **Fix**: Reformat these three files to four spaces in one commit.
- **Decision**: FIXED (tabs converted to four spaces in the three files)

### F7 — A test name promises more than it asserts

- **Severity**: 🔍 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Pattern Consistency
- **Location**: src/test/java/com/kenez92/plateplan/ApplicationTest.java:34-36
- **Detail**: `shouldCreateTheDataSourceAndEntityManagerFactoryWithoutConnecting` only asserts that both beans are not null. "Without connecting" is proven indirectly by the context loading against a closed port. The name comes from the plan.
- **Fix**: Keep the name (it is in the plan), or rename to `shouldCreateTheDataSourceAndEntityManagerFactory`.
- **Decision**: FIXED (test renamed to shouldCreateTheDataSourceAndEntityManagerFactory)
