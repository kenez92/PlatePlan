# Spring Security Sign-In — Plan Brief

> Full plan: `context/changes/spring-security-sign-in/plan.md`
> Research: `context/changes/spring-security-sign-in/research.md`

## What & Why

PlatePlan must show an account's data only to that signed-in account (roadmap F-02). This change adds Spring Security as a boundary that is closed by default, the single `account` table that sign-in reads, and removes internal Actuator endpoints from the web. Registration, automatic sign-in, and signed-in screen state stay in S-01.

## Starting Point

There is no security code or dependency. The header login form already posts `username` and `password` to `/login`, but nothing handles it. `application.properties` exposes every Actuator endpoint, and apart from the closed heap dump, shutdown, and read-only loggers, `beans` and the rest are public. The only changeSet is `SELECT 1`.

## Desired End State

A signed-out visitor sees only `/`, `/register`, `/css/**`, `/error`, `/actuator/health`, and `/actuator/info`; any other path redirects to `/`, the login window. The existing header form signs in against the `account` table (BCrypt hash), and a wrong login returns to `/?error`. Only `health` and `info` exist on the web.

## Key Decisions Made

| Decision | Choice | Why (1 sentence) | Source |
| -------- | ------ | ---------------- | ------ |
| F-02 / S-01 boundary | F-02 = filter, table, account lookup, hasher; no register | Login is testable end to end while S-01 adds only registration and auto sign-in. | Plan |
| Account table | One `account` table: username, password hash, created time; no roles | Matches "no roles" and S-03 can extend it. | Plan |
| Login UX | Keep the existing header form; failure returns to `/?error` | The user confirmed the front is final; no template change. | Plan |
| Actuator | `include=health,info`, both public; the rest off the web | Open registration makes "signed in" meaningless as a gate for `beans` or `env`. | Plan |
| Sessions and CSRF | Default in-memory session, CSRF on | No new library; a Machine stop logs the user out. | Plan |
| Tests | Import the real `SecurityFilterChain` into slice tests | The boundary is tested, as `testing.mdc` requires. | Plan |
| Hasher | `BCryptPasswordEncoder` | Needs no extra library. | Plan |
| Default policy | `anyRequest().authenticated()` with a short permit list | Future routes are protected without remembering to add a rule. | Research |
| Username case | Unique on `lower(username)`, lookup with the same function | Avoids two accounts differing only in case. | Plan |

## Scope

**In scope:** security starters; `SecurityConfiguration`; `002-create-account.xml`; `Account`, `AccountRepository`, `AccountUserDetailsService`; repaired and new tests; Actuator exposure; updates to `tech-stack.md`, `AGENTS.md`, `infrastructure.md`.

**Out of scope:** `POST /register`, auto sign-in, logout button, header signed-in state, error text, profile fields, roles, persistent sessions, Actuator authentication, changes to tolerant Liquibase.

## Architecture / Approach

`config/SecurityConfiguration` defines a `SecurityFilterChain` and a `BCryptPasswordEncoder`, with no database dependency. An `account` package holds the entity, repository, and a `UserDetailsService` that queries only when a login arrives, so the application still starts without a database. Slice tests import the real configuration.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| ----- | ---------------- | -------- |
| 1. Dependency and chain | Starters, deny-by-default chain, repaired slice tests, boundary test | Existing slice tests fail until the configuration is imported |
| 2. Account table and lookup | `002-create-account.xml`, entity, repository, lookup service, login tests | A skipped migration at start leaves logins failing until restart |
| 3. Close Actuator and docs | `include=health,info`, rewritten Actuator test, three documents | Documents still saying "unauthenticated until F-02" |

**Prerequisites:** F-01 is done (database configured, archived). A feature branch and a pull request into `main`.
**Estimated effort:** ~2 sessions across 3 phases.

## Open Risks & Assumptions

- Between Phases 1 and 2 Spring Boot creates a default in-memory user and logs a generated password, but the BCrypt encoder rejects it, so no login succeeds until Phase 2; the PR merges both phases together.
- A successful sign-in can be seen on the running app only after Phase 2, with an account inserted by hand in Supabase (no registration until S-01).
- Sessions are in memory, so a stopped or restarted Fly Machine logs the user out.
- There are no accounts until S-01, so nobody can sign in after this change alone.
- Artifact names `spring-boot-starter-security` and `-security-test` come from external documentation; the build in Phase 1 confirms them.
- The `/?error` failure shows no message because the front is unchanged; S-01 can add one.

## Success Criteria (Summary)

- A signed-out request to any path outside the public list redirects to `/`.
- The header form signs in against `account`, and a wrong password returns to `/?error`.
- Only `/actuator/health` and `/actuator/info` exist on the web, and `.\gradlew.bat test` passes.
