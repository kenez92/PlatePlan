<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Register and Sign In

- **Plan**: context/changes/register-and-sign-in/plan.md
- **Scope**: Full plan
- **Reviewed phases**: 1, 2, 3
- **Date**: 2026-10-02
- **Verdict**: NEEDS ATTENTION
- **Findings**: 0 critical, 4 warnings, 5 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | PASS |
| Scope Discipline | WARNING |
| Safety & Quality | WARNING |
| Architecture | PASS |
| Pattern Consistency | WARNING |
| Success Criteria | PASS |

Automated checks run in this review: `.\gradlew.bat test` BUILD SUCCESSFUL; `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest` BUILD SUCCESSFUL; the 1.3 logging `rg` returns no match; `rg -n "throttl"` matches in `AGENTS.md:39` and `context/foundation/roadmap.md:126`. All 14 Progress rows are `[x]` with a commit SHA; the manual rows were confirmed by the user in the conversation.

## Findings

### F1 — BCrypt hash is stored in the session after registration

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/account/AccountSignInService.java:32-38
- **Detail**: `signIn` puts the `UserDetails` built by `AccountPrincipalService` (it carries the password hash) into the `Authentication` and saves it in the session. Form login goes through `ProviderManager`, which erases the credentials of the principal, so the two sign-in paths store different principals. `AccountSignInServiceTest.signedInContext()` locks the hash in as the expected behaviour.
- **Fix**: Erase the credentials of the principal in `signIn` before building the token (`User` implements `CredentialsContainer`), and change the expected principal in the test.
- **Decision**: FIXED

### F2 — The login can reach the log through Hibernate when two registrations race

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/account/RegistrationService.java:38-39; src/main/resources/application.properties
- **Detail**: The `catch` blocks log nothing, but Hibernate's `SqlExceptionHelper` logs the SQL error before the exception arrives. A unique-index hit on `upper(username)` makes PostgreSQL report `Key (upper((username)::text))=(ALICE) already exists`, so the login ends up in the log. The plan says never to log the login or the exception message. The normal duplicate path (lookup before insert) is not affected; only the race is.
- **Fix A ⭐ Recommended**: Set `logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper=OFF` in `application.properties`.
  - Strength: Closes the leak at the one place that writes the message; one line.
  - Tradeoff: SQL errors from other code paths are no longer logged by Hibernate either.
  - Confidence: MED — the logger name is Hibernate's standard one, but I did not reproduce the race against PostgreSQL.
  - Blind spot: Not verified with a real duplicate race on Supabase.
- **Fix B**: Accept the risk and record it as known (the race needs two simultaneous registrations of the same login).
  - Strength: No loss of diagnostics.
  - Tradeoff: Leaves a login in the log in a rare case, against the plan's own rule.
  - Confidence: HIGH — nothing changes.
  - Blind spot: Logs may be kept or shared by Fly.
- **Decision**: FIXED (Fix A: Hibernate SqlExceptionHelper logger turned off)

### F3 — Database failures at registration leave no trace

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/account/RegistrationService.java:38-42
- **Detail**: Any `DataAccessException` becomes `UNAVAILABLE` and any `DataIntegrityViolationException` becomes `LOGIN_TAKEN`, with no log line. A NOT NULL or length violation, or schema drift, would show the user "Login jest zajęty." while nobody can see the cause in production. This slowed the diagnosis during manual testing of this very change.
- **Fix A ⭐ Recommended**: Log only the exception class names (the pattern `LiquibaseConfiguration` already uses), never the message; update the plan's 1.3 check so it allows that one logger.
  - Strength: Restores a signal in production without exposing the login.
  - Tradeoff: Adds a logger to the `account` package and changes the 1.3 verification command.
  - Confidence: HIGH — same approach as `LiquibaseConfiguration`.
  - Blind spot: Class names of a driver exception can still hint at a constraint name; not verified.
- **Fix B**: Keep it silent as planned and rely on the user-visible messages.
  - Strength: Matches the plan literally.
  - Tradeoff: Production errors stay invisible.
  - Confidence: HIGH — nothing changes.
  - Blind spot: None significant.
- **Decision**: FIXED (Fix A: only exception class names are logged)

### F4 — Roadmap S-01 section still says `in-progress`

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Scope Discipline
- **Location**: context/foundation/roadmap.md:128
- **Detail**: The table row (line 47) was set to `done`, but the S-01 section's `**Status:**` line still says `in-progress`. The two disagree.
- **Fix**: Set the S-01 section status to `done`.
- **Decision**: FIXED

### F5 — Login normalization: look-alike logins and untrimmed header login

- **Severity**: ℹ️ OBSERVATION
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/account/RegistrationService.java:32; src/main/java/com/kenez92/plateplan/account/RegistrationValidator.java:43-45
- **Detail**: `String.trim()` strips only characters up to U+0020. A login of NBSP or U+3000, one with control or zero-width characters, or a different Unicode normalization is accepted, which allows look-alike logins. The header login form does not trim, so " alice" cannot sign in although registration stores "alice". No test uses a surrogate pair.
- **Fix**: Use `strip()`, reject control and format characters, and normalize to NFC; add tests for these cases.
  - Strength: Closes look-alike accounts before there is data to migrate.
  - Tradeoff: New rules to decide (which characters are allowed); changes the stored login for existing users only if they used such characters.
  - Confidence: MED — the exact allowed character set is a product decision.
  - Blind spot: Existing accounts were not scanned for such characters.
- **Decision**: FIXED

### F6 — Session, CSRF token and security-context details of the programmatic sign-in

- **Severity**: ℹ️ OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/account/AccountSignInService.java:34-47; src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java:33,51-54
- **Detail**: The CSRF token is not rotated after registration (form login rotates it); the session id is changed, so the risk is low. `SecurityContextHolder.setContext` is not needed for `saveContext`, and the filter clears it afterwards. The plain `HttpSessionSecurityContextRepository` drops the request-attribute delegate Spring adds by default; harmless today because `/error` is public.
- **Fix**: Leave as is; revisit if error pages ever need the signed-in header.
- **Decision**: FIXED

### F7 — Test conventions and coverage gaps

- **Severity**: ℹ️ OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Pattern Consistency
- **Location**: src/test/java/com/kenez92/plateplan/account/AccountSignInServiceTest.java; AccountUserDetailsServiceTest.java; RegistrationServiceTest.java; HomeControllerTest.java:5-7; RegisterControllerTest.java:11-12
- **Detail**: `@ExtendWith(MockitoExtension.class)` is on three classes that only call `Mockito.mock()` and never use `@Mock`; `testing.mdc` says to add it only when a test needs a mock. In two files the `com.kenez92` import sits between `org.springframework` imports. There is no case where `save` throws `DataAccessException` and none with a surrogate-pair login or password.
- **Fix**: Drop the unneeded extension (or switch to `@Mock`), reorder the imports, and add the two missing cases.
- **Decision**: FIXED (note: @ExtendWith(MockitoExtension.class) kept for strict stubs)

### F8 — Additions outside the plan's wording

- **Severity**: ℹ️ OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Scope Discipline
- **Location**: src/main/resources/application.properties:18; AGENTS.md:25; src/main/resources/static/css/site.css:469-473
- **Detail**: The `SESSION_COOKIE_SECURE` switch and the `AGENTS.md` sentence about it are not in the plan (the Phase 3 contract said "nothing else" for Account flow); both are documented and the default is the safe `true`. The `.form-error` block at the end of `site.css` is indented with spaces (the file uses tabs) and sits after the `prefers-reduced-motion` block. The plan's NOT-DOING list is respected: no throttling, no schema change, no new library except `commons-lang3`.
- **Fix**: Record the `SESSION_COOKIE_SECURE` addition in the plan as an addendum and move the `.form-error` rule next to `.login-error` with tab indentation.
- **Decision**: FIXED (plan addendum, site.css tidy-up)

### F9 — Taken-login message, timing and missing lockout

- **Severity**: ℹ️ OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/account/RegistrationService.java:37-40
- **Detail**: The explicit "Login jest zajęty." message and the early lookup allow user enumeration and a timing signal. Both the message and the absence of throttling are deliberate decisions in the plan and are written in `roadmap.md` and `AGENTS.md`.
- **Fix**: None; noted only.
- **Decision**: SKIPPED (annotation only)
