# Register and Sign In — Plan Brief

> Full plan: `context/changes/register-and-sign-in/plan.md`

## What & Why

A visitor must be able to create the one account, land signed in without a second step, and sign in again on a later visit (roadmap S-01; FR-001, FR-002, US-01). F-02 already built the boundary and the login lookup; this change adds registration, automatic sign-in, and the screen states that make sign-in visible.

## Starting Point

The login bar and the register form exist, and login against the `account` table works. `POST /register` does not exist. The header always shows the login form, even for a signed-in user. There is no logout, and a failed login (`/?error`) shows no message. The F-02 review left a list for this slice: login capped at 50, password at 72 bytes, check for a duplicate before insert, and never log the duplicate-insert message.

## Desired End State

Registering lands on `/` already signed in, with the login and a "Wyloguj" button in the header. Signing out returns to the login form. A taken login (any letter case), a bad login or password, a wrong password, and an unreachable database each show their own message and never a 500. Passwords are never echoed or logged.

## Key Decisions Made

| Decision | Choice | Why (1 sentence) | Source |
| -------- | ------ | ---------------- | ------ |
| Login throttling | Deferred, recorded in the roadmap | No rate-limit library in `tech-stack.md`; one-person MVP; BCrypt already slows guessing. | Planning question |
| Credential rules | Login trimmed, 3–50; password 8 characters, at most 72 bytes UTF-8 | Bytes (not characters) stop BCrypt's silent truncation; the login cap matches `varchar(50)`. | Planning question |
| Signed-in experience | Header shows login + "Wyloguj"; no `/konto` page | Smallest change that makes sign-in visible; S-02 and S-03 bring the real screens. | Planning question |
| Taken login | Explicit "Login jest zajęty." | Best usability; a one-person app gains little from hiding it. | Planning question |
| Duplicate check | Lookup before insert, plus catch of the unique-index violation | The lookup is the normal path; the catch covers the race; neither logs the message. | F-02 review |
| Validation | Plain code in `RegistrationService`, result type instead of exceptions | Bean Validation is not in `tech-stack.md`. | Plan |
| Automatic sign-in | Set the security context, change the session id, save through the chain's own `SecurityContextRepository` | Programmatic login skips the form filter, so session fixation and storage must be handled by hand. | Plan |
| Signed-in login in the view | `@ControllerAdvice` model attribute | Thymeleaf Security extras are not declared and Thymeleaf 3.1 hides `#request`. | Plan |
| Database failure | Form shown again with a retry message (status 200) | Matches the F-02 rule that an unreachable database is not a 500. | Plan |

## Scope

**In scope:** `RegistrationService` and its result types; `POST /register` with automatic sign-in; logout configuration; `CurrentAccountAdvice`; header, register form, and error messages; tests; roadmap S-01 unknowns and `AGENTS.md` "Account flow".

**Out of scope:** throttling or lockout, password reset, email, roles, profile fields, `/konto`, schema changes, persistent sessions, new libraries.

## Architecture / Approach

`RegistrationService` in the `account` package applies the rules and returns an `Account` or an error code. `RegisterController` only routes: it shows the form for a refused registration, or on success calls `AccountSignInService`, which builds the principal through `AccountPrincipalService` (shared with the login lookup), changes the session id, and saves the security context through a `SecurityContextRepository` bean now exposed by `SecurityConfiguration`. A `@ControllerAdvice` gives every page the signed-in login for the shared header.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| ----- | ---------------- | -------- |
| 1. Registration and auto sign-in | Service, `POST /register`, session sign-in, unit and slice tests | User looks signed in on the redirect but signed out on the next request |
| 2. Errors, header, sign-out | Messages, signed-in header, `POST /logout`, view tests | Header layout at narrow width; logout redirect defaults to `/login?logout` |
| 3. Documents | Roadmap S-01 unknowns, `AGENTS.md` "Account flow" | Docs drifting from the code |

**Prerequisites:** F-02 is done and archived. A feature branch and a pull request into `main`.
**Estimated effort:** ~2 sessions across 3 phases.

## Open Risks & Assumptions

- Without throttling, repeated logins or registrations cost one BCrypt hash each. Accepted for the MVP and written down.
- An explicit "login taken" message lets anyone test which logins exist. Accepted by the decision above.
- Sessions are in memory, so a deploy or Machine restart signs everyone out.
- The automatic sign-in keeps the existing CSRF token; Spring Security rotates it only on the form-login path. Low risk here, but worth a glance in review.
- Manual checks need the Supabase database and create test accounts that must be deleted afterwards.

## Success Criteria (Summary)

- Registering creates the account, redirects to `/`, and the next request is still signed in.
- Sign-in, sign-out, and every error state show their own message; no 500 for a taken login or an unreachable database.
- `.\gradlew.bat test` passes, including `ApplicationTest` against an unreachable database.
