# Test Plan

> Phased test rollout for this project. Strategy is frozen at the top
> (§1–§5); cookbook patterns at the bottom (§6) fill in as phases ship.
> Read before writing any new test.
>
> Refresh: re-run `/10x-test-plan --refresh` when stale (see §8).
>
> Last updated: 2026-10-07

## 1. Strategy

Tests follow three non-negotiable principles for this project:

1. **Cost × signal.** The cheapest test that gives a real signal for the risk wins. Do not promote to e2e because e2e "feels safer." Do not put a vision model on top of a deterministic visual diff that already catches the regression. The JSON contract and error matrix stay on `@WebMvcTest`. Playwright is only for failures that exist solely in the browser: `fetch` + CSRF + two named PDFs.
2. **User concerns are first-class evidence.** Risks anchored in "the team is worried about X, and the failure would surface somewhere in an area" carry the same weight as PRD lines or hot-spot data. The Phase 2 interview stopped after three `skip`s — this guide rests on documents and hot-spots, not interview concerns.
3. **Risks are scenarios, not code locations.** This plan documents *what could fail* and *why we believe it's likely* — drawn from documents, interview, and codebase *signal* (churn, structure, test base). It does NOT claim to know which line owns the failure. That knowledge is produced by `/10x-research` during each rollout phase. If the plan and research disagree about where the failure lives, research is the ground truth.

Hot-spot scope used for likelihood weighting: `src/main/java`, `src/main/resources`.

## 2. Risk Map

The top failure scenarios this project must protect against, ordered by risk = impact × likelihood. Risks are failure scenarios in user / business terms, not test names. The Source column cites the *evidence that surfaced this risk* — never a specific file as "where the failure lives."

| # | Risk (failure scenario) | Impact | Likelihood | Source (evidence — not anchor) |
|---|-------------------------|--------|------------|--------------------------------|
| 1 | After calories are confirmed the user does not get two PDFs, or gets a 500 instead of JSON with only `error` | High | High | PRD Success Primary + FR-006; archive `generate-diet-with-ollama`; hot-spot `src/main/java/.../plan` (22 commits/30d) |
| 2 | A signed-in account reads or generates from another login's data | High | High | PRD Access Control + Guardrails; archive `save-user-profile`; AGENTS "own data only"; hot-spot `profile` (34) + `account` (28) |
| 3 | The calorie number is wrong, preferences change it, or edit/recalculate corrupts other columns | High | Medium | PRD FR-004/FR-005 + Business Logic; archive `calorie-formula`; hot-spot `profile` (34) |
| 4 | A refused profile save still writes a row; a name on both lists is accepted | Medium | Medium | AGENTS Profile; archive `save-user-profile`; hot-spot `profile` (34) |
| 5 | The plan or shopping list is stored, or a log/response leaks PII, the prompt, or the Ollama key | High | Medium | PRD Guardrails + FR-006; AGENTS "do not persist / do not log"; archive `generate-diet-with-ollama` |
| 6 | A POST without CSRF succeeds (register/login/logout/generate), or a legitimate POST loses the session and gets 403 | High | Medium | archive `register-and-sign-in` (Secure cookie); archive `generate-diet-with-ollama` (CSRF on fetch); archive `spring-security-sign-in` |

An Ollama Cloud outage (High × Low) belongs to observability — the contract is `UNAVAILABLE`, not a vendor test. Login throttling is deferred in S-01 and is not a defect in the current product.

### Risk Response Guidance

| Risk | What would prove protection | Must challenge | Context `/10x-research` must ground | Likely cheapest layer | Anti-pattern to avoid |
|------|-----------------------------|----------------|--------------------------------------|-----------------------|-----------------------|
| #1 | Signed-in generate with a profile and calories returns two PDF fields and no `error`; missing preconditions return only `PROFILE_REQUIRED` / `CALORIES_REQUIRED` / `UNAVAILABLE` at HTTP 200; anonymous and CSRF-less POST do not return PDFs. Two named browser downloads that vanish on refresh wait for the Playwright phase | "JSON with two fields means download and no-persist work"; "e2e is safer, so move the JSON matrix to Playwright" | Entry `POST /plan/generate`, CSRF, Principal as the key, error translation, download-button behavior in the browser | integration (`@WebMvcTest`); e2e only for downloads | Playwright on the JSON contract; an assertion copied from the DTO without the error matrix |
| #2 | The request cannot choose whose row to read; another login never appears in profile or generate | "Signed in means they see their own data" | Whether login comes from Principal or from body/query | integration | Happy-path-only `@WithMockUser` |
| #3 | First body save stores the formula; a later body save does not take calories from the client; `POST .../calories` writes only that column (800–6000); recalculate overwrites from the stored body; preferences do not change the number | "A unit test of the formula equals the whole calorie write path" | Three write paths; gold numbers from the PRD, not from the code | unit + integration | Oracle copied from the calculator implementation |
| #4 | A refusal writes nothing; a name on both lists fails on the excluded field; an illegal name fails on the server | "DTO validation is enough — the client is trusted" | Server validators vs annotations; persist-on-refuse | unit + integration | Annotation-only DTO tests |
| #5 | Generate does not persist files; logs and errors do not carry the key, prompt, lists, calories, or PDF bytes; the model does not receive age/height/weight/sex/activity/login | "No plan table means nothing is stored" | Persist boundary, logging, prompt contract from AGENTS/PRD | integration (+ log capture) | Snapshot of the full prompt; logging the prompt in the test |
| #6 | Form POST without CSRF → 403 on register/login/logout; with a token it succeeds. CSRF on the generate `fetch` header waits for the Playwright phase | "CSRF on one controller means CSRF everywhere" | The POST list, Secure-cookie behavior, where JS reads the token | integration (forms); e2e (`fetch`) | Treating local `SESSION_COOKIE_SECURE=true` as a product bug; e2e for form CSRF |

## 3. Phased Rollout

Each row is a discrete rollout phase that will open its own change folder via `/10x-new`. Status moves left-to-right through the values below; the orchestrator updates Status as artifacts appear on disk.

| # | Phase name | Goal (one line) | Risks covered | Test types | Status | Change folder |
|---|------------|-----------------|---------------|------------|--------|---------------|
| 1 | Critical-path coverage | Prove the success path and data ownership do not fail silently | #1, #2 | unit + integration | researched | context/changes/testing-critical-path-coverage/ |
| 2 | Integration around hot-spots | Prove calorie invariants and refused-save behavior under the highest churn | #3, #4 | unit + integration | not started | — |
| 3 | Persistence, leak and session contract | Prove PDFs are not stored, PII does not leak, and form POSTs require CSRF | #5, #6 | integration | not started | — |
| 4 | Browser critical path | Prove US-01 in a browser: generate, CSRF `fetch`, two named PDFs, refresh drops the files | #1, #6 | e2e (Playwright) | not started | — |
| 5 | Quality-gates wiring | Wire the one Playwright test in CI and fill the cookbook; add Playwright to `tech-stack.md` | cross-cutting | gates | not started | — |

Status vocabulary (parser literals): `not started` → `change opened` → `researched` → `planned` → `implementing` → `complete`.

## 4. Stack

The classic test base for this project. AI-native tools carry a `checked:` date so future readers can see which lines need re-verification.

| Layer | Tool | Version | Notes |
|-------|------|---------|-------|
| unit + integration | JUnit Platform + `spring-boot-starter-webmvc-test` / `security-test` | Spring Boot 4.1.1 | CI suite: `./gradlew test`. 30 `*Test.java` files (profile 11, account 8, plan 5, config 3, home 2). `@SpringBootTest` only for e2e / `ApplicationTest` |
| API mocking | Mockito (`@MockitoBean` in slices) | from the test starter | Mock controller collaborators only; import the Security chain when the assertion needs it |
| e2e | Playwright Java | planned — see §3 Phase 4 | Not yet in `build.gradle.kts`. US-01 in the browser only. Official API: Playwright + JUnit, `@SpringBootTest(RANDOM_PORT)` |
| accessibility | none yet | — | No phase; do not add axe without a risk |
| (optional) AI-native | cursor-ide-browser — checked: 2026-10-07 | n/a | Agent verification, not the suite. Do not use vision instead of a PDF-download assertion |

Test base: **meaningful** — JUnit Platform is configured, 30 tests spread across packages.

**Stack grounding tools (current session):**
- Docs: none (Context7 / Spring docs MCP not available in this session) — grounded via Web Search to docs.spring.io 4.1 (`@WebMvcTest`) and playwright.dev/java; checked: 2026-10-07
- Search: cursor Web Search (Exa.ai not available in this session) — Spring Boot 4.1 testing + Playwright Java test runners; checked: 2026-10-07
- Runtime/browser: cursor-ide-browser — can confirm the `/plan` screen for an agent, not a CI layer; checked: 2026-10-07
- Provider/platform: cursor-origin (Origin repos, not GitHub Actions workflows) — not used for gates; checked: 2026-10-07

## 5. Quality Gates

The full set of gates that must pass before a change reaches production.

| Gate | Where | Required? | Catches |
|------|-------|-----------|---------|
| unit + integration (`./gradlew test`) | local + CI | required now (already in `.github/workflows/ci.yml`) | logic regressions, HTTP slices, form CSRF, the `error` matrix |
| e2e on the US-01 path (Playwright) | CI on PR | required after §3 Phase 5 | `fetch` without CSRF, missing download buttons, wrong file names, files that "stick" after refresh |

No lint, coverage, or agent-hook gates — AGENTS.md does not require them, and this guide does not invent them.

## 6. Cookbook Patterns

How to add new tests in this project. Each sub-section is filled in once the relevant rollout phase ships; before that, the sub-section reads "TBD — see §3 Phase N."

### 6.1 Adding a unit test

TBD — see §3 Phase 2 for calorie-formula / preference-isolation pattern.

### 6.2 Adding an integration test

Cite the existing generate JSON matrix. Prove the request cannot choose the login. Do not treat `@WithMockUser` as ownership.

- **Location**: Same package as the controller under `src/test/java`.
- **Naming**: `*Test` class; every method starts with `should`.
- **Mocking policy**: `@WebMvcTest` plus `@Import(SecurityConfiguration.class)`. Stub application services. Do not use `@SpringBootTest`. Do not call live Ollama.
- **Reference test**: Generate JSON matrix — `src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java` (cite it; do not re-list the error codes). Own-data — `shouldGenerateThePlanOfTheSignedInLoginOnly` in that class, and `shouldLoadAndSaveTheProfileOfTheSignedInLoginOnly` in `src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java`.
- **Run locally**: `.\gradlew.bat test --tests <Fqcn>`

### 6.3 Adding an e2e test

TBD — see §3 Phase 4 for two named PDF downloads after generate (Playwright, not JSON matrix).

### 6.4 Adding a test for a new signed-in POST

Import the security chain. Prove Principal-only key, CSRF, and HTTP 200 on expected failure. Do not invent a 500 oracle. Do not move the JSON matrix to Playwright.

- **Location**: Same package as the controller under `src/test/java` (see §6.2).
- **Naming**: `should*` methods on the controller `*Test`.
- **Pattern**: Send CSRF; without a token expect 403 and the service never called. Send `login=bob` and verify the service received `Principal.getName()`. Treat expected product failures as HTTP 200 result values, not 500.
- **Reference test**: CSRF-less generate `shouldRejectAGeneratePostWithoutACsrfToken` and the existing generate JSON matrix in `src/test/java/com/kenez92/plateplan/plan/controller/PlanControllerTest.java`. Calorie-write own-data `shouldUpdateAndRecalculateTheCaloriesOfTheSignedInLoginOnly` in `src/test/java/com/kenez92/plateplan/profile/controller/ProfileControllerTest.java`. Anonymous generate-with-CSRF stays in `src/test/java/com/kenez92/plateplan/config/SecurityConfigurationTest.java` `shouldRedirectASignedOutGeneratePostToTheLoginWindow` (302 to `/`).
- **Run locally**: `.\gradlew.bat test --tests <Fqcn>`

### 6.5 Adding a test for a new profile validation rule

TBD — see §3 Phase 2 for refused-save-writes-nothing and cross-list exclusion pattern.

### 6.6 Per-rollout-phase notes

Phase 1 shipped generate and calorie-write `login=bob` mirrors (`shouldGenerateThePlanOfTheSignedInLoginOnly`, `shouldUpdateAndRecalculateTheCaloriesOfTheSignedInLoginOnly`). The existing generate JSON matrix in `PlanControllerTest` was left in place. No 500 test, no Playwright, no `shoppingListPdf` tidy-up.

## 7. What We Deliberately Don't Test

Interview Q5 was skipped. These exclusions come from the documents, not from the conversation.

- **Multiple people / roles** — out of MVP (PRD Non-Goals). Re-evaluate when the roadmap opens that slice.
- **Email instead of PDF** — open PRD question; it does not replace the two download buttons. Do not write mail tests until the decision lands in the PRD.
- **Login throttle / lockout** — deferred in S-01; one person, no rate-limit library on the stack.
- **The generate JSON matrix in Playwright** — the cheaper layer is `@WebMvcTest` (§1, risk #1).
- **Accessibility / CSS snapshots of templates** — no product risk; `site.css` is hot, but a failure is cosmetic (Low impact).

## 8. Freshness Ledger

- Strategy (§1–§5) last reviewed: 2026-10-07
- Stack versions last verified: 2026-10-07
- AI-native tool references last verified: 2026-10-07

Refresh (`/10x-test-plan --refresh`) when:

- a new top-3 risk surfaces from the roadmap or archive,
- a recommended tool's `checked:` date is older than three months,
- the project's tech stack changes (new framework, new test runner),
- §7 negative-space no longer matches what the team believes.
