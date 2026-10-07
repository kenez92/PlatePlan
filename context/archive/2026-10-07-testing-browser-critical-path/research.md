---
date: 2026-10-07T11:00:00+02:00
researcher: Daniel
git_commit: 75ec290
branch: feature/test_plan_controller
repository: PlatePlan
topic: "Ground test-plan Phase 4: Playwright US-01"
tags: [research, codebase, playwright, plan, csrf, e2e, testing]
status: complete
last_updated: 2026-10-07
last_updated_by: Daniel
last_updated_note: Phase 4 grounding for browser US-01
---

# Research: Ground test-plan Phase 4 (Playwright US-01)

**Date**: 2026-10-07T11:00:00+02:00
**Researcher**: Daniel
**Git Commit**: 75ec290
**Branch**: feature/test_plan_controller
**Repository**: PlatePlan

## Research Question

Ground rollout Phase 4 of `context/foundation/test-plan.md`. What is the cheapest Playwright Java test that proves generate `fetch` sends CSRF, two named PDF downloads appear, and refresh drops them — without re-testing the JSON error matrix?

## Summary

On the inspected `/plan` page, `plan-download.js` POSTs `fetch` with `Accept: application/json` and the CSRF header/token from `data-csrf-header` / `data-csrf-token` on `#generate-plan` (`plan.html:18-19`, `plan-download.js:44-49`). Success sets blob URLs on `#download-diet` (`download="dieta-na-jutro.pdf"`) and `#download-list` (`download="lista-zakupow.pdf"`) and unhides `#plan-downloads`. Refresh reloads the template with `#plan-downloads` `hidden` and no `href`.

JSON `error` codes are already locked in `PlanControllerTest`. This phase must not re-list them. Live Ollama and a reachable database are not available in `./gradlew test` (dummy `DATABASE_URL` like `ApplicationTest`). Cheapest server: `@SpringBootTest(RANDOM_PORT)` with `@Primary` test beans for `UserDetailsService` (in-memory alice) and `PlanService` (success `DietPlan` so real `PlanPdfWriter` still writes PDFs). `SESSION_COOKIE_SECURE=false` for HTTP. Playwright Java `com.microsoft.playwright:playwright` (current docs 1.63.0) + JUnit; Chromium headless. Node `@playwright/test` is out of stack.

## Detailed Findings

### Browser failure path (risks #1 / #6 fetch)

1. Signed-in GET `/plan` renders CSRF on the button.
2. Click `#generate-plan` → `fetch` POST `/plan/generate` with CSRF header.
3. Without the header the browser gets 403; `plan-download.js` treats non-JSON as login redirect or unavailable (`shouldReturnToLogin` / catch).
4. With two Base64 fields, JS creates blob URLs and shows the two `download` file names.
5. Reload drops blob URLs because they live only in page memory.

### Existing coverage (not this phase)

- Generate JSON matrix and CSRF-less 403: `PlanControllerTest` (MockMvc).
- Form CSRF: Phase 3.

### Cheapest remaining

One `should*` Playwright test: login via `#login-username` / `#login-password`, open `/plan`, click generate, assert CSRF header on the intercepted POST, assert the two `download` names and `blob:` hrefs, reload, assert `#plan-downloads` hidden and hrefs gone.

Do not assert `PROFILE_REQUIRED` / `CALORIES_REQUIRED` / `UNAVAILABLE` in Playwright.

### Speculative gaps to skip

- Clicking the `<a download>` to save files to disk (OS download UX).
- Headed browser, Firefox, WebKit.
- Node Playwright / 10x-e2e-setup (`@playwright/test` + `webServer`).
- Real Ollama, Testcontainers, H2 (not on tech-stack).

## Code References

- `src/main/resources/static/js/plan-download.js:38-107`
- `src/main/resources/templates/plan.html:18-26`
- `src/main/resources/templates/fragments/chrome.html:31-38`
- `src/test/java/com/kenez92/plateplan/ApplicationTest.java:12-16`
- https://playwright.dev/java/docs/intro (artifact 1.63.0)

## Architecture Insights

`@SpringBootTest` is allowed only at this e2e layer (`testing.mdc`). Collaborators are `@TestConfiguration` `@Bean` mocks with `@Primary`, same as slices — not `@MockitoBean`. Constructor injection for `@LocalServerPort`. Playwright objects stay as `final` locals in try-with-resources (one method).

## Historical Context (from prior changes)

- Archive `generate-diet-with-ollama` — **supported**: two download buttons; files stay in the browser; CSRF on fetch.
- Test-plan §1 — **supported**: Playwright is only for failures that exist solely in the browser.

## Open Questions

- CI browser install is Phase 5. Phase 4 adds the test to `./gradlew test`; first run downloads Chromium via Playwright.
- Declaring Playwright in `tech-stack.md` is Phase 5; Phase 4 must still add `testImplementation` so the test compiles (test-plan Phase 4 owns the test, Phase 5 owns the stack row).
