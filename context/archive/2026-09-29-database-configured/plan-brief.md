# Configure the Supabase Database — Plan Brief

> Full plan: `context/changes/database-configured/plan.md`

## What & Why

PlatePlan needs a database so sign-in, food preferences, and the confirmed calorie number can survive a later visit (roadmap F-01). This change connects the application to Supabase PostgreSQL through Hibernate, with every schema script installed by Liquibase. It creates no product tables. Connection details come from Fly secrets, not from any tracked file, and the application must keep serving pages when the database is down.

## Starting Point

Today there is no data layer: no JPA, no Liquibase, no driver, no datasource. `application.properties` exposes every Actuator endpoint, and heap dump and shutdown are public with no authentication. CI runs tests with no database and deploys to Fly on merge to `main`.

## Desired End State

The application declares Hibernate (Spring Data JPA), Liquibase, and the PostgreSQL driver and reads `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` from the environment, with no default in the file (a missing variable stops the start). With the database unreachable, it starts, logs that migrations were skipped, pages load, and `/actuator/health` answers 503 within seconds. With the Fly secrets set, health answers `UP` and Liquibase's tracking tables exist in Supabase. Heap dump and shutdown no longer respond, and the Supabase Data API is off.

## Key Decisions Made

| Decision | Choice | Why (1 sentence) |
| -------- | ------ | ---------------- |
| Database | Supabase PostgreSQL | Chosen by the user; external, so account data survives Fly Machine stops. |
| ORM | Hibernate through `spring-boot-starter-data-jpa`, `ddl-auto=none`, no JDBC metadata read at start | Chosen by the user; Hibernate must not change the schema or connect at start. |
| Migrations | Liquibase, XML changelogs, empty master changelog | Chosen by the user; all schema scripts go through Liquibase. |
| Liquibase run | At start, one attempt, failure logged and skipped | Keeps the application starting without a database; a skipped migration is retried at the next start. |
| Connection type | Supabase direct connection, port 5432 | Chosen by the user; IPv6-only host is a known risk with a secrets-only fallback to the session pooler. |
| Secret source | Fly secrets, set once by hand | The running app only sees Fly secrets; CI needs no database and `ci.yml` stays unchanged. |
| Start with an unreachable database | Hikari's own lazy pool, 5 s connection timeout, Hibernate metadata access off, tolerant Liquibase | The pool connects only at first use, and the two start-time callers are neutralized; no `initialization-fail-timeout` override. |
| Data source | Application-owned bean, `DataSourceAutoConfiguration` excluded | Chosen by the user; one explicit place builds a lazy Hikari pool from `spring.datasource.*`, and Hibernate and Liquibase use that bean. |
| Config defaults | None for URL, user, or password | Chosen by the user; Fly always supplies them, and a missing secret fails loudly instead of using a wrong database. |
| Health | DB stays in health, `DOWN` gives 503 | Shows the real state; Fly has no health check configured, so it does not restart the Machine. |
| Heap dump / shutdown | Closed in this change | The database password will be in memory, and a public dump would expose it. |
| Supabase exposure | Disable the Data API, keep `public` | The app uses JDBC only, and the public `anon` key must not read account data. |
| Test | `ApplicationTest` with an unreachable database, plus a unit test of the tolerant Liquibase wrapper | The one layer allowed to boot the app; no Testcontainers needed. |

## Scope

**In scope:** JPA, Liquibase, and driver dependencies; secret-driven datasource and Hibernate properties; empty XML master changelog; tolerant start-time migration; `tech-stack.md`, `AGENTS.md`, and `infrastructure.md` updates; closing `heapdump` and `shutdown` and the Actuator test; disabling the Supabase Data API; setting Fly secrets and verifying on Fly.

**Out of scope:** product tables and entities, Spring Security and Actuator authentication (F-02), migrations from CI or a Fly `release_command`, retry of migrations in the background, RLS or a dedicated schema, Testcontainers, any screen or product logic.

## Architecture / Approach

`application.properties` maps the datasource to three environment variables with no defaults. `Application` excludes `DataSourceAutoConfiguration`, and a configuration class builds the `HikariDataSource` bean from `spring.datasource.*`; its pool opens the first connection only at first use and fails within 5 s when the database is unreachable. Hibernate has an explicit dialect and never generates schema. A small `SpringLiquibase` wrapper runs the XML master changelog once at start and swallows a failure with a safe log line. Fly injects the secrets as environment variables. Closing the two dump/shutdown endpoints ships in the same pull request as the database configuration, and the secrets are staged so they reach the Machine only with that release.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| ----- | ---------------- | -------- |
| 1. JPA and secret-driven datasource | Hibernate, driver, application-owned data source, env-based config, `tech-stack.md`, start-without-DB test | Start can still fail on Hibernate reading metadata, or on the auto-configurations not accepting the custom data source |
| 2. Liquibase that cannot stop the application | Liquibase, empty XML master changelog, tolerant wrapper and its tests | The wrapper must still honor `spring.liquibase.*` and run before Hibernate |
| 3. Close heap dump and shutdown | Endpoints off, test and docs updated | Other Actuator endpoints stay public until F-02 |
| 4. Fly secrets, Supabase settings, live verification | Secret names documented, Data API off, app healthy on Fly against Supabase | Direct host is IPv6 only and Fly may not reach it |

**Prerequisites:** Supabase project with the direct-connection details; Fly access to run `fly secrets set` for `plate-plan`; a feature branch and pull request into `main`.
**Estimated effort:** ~3-4 sessions across 4 phases.

## Open Risks & Assumptions

- The direct host may be unreachable from Fly (IPv6). The fallback is changing two secrets to the session-pooler values.
- With no defaults, the release exits at start unless the three secrets exist. Stage them before merging (`fly secrets set --stage`, flag to be confirmed) so the first deploy starts.
- A migration skipped at start is applied only at the next start; after an outage during a deploy, restart the Machine.
- That the Hikari pool stays idle until first use, and that Hibernate and Liquibase accept the application-owned data source with the auto-configuration excluded, is proven by `ApplicationTest` in Phase 1. `initialization-fail-timeout` is added only if it fails.
- A free Supabase project may pause when idle; the application starts anyway and health shows `DOWN`.
- `/actuator/env` masking is assumed from the Spring Boot default and checked on Fly.

## Success Criteria (Summary)

- The application starts and serves pages when the database is unreachable.
- With the Fly secrets set, `/actuator/health` is `UP` against Supabase and Liquibase's tables exist.
- No password or connection detail is in git, the heap dump is not downloadable, and the Data API is off.
