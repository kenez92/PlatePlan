# Critical-path coverage — Plan Brief

> Full plan: `context/changes/testing-critical-path-coverage/plan.md`
> Research: `context/changes/testing-critical-path-coverage/research.md`

## What & Why

Rollout Phase 1 of `context/foundation/test-plan.md` must protect generate success/failure JSON (risk #1) and own-data (risk #2). Research shows the generate matrix already exists; the remaining cheap signal is that a request cannot choose whose row to read. This plan gap-fills that lock, then writes cookbook §6.2 / §6.4.

## Starting Point

`PlanControllerTest` already asserts two PDF fields, the three `error` codes at HTTP 200, and CSRF-less 403. `shouldLoadAndSaveTheProfileOfTheSignedInLoginOnly` already ignores `login=bob` on GET/POST `/profile`. Generate success checks `never().generate("bob")` without sending a `login` parameter. The two calorie POSTs have no such mirror.

## Desired End State

`POST /plan/generate`, `POST /profile/calories`, and `POST /profile/recalculate` ignore `login=bob` and use the signed-in principal. The test-plan cookbook tells the next author how to add an integration test and a signed-in POST without Playwright, a 500 oracle, or a re-listed error matrix.

## Key Decisions Made

| Decision | Choice | Why (1 sentence) | Source |
| ------------------------------ | ----------------- | ----------------- | ---------------- |
| Coverage posture | Gap-fill + cookbook, not greenfield | The generate HTTP matrix and profile GET/POST ownership already exist | Research |
| Risk #1 "500 instead of JSON" | Do not invent a 500 test | On inspected `plan/**`, expected failures are values at HTTP 200 | Research |
| Remaining tests | `login=bob` on generate + two calorie POSTs | Cheapest remaining proof that the client cannot choose the row | Research |
| `shoppingListPdf` on four error tests | Skip | Optional consistency; the DTO already nulls both PDF fields | Research / Plan |
| Signed-out generate without CSRF | Skip | 403 or 302 both withhold PDFs | Research / Plan |
| New unit tests | None | `PlanServiceTest` and `PlanFilesDtoTest` already cover factories and JSON shape | Research |
| Layer | Existing `@WebMvcTest` + Mockito | Playwright on this JSON contract is the named anti-pattern | Research |
| Phase split | Generate lock → calorie-write lock → cookbook | Cost × signal, cookbook last so it can cite the new methods | Plan |

## Scope

**In scope:**
- One generate `login=bob` method in `PlanControllerTest`
- One calorie-write `login=bob` method in `ProfileControllerTest` covering `/profile/calories` and `/profile/recalculate`
- Cookbook §6.2, §6.4, and a short §6.6 Phase 1 note

**Out of scope:**
- HTTP 500 test, Playwright, `@SpringBootTest`, live Ollama
- Re-listing the three generate error codes
- Download file names / `fetch` CSRF (Phase 4)
- Production or schema changes

## Architecture / Approach

No production change. Slice tests already import `SecurityConfiguration` and stub services. New methods send `.param("login", "bob")` and verify the collaborator received `"alice"`. Cookbook recipes point at those methods and at the existing matrix; they do not paste test source.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --------- | ----------------------- | ------------------------- |
| 1. Generate own-data lock | `login=bob` cannot become `PlanService.generate` argument | Future query/form login on generate |
| 2. Calorie-write own-data lock | Same lock on `/profile/calories` and `/profile/recalculate` | Future login field on calorie writes |
| 3. Cookbook §6.2 / §6.4 | Recipes for the next integration / signed-in POST test | Next agent re-lists codes or uses Playwright |

**Prerequisites:** `research.md` complete; existing `PlanControllerTest` and `ProfileControllerTest`.
**Estimated effort:** One short session across three phases.

## Open Risks & Assumptions

- `Principal.getName()` versus stored `user_profile.login` normalization can break own-row access; it is not a client-supplied IDOR on the inspected handlers and stays out of this phase.
- A 500 outside `PlanController.toFiles` was not inspected; this plan does not invent that case.

## Success Criteria (Summary)

- Generate and both calorie writes ignore `login=bob`.
- §6.2 and §6.4 name Principal-only key, CSRF, and HTTP 200 on expected failure.
- The existing generate matrix is unchanged and not copied into new tests.
