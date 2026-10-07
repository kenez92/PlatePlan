---
date: 2026-10-07T09:15:00+02:00
researcher: Daniel
git_commit: bf6d70f82dfe37a01aed024e0ee60fd9b6cfde6d
branch: feature/diet
repository: PlatePlan
topic: "Ground test-plan Phase 1: generate error matrix and own-data ownership"
tags: [research, codebase, plan, profile, security, testing]
status: complete
last_updated: 2026-10-07
last_updated_by: Daniel
last_updated_note: Phase 1 grounding for risks #1 and #2
---

# Research: Ground test-plan Phase 1 (generate error matrix and own-data)

**Date**: 2026-10-07T09:15:00+02:00
**Researcher**: Daniel
**Git Commit**: bf6d70f82dfe37a01aed024e0ee60fd9b6cfde6d
**Branch**: feature/diet
**Repository**: PlatePlan

## Research Question

Ground rollout Phase 1 of `context/foundation/test-plan.md` ("Critical-path coverage").

Risks to verify:

- **#1**: After calories are confirmed the user does not get two PDFs, or gets a 500 instead of JSON with only `error`.
- **#2**: A signed-in account reads or generates from another login's data.

For each risk: ground the real failure path, quote lines, verify or correct the response guidance, locate existing tests, name the cheapest useful layer, and flag speculative risks or misleading hot-spot evidence.

## Summary

On the inspected generate path, `POST /plan/generate` calls `planService.generate(principal.getName())` and maps `PlanResult` to `PlanFilesDto` ([PlanController.java:44-66](src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java)). Expected outcomes on this inspected handler are HTTP 200 JSON: two Base64 PDF fields and no `error` on success, or a single `error` of `PROFILE_REQUIRED`, `CALORIES_REQUIRED`, or `UNAVAILABLE`. The compact constructor of `PlanFilesDto` nulls both PDF fields when `error != null` ([PlanFilesDto.java:21-28](src/main/java/com/kenez92/plateplan/plan/controller/dto/PlanFilesDto.java)).

The test-plan wording "500 instead of JSON" is **speculative on this inspected path**. `PlanResult` documents expected outcomes as values, not exceptions ([PlanResult.java:3-6](src/main/java/com/kenez92/plateplan/plan/model/PlanResult.java)). PDF-write exceptions on this inspected controller path become `UNAVAILABLE` at 200 ([PlanController.java:58-66](src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java)). This research did not inspect a global error handler, so a 500 outside `plan/**` is an explicit gap, not a proven absence.

On the inspected profile and plan handlers, the account key is `Principal.getName()`. The five inspected controller methods that load or write account data pass that name into services ([ProfileController.java:52-99](src/main/java/com/kenez92/plateplan/profile/controller/ProfileController.java), [PlanController.java:44-45](src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java)). `GET /plan` loads no profile ([PlanController.java:37-39](src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java)). `ProfileFormDto` on this inspected record has no login field ([ProfileFormDto.java:29-50](src/main/java/com/kenez92/plateplan/profile/controller/dto/ProfileFormDto.java)).

Existing `@WebMvcTest` coverage already asserts the generate JSON matrix, signed-in CSRF-less generate (403), anonymous generate-with-CSRF (302 to `/`), and profile `login=bob` ignored. The cheapest useful layer for Phase 1 remains `@WebMvcTest` plus existing `PlanServiceTest` units. Playwright on this JSON contract is the anti-pattern the test plan already forbids.

Phase 1 is therefore **gap-fill and cookbook**, not greenfield coverage. Remaining cheap signal on the inspected tests: an extra `login=bob` parameter on generate and on the two calorie POSTs (those three inspected methods have no parallel to `shouldLoadAndSaveTheProfileOfTheSignedInLoginOnly`). Asserting `shoppingListPdf` absence on the four inspected error tests that only check `dietPdf` is optional consistency, not a new behavior.

## Detailed Findings

### Risk #1 — generate success and expected failure

**Entry (this inspected handler).** `PlanController.generate` is `POST /plan/generate`, `produces = APPLICATION_JSON`, `@ResponseBody`, and returns `toFiles(planService.generate(principal.getName()))` ([PlanController.java:42-46](src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java)).

**How this inspected service decides the result.** `PlanService.generate(login)` on this inspected method:

- empty `findById(login)` → `PlanResult.noProfile()` ([PlanService.java:41-43](src/main/java/com/kenez92/plateplan/plan/service/PlanService.java));
- `confirmedCalories == null` on the loaded row → `PlanResult.noCalories()` ([PlanService.java:50-54](src/main/java/com/kenez92/plateplan/plan/service/PlanService.java));
- `DataAccessException` on this try → `PlanResult.unavailable()` ([PlanService.java:44-47](src/main/java/com/kenez92/plateplan/plan/service/PlanService.java));
- otherwise `dietGenerator.generate(...)` ([PlanService.java:55-59](src/main/java/com/kenez92/plateplan/plan/service/PlanService.java)).

**How this inspected generator produces `UNAVAILABLE`.** On this inspected `DietGenerator.generate` method, a blank API key, a null `DietPlan`, or any caught `Exception` returns `PlanResult.unavailable()` ([DietGenerator.java:71-85](src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java)). This method does not return `noProfile()` or `noCalories()`.

**How this inspected controller maps the result.** `toFiles` on this inspected private method checks `missingProfile`, then `missingCalories`, then `!isSuccessful()`, then writes PDFs, then catches `Exception` → `unavailable()` ([PlanController.java:48-66](src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java)).

**JSON shape (this inspected DTO).** When `error != null`, this compact constructor assigns both PDF fields to null. When `error == null`, both PDF fields must be non-null or it throws `IllegalArgumentException` ([PlanFilesDto.java:21-28](src/main/java/com/kenez92/plateplan/plan/controller/dto/PlanFilesDto.java)). Jackson `@JsonInclude(NON_NULL)` is on the record ([PlanFilesDto.java:10-11](src/main/java/com/kenez92/plateplan/plan/controller/dto/PlanFilesDto.java)).

**Auth and CSRF on inspected tests, not inferred from silence.**

- Signed-in `POST /plan/generate` without CSRF on `PlanControllerTest.shouldRejectAGeneratePostWithoutACsrfToken` expects 403 and `generate` never called ([PlanControllerTest.java:167-171](src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java)).
- Signed-out `POST /plan/generate` with CSRF on `SecurityConfigurationTest.shouldRedirectASignedOutGeneratePostToTheLoginWindow` expects 302 to `/` ([SecurityConfigurationTest.java:126-129](src/test/java/com/kenez92/plateplan/config/SecurityConfigurationTest.java)).
- The inspected filter chain ends with `anyRequest().authenticated()` after the listed permit matchers ([SecurityConfiguration.java:40-44](src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java)).

**500 claim.** On this inspected generate handler, PDF-write failure is caught and returned as `UNAVAILABLE` ([PlanController.java:58-66](src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java)). A null `Principal` on this handler would call `getName()` unguarded ([PlanController.java:45](src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java)); that case is not covered by an inspected test. Combined with `anyRequest().authenticated()`, this research treats unauthenticated generate as the redirect test above, not as a 500 matrix case.

### Risk #2 — own-data key

**Inspected profile handlers.** These four methods pass `principal.getName()` into services: `show` load ([ProfileController.java:52-58](src/main/java/com/kenez92/plateplan/profile/controller/ProfileController.java)), `updateCalories` ([ProfileController.java:77](src/main/java/com/kenez92/plateplan/profile/controller/ProfileController.java)), `recalculate` ([ProfileController.java:85](src/main/java/com/kenez92/plateplan/profile/controller/ProfileController.java)), `save` ([ProfileController.java:99](src/main/java/com/kenez92/plateplan/profile/controller/ProfileController.java)). None of those four method signatures take a login `@RequestParam` or `@PathVariable`.

**Inspected plan handlers.** `show` takes no `Principal` and returns the view name only ([PlanController.java:37-39](src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java)). `generate` takes `Principal` and passes `getName()` ([PlanController.java:44-45](src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java)).

**Inspected form records.** `ProfileFormDto` components on this record are age, height, weight, sex, goal, activity, and two product lists — no login ([ProfileFormDto.java:29-50](src/main/java/com/kenez92/plateplan/profile/controller/dto/ProfileFormDto.java)). `DailyCaloriesForm` on this record is `dailyCalories` only ([DailyCaloriesForm.java:11-15](src/main/java/com/kenez92/plateplan/profile/controller/dto/DailyCaloriesForm.java)).

**Existing ownership test (profile GET/POST only).** `shouldLoadAndSaveTheProfileOfTheSignedInLoginOnly` sends `login=bob` on GET `/profile` and POST `/profile` and verifies `load("alice")` / `save("alice", …)` ([ProfileControllerTest.java:158-170](src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java)). This inspected method does not POST `/profile/calories` or `/profile/recalculate` with `login=bob`.

**Existing ownership check (generate success only).** `shouldReturnTwoBase64PdfsWithoutErrorWhenGenerationSucceeds` verifies `generate("alice")` and `never().generate("bob")` ([PlanControllerTest.java:103-104](src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java)). That test does not send a `login` request parameter.

**Challenge result.** "Signed in means they see their own data" is the wrong assumption to test as a feeling; the grounded check is "the inspected handlers do not read a client-supplied login." On the inspected controller source, that check holds. A `login=bob` test on generate would lock the current signature, not discover a hidden parameter.

### Existing tests versus the Phase 1 matrix

**Risk #1 — present on inspected files.**

| Outcome | Inspected test method | Anchor |
|---------|----------------------|--------|
| Success: 200, two PDF fields, no `error` | `shouldReturnTwoBase64PdfsWithoutErrorWhenGenerationSucceeds` | [PlanControllerTest.java:88-101](src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java) |
| `PROFILE_REQUIRED` at 200, no `dietPdf` / `shoppingListPdf` | `shouldReturnProfileRequiredWhenThereIsNoProfile` | [PlanControllerTest.java:108-115](src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java) |
| `CALORIES_REQUIRED` at 200, no `dietPdf` | `shouldReturnCaloriesRequiredWhenTheTargetIsMissing` | [PlanControllerTest.java:121-127](src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java) |
| `UNAVAILABLE` (model) at 200, no `dietPdf` | `shouldReturnUnavailableWhenTheModelFails` | [PlanControllerTest.java:133-139](src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java) |
| `UNAVAILABLE` (PDF IO / runtime) at 200 | `shouldReturnUnavailableWhenPdfWritingFails`, `shouldReturnUnavailableWhenPdfWritingThrowsARuntimeException` | [PlanControllerTest.java:145-163](src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java) |
| DTO-only success/failure JSON | `shouldSerializeSuccessAsTwoBase64PdfFieldsWithoutError`, `shouldSerializeFailureAsOnlyTheErrorField` | [PlanFilesDtoTest.java:17-33](src/test/java/com/kenez92/plateplan/plan/controller/dto/PlanFilesDtoTest.java) |
| Service: no row / null calories / DB / generator | four `PlanServiceTest` methods | [PlanServiceTest.java:51-94](src/test/java/com/kenez92/plateplan/plan/service/PlanServiceTest.java) |
| Signed-in CSRF-less generate → 403 | `shouldRejectAGeneratePostWithoutACsrfToken` | [PlanControllerTest.java:167-171](src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java) |
| Signed-out generate with CSRF → 302 `/` | `shouldRedirectASignedOutGeneratePostToTheLoginWindow` | [SecurityConfigurationTest.java:126-129](src/test/java/com/kenez92/plateplan/config/SecurityConfigurationTest.java) |

**Risk #1 — gaps on inspected files (not claimed missing from the whole suite).**

- Signed-out generate **without** CSRF is not an inspected test method. Spring's CSRF vs authenticate order on that exact request was not exercised here.
- Four inspected error tests omit `jsonPath("$.shoppingListPdf").doesNotExist()` ([PlanControllerTest.java:121-163](src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java)). `PROFILE_REQUIRED` already asserts both PDF keys absent ([PlanControllerTest.java:114-115](src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java)). The DTO constructor already nulls both fields when `error != null` ([PlanFilesDto.java:22-24](src/main/java/com/kenez92/plateplan/plan/controller/dto/PlanFilesDto.java)).
- No inspected test names HTTP 500. The archive contract is "Nie 500, nie 503" ([plan.md:189](context/archive/2026-10-05-generate-diet-with-ollama/plan.md)); the live tests use `status().isOk()` instead.

**Risk #2 — present / thin on inspected files.**

- Profile GET/POST ignore `login=bob` ([ProfileControllerTest.java:158-170](src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java)).
- Generate success verifies the service argument is `alice` not `bob` ([PlanControllerTest.java:103-104](src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java)).
- No inspected test sends `login=bob` on `/profile/calories`, `/profile/recalculate`, or `/plan/generate`.

### Response-guidance verdict

| Guidance | Verdict on inspected evidence |
|----------|-------------------------------|
| Two PDF fields, no `error` on success | Supported by controller + DTO + `PlanControllerTest` |
| Three `error` codes at HTTP 200 | Supported |
| Anonymous and CSRF-less POST do not return PDFs | Supported as two separate inspected cases (302 with CSRF; 403 signed-in without CSRF), not one combined case |
| Principal is the key | Supported on the six inspected controller methods listed above |
| `@WebMvcTest` for the JSON matrix; Playwright only for downloads | Supported; adding Playwright for this matrix would duplicate `PlanControllerTest` |
| "500 instead of JSON" as a Phase 1 failure to newly prove | Speculative on `plan/**`. Expected failures are values. Do not write a test whose oracle is "the current mapper never throws" |
| Happy-path-only `@WithMockUser` as the anti-pattern | Partially already avoided on profile GET/POST; calorie POSTs and generate-with-`login` param are the remaining cheap mirrors |

### Cheapest useful layer

For Phase 1 on this stack, the cheapest layer that already gives the signal is **`@WebMvcTest` + Mockito collaborators**, with `PlanServiceTest` for the three `PlanResult` factories. Do not add Playwright, `@SpringBootTest`, or a live Ollama call to prove the JSON matrix.

If Phase 1 ships any new test, keep it to:

1. `POST /plan/generate?login=bob` (or form/query `login=bob`) still calls `generate("alice")` — mirrors the profile ownership test.
2. `POST /profile/calories` and `POST /profile/recalculate` with `login=bob` still use `"alice"` — same pattern on the two inspected calorie writes that lack it.

Do not re-list the three error codes. Do not snapshot prompt text. Do not assert download file names (Phase 4).

### Hot-spot correction

`src/main/java/.../plan` (22 commits/30d in the test plan) is **churn evidence**, not a missing-test location. On this commit the generate HTTP matrix already lives in `PlanControllerTest`. A plan that "adds coverage to the plan package" without naming a gap would raise coverage, not catch a new regression.

## Code References

- `src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java:42-66` — generate entry and `toFiles` mapping
- `src/main/java/com/kenez92/plateplan/plan/service/PlanService.java:39-59` — login → row → `PlanResult`
- `src/main/java/com/kenez92/plateplan/plan/model/PlanResult.java:3-35` — value outcomes, `isSuccessful()`
- `src/main/java/com/kenez92/plateplan/plan/controller/dto/PlanFilesDto.java:10-44` — exclusive PDF vs `error`
- `src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java:71-85` — `UNAVAILABLE` only
- `src/main/java/com/kenez92/plateplan/profile/controller/ProfileController.java:52-99` — Principal key on show/save/calories/recalculate
- `src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java:40-44` — deny-by-default after permits
- `src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java:88-171` — generate HTTP matrix and CSRF
- `src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java:158-170` — `login=bob` ignored on profile GET/POST
- `src/test/java/com/kenez92/plateplan/config/SecurityConfigurationTest.java:112-129` — anonymous `/plan` GET and generate POST

## Architecture Insights

Slice tests import real `SecurityConfiguration` and stub application services. Expected product failures on the inspected plan and profile write paths are result objects rendered as 200, not thrown exceptions. The generate POST is JSON (`@ResponseBody`); CSRF therefore cannot come from a Thymeleaf hidden field on that request — archive and `plan.html` put the token on the page for `fetch` (browser behavior is Phase 4).

`PlanService` on this inspected class logs only exception class names on database failure ([PlanService.java:62-68](src/main/java/com/kenez92/plateplan/plan/service/PlanService.java)). Persist-and-leak (risk #5) is out of Phase 1 scope.

## Historical Context (from prior changes)

- `context/archive/2026-10-05-generate-diet-with-ollama/plan.md:189` — **supported** against current `PlanController` / `PlanFilesDto`: HTTP 200 on expected results; Principal-only login; CSRF required; three error codes; not 500/503 on that contract.
- `context/archive/2026-10-05-generate-diet-with-ollama/plan.md:144-150` — **supported** against `PlanFilesDto` compact constructor: success two PDF fields; failure only `error`.
- `context/archive/2026-10-05-generate-diet-with-ollama/plan.md:205-206` — **supported** against `PlanControllerTest` + `SecurityConfigurationTest` for GET, CSRF 403, success JSON, three errors, signed-out redirect. "Inny login nigdy nie jest argumentem serwisu" is **partial** in tests: success path verifies `never().generate("bob")` but does not send a `login` parameter.
- `context/archive/2026-10-02-save-user-profile/plan.md:12` and `:61` — **supported** against `ProfileController`: `Principal.getName()`; a `login` request parameter must be ignored. The live test covers GET/POST `/profile` only.
- `context/archive/2026-10-01-spring-security-sign-in/plan.md:71` — **partial**: CSRF still on and `anyRequest().authenticated()` still ends the chain ([SecurityConfiguration.java:39-44](src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java)). The F-02 permit list in that archive line does not include `/js/**` / `/.well-known/**`, which the current matcher list does ([SecurityConfiguration.java:41](src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java)). That staleness does not change `/plan` or `/profile` denial.
- Generate-archive prompt "kcal + lists only" is **partial**: current `DietGenerator.generate` also takes `Goal` ([DietGenerator.java:68-76](src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java)). Irrelevant to the Phase 1 JSON matrix; do not freeze a pre-addendum prompt in a Phase 1 test.

## Related Research

No other `research.md` under `context/changes/` on this topic. Archived slice research was not re-read; contracts were taken from the three archive `plan.md` files named above.

## Open Questions

- Global `/error` or exception-resolver behavior for an uncaught exception **outside** `PlanController.toFiles` was not inspected. A Phase 1 test should not invent that 500 case.
- Exact `Principal.getName()` string versus stored `user_profile.login` (normalization at registration) was not inspected. That can break own-row access; it is not a client-supplied IDOR on the inspected handlers.
- Signed-out generate **without** CSRF: 403 vs 302 was not exercised. Either outcome still withholds PDFs; do not spend Phase 1 budget on the status code unless planning wants one extra `SecurityConfigurationTest`.
- Browser `fetch` CSRF header and named PDF downloads remain Phase 4 (`test-plan.md` §3 row 4).
