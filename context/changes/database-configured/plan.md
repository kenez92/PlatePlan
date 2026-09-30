# Configure the Supabase Database Implementation Plan

## Overview

Connect PlatePlan to a Supabase PostgreSQL database through Hibernate (Spring Data JPA), with every schema script installed by Liquibase. The URL, user name, and password are read from environment variables that Fly secrets provide; `application.properties` holds only variable names. The application must start and serve pages when the database cannot be reached, so Liquibase runs at start with one attempt whose failure is logged and does not stop the application. Because the database password will sit in process memory, the public heap dump and shutdown endpoints are closed in this same change. No product tables or entities are created (F-02 and later slices own those).

## Current State Analysis

- No data layer exists: `build.gradle.kts` has no JPA starter, no Liquibase, and no database driver (`build.gradle.kts:20-27`). `application.properties` has no datasource (`src/main/resources/application.properties:1-5`).
- `AGENTS.md` allows a new library only when `context/foundation/tech-stack.md` lists it, so that file changes in this plan. It lists no database library today.
- Fly config sets only `SERVER_ADDRESS` and `SERVER_PORT` (`fly.toml:18-20`, `Dockerfile:11-12`). `infrastructure.md` already says secrets go through `fly secrets set` and never into `fly.toml`.
- `ci.yml` runs `./gradlew test` with no database and deploys to Fly on a push to `main` (`.github/workflows/ci.yml:10-40`). Merging the pull request deploys. GitHub Actions has no IPv6, and the chosen Supabase direct host is IPv6 only, so migrations cannot run from CI.
- `application.properties` exposes every Actuator endpoint and sets `heapdump` and `shutdown` to `unrestricted` (`application.properties:3-5`). No Spring Security is present, so both are public. `ActuatorEndpointsTest` asserts that public exposure (`src/test/java/com/kenez92/plateplan/controller/ActuatorEndpointsTest.java:25-66`).
- `ApplicationTest.shouldLoadContext` uses `@SpringBootTest` (`src/test/java/com/kenez92/plateplan/ApplicationTest.java:6-13`). It is the one place allowed to boot the application, so it is where "starts without a database" is proven.

## Desired End State

- `build.gradle.kts` declares `spring-boot-starter-data-jpa` (Hibernate), Liquibase, and the PostgreSQL driver; `tech-stack.md` lists them.
- `application.properties` reads `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`. No host, user, or password is written in any tracked file.
- Hibernate never changes the schema (`ddl-auto=none`) and does not read JDBC metadata at start. Liquibase XML changelogs under `src/main/resources/db/changelog/` are the only way the schema changes. The master changelog includes one changeSet, a `SELECT 1` smoke test that creates nothing.
- The three variables are required and have no default in the file. A missing variable stops the start with an unresolved-placeholder error, so a forgotten Fly secret is loud instead of silently pointing at a wrong database.
- With the three variables pointing at an unreachable database (for example a closed local port), the application starts, logs that migrations were skipped, and `GET /` answers 200. `/actuator/health` answers 503 (`DOWN`) within a few seconds instead of hanging.
- With the three Fly secrets set to the Supabase direct connection, `/actuator/health` answers `UP` and Supabase contains `DATABASECHANGELOG` and `DATABASECHANGELOGLOCK`.
- `/actuator/heapdump` and `/actuator/shutdown` do not respond (404). The Supabase Data API is disabled for the project.

### Key Discoveries:

- Fly always supplies the three variables before the application starts, so the file has no default for any of them. The consequence is that every run needs them: `ApplicationTest` sets them in its annotation, `@WebMvcTest` slices never bind the datasource, and a local `bootRun` needs them exported (a dummy unreachable URL is enough).
- The application owns its `DataSource`: `DataSourceAutoConfiguration` is excluded and a configuration class builds the bean. Built through `DataSourceBuilder`, the Hikari data source uses the no-argument constructor, so the pool opens its first connection only at the first `getConnection()`. The callers that would do that during start are Hibernate (stopped by `allow_jdbc_metadata_access=false` and an explicit dialect) and Liquibase (the tolerant wrapper). `hikari.initialization-fail-timeout=-1` is therefore not needed, and `ApplicationTest` proves the start without it; add it only if that test shows otherwise.
- Excluding `DataSourceAutoConfiguration` also removes the `DataSourceProperties` bean and the binding of `spring.datasource.*`, so the new configuration class must recreate both. `HibernateJpaAutoConfiguration` and `LiquibaseAutoConfiguration` need only a `DataSource` bean and accept the application's own; `ApplicationTest` proves that too.
- Spring Boot 4.1 also has `spring.datasource.connection-fetch=lazy` (a proxy that fetches JDBC connections as late as possible inside transactions). It does not stop Hibernate or Liquibase from connecting at start, so it is not used here.
- Hibernate connects at start to detect the dialect unless `hibernate.boot.allow_jdbc_metadata_access` is false and a dialect is set explicitly.
- Liquibase run by Spring Boot connects at start and, on failure, stops the context. Boot's `LiquibaseAutoConfiguration` backs off when a `SpringLiquibase` bean already exists, so a tolerant replacement must still honor `spring.liquibase.*` and must still run before the `EntityManagerFactory`.
- The DB health indicator uses the pool's connection timeout. Its default of 30 s would make `/actuator/health` hang, so it is set to 5000 ms.
- Supabase exposes the `public` schema through its Data API with a public `anon` key. The application connects over JDBC and does not need that API, so it is disabled.
- Env values are masked in `/actuator/env` by default (Spring Boot 3+); verify that rather than assume it (Phase 4).

## What We're NOT Doing

- No tables, no entities, no repositories. The master changelog holds only a `SELECT 1` smoke-test changeSet; only Liquibase's own tracking tables appear.
- No Spring Security and no authentication on Actuator. F-02 owns that; here only `heapdump` and `shutdown` are closed. F-02's sign-in tables will be added as Liquibase changelogs.
- No change to `ci.yml`. GitHub secrets are not used and migrations do not run from CI.
- No Fly `release_command` and no background retry of migrations. If the database is down at start, migrations are skipped until the next start.
- No Testcontainers and no Docker-based test database.
- No default values for the three variables, no `.env` file, and no `application-local.properties`. A local run exports the three variables in the shell: real Supabase values for a live database, or a dummy unreachable URL to run without one.
- No `spring.main.lazy-initialization`, no `spring.datasource.connection-fetch=lazy`, and no `initialization-fail-timeout` override. The lazy pool plus the two settings under Phase 1 and the wrapper under Phase 2 are enough.
- No custom health indicator or metrics for the data source; the standard ones attach to any `DataSource` bean.
- No RLS policies and no dedicated schema; the project keeps `public` with the Data API off.
- No change to the calorie formula, PDFs, model, or any screen.

## Implementation Approach

Phase 1 adds JPA, the driver, an application-owned `DataSource` (auto-configuration excluded) that reads the secrets and never connects at creation, and Hibernate that never touches the schema. Phase 2 adds Liquibase and a small wrapper so one failed attempt cannot stop the application, plus the empty master changelog. Phase 3 closes `heapdump` and `shutdown` and updates the test and the docs that describe them. Phase 4 documents the secret names and the Supabase settings, then performs the human steps on Fly. Phases 1 to 3 ship in one pull request, so the release that first runs with a database secret already has the dump endpoint closed.

## Critical Implementation Details

- **State sequencing** — the release that contains Phases 1 to 3 does not start without the three secrets, and CI deploys it on merge. Create the Fly app once (`fly apps create plate-plan`), then run `fly secrets set --stage ...` before merging, so the secrets reach the Machine only with that release, which already has heap dump and shutdown closed (confirm the `--stage` flag with `fly secrets set --help`). If the flag is unavailable, merge first: the release exits at start without secrets, exposes nothing while down, and `fly secrets set` afterwards starts it. Never put the secrets on a release that still has the public heap dump.
- **State sequencing** — Supabase direct connection (`db.<project-ref>.supabase.co:5432`) is IPv6 only. If Fly cannot reach it, the health check stays `DOWN` and the log shows a network or unknown-host error. The fallback is a change of the `DATABASE_URL` and `DATABASE_USERNAME` secrets to the session pooler values (host from the Supabase dashboard, user `postgres.<project-ref>`, port 5432). No code change is needed.
- **Timing & lifecycle** — a skipped migration is retried only at the next start. Fly stops an idle Machine and starts it on the next request, so a restart happens often; after a database outage during a deploy, `fly machine restart` applies pending migrations.

## Phase 1: JPA and secret-driven datasource

### Overview

Add Hibernate (through Spring Data JPA) and the PostgreSQL driver, register them in `tech-stack.md`, and configure a datasource that reads secrets from the environment and never connects at start.

### Changes Required:

#### 1. Dependencies

**File**: `build.gradle.kts`

**Intent**: Give the application Hibernate through Spring Data JPA and a PostgreSQL driver, at the versions the Spring Boot dependency management already provides.

**Contract**: `implementation("org.springframework.boot:spring-boot-starter-data-jpa")` and `runtimeOnly("org.postgresql:postgresql")` in the `dependencies` block. `spring-boot-starter-data-jpa` exists for Spring Boot 4.1.1 on Maven Central (checked while planning).

#### 2. Datasource and Hibernate configuration

**File**: `src/main/resources/application.properties`

**Intent**: Point the datasource at environment variables so no connection detail is written in the file, make a failed connection fail fast, and stop Hibernate from touching the schema or the database at start.

**Contract**: `spring.datasource.url=${DATABASE_URL}`, `spring.datasource.username=${DATABASE_USERNAME}`, `spring.datasource.password=${DATABASE_PASSWORD}` (no defaults), `spring.datasource.hikari.connection-timeout=5000`, `spring.jpa.hibernate.ddl-auto=none`, `spring.jpa.open-in-view=false`, an explicit PostgreSQL dialect, and `spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false`. Nothing else in the file names a host, user, or password. Do not log the URL or the password.

#### 3. Application-owned data source

**Files**: `src/main/java/com/kenez92/plateplan/Application.java` and a new configuration class in `src/main/java/com/kenez92/plateplan/config/` (for example `DataSourceConfiguration`)

**Intent**: Take the data source out of Spring Boot's auto-configuration and create it in one place the application controls, without opening a connection when the bean is created.

**Contract**: `Application` excludes `DataSourceAutoConfiguration` (`org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration` in Spring Boot 4.1.1), through `@SpringBootApplication(exclude = ...)` or `@EnableAutoConfiguration(exclude = ...)`. The configuration class publishes `DataSourceProperties` and one `HikariDataSource` bean, mirroring what the excluded auto-configuration did: the property names stay `spring.datasource.url`, `.username`, `.password`, and `spring.datasource.hikari.*`, and the bean is built through `DataSourceBuilder` so the pool stays idle until first use. Hibernate and Liquibase receive this bean. Nothing in the class logs the URL, user, or password.

#### 4. Tech stack record

**File**: `context/foundation/tech-stack.md`

**Intent**: List the new libraries and the database so the `AGENTS.md` rule for adding libraries holds.

**Contract**: In "Why this stack", add `spring-boot-starter-data-jpa` (Hibernate) and the PostgreSQL driver to the declared dependencies, and state that the database is Supabase PostgreSQL, that credentials come from Fly secrets, and that schema changes go only through Liquibase (added in Phase 2).

#### 5. Agent guide

**File**: `AGENTS.md`

**Intent**: Tell future agents where the connection settings come from, that the application starts without a database, that the data source is built by the application rather than auto-configured, and that Hibernate does not own the schema.

**Contract**: In "Layout", add that the `DataSource` is created in `config/DataSourceConfiguration` with `DataSourceAutoConfiguration` excluded, that `application.properties` reads `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` from the environment (Fly secrets in production), that no value is written in the repository and there is no default (a missing variable stops the start), that the application starts when the database is unreachable, and that Hibernate runs with `ddl-auto=none`.

#### 6. Start-without-database test

**File**: `src/test/java/com/kenez92/plateplan/ApplicationTest.java`

**Intent**: Prove the context loads when the database cannot be reached, independent of the environment the test runs in, with the application-owned data source and Hibernate accepting it.

**Contract**: `@SpringBootTest` gets `properties` that set `DATABASE_URL` to `jdbc:postgresql://127.0.0.1:1/plateplan` and dummy `DATABASE_USERNAME` and `DATABASE_PASSWORD`. Keep `shouldLoadContext`. Add `shouldCreateTheDataSourceAndEntityManagerFactoryWithoutConnecting`, which receives `DataSource` and `EntityManagerFactory` through the constructor (final fields) and asserts neither is null. Follow `.cursor/rules/testing.mdc`: names start with `should`, tabs, `final` fields and locals.

### Success Criteria:

#### Automated Verification:

- Full suite passes with no `DATABASE_*` variables set: `.\gradlew.bat test`
- Context loads with an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest`
- JPA and the driver are declared and listed: `rg "spring-boot-starter-data-jpa|postgresql" build.gradle.kts context/foundation/tech-stack.md` matches both files for each name
- No connection detail in tracked runtime files: `git grep -nEi "supabase|jdbc:postgresql://[a-z0-9.-]+\.[a-z]" -- src fly.toml Dockerfile .github` prints nothing

#### Manual Verification:

- With `DATABASE_URL=jdbc:postgresql://127.0.0.1:1/plateplan` and dummy `DATABASE_USERNAME` and `DATABASE_PASSWORD` exported, `.\gradlew.bat bootRun` starts, and `http://localhost:8080/` answers 200
- With the three variables unset, `.\gradlew.bat bootRun` stops with an error naming the missing placeholder
- In the same run, `http://localhost:8080/actuator/health` answers 503 within about 10 seconds, and the log contains no password or full URL

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 2: Liquibase that cannot stop the application

### Overview

Add Liquibase with an empty XML master changelog and make the start-time migration tolerant: one attempt, and a failure is logged and swallowed.

### Changes Required:

#### 1. Dependency

**File**: `build.gradle.kts`

**Intent**: Add Liquibase at the version Spring Boot manages.

**Contract**: `implementation("org.springframework.boot:spring-boot-starter-liquibase")`. The artifact exists for Spring Boot 4.1.1 on Maven Central (checked while planning). Record it in `tech-stack.md`.

#### 2. Master changelog

**File**: `src/main/resources/db/changelog/db.changelog-master.xml`

**Intent**: Give every future schema change one place to be included from, without creating any table now.

**Contract**: A valid Liquibase XML `databaseChangeLog` that includes `db/changelog/001-select-1.xml`: one changeSet whose `<sqlFile>` runs `db/scripts/select-1.sql` (`SELECT 1;`, creates nothing). SQL scripts live in `src/main/resources/db/scripts/`. Future changes add XML files under `db/changelog/` and `<include>` them here; nobody edits a changeSet that has been applied. `application.properties` sets `spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml`.

#### 3. Tolerant start-time migration

**File**: `src/main/java/com/kenez92/plateplan/config/` (new class `LiquibaseConfiguration`, a `@Configuration` that extends `SpringLiquibase` and is built through its constructor)

**Intent**: Run the Liquibase update once at start, and when the database is unreachable or the update fails, log one line and let the application continue.

**Contract**: A `SpringLiquibase` whose `afterPropertiesSet()` catches the failure of the update and does not rethrow it. It honors the `spring.liquibase.*` properties (change log, enabled flag), so `spring.liquibase.enabled=false` still turns it off, and it still completes before the `EntityManagerFactory` is built. The log line contains the exception class and message with no URL, user, or password; if a driver message includes the host, log only the exception class. Wire it as the `SpringLiquibase` bean, or wrap the auto-configured one; the plan requires only the behavior above.

#### 4. Tests

**File**: `src/test/java/com/kenez92/plateplan/config/LiquibaseConfigurationTest.java`

**Intent**: Prove the wrapper does not throw when the database is unreachable, without a Spring context.

**Contract**: Unit test with `@ExtendWith(MockitoExtension.class)`. A mocked `DataSource` whose `getConnection()` throws `SQLException` is set on the instance, and `afterPropertiesSet()` completes without an exception (`shouldNotThrowWhenTheDatabaseIsUnreachable`). `ApplicationTest` from Phase 1 already covers the same behavior inside the context, now with Liquibase active.

#### 5. Docs

**Files**: `context/foundation/tech-stack.md`, `AGENTS.md`

**Intent**: Record that Liquibase XML is the only way the schema changes and that a migration failure at start does not stop the application.

**Contract**: `tech-stack.md` lists the Liquibase artifact and the changelog location. `AGENTS.md` "Layout" states: schema changes only as Liquibase XML changelogs included from `db.changelog-master.xml`, Hibernate never generates schema, and a failed migration at start is logged and skipped until the next start.

### Success Criteria:

#### Automated Verification:

- Suite passes, including the new unit test: `.\gradlew.bat test`
- Context loads with Liquibase active and an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest`
- The master changelog exists and holds only the smoke-test changeSet: `Test-Path src/main/resources/db/changelog/db.changelog-master.xml` is true, `rg -c "<changeSet" src/main/resources/db` shows one match (`001-select-1.xml`), and `rg -i "createTable|dropTable|alterTable" src/main/resources/db` prints nothing
- Hibernate never generates schema: `rg "ddl-auto" src` shows only `none`

#### Manual Verification:

- `.\gradlew.bat bootRun` with the dummy unreachable variables from Phase 1 starts, logs one line saying the migration was skipped, and that line contains no password or full URL

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 3: Close heap dump and shutdown

### Overview

Remove the public heap dump and shutdown endpoints so the database password in memory cannot be downloaded. Update the test and the documents that describe the old behavior.

### Changes Required:

#### 1. Endpoint access

**File**: `src/main/resources/application.properties`

**Intent**: Stop `heapdump` and `shutdown` from answering.

**Contract**: `management.endpoint.heapdump.access=none` and `management.endpoint.shutdown.access=none` replace the two `unrestricted` lines. If either endpoint still appears at `/actuator` afterwards, also add it to `management.endpoints.web.exposure.exclude`. Other Actuator endpoints stay as they are; F-02 decides on them.

#### 2. Actuator test

**File**: `src/test/java/com/kenez92/plateplan/controller/ActuatorEndpointsTest.java`

**Intent**: The test currently requires public `heapdump` and `shutdown`. Replace that with a check that they are gone while health and beans still answer.

**Contract**: Rename the method to describe the new behavior (starts with `should`). `/actuator` index contains `/actuator/beans`, `/actuator/health`, and `/actuator/info` and does not contain `/actuator/heapdump` or `/actuator/shutdown`; `GET /actuator/heapdump` and `POST /actuator/shutdown` return 404. Keep the existing `@WebMvcTest(useDefaultFilters = false)` slice and its imports.

#### 3. Documents

**Files**: `AGENTS.md`, `context/foundation/infrastructure.md`

**Intent**: Remove the statement that heap dump and shutdown are unrestricted, so the guide and the risk register match the code.

**Contract**: In `AGENTS.md` "Layout", drop "with heap dump and shutdown unrestricted" and say that heap dump and shutdown are closed while the other endpoints stay exposed. In `infrastructure.md`, the risk-register row "Heap dump and shutdown are public and unauthenticated" and the passages that repeat it (Devil's Advocate item 2, Getting Started step 2) state that both are closed since `database-configured`, and that the remaining endpoints are still unauthenticated until F-02.

### Success Criteria:

#### Automated Verification:

- Suite passes with the updated Actuator test: `.\gradlew.bat test`
- No file still says the endpoints are unrestricted: `rg -i "heap dump and shutdown unrestricted|heapdump.access=unrestricted" AGENTS.md src context/foundation` prints nothing

#### Manual Verification:

- With `.\gradlew.bat bootRun`, `GET http://localhost:8080/actuator/heapdump` answers 404 and `POST http://localhost:8080/actuator/shutdown` answers 404
- `http://localhost:8080/actuator/health` still answers

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 4: Fly secrets, Supabase settings, and live verification

### Overview

Document the secret names and the Supabase settings and connect the deployed application to Supabase. The steps that need Fly and Supabase access are done by the human.

### Changes Required:

#### 1. Secret and Supabase instructions

**File**: `context/foundation/infrastructure.md`

**Intent**: Replace the single `DATABASE_URL` example with the three secrets the application actually reads, record the direct-connection choice and its fallback, and record the Supabase Data API decision.

**Contract**: Getting Started step 5 lists `DATABASE_URL` (`jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require`), `DATABASE_USERNAME` (`postgres`), and `DATABASE_PASSWORD`, set with one `fly secrets set ... --app plate-plan`. State that the application does not start without all three, so they are staged before the deploy (see Critical Implementation Details). Add: the session-pooler fallback from Critical Implementation Details; that Liquibase runs at start and a failed attempt is skipped until the next start; that the Supabase Data API must stay disabled because the application uses JDBC and account data must not be readable with the public `anon` key; and that migrations cannot run from GitHub Actions because the direct host is IPv6 only. The document does not contain a real host, project reference, or password.

#### 2. Supabase and Fly (human)

**Contract**: In the Supabase dashboard, disable the Data API for the project. Create the Fly app if it does not exist (`fly apps create plate-plan --org personal`), then run `fly secrets set --stage DATABASE_URL=... DATABASE_USERNAME=... DATABASE_PASSWORD=... --app plate-plan` from a shell whose history is not shared. Open a pull request from the feature branch into `main` with Phases 1 to 3; CI runs `./gradlew test`; merging deploys, and the new Machine starts with the secrets and runs Liquibase. If `--stage` is not available, merge first and run `fly secrets set` after the deploy, as described in Critical Implementation Details. `fly secrets list` shows names only.

#### 3. Roadmap

**File**: `context/foundation/roadmap.md`

**Intent**: Reflect that F-01 has moved from backlog to work.

**Contract**: Status of F-01 is `planning` in the "At a glance" table and in its body. This is done while saving the plan. The implementation skills advance it later.

### Success Criteria:

#### Automated Verification:

- Full suite passes: `.\gradlew.bat test`
- The three secret names appear in the infrastructure notes and no secret value does: `rg "DATABASE_USERNAME" context/foundation/infrastructure.md` matches, and `git grep -nEi "supabase\.co" -- src fly.toml Dockerfile .github` prints nothing

#### Manual Verification:

- After the deploy with the staged secrets, `fly status` shows one Machine that started and stays up, and `https://plate-plan.fly.dev/` answers 200
- The Supabase Data API is disabled, and a request to the project's `/rest/v1/` with the `anon` key does not return data
- On that deploy, `https://plate-plan.fly.dev/actuator/health` answers `{"status":"UP"}` and the Supabase table editor shows `DATABASECHANGELOG` and `DATABASECHANGELOGLOCK`
- `fly logs --no-tail` shows no password and no full database URL
- `https://plate-plan.fly.dev/actuator/heapdump` answers 404 and `https://plate-plan.fly.dev/actuator/env` shows masked values, not the password
- With a deliberately wrong `DATABASE_PASSWORD` secret, the site at `/` still answers 200, `/actuator/health` answers 503, and the log says the migration was skipped; restoring the correct value and the restart returns health to `UP`

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Testing Strategy

### Unit Tests:

- `LiquibaseConfigurationTest`: a mocked `DataSource` that throws `SQLException` on `getConnection()`; `afterPropertiesSet()` does not throw.

### Integration Tests:

- `ApplicationTest` (`@SpringBootTest`, the only layer allowed to boot the application): with Liquibase active and an unreachable database, the context loads and a `DataSource` and an `EntityManagerFactory` exist.
- `ActuatorEndpointsTest` (`@WebMvcTest(useDefaultFilters = false)`): `heapdump` and `shutdown` are absent and return 404; `health` and `beans` answer.
- DB health returning 503 is not automated: a `@WebMvcTest` slice does not load a `DataSource`, and booting the application for one endpoint is against the test rules. It is checked manually in Phases 1 and 4.

### Manual Testing Steps:

1. Export a dummy unreachable `DATABASE_URL` (`jdbc:postgresql://127.0.0.1:1/plateplan`) with dummy user and password and run `.\gradlew.bat bootRun`: the page loads, the log shows one skipped-migration line, health is 503 within about 10 s. With the variables unset, the start stops on the missing placeholder.
2. Set the three variables in the shell to the Supabase values and run again: health is `UP` and the two Liquibase tables exist. This step is optional locally, and needs a network that can reach the direct host.
3. On Fly, follow the Phase 4 manual list.

## Performance Considerations

Hikari's connection timeout is 5 s, so a start without the database waits up to 5 s inside the failed migration attempt, and a request that needs the database fails within 5 s rather than 30 s. With no database, the pool retries in the background; the log shows repeated warnings without secrets.

## Migration Notes

The only schema objects are Liquibase's two tracking tables in `public`. Rollback is `fly deploy --image` of the previous image; secrets stay set and are simply unused by the old image, and the tracking tables remain. The Supabase project is external and is not affected by an app rollback or `fly apps destroy`. The free Supabase tier may pause an idle project; the application still starts, and health shows `DOWN` until the project is resumed and the Machine restarts.

## References

- Roadmap item: `context/foundation/roadmap.md` (F-01, Change ID `database-configured`)
- Infrastructure and secrets: `context/foundation/infrastructure.md`
- Test conventions: `.cursor/rules/testing.mdc`
- Current Actuator configuration: `src/main/resources/application.properties:1-5`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: JPA and secret-driven datasource

#### Automated

- [x] 1.1 Full suite passes with no `DATABASE_*` variables set: `.\gradlew.bat test` — c5a4959
- [x] 1.2 Context loads with an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest` — c5a4959
- [x] 1.3 JPA and the driver are declared in `build.gradle.kts` and listed in `tech-stack.md` — c5a4959
- [x] 1.4 No connection detail in tracked runtime files: the `git grep` over `src fly.toml Dockerfile .github` prints nothing — c5a4959

#### Manual

- [x] 1.5 `bootRun` with the dummy unreachable `DATABASE_*` variables starts and `/` answers 200 — c5a4959
- [x] 1.6 `bootRun` with the three variables unset stops with an error naming the missing placeholder — c5a4959
- [x] 1.7 In the dummy-variable run `/actuator/health` answers 503 within about 10 seconds and the log has no password or full URL — c5a4959

### Phase 2: Liquibase that cannot stop the application

#### Automated

- [x] 2.1 Suite passes, including the new unit test: `.\gradlew.bat test` — 5e4bd00
- [x] 2.2 Context loads with Liquibase active and an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest` — 5e4bd00
- [x] 2.3 The master changelog exists and holds only the `SELECT 1` smoke-test changeSet — 5e4bd00
- [x] 2.4 Hibernate never generates schema: `rg "ddl-auto" src` shows only `none` — 5e4bd00

#### Manual

- [x] 2.5 `bootRun` with the dummy unreachable variables starts and logs one skipped-migration line with no password or full URL — 5e4bd00

### Phase 3: Close heap dump and shutdown

#### Automated

- [x] 3.1 Suite passes with the updated Actuator test: `.\gradlew.bat test` — 9f12376
- [x] 3.2 No file still says the endpoints are unrestricted: the `rg` check prints nothing — 9f12376

#### Manual

- [x] 3.3 Locally `GET /actuator/heapdump` and `POST /actuator/shutdown` answer 404 — 9f12376
- [x] 3.4 Locally `/actuator/health` still answers — 9f12376

### Phase 4: Fly secrets, Supabase settings, and live verification

#### Automated

- [x] 4.1 Full suite passes: `.\gradlew.bat test`
- [x] 4.2 The three secret names appear in the infrastructure notes and no secret value appears in tracked files

#### Manual

- [ ] 4.3 After the deploy with the staged secrets, `fly status` shows one Machine that stays up and `/` answers 200 on Fly
- [ ] 4.4 The Supabase Data API is disabled and an `anon` request to `/rest/v1/` returns no data
- [ ] 4.5 On that deploy `/actuator/health` on Fly answers `UP` and Supabase shows the two Liquibase tables
- [ ] 4.6 `fly logs --no-tail` shows no password and no full database URL
- [ ] 4.7 On Fly `/actuator/heapdump` answers 404 and `/actuator/env` shows masked values
- [ ] 4.8 With a wrong `DATABASE_PASSWORD` secret `/` answers 200, health answers 503, and the log says the migration was skipped; the correct value restores `UP`
