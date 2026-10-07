# Calorie invariants and refused-save Implementation Plan

## Overview

Fill the cheapest remaining tests for rollout Phase 2 of `context/foundation/test-plan.md`: preferences do not change calories, calorie writes do not touch body columns, body save does not call calorie services, refused product/cross-list POSTs never call save. Then write cookbook §6.1 and §6.5. No production code.

## Current State Analysis

`CalorieServiceTest` already locks PRD arithmetic for the male-moderate fixture (2767 / 2517 / 3017). `ProfileServiceTest` already stores 2767 on first save and keeps a client 2000 on later body save. `ConfirmedCaloriesServiceTest` already proves `update`/`recalculate` never call `save`. `ProductListsValidatorTest` already reports cross-list on `excludedProducts`. MVC `never().save` exists for age refusal only.

## Desired End State

Same body + different product lists still persist 2767. Calorie update/recalculate never call `replaceBodyAndProducts`. Successful `POST /profile` never calls `ConfirmedCaloriesService`. Illegal name and cross-list POSTs never call `profileService.save`. Cookbook §6.1 / §6.5 name those patterns.

### Key Discoveries:

- Gold 2767 is PRD: BMR = 10×82.5 + 6.25×180 − 5×34 + 5 = 1785; ×1.55 = 2766.75 HALF_UP.
- `CalorieService` has no product parameters (`CalorieService.java:41-48`).
- Anti-pattern: a new unit test that only re-invokes `CalorieService` and asserts 2767.

## What We're NOT Doing

- New `CalorieServiceTest` methods that copy the calculator.
- `@SpringBootTest` / real DB persist-on-refuse.
- Recalculate CSRF (Phase 3 / risk #6).
- Expanding `ProfileFormDtoValidationTest` as the risk #4 proof.
- Playwright, production, schema.

## Implementation Approach

Gap-fill existing test classes. Cookbook last so it can name the new methods.

## Phase 1: Calorie write-path locks

### Overview

- **Behavior asserted**: Preferences do not change stored formula calories; calorie writes do not replace body/products; body POST does not call calorie services.
- **Regression caught**: Wiring products into `formulaCalories`; `update`/`recalculate` calling `replaceBodyAndProducts`; `POST /profile` writing calories.
- **Research source**: `research.md` Risk #3 cheapest remaining.
- **Edge / error / boundary**: Same body, different lists; null vs populated products already exist as two tests — this phase makes equality explicit.
- **Anti-pattern avoided**: Oracle copied from `CalorieService` without PRD inputs; treating `CalorieServiceTest` as the whole path.

### Changes Required:

#### 1. Preferences isolation

**File**: `src/test/java/com/kenez92/plateplan/profile/service/ProfileServiceTest.java`

**Intent**: Prove first save stores 2767 for the PRD male-moderate fixture whether products differ.

**Contract**: One `should*` that saves twice (or two forms) with identical body 34/180/82.5/MALE/MAINTAIN/MODERATE and different product lists. Both persisted rows have `confirmedCalories == 2767`. Do not call `CalorieService.dailyCalories` in the test to obtain 2767.

#### 2. Column isolation on calorie writes

**File**: `src/test/java/com/kenez92/plateplan/profile/service/ConfirmedCaloriesServiceTest.java`

**Intent**: Prove edit and recalculate cannot corrupt body/product columns.

**Contract**: On `shouldReplaceOnlyTheDailyCalories` and `shouldRecalculateOnlyTheDailyCaloriesFromTheStoredBody`, add `verify(repository, never()).replaceBodyAndProducts(...)`.

#### 3. Body save does not use calorie service

**File**: `src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java`

**Intent**: Prove `POST /profile` is not a calorie write.

**Contract**: On `shouldRedirectWithASavedNoticeAfterASave`, `verify(confirmedCaloriesService, never()).update(any(), anyInt())` and `never().recalculate(any())`.

### Success Criteria:

#### Automated Verification:

- `ProfileServiceTest` includes a `should*` that persists 2767 for the same body with different product lists
- `ConfirmedCaloriesServiceTest` verifies `never().replaceBodyAndProducts` on update and recalculate
- `ProfileControllerTest` verifies a successful body save never calls calorie update/recalculate
- Targeted suites pass: `.\gradlew.bat test --tests com.kenez92.plateplan.profile.service.ProfileServiceTest --tests com.kenez92.plateplan.profile.service.ConfirmedCaloriesServiceTest --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest`

#### Manual Verification:

- The 2767 oracle is the PRD fixture arithmetic, not a fresh `CalorieService` call inside the new test

## Phase 2: Refused-save MVC locks

### Overview

- **Behavior asserted**: Illegal product name and cross-list conflict never call `profileService.save`.
- **Regression caught**: Skipping `productListsValidator.validate` or saving despite errors.
- **Research source**: `research.md` Risk #4 cheapest remaining.
- **Edge / error / boundary**: `"a"` illegal name; `ser` / `Ser` conflict on excluded.
- **Anti-pattern avoided**: Annotation-only DTO tests as the proof.

### Changes Required:

#### 1. Illegal name never saves

**File**: `src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java`

**Intent**: Close persist-on-refuse for the existing illegal-name MVC test.

**Contract**: In `shouldShowTheMessageForEachProblemNextToItsField`, add `verify(profileService, never()).save(any(), any())`.

#### 2. Cross-list never saves

**File**: `src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java`

**Intent**: Pin the HTTP slice to the validator conflict rule.

**Contract**: One new `should*` using a valid body plus preferred `ser` and excluded `Ser`. Expect 200, conflict text `Ten produkt jest też na liście preferowanych`, `never().save`.

### Success Criteria:

#### Automated Verification:

- `shouldShowTheMessageForEachProblemNextToItsField` verifies `never().save`
- A new `should*` refuses a cross-list POST with `never().save` and the Polish conflict message
- Targeted suite: `.\gradlew.bat test --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest`

#### Manual Verification:

- The new method uses server validators (imported `ProductListsValidator`), not JS-only rules

## Phase 3: Cookbook §6.1 / §6.5

### Overview

Record unit-formula and refused-save patterns. Leave §6.3 TBD.

### Changes Required:

#### 1. Unit-test recipe

**File**: `context/foundation/test-plan.md` (§6.1)

**Intent**: Replace Phase 2 TBD with the formula-from-PRD pattern.

**Contract**: Location same package under `src/test/java`. Naming `should*`. Oracle from PRD inputs (cite `CalorieServiceTest` as reference; do not copy implementation). Preferences isolation via `ProfileServiceTest` new method. Run `.\gradlew.bat test --tests <Fqcn>`. No test code blocks. Do not change §1–§5 strategy or §3 Status except this rollout row if already `implementing`.

#### 2. Validation recipe

**File**: `context/foundation/test-plan.md` (§6.5)

**Intent**: Replace Phase 2 TBD with refused-save-writes-nothing and cross-list-on-excluded.

**Contract**: MVC `@WebMvcTest` + real validators; `never().save` on refusal; conflict on excluded. Reference the Phase 2 MVC methods. Do not treat `ProfileFormDtoValidationTest` as sufficient.

#### 3. Phase note

**File**: `context/foundation/test-plan.md` (§6.6)

**Intent**: Note Phase 2 shipped.

**Contract**: Two to three lines: preferences isolation, `never().replaceBodyAndProducts`, refused product/cross-list `never().save`. Bump `Last updated:` if needed. Leave §6.3 TBD.

### Success Criteria:

#### Automated Verification:

- §6.1 and §6.5 no longer say TBD
- §6.3 remains TBD
- §6.6 records Phase 2 gap-fill
- §2 risk table has no new file:line anchors

#### Manual Verification:

- Reading §6.1 / §6.5 names PRD oracle and `never().save` on refusal

## Testing Strategy

### Unit Tests:

- Preferences isolation on `ProfileService.save`.
- `never().replaceBodyAndProducts` on calorie writes.

### Integration Tests:

- Body POST never calls calorie service.
- Illegal name and cross-list MVC `never().save`.

### Manual Testing Steps:

1. Confirm 2767 is the PRD fixture, not a calculator call in the new test.
2. Confirm cross-list test hits the server validator.

## Performance Considerations

None.

## Migration Notes

None.

## References

- `context/changes/testing-integration-hotspots/research.md`
- `context/foundation/prd.md` Business Logic
- `context/archive/2026-10-05-calorie-formula/plan.md:182`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles.

### Phase 1: Calorie write-path locks

#### Automated

- [x] 1.1 `ProfileServiceTest` includes a `should*` that persists 2767 for the same body with different product lists
- [x] 1.2 `ConfirmedCaloriesServiceTest` verifies `never().replaceBodyAndProducts` on update and recalculate
- [x] 1.3 `ProfileControllerTest` verifies a successful body save never calls calorie update/recalculate
- [x] 1.4 Targeted suites pass: `.\gradlew.bat test --tests com.kenez92.plateplan.profile.service.ProfileServiceTest --tests com.kenez92.plateplan.profile.service.ConfirmedCaloriesServiceTest --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest`

#### Manual

- [x] 1.5 The 2767 oracle is the PRD fixture arithmetic, not a fresh `CalorieService` call inside the new test

### Phase 2: Refused-save MVC locks

#### Automated

- [x] 2.1 `shouldShowTheMessageForEachProblemNextToItsField` verifies `never().save`
- [x] 2.2 A new `should*` refuses a cross-list POST with `never().save` and the Polish conflict message
- [x] 2.3 Targeted suite: `.\gradlew.bat test --tests com.kenez92.plateplan.profile.controller.ProfileControllerTest`

#### Manual

- [x] 2.4 The new method uses server validators (imported `ProductListsValidator`), not JS-only rules

### Phase 3: Cookbook §6.1 / §6.5

#### Automated

- [x] 3.1 §6.1 and §6.5 no longer say TBD
- [x] 3.2 §6.3 remains TBD
- [x] 3.3 §6.6 records Phase 2 gap-fill
- [x] 3.4 §2 risk table has no new file:line anchors

#### Manual

- [x] 3.5 Reading §6.1 / §6.5 names PRD oracle and `never().save` on refusal
