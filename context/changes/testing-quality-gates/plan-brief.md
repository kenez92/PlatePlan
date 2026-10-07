# Wire Playwright in CI and tech-stack — Plan Brief

> Full plan: `context/changes/testing-quality-gates/plan.md`

## What & Why

Phase 4's Playwright test is invisible to CI until Chromium and OS deps are installed. AGENTS.md requires tech-stack to list the library.

## Starting Point

`./gradlew test` includes `PlanDownloadE2eTest`. CI does not install browsers. `tech-stack.md` omits Playwright.

## Desired End State

CI installs Chromium with deps then runs the suite. tech-stack and §5 match that.

## Scope

**In:** Gradle `playwright` task, ci.yml install step, tech-stack sentence, test-plan §5/§6.6.

**Out:** Extra jobs, coverage, Node Playwright, production.
