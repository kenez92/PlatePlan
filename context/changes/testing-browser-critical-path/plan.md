# Browser US-01 PDF downloads Implementation Plan

## Overview

Add Playwright Java and one `@SpringBootTest(RANDOM_PORT)` test that signs in, generates via the page `fetch`, asserts the CSRF header, shows two named PDF links, and proves refresh drops them. Fill cookbook §6.3. No production code. Do not move the JSON error matrix to Playwright.

## Current State Analysis

`PlanControllerTest` already locks JSON and CSRF-less 403. `plan-download.js` is untested in a browser. `build.gradle.kts` has no Playwright. `ApplicationTest` is the only `@SpringBootTest` and uses a dummy unreachable database.

## Desired End State

One `should*` in a new e2e test class. Chromium hits the running app. Generate `fetch` carries CSRF. `#download-diet` / `#download-list` have `dieta-na-jutro.pdf` / `lista-zakupow.pdf` and `blob:` hrefs. After reload those hrefs are gone. §6.3 describes that recipe.

### Key Discoveries:

- Official Java artifact `com.microsoft.playwright:playwright:1.63.0`.
- In-memory `UserDetailsService` + mocked `PlanService` avoid Ollama and PostgreSQL.
- `SESSION_COOKIE_SECURE=false` is required on HTTP (not a product bug).

## What We're NOT Doing

- JSON error-code matrix in Playwright.
- Node `@playwright/test` / 10x-e2e-setup.
- CI workflow (Phase 5).
- `tech-stack.md` Playwright row (Phase 5).
- Live Ollama, H2, Testcontainers.
- Production JS/HTML changes.
- Saving files to the OS download folder.

## Implementation Approach

Dependency first, then one test class, then cookbook §6.3.

## Phase 1: Playwright Java dependency

### Overview

- **Behavior asserted**: The test classpath can import `com.microsoft.playwright`.
- **Regression caught**: Missing artifact.
- **Research source**: `research.md` Playwright Java 1.63.0.
- **Edge / error / boundary**: test scope only.
- **Anti-pattern avoided**: Adding Node Playwright.

### Changes Required:

#### 1. Gradle

**File**: `build.gradle.kts`

**Intent**: Declare Playwright Java as `testImplementation`.

**Contract**: `testImplementation("com.microsoft.playwright:playwright:1.63.0")`. Do not add a Node package. Do not change the existing `tasks.withType<Test> { useJUnitPlatform() }` beyond keeping JUnit Platform.

### Success Criteria:

#### Automated Verification:

- `build.gradle.kts` lists `com.microsoft.playwright:playwright:1.63.0` as `testImplementation`

#### Manual Verification:

- No `package.json` / `@playwright/test` was added

## Phase 2: US-01 browser test

### Overview

- **Behavior asserted**: Signed-in generate via page JS yields two named blob downloads that vanish on refresh; the generate POST includes the CSRF header.
- **Regression caught**: Fetch without CSRF header; wrong `download` file names; files that stick after reload.
- **Research source**: `research.md` cheapest remaining.
- **Edge / error / boundary**: Dummy DB; mocked generate success; HTTP cookie not Secure.
- **Anti-pattern avoided**: Re-listing `PROFILE_REQUIRED` / `CALORIES_REQUIRED` / `UNAVAILABLE`; `@MockitoBean`.

### Changes Required:

#### 1. E2E test class

**File**: `src/test/java/com/kenez92/plateplan/plan/controller/PlanDownloadE2eTest.java`

**Intent**: The only browser proof for US-01.

**Contract**:
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` with the same dummy `DATABASE_*` as `ApplicationTest`, plus `SESSION_COOKIE_SECURE=false`.
- Constructor-injected `@LocalServerPort`. All fields and locals `final`. Method name `should*`.
- Nested `@TestConfiguration`: `@Primary` `UserDetailsService` for login `alice` / `password1` (encoded with the app `PasswordEncoder`); `@Primary` `PlanService` mock returning `PlanResult.success` with a four-meal `DietPlan` so `PlanPdfWriter` runs.
- One test method: Playwright Chromium headless in try-with-resources. Login via `#login-username` / `#login-password`. Open `/plan`. Intercept POST `/plan/generate` and require a CSRF header. Click `#generate-plan`. Wait until `#plan-downloads` is visible. Assert `download` attributes `dieta-na-jutro.pdf` and `lista-zakupow.pdf`, and `href` starts with `blob:`. Reload. Assert `#plan-downloads` is hidden and both links lack `href`.
- Do not assert JSON `error` codes. Do not call live Ollama.

### Success Criteria:

#### Automated Verification:

- `PlanDownloadE2eTest` contains one `should*` matching the contract
- `.\gradlew.bat test --tests com.kenez92.plateplan.plan.controller.PlanDownloadE2eTest` passes

#### Manual Verification:

- The test does not list `PROFILE_REQUIRED`, `CALORIES_REQUIRED`, or `UNAVAILABLE`

## Phase 3: Cookbook §6.3

### Overview

Replace TBD with the Playwright recipe. Leave CI and tech-stack to Phase 5.

### Changes Required:

#### 1. E2E recipe

**File**: `context/foundation/test-plan.md` (§6.3 and §6.6; §4 e2e row may name Playwright Java now that the test exists, without a full tech-stack rewrite)

**Intent**: How to add another browser test.

**Contract**: Location same package as the controller. Naming `should*`. Playwright Java + `@SpringBootTest(RANDOM_PORT)`. Mock `PlanService`; do not call Ollama. Do not put the JSON matrix here. Reference `PlanDownloadE2eTest`. Run `.\gradlew.bat test --tests <Fqcn>`. §6.6 notes Phase 4 shipped. Do not edit `.github/workflows/ci.yml` or `tech-stack.md`.

### Success Criteria:

#### Automated Verification:

- §6.3 no longer says TBD
- §6.6 records Phase 4
- `ci.yml` and `tech-stack.md` unchanged

#### Manual Verification:

- Reading §6.3 names two download file names and refresh-drops-files, not the JSON matrix

## Testing Strategy

### End to end:

- One Chromium test through login + `/plan` generate.

### Manual Testing Steps:

1. Confirm no error-code strings in the new test.
2. Confirm no Node Playwright files.

## Performance Considerations

First Playwright run downloads Chromium. Keep a single test.

## Migration Notes

None. Phase 5 wires CI browser install and `tech-stack.md`.

## References

- `context/changes/testing-browser-critical-path/research.md`
- https://playwright.dev/java/docs/intro
- `context/foundation/test-plan.md` §3 Phase 4

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles.

### Phase 1: Playwright Java dependency

#### Automated

- [x] 1.1 `build.gradle.kts` lists `com.microsoft.playwright:playwright:1.63.0` as `testImplementation`

#### Manual

- [x] 1.2 No `package.json` / `@playwright/test` was added

### Phase 2: US-01 browser test

#### Automated

- [x] 2.1 `PlanDownloadE2eTest` contains one `should*` matching the contract
- [x] 2.2 `.\gradlew.bat test --tests com.kenez92.plateplan.plan.controller.PlanDownloadE2eTest` passes

#### Manual

- [x] 2.3 The test does not list `PROFILE_REQUIRED`, `CALORIES_REQUIRED`, or `UNAVAILABLE`

### Phase 3: Cookbook §6.3

#### Automated

- [x] 3.1 §6.3 no longer says TBD
- [x] 3.2 §6.6 records Phase 4
- [x] 3.3 `ci.yml` and `tech-stack.md` unchanged

#### Manual

- [x] 3.4 Reading §6.3 names two download file names and refresh-drops-files, not the JSON matrix
