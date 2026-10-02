<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Spring Security Sign-In

- **Plan**: context/changes/spring-security-sign-in/plan.md
- **Scope**: Full plan
- **Reviewed phases**: 1, 2, 3
- **Date**: 2026-10-02
- **Verdict**: APPROVED
- **Findings**: 0 critical, 1 warning, 6 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | PASS |
| Scope Discipline | PASS |
| Safety & Quality | WARNING |
| Architecture | PASS |
| Pattern Consistency | PASS |
| Success Criteria | PASS |

Automated checks run during this review: `.\gradlew.bat test` BUILD SUCCESSFUL; `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest` BUILD SUCCESSFUL; `rg "include=\*" src/main/resources/application.properties` returns no match. All 13 Progress rows are `[x]` with a commit SHA, and the manual rows have observable evidence (2.3 and 2.5 were confirmed by the user on Supabase; 3.3 was checked on a local run).

## Findings

### F1 — Session cookie is not Secure behind the Fly proxy

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/resources/application.properties (no session or forwarded-header setting); src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java:22-32
- **Detail**: This change introduces `JSESSIONID` sessions on a public site. Fly terminates TLS at its proxy (`force_https = true`), so the application sees plain HTTP. Without `server.forward-headers-strategy` the request is not treated as secure, the cookie is not marked `Secure`, and Spring Security's HSTS header is not written. The cookie is also sent on the first plain-HTTP request before Fly redirects.
- **Fix**: Add `server.forward-headers-strategy=native`, `server.servlet.session.cookie.secure=true`, and `server.servlet.session.cookie.same-site=lax` to `application.properties`.
  - Strength: Standard Boot properties, no new code or library; closes the cookie exposure before S-01 makes real accounts.
  - Tradeoff: A plain-HTTP local run on a non-localhost host will not keep the session; localhost is treated as secure by current browsers.
  - Confidence: MEDIUM — based on the Fly `force_https` setting and Boot defaults; not verified on a deployed Machine.
  - Blind spot: Not checked against the live Fly response headers.
- **Decision**: FIXED (Fix applied): added server.forward-headers-strategy=native, server.servlet.session.cookie.secure=true and same-site=lax to application.properties; suite green. Not verified on a live Fly Machine.

### F2 — Lookup failure at login has no automated test

- **Severity**: 💡 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Success Criteria
- **Location**: src/test/java/com/kenez92/plateplan/config/SecurityConfigurationTest.java:43-95
- **Detail**: The plan requires that an unreachable database at login ends as `/?error`, not a 500. Only manual row 2.4 covers it; the test stub never throws. Spring Security's `DaoAuthenticationProvider` wraps a non-`UsernameNotFoundException` in `InternalAuthenticationServiceException`, which goes to the failure URL, so the behaviour should hold, but nothing locks it in.
- **Fix**: Add `shouldSendALoginBackToTheLoginWindowWhenTheLookupFails` with a stub `UserDetailsService` that throws `DataAccessResourceFailureException`, expecting `redirectedUrl("/?error")` and `unauthenticated()`.
- **Decision**: FIXED (Fix applied): added shouldSendALoginBackToTheLoginWindowWhenTheLookupFails to SecurityConfigurationTest; the stub throws DataAccessResourceFailureException for one login; suite green.

### F3 — Dead `loggers` access line

- **Severity**: 💡 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Pattern Consistency
- **Location**: src/main/resources/application.properties:19
- **Detail**: `management.endpoint.loggers.access=read-only` no longer does anything because `loggers` is not exposed (`include=health,info`). It reads like an intentionally open endpoint. The plan told us to keep it as defense in depth, so this is a plan decision, not drift.
- **Fix**: Delete the line; keep the `heapdump` and `shutdown` `access=none` lines.
- **Decision**: FIXED (Fix applied): removed management.endpoint.loggers.access from application.properties.

### F4 — Roadmap status still `in-progress`

- **Severity**: 💡 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Plan Adherence
- **Location**: context/foundation/roadmap.md:45 and the F-02 section
- **Detail**: F-02 is `in-progress` while `change.md` is `implemented`. This is normal until the pull request merges.
- **Fix**: Set F-02 to `done` in the roadmap in the same pull request, or when the change is archived.
- **Decision**: FIXED (Fix applied): F-02 set to done in the roadmap table and in the F-02 section.

### F5 — Plan text differs from the implemented tests, and some doc edits are not in the plan

- **Severity**: 💡 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Scope Discipline
- **Location**: plan.md "Phase 2 → Tests" (`@MockitoBean UserDetailsService`); src/test/java/com/kenez92/plateplan/config/SecurityConfigurationTest.java:81-95; AGENTS.md Style; infrastructure.md:121
- **Detail**: The plan says `@MockitoBean`; the implementation uses a nested `@TestConfiguration` stub (`AccountLookupStub`) because the repo test rule forces final fields. The stub uses the real `PasswordEncoder` bean, which is arguably better. Separately, the Phase 2 commit added two `AGENTS.md` Style rules (createTable instead of raw SQL, `@Repository` and final parameters) that follow from the code review amendment but are not listed as `AGENTS.md` changes, and the account-table note sits in the deploy steps (`infrastructure.md:121`) rather than a data section. All harmless; the plan is simply a little behind.
- **Fix**: Add a one-line addendum under Phase 2 in the plan naming the stub and the extra `AGENTS.md` rules.
- **Decision**: FIXED (Fix applied): addendum added under Phase 2 in plan.md.

### F6 — Controller tests still use tabs

- **Severity**: 💡 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Pattern Consistency
- **Location**: src/test/java/com/kenez92/plateplan/controller/HomeControllerTest.java, RegisterControllerTest.java
- **Detail**: `AGENTS.md` says to reformat a file when you change it. Both files were changed (import, annotation, final constructor parameter) but keep tabs. `ActuatorEndpointsTest` was reformatted in phase 3.
- **Fix**: Reformat both files to four spaces.
- **Decision**: FIXED (Fix applied): tabs replaced by four spaces in HomeControllerTest and RegisterControllerTest (whitespace-only change); suite green.

### F7 — Hardening items to carry into S-01 and later

- **Severity**: 💡 OBSERVATION
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java:24; src/main/java/com/kenez92/plateplan/account/Account.java; src/main/resources/db/changelog/changes/002-create-account.xml:17
- **Detail**: None of these is a regression and none can be reached before registration exists. (a) No login throttling or lockout, and `/actuator/health` is public and probes the database on every call (each call can hold a thread for the 5 s Hikari timeout when the database is down). (b) Registration must cap the username at 50 characters (column is `varchar(50)`) and the password at 72 bytes (BCrypt truncates silently). (c) A duplicate-login insert will raise `DataIntegrityViolationException`, a 500 whose message contains the login; check for an existing login first and do not log that message. (d) The permit rules hard-code `/actuator/health` and `/actuator/info`; a changed base path fails closed.
- **Fix**: Record these as notes on roadmap S-01 (and a rate-limiting item), not as work in this change.
- **Decision**: FIXED (Fix applied): notes added to Unknowns of S-01 in the roadmap (length caps, duplicate login handling, throttling).
