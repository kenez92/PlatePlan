# Wire Playwright in CI and tech-stack Implementation Plan

## Overview

Install Chromium (with OS deps) in GitHub Actions before `./gradlew test`, declare Playwright Java on `tech-stack.md`, and mark the e2e gate required in the test-plan. No new product tests. No lint/coverage/hooks.

## Current State Analysis

Phase 4 added Playwright 1.63.0 and `PlanDownloadE2eTest`. CI still only runs `./gradlew test`. `tech-stack.md` does not list Playwright. Test-plan §5 still says the e2e gate is required after Phase 5.

## Desired End State

PR CI installs Chromium with deps, then runs the same JUnit suite (including the Playwright test). `tech-stack.md` names Playwright Java. §5 marks the e2e gate required now. §6.6 notes Phase 5.

## What We're NOT Doing

- Extra CI jobs, Playwright Docker image, Firefox/WebKit.
- Coverage, Checkstyle, agent hooks.
- Production code.
- Node Playwright.

## Implementation Approach

Gradle CLI task, then CI, then docs.

## Phase 1: CI install + Gradle CLI

### Overview

- **Behavior asserted**: CI can install Chromium before JUnit.
- **Regression caught**: Headless Chrome missing libs on ubuntu-latest.
- **Research source**: `research.md` Playwright Java CI.
- **Edge / error / boundary**: Chromium only (`--with-deps`).
- **Anti-pattern avoided**: Separate e2e workflow.

### Changes Required:

#### 1. Gradle Playwright CLI

**File**: `build.gradle.kts`

**Intent**: One task agents and CI can run.

**Contract**: `tasks.register<JavaExec>("playwright")` with `sourceSets["test"].runtimeClasspath` and `mainClass = com.microsoft.playwright.CLI`. Do not change the `test` task's JUnit Platform.

#### 2. GitHub Actions

**File**: `.github/workflows/ci.yml`

**Intent**: Install Chromium with OS deps, then the existing test suite.

**Contract**: In job `test`, after `setup-java`, run `chmod +x gradlew && ./gradlew playwright --args="install --with-deps chromium" --no-daemon` then `./gradlew test --no-daemon`. Keep deploy unchanged. Do not add secrets.

### Success Criteria:

#### Automated Verification:

- `build.gradle.kts` registers `playwright` JavaExec with CLI main class
- `ci.yml` job `test` runs Playwright install with `--with-deps chromium` before `./gradlew test`

#### Manual Verification:

- Deploy job still needs `test` and still deploys only on push to `main`

## Phase 2: tech-stack and cookbook

### Overview

- **Behavior asserted**: Playwright is a declared test library; the e2e gate is required.
- **Regression caught**: Next change adding a test library without tech-stack.
- **Research source**: AGENTS.md library rule; test-plan §5.
- **Edge / error / boundary**: test scope, Chromium, `PlanDownloadE2eTest`.
- **Anti-pattern avoided**: Rewriting §1–§2 risk map; adding coverage gates.

### Changes Required:

#### 1. tech-stack.md

**File**: `context/foundation/tech-stack.md`

**Intent**: Satisfy AGENTS.md for the library Phase 4 added.

**Contract**: In "Why this stack", name `com.microsoft.playwright:playwright` 1.63.0 as `testImplementation`, JUnit + Chromium, `PlanDownloadE2eTest`. Do not add Node Playwright.

#### 2. test-plan.md

**File**: `context/foundation/test-plan.md`

**Intent**: Close the rollout.

**Contract**: §3 Phase 4 `complete`, Phase 5 `implementing` then complete at archive. §5 e2e gate "required now" (CI on PR). §6.6 Phase 5 shipped (CI install + tech-stack). Bump §4 unit+integration file count if it still says 30. Do not change §1–§2 strategy text except freshness if required.

### Success Criteria:

#### Automated Verification:

- `tech-stack.md` names Playwright Java 1.63.0 as test-scoped
- §5 e2e row says required now / CI on PR
- §6.6 records Phase 5
- No lint/coverage/hook gate rows added

#### Manual Verification:

- Reading tech-stack makes Playwright a declared library, not an undeclared Gradle extra

## Testing Strategy

### Automated:

- Gradle still uses JUnit Platform. CI install is proven when the PR runs; locally `.\gradlew.bat test --tests com.kenez92.plateplan.plan.controller.PlanDownloadE2eTest` already passed in Phase 4.

### Manual Testing Steps:

1. Confirm deploy job unchanged except still `needs: test`.

## References

- `context/changes/testing-quality-gates/research.md`
- https://playwright.dev/java/docs/ci
- `context/archive/2026-10-07-testing-browser-critical-path/`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles.

### Phase 1: CI install + Gradle CLI

#### Automated

- [x] 1.1 `build.gradle.kts` registers `playwright` JavaExec with CLI main class
- [x] 1.2 `ci.yml` job `test` runs Playwright install with `--with-deps chromium` before `./gradlew test`

#### Manual

- [x] 1.3 Deploy job still needs `test` and still deploys only on push to `main`

### Phase 2: tech-stack and cookbook

#### Automated

- [x] 2.1 `tech-stack.md` names Playwright Java 1.63.0 as test-scoped
- [x] 2.2 §5 e2e row says required now / CI on PR
- [x] 2.3 §6.6 records Phase 5
- [x] 2.4 No lint/coverage/hook gate rows added

#### Manual

- [x] 2.5 Reading tech-stack makes Playwright a declared library, not an undeclared Gradle extra
