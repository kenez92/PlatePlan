# Browser US-01 PDF downloads — Plan Brief

> Full plan: `context/changes/testing-browser-critical-path/plan.md`
> Research: `context/changes/testing-browser-critical-path/research.md`

## What & Why

US-01 can pass MockMvc JSON and still fail in the browser if `fetch` omits CSRF, download `download` names are wrong, or blob URLs survive refresh.

## Starting Point

No Playwright. Generate JSON is already in `PlanControllerTest`. JS in `plan-download.js` is untested.

## Desired End State

One Playwright Java `@SpringBootTest` test proves CSRF-on-fetch, two named PDFs, refresh drops files. Cookbook §6.3 filled. CI/tech-stack wait for Phase 5.

## Key Decisions Made

| Decision | Choice | Why | Source |
|----------|--------|-----|--------|
| Runner | Playwright Java 1.63.0 + JUnit | Test-plan §4 | Research |
| Server | `@SpringBootTest(RANDOM_PORT)` | Only allowed e2e layer | testing.mdc |
| Collaborators | `@Primary` mock `PlanService` + in-memory `UserDetailsService` | No Ollama/Postgres in CI unit job | Research |
| Node Playwright | Out | Wrong stack | Research |

## Scope

**In:** Gradle dep, one e2e test, §6.3 / §6.6.

**Out:** CI, tech-stack.md, JSON matrix, production.

## Phases at a Glance

| Phase | What it delivers | Key risk |
|-------|------------------|----------|
| 1. Dependency | Playwright Java on the test classpath | Missing artifact |
| 2. US-01 test | Browser proof | CSRF fetch / sticky files |
| 3. Cookbook §6.3 | Recipe | Next agent copies JSON matrix into Playwright |
