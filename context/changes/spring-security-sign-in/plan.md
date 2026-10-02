# Spring Security Sign-In Implementation Plan

## Overview

Add Spring Security to PlatePlan as a form-login boundary that is closed by default, add the single `account` table that sign-in reads, and shrink the web-exposed Actuator endpoints to `health` and `info`. This is roadmap F-02. It supplies the boundary, the account lookup, and the password hasher. It does not add registration, automatic sign-in, or any signed-in screen state; those stay in S-01.

## Current State Analysis

- No security code or dependency exists. A case-insensitive search of `src/` for `security`, `UserDetails`, `PasswordEncoder`, `@EnableWebSecurity`, and `csrf` found nothing, and `build.gradle.kts` has no security starter (`build.gradle.kts:20-30`). `tech-stack.md` already names Spring Security as the chosen library (`context/foundation/tech-stack.md:24`).
- The header login form on every page already posts `username` and `password` to `/login` (`src/main/resources/templates/fragments/chrome.html:22-31`), and the register form posts the same fields to `/register` (`templates/register.html:18-25`). No controller handles either POST; `HomeController` and `RegisterController` map only `GET` (`controller/HomeController.java:11-14`, `controller/RegisterController.java:11-14`). The user confirmed this front is final for F-02.
- `application.properties:17-20` exposes every web endpoint (`include=*`), closes `heapdump` and `shutdown` (`access=none`), and leaves `loggers` read-only. Every other exposed endpoint, including `beans`, is unauthenticated (`context/foundation/infrastructure.md:98`). The previous change deferred this to F-02 (`context/archive/2026-09-29-database-configured/reviews/impl-review.md:32-38`).
- The only Liquibase changeSet is `SELECT 1` (`db/changelog/changes/001-test.xml:9-11`). `db.changelog-master.xml:11` includes `changes/` alphabetically. A failed migration at start is logged and skipped (`config/LiquibaseConfiguration.java:46-53`).
- The data source is an application-owned lazy Hikari bean with `DataSourceAutoConfiguration` excluded (`Application.java:7`, `config/DataSourceConfiguration.java:13-28`). `ApplicationTest` boots the full context against an unreachable database (`src/test/java/.../ApplicationTest.java:11-39`).
- Three slice tests exist: `HomeControllerTest` and `RegisterControllerTest` (`@WebMvcTest` on one controller each) and `ActuatorEndpointsTest` (`useDefaultFilters = false`, explicit endpoint auto-configurations).

## Desired End State

Every route is denied to a signed-out visitor except `/`, `/register`, `/css/**`, `/error`, `/actuator/health`, and `/actuator/info`. A signed-out request to any other path redirects to `/`, which is the login window. A visitor can sign in through the existing header form with a login and password stored in the `account` table (BCrypt hash); a wrong login returns to `/?error`. Heap dump, shutdown, `beans`, `env`, `threaddump`, `logfile`, `metrics`, and every other Actuator endpoint other than `health` and `info` are not reachable over the web, signed in or not. Verify with `.\gradlew.bat test` plus the manual steps in each phase.

### Key Discoveries:

- The existing forms already use the field names (`username`, `password`) and `th:action` that Spring Security's form login and Thymeleaf CSRF handling expect (`chrome.html:22-31`, `register.html:18-25`), so no template change is needed.
- A `SecurityFilterChain` declared in an ordinary `@Configuration` class is not picked up by a `@WebMvcTest` slice's component scan, so each slice test must import it explicitly ([Spring Boot testing docs](https://docs.spring.io/spring-boot/4.0/how-to/testing.html)); this is why all three existing slice tests change.
- Spring Boot 4 splits starters: `spring-boot-starter-security` for the library and `spring-boot-starter-security-test` for `@WithMockUser` and MockMvc request helpers to work ([Boot 4.0 migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)).
- Hibernate has `ddl-auto=none` and no JDBC metadata read at start (`application.properties:10-13`), so adding an entity and repository does not need a reachable database at start; `ApplicationTest` is the proof.
- Registration is open, so any "signed-in" gate means "anyone who registered". With no roles, the only safe way to keep internals private is to remove them from the web, not to put them behind login.

## What We're NOT Doing

- No `POST /register`, no automatic sign-in after registration, no password rules or username validation (S-01).
- No signed-in state in the header, no logout button, no error message text for `/?error` (S-01; the front is unchanged in F-02). A logout endpoint is not configured beyond Spring Security's default.
- No profile fields (age, height, weight, sex, goal, activity, calories, preferences) on `account` (S-02, S-03).
- No roles, authorities table, or Spring's stock `users`/`authorities` schema.
- No JDBC or Redis sessions (`spring-session-jdbc` is not in `tech-stack.md`); sessions stay in memory and are lost when the Fly Machine stops or restarts.
- No CSRF exceptions and no CORS configuration.
- No change to the tolerant Liquibase wrapper. A skipped migration is retried at the next start, as accepted in `impl-review.md:62`.
- No new Actuator authentication scheme (HTTP basic or otherwise); endpoints other than `health` and `info` are removed from the web instead.

## Implementation Approach

Three phases, each ending with a green `.\gradlew.bat test`. Phase 1 adds the library and the deny-by-default chain and repairs the existing slice tests, so the boundary is proven before any account exists. Phase 2 adds the account table, entity, repository, and the `UserDetailsService` that the chain reads. Phase 3 closes Actuator and updates the documents that state the old exposure. New classes go in `com.kenez92.plateplan`: `config/SecurityConfiguration` beside the existing config classes, and an `account` package for the entity, repository, and lookup service. Formatting follows `config/LiquibaseConfiguration.java` (four spaces, IntelliJ style).

## Critical Implementation Details

- **State sequencing** — Between Phase 1 and Phase 2 the application has no `UserDetailsService` bean, so Spring Boot creates its in-memory default user and logs a generated password. That password cannot sign in: with a `PasswordEncoder` bean present, Boot stores it as plain text and `BCryptPasswordEncoder` rejects it (confirmed by running the application in Phase 1). Every login attempt in Phase 1 therefore ends at `/?error`, which is all that Phase 1 verifies. This is a transitional state that exists only inside the branch; Phase 2 replaces it, and the pull request merges both phases together.
- **Timing & lifecycle** — `AccountUserDetailsService` queries the database only when a login request arrives, never at start. With the database unreachable, a login attempt must end as a failed login (redirect to `/?error`), not a 500, and the pages must still render.

## Phase 1: Security dependency and deny-by-default chain

### Overview

Add the library and a security configuration that denies everything except the public pages, then make the existing tests pass with it and add a test of the boundary itself.

### Changes Required:

#### 1. Dependencies

**File**: `build.gradle.kts`

**Intent**: Declare Spring Security and its test support. The library rule in `AGENTS.md` allows this because `tech-stack.md` lists it.

**Contract**: add `implementation("org.springframework.boot:spring-boot-starter-security")` and `testImplementation("org.springframework.boot:spring-boot-starter-security-test")` to the `dependencies` block.

#### 2. Security configuration

**File**: `src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java` (new)

**Intent**: One explicit `@Configuration(proxyBeanMethods = false)` class, like the existing config classes, that defines the filter chain and the password encoder. The login window is the home page, so unauthenticated requests and failed logins return there.

**Contract**:
- Bean `SecurityFilterChain`: permit any method on `/`, `/register`, `/css/**`, `/error`; permit `/actuator/health` and `/actuator/info`; `anyRequest().authenticated()`. Form login with `loginPage("/")`, `loginProcessingUrl("/login")`, default success URL `/`, default failure URL `/?error`, parameters `username` and `password`. CSRF stays on (default). Sessions use the default in-memory `HttpSession`.
- Bean `PasswordEncoder`: `BCryptPasswordEncoder` (no extra library needed).
- The class has no database dependency and no constructor collaborators, so it can be imported into slice tests as is.

#### 3. Existing slice tests

**Files**: `src/test/java/com/kenez92/plateplan/controller/HomeControllerTest.java`, `RegisterControllerTest.java`, `ActuatorEndpointsTest.java`

**Intent**: Keep the existing assertions valid under the new boundary, without weakening what they check.

**Contract**: add `@Import(SecurityConfiguration.class)` to each class. In `ActuatorEndpointsTest`, run the existing method as a signed-in user (`@WithMockUser`) and add `.with(csrf())` to the `POST /actuator/shutdown` request, so the current 200/404 assertions still hold; Phase 3 rewrites this test. Follow `.cursor/rules/testing.mdc` (names start with `should`, all locals and fields `final`).

#### 4. Boundary test

**File**: `src/test/java/com/kenez92/plateplan/config/SecurityConfigurationTest.java` (new)

**Intent**: Prove the deny-by-default rule and CSRF without the whole application.

**Contract**: `@ExtendWith(SpringExtension.class)`, `@WebMvcTest(controllers = {HomeController.class, RegisterController.class})`, `@Import(SecurityConfiguration.class)`, constructor-injected `MockMvc`. Test methods:
- `shouldServeThePublicPagesWithoutSignIn`: `GET /`, `GET /register`, `GET /css/site.css` return 200.
- `shouldRedirectAnyOtherPathToTheLoginWindow`: `GET /account/anything` (no mapping) returns 302 with `Location` ending in `/`.
- `shouldRejectALoginPostWithoutACsrfToken`: `POST /login` with `username` and `password` and no CSRF token returns 403.

### Success Criteria:

#### Automated Verification:

- Suite passes with the dependency, the chain, and the repaired slice tests: `.\gradlew.bat test`
- Full context still loads with an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest`

#### Manual Verification:

- With dummy `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` set (see `AGENTS.md`), `.\gradlew.bat bootRun` serves `/` and `/register` with their styling, and opening `/anything` redirects to `/`.
- Submitting the header form with any credentials returns to `/?error`.

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 2: Account table and lookup

### Overview

Create the `account` table through Liquibase, map it with JPA, and give the chain a `UserDetailsService` that reads it.

**Amended after code review:** the changeSet uses `createTable` and `createIndex` instead of raw SQL, the repository uses a plain derived `findByUsernameIgnoreCase` (no `@Query`) and carries `@Repository`, all method parameters are `final`, and the login is case-insensitive through a unique index on `upper(username)` (the expression the derived query compares, so the index serves the lookup and keeps two logins differing only in case from existing). Progress rows 2.3 and 2.5 keep their original titles; read "unique index on `lower(username)`" in 2.3 as "unique index `account_username_upper_idx` on `upper(username)`".

### Changes Required:

#### 1. Account table

**File**: `src/main/resources/db/changelog/changes/002-create-account.xml` (new)

**Intent**: The one sign-in table, for one account with no roles. Follows the pattern of `001-test.xml` (whole changeSet in one file) but uses Liquibase change types, not raw SQL (code review). Never edit it once applied.

**Contract**: changeSet id `002-create-account`, author `plateplan`, a `createTable` for `account` with `id bigint` (auto-increment, primary key), `username varchar(50)` (not null), `password_hash varchar(100)` (not null), and `created_at timestamp with time zone` (not null, default `CURRENT_TIMESTAMP`), plus a `createIndex` named `account_username_upper_idx`, unique, on the computed column `upper(username)`. No other columns.

#### 2. Entity and repository

**Files**: `src/main/java/com/kenez92/plateplan/account/Account.java`, `AccountRepository.java` (new)

**Intent**: Map the table for reading by the sign-in lookup; Hibernate never generates schema (`ddl-auto=none`).

**Contract**: `Account` is a JPA entity on table `account` with fields matching the columns above. `AccountRepository` extends Spring Data's `JpaRepository<Account, Long>` and is annotated `@Repository` and exposes the derived query `Optional<Account> findByUsernameIgnoreCase(final String username)` (no `@Query`; Hibernate builds it).

#### 3. Lookup service

**File**: `src/main/java/com/kenez92/plateplan/account/AccountUserDetailsService.java` (new)

**Intent**: Let Spring Security authenticate against `account`, with no roles.

**Contract**: `@Service` implementing `UserDetailsService`. `loadUserByUsername` returns a `UserDetails` with the stored username, the stored hash, and an empty authority list, or throws `UsernameNotFoundException` when no row matches. It queries only when called, never at start, and does not log the username or any account data (`AGENTS.md` hard rules).

#### 4. Tests

**Files**: `src/test/java/com/kenez92/plateplan/account/AccountUserDetailsServiceTest.java` (new), `SecurityConfigurationTest.java` (extend)

**Intent**: Cover the lookup as a unit and the login flow through the real chain.

**Contract**:
- `AccountUserDetailsServiceTest`: unit test, `@ExtendWith(MockitoExtension.class)` with a mocked `AccountRepository`. `shouldReturnUserDetailsWhenTheAccountExists` compares the whole `UserDetails` with `usingRecursiveComparison()`; `shouldThrowWhenTheAccountDoesNotExist`.
- `SecurityConfigurationTest` additions, using `@MockitoBean UserDetailsService` (the `@Service` is not part of the slice) and `new BCryptPasswordEncoder()` to build the stored hash: `shouldSignInWithTheCorrectPassword` (`formLogin()` request builder; redirect to `/`, authenticated) and `shouldSendAWrongPasswordBackToTheLoginWindow` (redirect to `/?error`, not authenticated).

### Success Criteria:

#### Automated Verification:

- Suite passes, including the new unit and login tests: `.\gradlew.bat test`
- Context loads with the entity and repository and an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest`

#### Manual Verification:

- After the next start against the Supabase database (a Fly deploy or a local run with the real secrets), the `databasechangelog` table has a row for `002-create-account`, and `account` exists with the unique index `account_username_upper_idx` on `upper(username)`.
- With the database unreachable, submitting the header form returns to `/?error` and the page still renders (no 500).
- With the table created, insert one account by hand in Supabase (the hash comes from pgcrypto: `insert into account (username, password_hash) values ('<login>', crypt('<password>', gen_salt('bf')))`; Spring's `BCryptPasswordEncoder` accepts that `$2a$` hash). Signing in through the header form with those values redirects to `/` and the session is authenticated; the same login in different letter case also signs in; a wrong password returns to `/?error`. There is no registration until S-01, so this is the only way to see a successful sign-in on the running application. Delete the test account afterwards.

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 3: Close Actuator and update the documents

### Overview

Remove every Actuator endpoint except `health` and `info` from the web, and correct the documents that say the endpoints are unauthenticated until F-02.

### Changes Required:

#### 1. Actuator exposure

**File**: `src/main/resources/application.properties`

**Intent**: Open registration means "signed in" is not a trust boundary for internals such as `beans` or `env`, so they leave the web.

**Contract**: replace `management.endpoints.web.exposure.include=*` with `management.endpoints.web.exposure.include=health,info`. Keep the `heapdump`, `shutdown`, and `loggers` access lines as defense in depth if the list is ever widened.

#### 2. Actuator test

**File**: `src/test/java/com/kenez92/plateplan/controller/ActuatorEndpointsTest.java`

**Intent**: Replace the Phase 1 stopgap with assertions about the final exposure and the boundary.

**Contract**: keep `@Import(SecurityConfiguration.class)`. Methods: `shouldAnswerHealthAndInfoWithoutSignIn` (200 for `/actuator/health` and `/actuator/info`, no session); `shouldNotShowTheActuatorIndexWithoutSignIn` (`GET /actuator` redirects to `/`); `shouldKeepTheRestOfActuatorClosedEvenWhenSignedIn` (as a signed-in user, the `/actuator` index lists `health` and `info` and does not contain `beans`, `heapdump`, or `shutdown`; `GET /actuator/beans` and `GET /actuator/heapdump` return 404; `POST /actuator/shutdown` with a CSRF token returns 404).

#### 3. Documents

**Files**: `context/foundation/tech-stack.md`, `AGENTS.md`, `context/foundation/infrastructure.md`

**Intent**: State the new reality in place; no text may still say Actuator is unauthenticated until F-02.

**Contract**:
- `tech-stack.md`: add `spring-boot-starter-security` to the declared dependencies and remove "is absent from `build.gradle.kts`" (`tech-stack.md:24`).
- `AGENTS.md` "Layout": Actuator exposes `health` and `info` only; everything else is deny-by-default behind form login; the change list now includes `002-create-account.xml` (the paragraph that says the only change so far is `001-test.xml`).
- `infrastructure.md`: update the passages at `:65` and `:98` (and the repeat near `:121`) to say the other endpoints are not exposed on the web, and note the account table in the data section.

### Success Criteria:

#### Automated Verification:

- Suite passes with the rewritten Actuator test: `.\gradlew.bat test`
- No wildcard exposure remains: `rg "include=\*" src/main/resources/application.properties` returns no match.

#### Manual Verification:

- On a local run or on Fly, `/actuator/health` and `/actuator/info` answer without signing in, and `/actuator/beans` and `/actuator/env` do not return data.
- `AGENTS.md`, `tech-stack.md`, and `infrastructure.md` contain no sentence that says Actuator is unauthenticated until F-02.

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Testing Strategy

### Unit Tests:

- `AccountUserDetailsServiceTest`: existing account maps to `UserDetails` with no authorities; missing account throws `UsernameNotFoundException`.

### Integration Tests:

- `@WebMvcTest` slices with the real `SecurityConfiguration` imported: public pages open, unknown path redirects to `/`, login POST without CSRF is 403, correct password signs in, wrong password goes to `/?error`, Actuator exposure as in Phase 3. No `@SpringBootTest` outside `ApplicationTest` (`.cursor/rules/testing.mdc`).
- `ApplicationTest` stays the single test that boots the context, now with the entity, repository, and security beans.

### Manual Testing Steps:

1. Run with dummy database variables; open `/`, `/register`, and `/anything`.
2. Submit the header form with a wrong login; confirm `/?error`.
3. After a start against Supabase, confirm the `account` table and the changelog row.
4. Check `/actuator/health`, `/actuator/info`, and `/actuator/beans`.

## Performance Considerations

None beyond one indexed query per login attempt. The unique index on `upper(username)` serves the lookup.

## Migration Notes

`002-create-account.xml` adds a table only; nothing existing changes. Rollback is a new changeSet that drops the table, never an edit of the applied file. If the database is unreachable during a deploy, Liquibase skips the migration (`LiquibaseConfiguration`), logins fail with `/?error`, and a Machine restart retries it. There are no accounts until S-01, so no user is locked out by this change.

## References

- Related research: `context/changes/spring-security-sign-in/research.md`
- Roadmap item: `context/foundation/roadmap.md:91-102` (F-02), `:120-129` (S-01)
- Pattern for a config class: `src/main/java/com/kenez92/plateplan/config/LiquibaseConfiguration.java`
- Pattern for a changeSet: `src/main/resources/db/changelog/changes/001-test.xml`
- Prior decisions: `context/archive/2026-09-29-database-configured/reviews/impl-review.md:32-38, 62`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Security dependency and deny-by-default chain

#### Automated

- [x] 1.1 Suite passes with the dependency, the chain, and the repaired slice tests: `.\gradlew.bat test` — b614d8f
- [x] 1.2 Full context still loads with an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest` — b614d8f

#### Manual

- [x] 1.3 With dummy database variables set, `bootRun` serves `/` and `/register` with their styling, and `/anything` redirects to `/` — b614d8f
- [x] 1.4 Submitting the header form with any credentials returns to `/?error` — b614d8f

### Phase 2: Account table and lookup

#### Automated

- [x] 2.1 Suite passes, including the new unit and login tests: `.\gradlew.bat test` — 0c0217d
- [x] 2.2 Context loads with the entity and repository and an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest` — 0c0217d

#### Manual

- [x] 2.3 After a start against Supabase, `databasechangelog` has a row for `002-create-account` and `account` exists with a unique index on `lower(username)` — 0c0217d
- [x] 2.4 With the database unreachable, submitting the header form returns to `/?error` and the page still renders — 0c0217d
- [x] 2.5 With one account inserted by hand in Supabase (pgcrypto hash), the header form signs in (also in different letter case), a wrong password returns to `/?error`, and the test account is deleted afterwards — 0c0217d

### Phase 3: Close Actuator and update the documents

#### Automated

- [x] 3.1 Suite passes with the rewritten Actuator test: `.\gradlew.bat test` — 72e2c3c
- [x] 3.2 No wildcard exposure remains: `rg "include=\*" src/main/resources/application.properties` returns no match — 72e2c3c

#### Manual

- [x] 3.3 On a local run or on Fly, `/actuator/health` and `/actuator/info` answer without signing in, and `/actuator/beans` and `/actuator/env` do not return data — 72e2c3c
- [x] 3.4 `AGENTS.md`, `tech-stack.md`, and `infrastructure.md` contain no sentence that says Actuator is unauthenticated until F-02 — 72e2c3c
