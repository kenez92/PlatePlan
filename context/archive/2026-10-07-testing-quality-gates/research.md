---
date: 2026-10-07T12:10:00+02:00
researcher: Daniel
git_commit: f6e58c1
branch: feature/test_plan_controller
repository: PlatePlan
topic: "Ground test-plan Phase 5: CI and tech-stack"
tags: [research, ci, playwright, tech-stack, testing]
status: complete
last_updated: 2026-10-07
last_updated_by: Daniel
last_updated_note: Phase 5 grounding for quality-gates wiring
---

# Research: Ground test-plan Phase 5 (CI and tech-stack)

**Date**: 2026-10-07T12:10:00+02:00
**Researcher**: Daniel
**Git Commit**: f6e58c1
**Branch**: feature/test_plan_controller
**Repository**: PlatePlan

## Research Question

How to run `PlanDownloadE2eTest` on GitHub Actions `ubuntu-latest` and declare Playwright Java on the locked stack, without adding lint/coverage/hook gates.

## Summary

`.github/workflows/ci.yml` job `test` runs `./gradlew test --no-daemon` after Temurin 21. `PlanDownloadE2eTest` is on that classpath (`build.gradle.kts` `com.microsoft.playwright:playwright:1.63.0`). On this inspected local Windows run, `Playwright.create()` downloaded Chromium itself. CI Ubuntu still needs OS libraries (`install --with-deps`) or the headed/headless shell fails. Official Playwright Java CI uses `com.microsoft.playwright.CLI` with `install --with-deps` before tests (https://playwright.dev/java/docs/ci).

`tech-stack.md` "Why this stack" lists declared libraries and does not mention Playwright. AGENTS.md forbids adding a library unless that file lists it; Phase 4 already added the Gradle coordinate. Phase 5 must name Playwright Java 1.63.0 as `testImplementation`, Chromium-only, used by `PlanDownloadE2eTest`.

Cookbook §6.3 is filled. §5 still says e2e is "required after §3 Phase 5". This phase flips that to required now. No new test class.

## Detailed Findings

**CI gap.** One step, no Playwright install. Cheapest fill: Gradle `JavaExec` task whose main class is `com.microsoft.playwright.CLI`, args `install --with-deps chromium`, invoked in `ci.yml` before `test`.

**Stack gap.** Add Playwright to the declared-dependencies sentence in `tech-stack.md`. Do not add Node `@playwright/test`.

**Anti-pattern.** A second CI job that only runs e2e, or Playwright Docker, when one job already runs the whole JUnit suite.

## Code References

- `.github/workflows/ci.yml:12-21`
- `build.gradle.kts:39-47`
- `src/test/java/com/kenez92/plateplan/plan/controller/PlanDownloadE2eTest.java`
- `context/foundation/test-plan.md` §5
- https://playwright.dev/java/docs/ci

## Open Questions

- `install --with-deps` is for the CI Ubuntu job. Local `.\gradlew.bat test` already downloaded browsers without that task.
