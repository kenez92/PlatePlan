---
date: 2026-10-07T10:30:00+02:00
researcher: Daniel
git_commit: a19dc8b8a4b294b0c6d9857d5ba7ff61b02507d0
branch: feature/test_plan_controller
repository: PlatePlan
topic: "Ground test-plan Phase 2: calorie write paths and refused-save"
tags: [research, codebase, profile, calories, validation, testing]
status: complete
last_updated: 2026-10-07
last_updated_by: Daniel
last_updated_note: Phase 2 grounding for risks #3 and #4
---

# Research: Ground test-plan Phase 2 (calorie paths and refused-save)

**Date**: 2026-10-07T10:30:00+02:00
**Researcher**: Daniel
**Git Commit**: a19dc8b8a4b294b0c6d9857d5ba7ff61b02507d0
**Branch**: feature/test_plan_controller
**Repository**: PlatePlan

## Research Question

Ground rollout Phase 2 of `context/foundation/test-plan.md` (risks #3 and #4). For each risk: real failure path, existing tests, cheapest remaining layer, speculative gaps.

## Summary

On the inspected write paths, calories have three entries: `ProfileService.save` (first row stores formula; later body save keeps `confirmed_calories` unless null), `ConfirmedCaloriesService.update` (only `replaceConfirmedCalories`, 800–6000 at the form), and `ConfirmedCaloriesService.recalculate` (formula from stored body, same column). `CalorieService.dailyCalories` takes six body fields and no product lists (`CalorieService.java:41-48`).

PRD Business Logic states preferences do not change the number and gives the BMR × activity ±250 HALF_UP rule. On the inspected fixture 34 / 180 cm / 82.5 kg / MALE / MODERATE / MAINTAIN that rule is 2767 kcal (BMR 1785 × 1.55 = 2766.75, HALF_UP). That integer is also locked in `CalorieServiceTest` and archive `calorie-formula`. The test-plan anti-pattern is treating the unit formula test as the whole write path, or copying the calculator as oracle without naming the PRD inputs.

Risk #4: `ProfileController.save` runs `@Valid` then `productListsValidator.validate`; `profileService.save` is skipped when `bindingResult.hasErrors()` (`ProfileController.java:89-99`). `ProfileService` does not validate. Cross-list conflict is reported on `excludedProducts`. Existing `@WebMvcTest` proves `never().save` for age 9 only. Illegal product `"a"` shows the message but does not `never().save`. No MVC test for a name on both lists.

Phase 2 is gap-fill plus cookbook §6.1 / §6.5.

## Detailed Findings

### Risk #3 — calorie write paths

**First body save.** Empty `findById` → `repository.save` with `formulaCalories(details)` (`ProfileService.java:84-88`). Covered by `shouldCreateTheRowOnTheFirstSave` (2767 with products) and `shouldStoreNullWhenBothProductListsAreEmpty` (2767 with empty lists). Those two together already imply products do not change the stored number; a dedicated same-body different-lists test would make that explicit.

**Later body save.** Non-null `confirmedCalories` → `replaceBodyAndProducts` only, never `replaceConfirmedCalories` (`ProfileService.java:91-104`). Covered by `shouldKeepTheStoredDailyCaloriesWhenSavingTheRestOfTheProfile`.

**Edit calories.** `update` → `replaceConfirmedCalories` only (`ConfirmedCaloriesService.java:52-58`). Covered by `shouldReplaceOnlyTheDailyCalories` (`never().save()`, `never().findById()`). This inspected method does not `never().replaceBodyAndProducts(...)`.

**Recalculate.** Formula from stored row, then `replaceConfirmedCalories` (`ConfirmedCaloriesService.java:64-71`). Covered by `shouldRecalculateOnlyTheDailyCaloriesFromTheStoredBody`. Same missing `never().replaceBodyAndProducts`.

**Controller.** `POST /profile` never calls `ConfirmedCaloriesService` (`ProfileController.java:89-105`). `shouldRedirectWithASavedNoticeAfterASave` does not `never().update` / `never().recalculate`. Calorie POSTs already `never().save` on `ProfileService`.

**Cheapest remaining (this inspected suite):**

1. Explicit same-body different product lists → same 2767 on first save (`ProfileServiceTest`).
2. `never().replaceBodyAndProducts` on update and recalculate (`ConfirmedCaloriesServiceTest`).
3. `never().update` / `never().recalculate` on successful `POST /profile` (`ProfileControllerTest`).

Do not add a second `CalorieServiceTest` that re-copies 2767 from the calculator. Do not `@SpringBootTest` for column isolation (repository method names already isolate columns; Mockito `never()` is the cheap signal).

### Risk #4 — refused save

**Gate.** BV on body fields, then `ProductListsValidator` (names, max 50, cross-list). Save only if no errors.

**Existing.** `ProductListsValidatorTest.shouldRefuseAProductOnBothListsAndReportItOnTheExcludedField`. `ProductNameValidatorTest` for illegal names. `ProfileControllerTest.shouldShowTheFormAgainWithTheTypedValuesWhenTheSaveIsRefused` (`never().save`, age 9). `shouldShowTheMessageForEachProblemNextToItsField` shows product `"a"` message without `never().save`. `ProfileFormDtoValidationTest` is BV-only (the named anti-pattern; do not expand it as the risk #4 proof).

**Cheapest remaining:**

1. Add `never().save` to the existing illegal-name MVC test.
2. One new MVC `should*` with valid body, `ser` preferred and `Ser` excluded, Polish conflict text, `never().save`.

## Code References

- `src/main/java/com/kenez92/plateplan/profile/service/ProfileService.java:84-117`
- `src/main/java/com/kenez92/plateplan/profile/service/ConfirmedCaloriesService.java:52-71`
- `src/main/java/com/kenez92/plateplan/profile/service/CalorieService.java:41-48`
- `src/main/java/com/kenez92/plateplan/profile/controller/ProfileController.java:68-99`
- `src/main/java/com/kenez92/plateplan/profile/validator/ProductListsValidator.java:42-54`
- `src/test/java/com/kenez92/plateplan/profile/service/CalorieServiceTest.java`
- `src/test/java/com/kenez92/plateplan/profile/service/ProfileServiceTest.java:68-143`
- `src/test/java/com/kenez92/plateplan/profile/service/ConfirmedCaloriesServiceTest.java:59-86`
- `src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java:193-241`

## Architecture Insights

Expected product failures on profile writes are BindingResult or result objects, not 500. Slice tests stub `ProfileService` / `ConfirmedCaloriesService`; unit tests mock the repository. Formula gold for the locked male-moderate fixture is the PRD arithmetic 2767, not a second call to `CalorieService` inside a new test.

## Historical Context (from prior changes)

- `context/archive/2026-10-05-calorie-formula/plan.md:182` — **supported** against current `CalorieServiceTest`: maintain 2767, lose 2517, gain 3017 for 34 / 180 / 82.5 / MALE / MODERATE.
- `context/archive/2026-10-02-save-user-profile/plan.md` — **supported** against `ProductListsValidator`: conflict on excluded; controller validates before save.

## Related Research

Phase 1 archive `context/archive/2026-10-07-testing-critical-path-coverage/` covered risks #1–#2 only.

## Open Questions

- No `@DataJpaTest` on this inspected tree. A refused save against a real row is out of cheapest-layer scope for Phase 2.
- Recalculate CSRF is risk #6 (Phase 3), not Phase 2.
