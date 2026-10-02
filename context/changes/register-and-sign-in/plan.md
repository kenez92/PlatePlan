# Register and Sign In Implementation Plan

## Overview

Make the existing login and register screens work. `POST /register` creates an `account` row and signs the new user in at once; a later visit signs in again from the header form (already served by F-02). The header shows who is signed in and offers sign-out, and the failed-login and registration errors get a visible message. This is roadmap S-01 (FR-001, FR-002, US-01). It adds no table, no library, and no profile fields. Login throttling is deliberately deferred.

## Current State Analysis

- F-02 supplies the boundary: `SecurityConfiguration` denies by default, permits `/`, `/register`, `/css/**`, `/error`, `/actuator/health`, `/actuator/info`, uses form login on `/login` with failure URL `/?error`, and defines `BCryptPasswordEncoder` (`src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java:21-38`). Logout is not configured; Spring's default `POST /logout` exists but redirects to `/login?logout`, which the chain does not know.
- `account` has `username varchar(50)`, `password_hash varchar(100)`, `created_at`, and a unique index on `upper(username)` (`src/main/resources/db/changelog/changes/002-create-account.xml:10-27`). `Account` has a public two-argument constructor and getters only (`account/Account.java:33-38`). `AccountRepository.findByUsernameIgnoreCase` exists (`account/AccountRepository.java:12`).
- `AccountUserDetailsService` maps an `Account` to a `UserDetails` with an empty authority list in a private method (`account/AccountUserDetailsService.java:31-36`). Registration must produce the same principal shape.
- `RegisterController` maps only `GET /register` (`controller/RegisterController.java:11-14`). The form posts `username` and `password` with `th:action="@{/register}"` and has no error area and no length limits (`templates/register.html:19-29`).
- The header always renders the login form and a Register link (`templates/fragments/chrome.html:21-35`); there is no signed-in state, no logout, and nothing reads `?error`. `HomeController` returns the same public page for everyone (`controller/HomeController.java:9-14`).
- The Thymeleaf Spring Security extras are not in `tech-stack.md`, so `sec:authorize` is not available. Thymeleaf 3.1 also no longer exposes `#request`; the signed-in login has to reach the view through the model.
- Tests: `RegisterControllerTest` is a `@WebMvcTest` slice with the real `SecurityConfiguration` imported (`src/test/.../controller/RegisterControllerTest.java:19-39`); `SecurityConfigurationTest` covers login with a nested `AccountLookupStub` (`src/test/.../config/SecurityConfigurationTest.java:92-113`). `.cursor/rules/testing.mdc` requires final fields, constructor injection, and whole-object comparison.
- Carried over from the F-02 review (F7, `context/archive/2026-10-01-spring-security-sign-in/reviews/impl-review.md`): cap the login at 50, cap the password at 72 bytes, check for an existing login before insert, never log the `DataIntegrityViolationException` message (it holds the login).

## Desired End State

A visitor opens `/register`, enters a login and password, and lands on `/` already signed in; the header now shows the login and a "Wyloguj" button instead of the login form. Signing out returns to `/` with the login form back. A later visit signs in from the header form. A taken login (also in a different letter case) shows "Login jest zajęty." and keeps the typed login; an invalid login or password shows its own message; a wrong password on login shows "Nieprawidłowy login lub hasło."; an unreachable database shows a retry message, never a 500. Passwords are never echoed or logged. Verify with `.\gradlew.bat test` plus the manual steps in each phase.

### Key Discoveries:

- Login rules (decided): login trimmed, 3–50 characters; password 8 characters minimum and at most 72 bytes in UTF-8, not trimmed. Counting bytes, not characters, is what stops BCrypt's silent truncation.
- Duplicate handling (decided): explicit message "Login jest zajęty.". Two layers: a lookup before insert for the normal case, and a catch of the unique-index violation for the race. Neither path logs the exception message.
- Throttling (decided): deferred. Not in this change; recorded in the roadmap in Phase 3.
- Signed-in experience (decided): the header changes state and offers sign-out; there is no `/konto` page.
- Programmatic sign-in does not go through the form-login filter, so the session must be handled by hand: change the session id (session fixation) and save the `SecurityContext` through the same `SecurityContextRepository` the chain uses. Without that, the next request would look signed out.
- `spring-boot-starter-validation` is not in `tech-stack.md`, so validation is plain code in a service, not Bean Validation.

## What We're NOT Doing

- No login throttling, lockout, or CAPTCHA. Logged as a deferred roadmap item; revisit when there is a real traffic or abuse signal.
- No password reset, email, email verification, "remember me", or password strength rules beyond length.
- No roles, no profile fields on `account`, no change to `002-create-account.xml` or any new changeSet.
- No `/konto` page and no other signed-in screen (S-02, S-03).
- No persistent sessions (`spring-session-jdbc` is not in `tech-stack.md`); a stopped Fly Machine still logs everyone out.
- No new library (no Bean Validation, no Thymeleaf Security extras, no rate-limit library). The one exception is `commons-lang3` for `StringUtils.EMPTY` (see the review amendment under Phase 1).
- No logging of logins, passwords, or exception messages from registration.

## Implementation Approach

Three phases, each ending with a green `.\gradlew.bat test`. Phase 1 is the backend: a registration service with the rules, `POST /register`, and automatic sign-in on the Spring Security boundary. Phase 2 is the visible part: form errors, the signed-in header, sign-out, and the login-error message. Phase 3 updates the roadmap and `AGENTS.md`. New classes go in `com.kenez92.plateplan`: the service and its result types in the existing `account` package, the controller changes in `controller`. Formatting follows `config/LiquibaseConfiguration.java` (four spaces, IntelliJ style, `final` parameters). Tests follow `.cursor/rules/testing.mdc`: unit tests for the service, `@WebMvcTest` slices for web behaviour, `ApplicationTest` unchanged.

## Critical Implementation Details

- **Timing & lifecycle** — Registration queries the database only when a request arrives, never at start, so the application still starts with an unreachable database. With the database down, `POST /register` must re-render the form with a retry message (status 200), not a 500.
- **State sequencing** — In `POST /register`, create the account first, and only then change the session id and write the security context. If the insert fails, nothing about the session changes. The security context must be saved with the same `SecurityContextRepository` bean the filter chain uses; a separate instance would leave the user signed out on the next request.
- **Constraint** — Never pass the caught `DataIntegrityViolationException` or `DataAccessException` to a logger or into the response; its message contains the login.

## Phase 1: Registration and automatic sign-in

### Overview

Add the registration service with the decided rules, wire `POST /register` to it, and sign the new account in. After this phase a registration works end to end through the existing form, with errors shown only as a generic re-render (the polished messages come in Phase 2).

**Amended after code review (before the phase commit):** (1) the Account → principal mapping is no longer a public static method on `AccountUserDetailsService`; it lives in its own `@Service`, `AccountPrincipalService.toUserDetails(final Account)`, used by `AccountUserDetailsService` and by the sign-in service. (2) `RegisterController` holds no sign-in logic; session renewal and saving the security context moved to `AccountSignInService.signIn(Account, HttpServletRequest, HttpServletResponse)`, and the controller only routes (redirect for a signed-in visitor, the form for a refused registration, `redirect:/` after sign-in). Read items 2–4 below with that in mind. Slice tests import the two real services (no database dependency) and stub only `RegistrationService`. Added tests: `AccountPrincipalServiceTest`, `AccountSignInServiceTest`. (3) The two `@RequestParam` fields became one form object, `RegistrationForm(username, password)` in `controller`, bound with `@ModelAttribute("registrationForm")`; a field the browser did not send is `null` and the service rejects it. Spring puts that object in the model, so the password is present in the model for the length of the request but is never rendered, and `toString()` is redacted (`RegistrationFormTest`). Phase 2 must refill the login from `registrationForm.username` and never print the form object or its password. (4) The rules of item 1 below live in their own class, `RegistrationValidator` (`@Component`, `Optional<RegistrationError> validate(login, password)`); `RegistrationService.register` trims the login, chains validator → taken-login lookup → save as an `Optional` pipeline, and keeps only the two database exception handlers, so it has no `if` ladder. Added `RegistrationValidatorTest`; `RegistrationServiceTest` now covers the pipeline (created, trimmed, case kept, invalid input never reaches the database, taken, race, unavailable). (5) Style rules from the review: in production code every constant is `private static final` and no string literal is used in a method body (view names, model attribute names, security paths and parameters, exception messages); annotation values stay literal. In test classes there are no `private static` constants (`.cursor/rules/testing.mdc` now says so). (6) An empty string in production code is `StringUtils.EMPTY` (tests keep `""`). That needs `org.apache.commons:commons-lang3`, which was only a transitive dependency; on the user's instruction it is now declared in `build.gradle.kts` (version from Spring Boot's dependency management) and listed in `tech-stack.md`, as `AGENTS.md` requires for a library. This is the one exception to "No new library" below. Progress rows keep their titles.

### Changes Required:

#### 1. Registration service and result

**Files**: `src/main/java/com/kenez92/plateplan/account/RegistrationService.java`, `RegistrationResult.java`, `RegistrationError.java` (new)

**Intent**: One place for the registration rules, so the controller only translates the result into a view or a sign-in. No exceptions cross the service boundary for expected outcomes.

**Contract**:
- `@Service RegistrationService(final AccountRepository, final PasswordEncoder)` with `RegistrationResult register(final String username, final String password)`.
- `RegistrationResult` holds either the created `Account` or one `RegistrationError`; `RegistrationError` is an enum: `LOGIN_INVALID`, `PASSWORD_INVALID`, `LOGIN_TAKEN`, `UNAVAILABLE`.
- Rules in this order: trim the login; `LOGIN_INVALID` if its length is not 3–50; `PASSWORD_INVALID` if the password is shorter than 8 characters or longer than 72 bytes in UTF-8 (password not trimmed); `LOGIN_TAKEN` if `findByUsernameIgnoreCase` returns a row; otherwise save `new Account(trimmedLogin, passwordEncoder.encode(password))`.
- A `DataIntegrityViolationException` on save returns `LOGIN_TAKEN` (the race at the unique index); any other `DataAccessException`, on the lookup or the save, returns `UNAVAILABLE`. Nothing is logged and no exception message is kept.
- The stored login keeps the letter case the user typed (the index and lookup are case-insensitive).

#### 2. Shared principal mapping

**File**: `src/main/java/com/kenez92/plateplan/account/AccountUserDetailsService.java`

**Intent**: Registration and login must create the same kind of principal, otherwise the signed-in state would differ by how the user got in.

**Contract**: extract the existing `Account` → `UserDetails` mapping (stored username, stored hash, empty authorities) into `AccountPrincipalService` (`@Service`, `toUserDetails(final Account)`) and have `AccountUserDetailsService` use it through its constructor, instead of copying it or making a private method public. Keep `loadUserByUsername` behaviour unchanged; the assertions of `AccountUserDetailsServiceTest` stay the same (only its constructor call changes).

#### 3. Security configuration

**File**: `src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java`

**Intent**: Make the security-context storage an explicit bean so the registration controller can save the new session through the very same repository the filter chain reads.

**Contract**: add a `SecurityContextRepository` bean (`HttpSessionSecurityContextRepository`) and set it on the chain with `securityContext(...)`. No other rule changes; `/register` already permits every method, and CSRF stays on.

#### 4. Register controller

**File**: `src/main/java/com/kenez92/plateplan/controller/RegisterController.java`

**Intent**: Handle the form post, show the result, and sign the user in on success.

**Contract**:
- Constructor takes `final RegistrationService` and `final AccountSignInService`.
- `AccountSignInService` (`@Service`, new, in `account`): `signIn(Account, HttpServletRequest, HttpServletResponse)` builds an authenticated token from `AccountPrincipalService`, changes the session id if a session exists (creates one otherwise), sets the context on `SecurityContextHolder`, and saves it through the `SecurityContextRepository` bean. It logs nothing.
- `POST /register` with the `RegistrationForm` (`username`, `password`). On `RegistrationError`, return the `register` view with status 200, the form (for the typed login) and the error code in the model; the password is never rendered or echoed. On success: call `AccountSignInService.signIn` and redirect to `/`. The controller contains no sign-in logic.
- A signed-in visitor sending `GET` or `POST /register` is redirected to `/` and no account is created.

#### 5. Tests

**Files**: `src/test/java/com/kenez92/plateplan/account/RegistrationServiceTest.java`, `AccountPrincipalServiceTest.java`, `AccountSignInServiceTest.java` (new), `src/test/java/com/kenez92/plateplan/controller/RegisterControllerTest.java` (extend)

**Intent**: Prove each rule and the sign-in on the real filter chain, without Spring in the service test.

**Contract**:
- `RegistrationServiceTest`: unit test, `@ExtendWith(MockitoExtension.class)`, mocked `AccountRepository` and `PasswordEncoder`; results compared whole with `usingRecursiveComparison()`. Methods: `shouldCreateTheAccountWithAHashedPasswordAndTheTrimmedLogin`, `shouldKeepTheLetterCaseTheUserTyped`, `shouldRejectALoginShorterThanThreeCharacters`, `shouldRejectALoginLongerThanFiftyCharacters`, `shouldRejectAPasswordShorterThanEightCharacters`, `shouldRejectAPasswordLongerThan72Bytes` (for example 40 characters of `ą`, which is 80 bytes), `shouldAcceptAPasswordOf72BytesExactly`, `shouldRejectATakenLoginIgnoringCase`, `shouldReportTheLoginAsTakenWhenTheInsertHitsTheUniqueIndex`, `shouldReportUnavailableWhenTheDatabaseFails`.
- `RegisterControllerTest` additions (`@WebMvcTest(RegisterController.class)` with `SecurityConfiguration` imported and a nested `@TestConfiguration` stub for `RegistrationService`, like `AccountLookupStub`): `shouldSignTheNewAccountInAndRedirectHome` (POST with CSRF; 302 to `/`; `authenticated().withUsername(...)`), `shouldKeepTheSessionSignedInOnTheNextRequest` (reuse the session from the POST in a `GET /register` that must redirect to `/`), `shouldShowTheFormAgainWhenTheRegistrationFails`, `shouldNotEchoThePasswordBack`, `shouldRejectARegistrationPostWithoutACsrfToken`, `shouldSendASignedInVisitorAwayFromTheRegistrationForm`.

### Success Criteria:

#### Automated Verification:

- Suite passes with the service, controller, and tests: `.\gradlew.bat test`
- Full context still loads with an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest`
- No logging of registration data: `rg -n "log(ger)?\.|System\.out" src/main/java/com/kenez92/plateplan/account src/main/java/com/kenez92/plateplan/controller/RegisterController.java` returns no match

#### Manual Verification:

- Against the Supabase database (or a local run with the real secrets), submitting `/register` with a new login creates a row in `account` with a `$2a$` hash and redirects to `/`; opening `/register` afterwards redirects to `/` (the session is signed in). Delete the test account afterwards.
- Registering the same login in a different letter case does not create a second row.
- With the database unreachable, submitting the form returns the form again, not an error page.

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 2: Visible errors, signed-in header, sign-out

### Overview

Show the registration and login errors, change the header for a signed-in visitor, and configure sign-out.

**Pulled forward during Phase 1 manual testing:** `CurrentAccountAdvice`, the signed-in header (login plus "Wyloguj"), the `logout` configuration with `logoutSuccessUrl("/")`, their tests (`CurrentAccountAdviceTest`, two cases in `HomeControllerTest`, two in `SecurityConfigurationTest`), and the `/?error` login message (committed with Phase 1, `01c5082`). Finished in Phase 2: the registration messages with the contract wording, refilling the login (escaped), `maxlength`/`minlength`, and the all-codes message, typed-login, and escaping tests. The lead text on `/register` is unchanged on purpose: a signed-in visitor is redirected away from that page, so "logujesz się w pasku u góry strony" is always true where it is shown.

### Changes Required:

#### 1. Current account in the model

**File**: `src/main/java/com/kenez92/plateplan/controller/CurrentAccountAdvice.java` (new)

**Intent**: Every page shares the header, and the Thymeleaf Security extras are not a declared dependency, so the signed-in login reaches the view through the model.

**Contract**: a `@ControllerAdvice` with a `@ModelAttribute` method that exposes the signed-in login (the `Principal` name) as a model attribute, or nothing for an anonymous visitor. It is picked up automatically by `@WebMvcTest` slices. It reads nothing from the database.

#### 2. Header

**File**: `src/main/resources/templates/fragments/chrome.html`

**Intent**: A signed-out visitor sees the login form and the Register link; a signed-in visitor sees their login and a sign-out form instead. A failed login shows a message.

**Contract**: render the login form block only when no account is signed in. Otherwise show the login as escaped text (`th:text`) and a `POST` form with `th:action="@{/logout}"` and a "Wyloguj" button (the CSRF token comes from the Thymeleaf action). When the request has the `error` parameter, show "Nieprawidłowy login lub hasło." with `role="alert"` in the login bar. Follow the existing Polish copy and the classes in `static/css/site.css`; add only the styles the new elements need.

#### 3. Registration form

**File**: `src/main/resources/templates/register.html`

**Intent**: Tell the user what went wrong and let them correct it without retyping the login.

**Contract**: add an error area (`role="alert"`) that shows the message for the error code: `LOGIN_INVALID` → "Login musi mieć od 3 do 50 znaków."; `PASSWORD_INVALID` → "Hasło musi mieć co najmniej 8 znaków i nie więcej niż 72 bajty."; `LOGIN_TAKEN` → "Login jest zajęty."; `UNAVAILABLE` → "Nie udało się założyć konta. Spróbuj ponownie za chwilę.". Re-fill the login field from `registrationForm.username` in the model, never the password. Add `maxlength="50"` to the login inputs (register form and header form) and `minlength="8"` to the register password input; the server rules stay authoritative. Update the lead text so it no longer says "logujesz się w pasku u góry" when that is not true for a signed-in visitor.

#### 4. Sign-out

**File**: `src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java`

**Intent**: Sign-out must return to the login window that this application uses, not to Spring's default `/login?logout`.

**Contract**: configure logout with the default `POST /logout` (CSRF required) and `logoutSuccessUrl("/")`; the session is invalidated. No other change.

#### 5. Tests

**Files**: `src/test/java/com/kenez92/plateplan/controller/HomeControllerTest.java`, `RegisterControllerTest.java`, `src/test/java/com/kenez92/plateplan/config/SecurityConfigurationTest.java`

**Intent**: Lock in the visible behaviour of each state.

**Contract**: string assertions on the HTML, whole-object comparison where the result is an object. Methods: `shouldShowTheLoginFormToASignedOutVisitor`, `shouldShowTheLoginAndTheSignOutButtonToASignedInVisitor` (`@WithMockUser`; no login form in the page), `shouldEscapeTheLoginInTheHeader` (a login such as `<b>x</b>` appears escaped), `shouldShowTheLoginErrorMessageAfterAFailedLogin` (`GET /?error`), `shouldShowTheMessageForEachRegistrationError` (all four codes), `shouldKeepTheTypedLoginInTheRegistrationForm`, and in `SecurityConfigurationTest` `shouldSignOutAndReturnToTheLoginWindow` (`POST /logout` with CSRF → 302 to `/`, unauthenticated) and `shouldRejectALogoutPostWithoutACsrfToken`.

### Success Criteria:

#### Automated Verification:

- Suite passes with the new view and logout tests: `.\gradlew.bat test`
- Full context still loads with an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest`

#### Manual Verification:

- On a local run, registering a new login lands on `/` with the login and "Wyloguj" in the header and no login form; "Wyloguj" returns to `/` with the login form.
- Signing in again from the header form with the same login (also in a different letter case) shows the signed-in header; a wrong password shows "Nieprawidłowy login lub hasło.".
- The taken-login, short-login, and short-password messages appear on `/register`, and the typed login stays in the field. Works at a narrow window width (the header still fits).
- Delete the test accounts in Supabase afterwards.

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 3: Documents

### Overview

Record the decisions where the project keeps them, so the next slice and the next agent do not re-ask.

### Changes Required:

#### 1. Roadmap

**File**: `context/foundation/roadmap.md`

**Intent**: Close the S-01 unknowns that this change settled and keep the one that was deferred visible.

**Contract**: rewrite the S-01 `Unknowns` to state: login capped at 50 and password at 72 bytes (done), existing login checked before insert and the exception message not logged (done), login throttling or lockout deferred with the reason (no rate-limit library in `tech-stack.md`, one-person MVP, BCrypt cost already slows guessing). Edit in place; no change to other items.

#### 2. AGENTS.md

**File**: `AGENTS.md`

**Intent**: The "Account flow" section states what now exists.

**Contract**: extend "Account flow" in one or two sentences: registration validates the login (3–50, trimmed) and password (8 characters, at most 72 bytes), reports a taken login explicitly, signs the new account in by saving the security context and changing the session id, and sign-out is `POST /logout` back to `/`. State that login attempts are not throttled. Do not add anything else.

### Success Criteria:

#### Automated Verification:

- Suite passes: `.\gradlew.bat test`
- The deferred throttling is written down: `rg -n "throttl" context/foundation/roadmap.md AGENTS.md` returns a match in both files

#### Manual Verification:

- Reading the S-01 section of the roadmap and "Account flow" in `AGENTS.md` gives the same rules as the code and no open question about length caps or duplicate logins.

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Testing Strategy

### Unit Tests:

- `RegistrationServiceTest`: every rule and boundary (login 2/3/50/51, password 7/8 characters, 72/73 bytes with a multi-byte character, taken ignoring case, unique-index race, database failure).

### Integration Tests:

- `@WebMvcTest` slices with the real `SecurityConfiguration`: registration signs in and the session survives the next request, CSRF is required for register and logout, the header differs by state, the login is escaped, all error messages render. No `@SpringBootTest` outside `ApplicationTest` (`.cursor/rules/testing.mdc`).

### Manual Testing Steps:

1. Register a new login on a local run against Supabase; confirm the redirect to `/`, the signed-in header, and the row in `account`.
2. Sign out, sign in again (also with different letter case), then try a wrong password.
3. Try a taken login, a 2-character login, a 7-character password, and a very long password.
4. Stop the database (or use a dummy URL) and submit the form; confirm the retry message.
5. Delete the test accounts.

## Performance Considerations

One lookup and one insert per registration, both on the unique index; BCrypt hashing is the intended cost. Without throttling, repeated registrations or logins cost one hash each; accepted for a one-person MVP and written down.

## Migration Notes

None. No schema change; `002-create-account.xml` is untouched. Rollback is a revert of the code. Accounts created by this change are ordinary rows and need no cleanup. Sessions are in memory, so a deploy or Machine restart signs everyone out.

## References

- Roadmap item: `context/foundation/roadmap.md:117-129` (S-01)
- Prior review notes carried into this slice: `context/archive/2026-10-01-spring-security-sign-in/reviews/impl-review.md` (F7)
- Boundary and password encoder: `src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java`
- Principal mapping: `src/main/java/com/kenez92/plateplan/account/AccountUserDetailsService.java:31-36`
- Test pattern with a stub: `src/test/java/com/kenez92/plateplan/config/SecurityConfigurationTest.java:92-113`
- Tech stack and library rule: `context/foundation/tech-stack.md`, `AGENTS.md`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Registration and automatic sign-in

#### Automated

- [x] 1.1 Suite passes with the service, controller, and tests: `.\gradlew.bat test` — 01c5082
- [x] 1.2 Full context still loads with an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest` — 01c5082
- [x] 1.3 No logging of registration data: `rg -n "log(ger)?\.|System\.out" src/main/java/com/kenez92/plateplan/account src/main/java/com/kenez92/plateplan/controller/RegisterController.java` returns no match — 01c5082

#### Manual

- [x] 1.4 Against Supabase, registering a new login creates an `account` row with a `$2a$` hash, redirects to `/`, and `/register` then redirects to `/`; test account deleted afterwards — 01c5082
- [x] 1.5 Registering the same login in a different letter case does not create a second row — 01c5082
- [x] 1.6 With the database unreachable, submitting the form returns the form again, not an error page — 01c5082

### Phase 2: Visible errors, signed-in header, sign-out

#### Automated

- [x] 2.1 Suite passes with the new view and logout tests: `.\gradlew.bat test`
- [x] 2.2 Full context still loads with an unreachable database: `.\gradlew.bat test --tests com.kenez92.plateplan.ApplicationTest`

#### Manual

- [x] 2.3 Registering lands on `/` with the login and "Wyloguj" in the header and no login form; "Wyloguj" returns to `/` with the login form
- [x] 2.4 Signing in again from the header (also in different letter case) shows the signed-in header; a wrong password shows "Nieprawidłowy login lub hasło."
- [x] 2.5 The taken-login, short-login, and short-password messages appear on `/register` with the typed login kept, and the header fits at a narrow window width; test accounts deleted afterwards

### Phase 3: Documents

#### Automated

- [ ] 3.1 Suite passes: `.\gradlew.bat test`
- [ ] 3.2 The deferred throttling is written down: `rg -n "throttl" context/foundation/roadmap.md AGENTS.md` returns a match in both files

#### Manual

- [ ] 3.3 The S-01 section of the roadmap and "Account flow" in `AGENTS.md` match the code and leave no open question about length caps or duplicate logins
