<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Save User Profile

- **Plan**: context/changes/save-user-profile/plan.md
- **Scope**: Full plan
- **Reviewed phases**: 1, 2, 3
- **Date**: 2026-10-05
- **Verdict**: NEEDS ATTENTION
- **Findings**: 0 critical 3 warnings 3 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | WARNING |
| Scope Discipline | WARNING |
| Safety & Quality | WARNING |
| Architecture | PASS |
| Pattern Consistency | PASS |
| Success Criteria | PASS |

## Findings

### F1 — Product lists redesigned against the plan's "not doing"

- **Severity**: ⚠️ WARNING
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Scope Discipline
- **Location**: src/main/resources/templates/profile.html, src/main/resources/static/js/profile-products.js, src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java
- **Detail**: The plan forbids JavaScript, per-product add/remove, and SecurityConfiguration changes. The shipped screen uses chip lists, Dodaj/Usuń, `profile-products.js`, `List<String>` binding, `/js/**` and `/.well-known/**` as public paths. This was directed during implementation. `AGENTS.md` already describes the new contract (not the plan's semicolon fields).
- **Fix A ⭐ Recommended**: Treat the shipped UI as the source of truth and add a short addendum to the plan so later reviews do not treat this as unfinished work.
  - Strength: Matches what the user asked for and what `AGENTS.md` already states.
  - Tradeoff: The original Phase 2 contract stays historically wrong unless annotated.
  - Confidence: HIGH — the UI and tests already assume add-button lists.
  - Blind spot: Manual 2.6 still describes semicolon input and weight `82,5`.
- **Fix B**: Revert products to two semicolon text fields and drop JS / `/js/**`.
  - Strength: Restores the written plan and "no JavaScript" rule.
  - Tradeoff: Throws away the UX the user just accepted.
  - Confidence: HIGH — mechanical, but a product regression.
  - Blind spot: None significant.
- **Decision**: FIXED via Fix A

### F2 — Live database credentials in the working-tree application.properties

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: src/main/resources/application.properties (unstaged; not in HEAD)
- **Detail**: HEAD still reads `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` from the environment. The local working copy replaces those with a live JDBC URL, username, and password. The file is tracked, so a later `git add` can leak secrets. Phase 2/3 commits correctly left this file out.
- **Fix**: Restore the env-var placeholders in the working copy and keep credentials only in the environment.
- **Decision**: FIXED

### F3 — Weight comma and product repeats no longer match the plan's rules

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Plan Adherence
- **Location**: src/main/java/com/kenez92/plateplan/profile/controller/dto/ProfileFormDto.java, ProductNameValidator.java
- **Detail**: The plan required both `,` and `.` as the weight decimal mark, names of 2–50 code points, and a refused in-list repeat. The code binds `BigDecimal` (comma is `typeMismatch`), names are 2–100, and repeats are dropped (first spelling, case-insensitive). Manual Progress 2.6 still says save weight `"82,5"` and preferred `"mleko 3,2%; jajka; ser"`. `AGENTS.md` matches the code, not those Progress titles.
- **Fix A ⭐ Recommended**: Keep the current rules (frontend sends `82.5`; Set uniqueness; 100-char names) and leave Progress titles as historical.
  - Strength: Matches the user's "frontend owns input" and uniqueness-via-Set decisions.
  - Tradeoff: The plan/Progress text stays stale for anyone who reads it as a spec.
  - Confidence: HIGH — this is what was implemented and confirmed in the browser.
  - Blind spot: A user who types `82,5` now sees a field error, not a save.
- **Fix B**: Restore comma-as-decimal and refuse in-list duplicates as errors.
  - Strength: Matches the original Key Discoveries.
  - Tradeoff: Reintroduces input rewriting the user already rejected.
  - Confidence: MEDIUM — conflicts with later implementation decisions.
  - Blind spot: Whether Polish users will type a comma in a `inputmode=decimal` field.
- **Decision**: FIXED via Fix A

### F4 — ProfileParser / ProfileInput stack never built

- **Severity**: 💡 OBSERVATION
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Plan Adherence
- **Location**: src/main/java/com/kenez92/plateplan/profile/controller/dto/ProfileFormDto.java, ProfileController.java
- **Detail**: Phase 1 called for `ProfileInput`, `ProfileParser`, `ProfileProblem`, and service-side parse-then-save. Those types are gone. The form is `ProfileFormDto` with Bean Validation; `ProductListsValidator` runs in the controller; the service only persists. This was an explicit redesign (empty checks at binding, no custom BV annotations, no converters).
- **Fix**: Keep the current split. Document it only if F1's plan addendum is accepted.
- **Decision**: FIXED

### F5 — Empty product lists are SQL NULL; Progress 1.5 still says all columns not null

- **Severity**: 💡 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Success Criteria
- **Location**: src/main/resources/db/changelog/changes/003-create-user-profile.xml:38-39
- **Detail**: Product columns are nullable; join of an empty list returns null. Progress 1.5 is checked and still describes nine columns all `not null`. The user asked for null to save space. Body columns remain not null.
- **Fix**: Leave the schema as-is. The Progress title is historical.
- **Decision**: FIXED

### F6 — Concurrent first save becomes a generic retry

- **Severity**: 💡 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/profile/service/ProfileService.java:103
- **Detail**: `findById` then `save` are separate transactions. Two first saves can hit the primary key; the service logs class names only and returns `unavailable`. The plan chose last write wins and this retry message. Logging is clean (class names only).
- **Fix**: Accept as specified. A later change can retry the second insert as an update if this shows up in production.
- **Decision**: FIXED
