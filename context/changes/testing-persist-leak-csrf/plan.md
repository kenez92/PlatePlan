# Persist, leak and CSRF Implementation Plan

## Overview

Fill the cheapest remaining tests for rollout Phase 3 of `context/foundation/test-plan.md`: generate does not write the profile, failure logs omit secrets, recalculate POST without CSRF is 403. Then note Phase 3 in cookbook §6.6. No production code. Generate `fetch` CSRF stays Phase 4.

## Current State Analysis

`DietGeneratorTest` already omits personal data from the prompt. `PlanFilesDtoTest` already omits PDF bytes from `toString`. CSRF-less 403 exists on every form POST except `/profile/recalculate`. `PlanServiceTest` success does not `never().save`. Plan package has no ListAppender leak test.

## Desired End State

Generate cannot persist a profile row. A planted exception message with key / calories / product name does not appear in the DietGenerator warn log. Recalculate without CSRF is 403 and never calls the service. §6.6 records those locks.

### Key Discoveries:

- Recalculate CSRF was the only missing form lock (`research.md` table).
- Prompt snapshot is an anti-pattern; keep targeted `contains` / `doesNotContain`.

## What We're NOT Doing

- Playwright / generate `fetch` CSRF (Phase 4).
- `@SpringBootTest` disk scans.
- Full prompt golden files.
- Treating `SESSION_COOKIE_SECURE=true` as a bug.
- Production or schema changes.
- Filling cookbook §6.3.

## Implementation Approach

Gap-fill existing test classes. Cookbook last.

## Phase 1: Persist and leak locks

### Overview

- **Behavior asserted**: Generate is a profile read; DietGenerator failure logs class names only; prompt omits activity.
- **Regression caught**: `generate` calling `save`/`replace*`; `LOGGER.warn(exception.getMessage())`; activity leaking into the prompt.
- **Research source**: `research.md` Risk #5 cheapest remaining.
- **Edge / error / boundary**: Exception message planted with `ollama-key`, `2000`, `jajka`.
- **Anti-pattern avoided**: Full prompt snapshot; logging the prompt in the test.

### Changes Required:

#### 1. Generate does not write the profile

**File**: `src/test/java/com/kenez92/plateplan/plan/service/PlanServiceTest.java`

**Intent**: Challenge "no plan table means nothing is stored" by proving this inspected path does not write `user_profile`.

**Contract**: On `shouldReturnTheDietPlanWhenTheProfileAndCaloriesArePresent`, `verify(repository, never()).save(any())`, `never().replaceConfirmedCalories(any(), anyInt())`, `never().replaceBodyAndProducts(...)`.

#### 2. DietGenerator logs class names only

**File**: `src/test/java/com/kenez92/plateplan/plan/service/DietGeneratorTest.java`

**Intent**: Prove a model failure does not log the key, calories, or product names.

**Contract**: New `shouldLogOnlyTheClassNamesWhenTheModelCallFails`. Throw with message containing `ollama-key`, `2000`, `jajka`. Assert one log line contains the exception class name and does not contain those three strings. Mirror `ProfileServiceTest.shouldLogOnlyTheClassNamesWhenTheDatabaseFails`. Do not log or assert the full prompt.

#### 3. Prompt omits activity

**File**: `src/test/java/com/kenez92/plateplan/plan/service/DietGeneratorTest.java`

**Intent**: Close the remaining personal-data omit from research.

**Contract**: Add `"MODERATE"` to `doesNotContain` on `shouldIncludeRoleGoalCaloriesAndProductNamesAndOmitPersonalDataFromThePrompt`.

### Success Criteria:

#### Automated Verification:

- Generate success verifies `never().save` and `never().replace*`
- DietGenerator failure log omits planted key / calories / product
- Prompt `doesNotContain("MODERATE")`
- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.PlanServiceTest --tests com.kenez92.plateplan.plan.service.DietGeneratorTest`

#### Manual Verification:

- The new log test plants strings in the exception message, not by printing the prompt

## Phase 2: Recalculate CSRF lock

### Overview

- **Behavior asserted**: `POST /profile/recalculate` without CSRF is 403 and never recalculates.
- **Regression caught**: Skipping CSRF on the recalculate mapping.
- **Research source**: `research.md` Risk #6 cheapest remaining.
- **Edge / error / boundary**: Signed-in user, no token.
- **Anti-pattern avoided**: Playwright for form CSRF; Secure-cookie as a CSRF bug.

### Changes Required:

#### 1. Recalculate without token

**File**: `src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java`

**Intent**: Close the only missing form CSRF-less 403.

**Contract**: New `shouldRejectARecalculatePostWithoutACsrfToken`: `post("/profile/recalculate")` without `.with(csrf())`, `status().isForbidden()`, `verify(confirmedCaloriesService, never()).recalculate(any())`. Mirror `shouldRejectACaloriePostWithoutACsrfToken`.

### Success Criteria:

#### Automated Verification:

- New `should*` exists and the targeted suite passes: `.\gradlew.bat test --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest`

#### Manual Verification:

- The method does not use Playwright and does not change `SESSION_COOKIE_SECURE`

## Phase 3: Cookbook §6.6

### Overview

Record Phase 3 gap-fill. Leave §6.3 TBD.

### Changes Required:

#### 1. Phase note

**File**: `context/foundation/test-plan.md` (§6.6)

**Intent**: Name the locks for the next agent.

**Contract**: Two to three lines: generate `never().save`/`never().replace*`; DietGenerator class-name logs; recalculate CSRF-less 403. Do not change §1–§5. Leave §6.3 TBD.

### Success Criteria:

#### Automated Verification:

- §6.6 records Phase 3
- §6.3 remains TBD
- §2 risk table has no new file:line anchors

#### Manual Verification:

- Reading §6.6 names the recalculate CSRF lock and the generate no-write verify

## Testing Strategy

### Unit Tests:

- Generate no-write; DietGenerator log capture; prompt omit activity.

### Integration Tests:

- Recalculate CSRF-less 403.

### Manual Testing Steps:

1. Confirm the log test does not print the prompt.
2. Confirm recalculate CSRF is MockMvc, not Playwright.

## Performance Considerations

None.

## Migration Notes

None.

## References

- `context/changes/testing-persist-leak-csrf/research.md`
- `context/foundation/test-plan.md` risks #5 and #6
- `context/archive/2026-10-07-testing-integration-hotspots/` (deferred recalculate CSRF)

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles.

### Phase 1: Persist and leak locks

#### Automated

- [x] 1.1 Generate success verifies `never().save` and `never().replace*`
- [x] 1.2 DietGenerator failure log omits planted key / calories / product
- [x] 1.3 Prompt `doesNotContain("MODERATE")`
- [x] 1.4 Targeted suites pass: `.\gradlew.bat test --tests com.kenez92.plateplan.plan.service.PlanServiceTest --tests com.kenez92.plateplan.plan.service.DietGeneratorTest`

#### Manual

- [x] 1.5 The new log test plants strings in the exception message, not by printing the prompt

### Phase 2: Recalculate CSRF lock

#### Automated

- [x] 2.1 New `should*` exists and the targeted suite passes: `.\gradlew.bat test --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest`

#### Manual

- [x] 2.2 The method does not use Playwright and does not change `SESSION_COOKIE_SECURE`

### Phase 3: Cookbook §6.6

#### Automated

- [x] 3.1 §6.6 records Phase 3
- [x] 3.2 §6.3 remains TBD
- [x] 3.3 §2 risk table has no new file:line anchors

#### Manual

- [x] 3.4 Reading §6.6 names the recalculate CSRF lock and the generate no-write verify
