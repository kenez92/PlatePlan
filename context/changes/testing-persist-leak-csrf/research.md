---
date: 2026-10-07T10:40:00+02:00
researcher: Daniel
git_commit: 198d2b91ac76bc144ab82c784b1ad93496312853
branch: feature/test_plan_controller
repository: PlatePlan
topic: "Ground test-plan Phase 3: persist, leak, CSRF"
tags: [research, codebase, plan, security, csrf, logging, testing]
status: complete
last_updated: 2026-10-07
last_updated_by: Daniel
last_updated_note: Phase 3 grounding for risks #5 and #6
---

# Research: Ground test-plan Phase 3 (persist, leak, CSRF)

**Date**: 2026-10-07T10:40:00+02:00
**Researcher**: Daniel
**Git Commit**: 198d2b91ac76bc144ab82c784b1ad93496312853
**Branch**: feature/test_plan_controller
**Repository**: PlatePlan

## Research Question

Ground rollout Phase 3 of `context/foundation/test-plan.md` (risks #5 and #6). For each risk: real failure path, existing tests, cheapest remaining layer, speculative gaps.

## Summary

On the inspected generate path, `PlanService` only calls `UserProfileRepository.findById`. There is no plan table in Liquibase. PDFs are `ByteArrayOutputStream` bytes returned as JSON Base64 (`PlanController.java:58-61`). That does not prove filesystem writes outside this path; it does prove this inspected path does not persist files through the repository or a plan entity.

Plan loggers (`PlanService`, `DietGenerator`, `PlanController`) write exception class names only. `PlanFilesDto.toString()` omits PDF bytes (`PlanFilesDtoTest.shouldOmitBytesFromToString`). The model prompt on this inspected path receives calories, English goal text, and product names — not age, height, weight, sex, activity, or login (`DietGeneratorTest.shouldIncludeRoleGoalCaloriesAndProductNamesAndOmitPersonalDataFromThePrompt`).

Risk #6: CSRF is enabled globally (`SecurityConfiguration.java:38-39`). CSRF-less 403 exists for `/register`, `/login`, `/logout`, `/profile`, `/profile/calories`, `/plan/generate`. **Missing** on this inspected suite: `POST /profile/recalculate`. Generate `fetch` CSRF is Phase 4.

## Detailed Findings

### Risk #5 — persist and leak

**No-persist.** `PlanService.generate` → `findById` then `dietGenerator.generate(calories, goal, preferred, excluded)` (`PlanService.java:41-59`). `PlanPdfWriter` saves a PDFBox document to a `ByteArrayOutputStream`. Existing `PlanServiceTest` success method does not `never().save` / `never().replace*`.

**Prompt contract.** Already covered by `DietGeneratorTest.shouldIncludeRoleGoalCaloriesAndProductNamesAndOmitPersonalDataFromThePrompt` (`doesNotContain` alice / 34 / 180 / 82.5 / MALE / LOSE_WEIGHT). Activity `MODERATE` is not in that omit list.

**Logs.** Profile already has `shouldLogOnlyTheClassNamesWhenTheDatabaseFails`. Plan package has no ListAppender test. Failure logs: `PlanService.java:63`, `DietGenerator.java:105`, `PlanController.java:63-64`.

**Cheapest remaining:**

1. `never().save` / `never().replaceConfirmedCalories` / `never().replaceBodyAndProducts` on generate success (`PlanServiceTest`).
2. One ListAppender test on `DietGenerator` model failure (exception message planted with key / calories / product name).
3. Optional: `doesNotContain("MODERATE")` on the existing prompt test.

Do not snapshot the full prompt. Do not log the prompt in a test. Do not `@SpringBootTest` to hunt files on disk.

### Risk #6 — CSRF

| Endpoint | CSRF-less 403 | With-token success |
|----------|---------------|-------------------|
| POST /register | `shouldRejectARegistrationPostWithoutACsrfToken` | `shouldSignTheNewAccountInAndRedirectHome` |
| POST /login | `shouldRejectALoginPostWithoutACsrfToken` | `shouldSignInWithTheCorrectPassword` |
| POST /logout | `shouldRejectALogoutPostWithoutACsrfToken` | `shouldSignOutAndReturnToTheLoginWindow` |
| POST /profile | `shouldRejectAPostWithoutACsrfToken` | `shouldRedirectWithASavedNoticeAfterASave` |
| POST /profile/calories | `shouldRejectACaloriePostWithoutACsrfToken` | `shouldSaveOnlyTheDailyCaloriesOnTheCalorieEndpoint` |
| POST /profile/recalculate | **NONE** | `shouldRecalculateTheStoredDailyCaloriesWithoutTakingANumber` |
| POST /plan/generate | `shouldRejectAGeneratePostWithoutACsrfToken` | `shouldReturnTwoBase64PdfsWithoutErrorWhenGenerationSucceeds` |

**Cheapest remaining:** `shouldRejectARecalculatePostWithoutACsrfToken` in `ProfileControllerTest`, `never().recalculate`. Skip generate `fetch` (Phase 4). Do not treat `SESSION_COOKIE_SECURE=true` as a product bug.

## Code References

- `src/main/java/com/kenez92/plateplan/plan/service/PlanService.java:41-63`
- `src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java:67-105`
- `src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java:42-66`
- `src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java:38-39`
- `src/test/java/com/kenez92/plateplan/plan/service/DietGeneratorTest.java:67-86`
- `src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java:405-408`

## Architecture Insights

Expected generate failures are HTTP 200 JSON `error`, not 500. CSRF-less POSTs are 403. Logging on this inspected path is class names, matching profile.

## Historical Context (from prior changes)

- Archive `generate-diet-with-ollama` — **supported**: do not persist PDFs; do not log key/prompt/lists/calories/PDF bytes.
- Archive `register-and-sign-in` — **supported**: CSRF on forms; Secure cookie is a session setting, not a CSRF gap.

## Related Research

Phase 2 archive deferred recalculate CSRF to this phase.

## Open Questions

- Wire-level RestClient DEBUG of the Authorization header is outside this inspected Java path.
