# Critical-path coverage Implementation Plan

## Overview

Fill the two remaining cheap ownership gaps for rollout Phase 1 of `context/foundation/test-plan.md`, then write cookbook §6.2 and §6.4 from the patterns that already exist plus the tests this change adds. The generate JSON matrix and the profile GET/POST `login=bob` test stay in place. No production code.

## Current State Analysis

`POST /plan/generate` maps `PlanResult` to HTTP 200 JSON: two Base64 PDF fields and no `error` on success, or a single `error` of `PROFILE_REQUIRED`, `CALORIES_REQUIRED`, or `UNAVAILABLE` (`PlanController.java:42-66`, `PlanFilesDto.java:21-28`). That matrix is already asserted in `PlanControllerTest` and `PlanFilesDtoTest`. `PlanServiceTest` already covers no row, null calories, database failure, and generator failure.

On the inspected profile and plan handlers the account key is `Principal.getName()`. `ProfileFormDto` and `DailyCaloriesForm` have no login field. `shouldLoadAndSaveTheProfileOfTheSignedInLoginOnly` already sends `login=bob` on GET/POST `/profile` and verifies `alice`. Generate success verifies `generate("alice")` / `never().generate("bob")` but does not send a `login` parameter. `/profile/calories` and `/profile/recalculate` have no `login=bob` mirror.

Research treats "500 instead of JSON" as speculative on this inspected `plan/**` path. Expected failures are values, not exceptions. Playwright on this JSON contract is the anti-pattern the test plan already forbids.

## Desired End State

A signed-in `login=bob` parameter cannot choose the row for generate or for the two calorie writes. `context/foundation/test-plan.md` §6.2 and §6.4 describe how to add the next integration test and the next signed-in POST test. The existing generate error matrix is cited, not rewritten.

Verify by running the two slice classes and reading §6.2 / §6.4: they name Principal-only key, CSRF, and HTTP 200 on expected failure, and they still say TBD for §6.1 / §6.3 / §6.5.

### Key Discoveries:

- Generate HTTP matrix already lives in `PlanControllerTest.java:88-171`; adding "coverage to the plan package" without a named gap would raise coverage, not catch a new regression (`research.md` Hot-spot correction).
- Cheapest remaining ownership signal is `login=bob` on generate and on the two calorie POSTs (`research.md` Cheapest useful layer).
- Compact `PlanFilesDto` constructor already nulls both PDF fields when `error != null` (`PlanFilesDto.java:21-28`); extra `shoppingListPdf` assertions on the four error tests are optional consistency, not a new behavior.
- Slice tests import real `SecurityConfiguration` and stub application services. Class-level `@WithMockUser(username = "alice")` is already on both controllers; it is not ownership proof by itself.

## What We're NOT Doing

- Inventing a test whose oracle is HTTP 500 on `plan/**`.
- Re-listing `PROFILE_REQUIRED` / `CALORIES_REQUIRED` / `UNAVAILABLE` as new tests.
- Playwright, `@SpringBootTest`, or a live Ollama call for this JSON / ownership contract.
- `shoppingListPdf` tidy-up on the four error tests that only check `dietPdf`.
- Signed-out generate **without** CSRF (403 vs 302); either outcome withholds PDFs.
- New `PlanServiceTest` or `PlanFilesDtoTest` methods.
- Download file names, `fetch` CSRF header, or refresh-drops-files (Phase 4).
- Prompt snapshots, persist-and-leak (risk #5), calorie-formula oracles (risk #3).
- Production, schema, or security-matcher changes.
- File:line anchors in test-plan §1–§2.

## Implementation Approach

Gap-fill first, cookbook last. Each test phase adds the cheapest `@WebMvcTest` method that proves the request cannot choose the login, mirroring `shouldLoadAndSaveTheProfileOfTheSignedInLoginOnly`. Phase 3 writes cookbook recipes that point at those methods and at the existing generate matrix, with no test code blocks in `test-plan.md`.

## Critical Implementation Details

Phase 3 must run after Phases 1 and 2 so §6 reference tests are the methods this change adds, not a promise of future names.

Class-level `@WithMockUser(username = "alice")` is already on both slice classes. A new method that never sends `login=bob` does not close risk #2. The existing generate success test already verifies `never().generate("bob")` without sending a `login` parameter — leave it; add a sibling that sends the parameter.

Stub `generate("alice")` with any `PlanResult` factory already used in `PlanControllerTest` so the request completes. Assert the service login argument, not the JSON error matrix.

## Phase 1: Generate own-data lock

### Overview

Prove `POST /plan/generate` cannot choose whose row `PlanService` reads.

- **Behavior asserted**: A signed-in generate with `login=bob` still calls `planService.generate("alice")` and never `generate("bob")`.
- **Regression caught**: A later handler that binds login from query or form would generate from another account.
- **Research source**: `research.md` Risk #2 cheapest remaining signal; `PlanController.java:44-45`; `PlanControllerTest.java:103-104` (argument check without a `login` param); `ProfileControllerTest.java:158-170` (the pattern to mirror).
- **Edge / error / boundary**: Extra `login` on a JSON POST whose handler signature has no login parameter. The request still completes for the signed-in principal.
- **Anti-pattern avoided**: Happy-path-only `@WithMockUser`; Playwright on the JSON contract; re-listing the three error codes.

### Changes Required:

#### 1. Generate ownership test

**File**: `src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java`

**Intent**: Lock the current Principal-only generate signature so a client-supplied login cannot become the service argument.

**Contract**: One new `should*` method. `POST /plan/generate` with CSRF, `Accept: application/json`, and `.param("login", "bob")`. Stub `generate("alice")` with an existing `PlanResult` factory from this class. `verify(planService).generate("alice")` and `verify(planService, never()).generate(eq("bob"))`. Do not assert the three `error` codes. Do not change production code.

### Success Criteria:

#### Automated Verification:

- `PlanControllerTest` includes a `should*` method that POSTs `/plan/generate` with `login=bob` and verifies `generate("alice")` / never `generate("bob")`
- Targeted suite passes: `.\gradlew.bat test --tests com.kenez92.plateplan.plan.controller.PlanControllerTest`

#### Manual Verification:

- The new method asserts the service login argument and does not re-list the three `error` codes

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 2: Calorie-write own-data lock

### Overview

Prove the two calorie writes that lack a `login=bob` mirror cannot choose whose row to update.

- **Behavior asserted**: `POST /profile/calories` and `POST /profile/recalculate` with `login=bob` still call `ConfirmedCaloriesService` with `"alice"`.
- **Regression caught**: A later login field on `DailyCaloriesForm` or a request parameter on either calorie write would let a signed-in account edit another row.
- **Research source**: `research.md` Risk #2 remaining gap; `ProfileController.java:77` and `:85`; `DailyCaloriesForm` is `dailyCalories` only; `ProfileControllerTest.java:158-170` covers GET/POST `/profile` only.
- **Edge / error / boundary**: Extra `login` on `/profile/calories` (body is the calorie number) and on `/profile/recalculate` (no calorie number). Both still write `alice`.
- **Anti-pattern avoided**: Happy-path-only `@WithMockUser` on the calorie POSTs.

### Changes Required:

#### 1. Calorie-write ownership test

**File**: `src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java`

**Intent**: Extend the existing profile ownership pattern to the two inspected calorie writes that do not have it.

**Contract**: One new `should*` method, same shape as `shouldLoadAndSaveTheProfileOfTheSignedInLoginOnly`. Stub `update("alice", 2000)` and `recalculate("alice")` with the saved results already used in this class. `POST /profile/calories` with CSRF, `dailyCalories=2000`, and `login=bob`. `POST /profile/recalculate` with CSRF and `login=bob`. Verify `update("alice", 2000)` and `recalculate("alice")`; never `update` / `recalculate` with `"bob"`. Do not add a login field to `DailyCaloriesForm`. Do not change production code.

### Success Criteria:

#### Automated Verification:

- `ProfileControllerTest` includes a `should*` method that POSTs `/profile/calories` and `/profile/recalculate` with `login=bob` and verifies `"alice"`
- Targeted suite passes: `.\gradlew.bat test --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest`

#### Manual Verification:

- Both calorie writes in the new method send `login=bob` and do not only rely on class-level `@WithMockUser`

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 3: Cookbook §6.2 / §6.4

### Overview

Record the shipped integration and signed-in POST patterns so the next test follows cost × signal instead of greenfield coverage.

- **Behavior asserted**: A reader adding a new signed-in POST is told to prove Principal-only key, CSRF, and HTTP 200 on expected failure, and to reuse the generate matrix rather than re-list it.
- **Regression caught**: A later agent that treats `@WithMockUser` as ownership, moves the JSON matrix to Playwright, or invents a 500 test.
- **Research source**: `test-plan.md` §6 placeholders; `research.md` Response-guidance verdict and Cheapest useful layer; schema forbids test code blocks in the cookbook.
- **Edge / error / boundary**: Cookbook names the existing matrix and the new `login=bob` methods as reference tests. §6.1 / §6.3 / §6.5 stay TBD.
- **Anti-pattern avoided**: File:line anchors in §1–§2; rewriting §3 Status here; dumping test source into `test-plan.md`.

### Changes Required:

#### 1. Integration-test recipe

**File**: `context/foundation/test-plan.md` (§6.2)

**Intent**: Replace the Phase 1 TBD with the generate-matrix and own-data denial recipe this rollout already uses.

**Contract**: Fill §6.2 with Location / Naming / Mocking policy / Reference test / Run locally. Location is the same package as the controller under `src/test/java`. Naming is `*Test` with `should*` methods. Mocking policy: `@WebMvcTest` + `@Import(SecurityConfiguration.class)` + stub application services; no `@SpringBootTest`, no live Ollama. Reference tests: `PlanControllerTest` for the generate JSON matrix (do not re-list the three codes); the Phase 1 generate `login=bob` method and `shouldLoadAndSaveTheProfileOfTheSignedInLoginOnly` for own-data. Run locally: `.\gradlew.bat test --tests <Fqcn>`. No test code blocks. No file:line in §1–§2.

#### 2. Signed-in POST recipe

**File**: `context/foundation/test-plan.md` (§6.4)

**Intent**: Replace the Phase 1 TBD with the CSRF + Principal-only + no-500-on-expected-failure recipe.

**Contract**: A new signed-in POST test must: import the security chain; send CSRF (without token → 403, service never called); send `login=bob` and verify the service received `Principal.getName()`; treat expected product failures as HTTP 200 result values, not 500. Point at `PlanControllerTest` (CSRF-less generate, existing matrix) and the Phase 2 calorie `login=bob` method. Anonymous generate-with-CSRF stays in `SecurityConfigurationTest` (302 to `/`). Do not invent a 500 oracle. No test code blocks.

#### 3. Phase note

**File**: `context/foundation/test-plan.md` (§6.6 and header date)

**Intent**: Leave a short Phase 1 note so later rollouts do not redo this gap-fill.

**Contract**: Two to three lines under §6.6: Phase 1 shipped generate and calorie-write `login=bob` mirrors; existing generate matrix left in place; no 500 test, no Playwright, no `shoppingListPdf` tidy-up. Bump the file header `Last updated:` to the implement date. Leave §6.1 / §6.3 / §6.5 as TBD. Do not change §1–§5 strategy text, §2 risk rows, or §3 Status.

### Success Criteria:

#### Automated Verification:

- `context/foundation/test-plan.md` §6.2 and §6.4 no longer say TBD
- §6.1, §6.3, and §6.5 remain TBD
- §6.6 records that Phase 1 shipped the own-data mirrors and left the generate matrix in place
- §1–§5 strategy text and the §2 risk table have no new file:line anchors

#### Manual Verification:

- Reading §6.2 and §6.4 as if adding a new signed-in POST names Principal-only key, CSRF, and HTTP 200 on expected failure

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Testing Strategy

### Unit Tests:

- None added. `PlanServiceTest` already covers the three `PlanResult` factories. `PlanFilesDtoTest` already covers exclusive PDF vs `error` JSON.

### Integration Tests:

- Phase 1: `login=bob` on `POST /plan/generate` → service argument is the signed-in login.
- Phase 2: `login=bob` on `POST /profile/calories` and `POST /profile/recalculate` → same key.
- Existing `PlanControllerTest` matrix and `SecurityConfigurationTest` anonymous/CSRF cases stay as the risk #1 proof.

### Manual Testing Steps:

1. Confirm each new method sends `login=bob` and verifies the service argument.
2. Confirm no new method re-lists the three generate `error` codes or expects HTTP 500.
3. Confirm cookbook §6.2 / §6.4 name those rules without pasting test source.

## Performance Considerations

None. Tests and cookbook only.

## Migration Notes

None. No schema or production code.

## References

- Related research: `context/changes/testing-critical-path-coverage/research.md`
- Test plan: `context/foundation/test-plan.md` §2 risks #1–#2, §3 Phase 1, §6.2 / §6.4 placeholders
- Ownership pattern: `src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java:158-170`
- Generate matrix: `src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java:88-171`
- Archive contract (HTTP 200, Principal-only, not 500/503): `context/archive/2026-10-05-generate-diet-with-ollama/plan.md:189`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Generate own-data lock

#### Automated

- [x] 1.1 `PlanControllerTest` includes a `should*` method that POSTs `/plan/generate` with `login=bob` and verifies `generate("alice")` / never `generate("bob")` — 16621d7
- [x] 1.2 Targeted suite passes: `.\gradlew.bat test --tests com.kenez92.plateplan.plan.controller.PlanControllerTest` — 16621d7

#### Manual

- [x] 1.3 The new method asserts the service login argument and does not re-list the three `error` codes — 16621d7

### Phase 2: Calorie-write own-data lock

#### Automated

- [x] 2.1 `ProfileControllerTest` includes a `should*` method that POSTs `/profile/calories` and `/profile/recalculate` with `login=bob` and verifies `"alice"` — d3b72fa
- [x] 2.2 Targeted suite passes: `.\gradlew.bat test --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest` — d3b72fa

#### Manual

- [x] 2.3 Both calorie writes in the new method send `login=bob` and do not only rely on class-level `@WithMockUser` — d3b72fa

### Phase 3: Cookbook §6.2 / §6.4

#### Automated

- [x] 3.1 `context/foundation/test-plan.md` §6.2 and §6.4 no longer say TBD
- [x] 3.2 §6.1, §6.3, and §6.5 remain TBD
- [x] 3.3 §6.6 records that Phase 1 shipped the own-data mirrors and left the generate matrix in place
- [x] 3.4 §1–§5 strategy text and the §2 risk table have no new file:line anchors

#### Manual

- [x] 3.5 Reading §6.2 and §6.4 as if adding a new signed-in POST names Principal-only key, CSRF, and HTTP 200 on expected failure
