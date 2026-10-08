---
type: observability-audit
date: 2026-10-08 09:19
mode: audit
commit: 53e5dcb
branch: feature/playwright
dirty_tree: false
areas: [plan-generate, register-and-sign-in, profile-and-calories]
area_source: foundation
runtime_proof: not-run
error_tracker: none
previous_report: null
findings: { critical: 4, high: 11, medium: 7, low: 1 }
---

# Observability audit — plan-generate, register-and-sign-in, profile-and-calories (2026-10-08)

## 1. TL;DR

- There is **no error tracker**. Production diagnosis is `fly logs` plus public `/actuator/health`.
- Domain failures are **caught and returned as HTTP 200** (JSON `error`, form flags, or `/?error`). Status-based monitoring stays green while the product is broken.
- Application logs pass **exception class names only**, never the throwable. Tests lock that in. Stacks appear only if something is truly uncaught and Spring logs it.
- Several realistic failures **never log at all**: missing `OLLAMA_API_KEY`, a null model entity, and a database outage on form login (shown as a bad password).
- Liquibase failures are **skipped at start**; Fly has **no HTTP health check** and machines auto-stop. A skipped schema plus a “healthy” process is a likely incident shape.
- Consequence: an Ollama key/outage or a database blip makes generate and login look like user error, with either no log line or a WARN of two FQCNs and no stack.

## 2. Capture model

How a failure travels from code to a human:

- **Runtime:** long-running Spring Boot 4.1.1 JVM in Docker on Fly.io app `plate-plan` (`fly.toml:1-22`). One `shared-cpu-1x` 1 GB Machine, `auto_stop_machines = "stop"`, `min_machines_running = 0`, HTTP 8080. No `[[http_service.checks]]`. PostgreSQL via env; Ollama Cloud on the request thread with a 120 s read timeout (`OllamaConfiguration.java:24,29-30`). No queues, cron, or workers. Thymeleaf SSR plus two vanilla scripts (`plan-download.js`, `profile-products.js`).
- **Tracker:** none. `build.gradle.kts:26-42` has Actuator, not Sentry/Datadog/OTel/Micrometer. The only `@ControllerAdvice` is `CurrentAccountAdvice.java:14-19` (login name on the model). Uncaught MVC exceptions become Boot `/error` (`SecurityConfiguration.java:26,41`), public, with default `include` of stack/message **never** in the HTTP body. Production jar runs without DevTools (`Dockerfile:8-14`).
- **Boundary:** there is no capture boundary. Designed failures never throw: `RegistrationResult`, `ProfileResult`, `ConfirmedCaloriesResult`, `PlanResult` → HTTP 200. `PlanController.java:42-65` returns JSON with `error` on the same 200 as success.
- **Logging:** default Logback (no `logback.xml`) → stdout → `fly logs`. Failure sites log WARN/INFO with two class-name placeholders, **not** the throwable: `DietGenerator.java:104-105`, `PlanService.java:62-63`, `PlanController.java:63-64`, `ProfileService.java:132-134`, `ConfirmedCaloriesService.java:90-92`, `RegistrationService.java:55-59`, `LiquibaseConfiguration.java:51-53`. `LOGGER.error` is unused in `src/main`. Hibernate `SqlExceptionHelper` is OFF (`application.properties:17-18`) so duplicate-login SQL text (which contains the login) never hits stdout. Logs are not wired to alerts in-repo.
- **Scrubbing:** application-level, not a tracker hook. Messages, logins, calories, products, API key, prompt, and PDF bytes are withheld by policy (`PlanService.java:17-18`, `DietGenerator.java:18-19`, `AccountSignInService.java:19`). `PlanFilesDto.toString()` omits PDF bytes (`PlanFilesDto.java:46-51`).
- **Deploy identity:** `version = "0.0.1-SNAPSHOT"` (`build.gradle.kts:8`); `Dockerfile:10` copies that jar. No `spring-boot-build-info`, no git info on `/actuator/info`. Single Fly app; no preview vs prod tag in the process. CI deploys on push to `main` (`.github/workflows/ci.yml:24-41`) with no post-deploy health curl.

## 3. What reaches the tracker

Static-only (no runtime probe this run; there is no tracker to ingest into). Inferred from code and tests:

| Failure shape | Response | Platform logs | Tracker | Verdict |
|---|---|---|---|---|
| Uncaught throw in a controller | 5xx HTML `/error` (stack not in body) | Servlet/Boot ERROR with stack (default) | none | poor — only if someone is tailing `fly logs` |
| Throw in generate after first JSON intent | same `/error` HTML; client treats non-JSON as session loss (`plan-download.js:75-77`) | possible ERROR stack | none | missed for plan UX |
| Domain catch → `PlanResult.unavailable()` | **200** `{"error":"UNAVAILABLE"}` | WARN FQCN pair **or nothing** | none | missed |
| Missing `OLLAMA_API_KEY` | **200** `UNAVAILABLE` | **no line** (`DietGenerator.java:71-72`) | none | missed |
| Null model entity | **200** `UNAVAILABLE` | **no line** (`DietGenerator.java:79-80`) | none | missed |
| Login when DB throws | **302** `/?error`, “Nieprawidłowy login lub hasło.” | no app log | none | missed |
| Profile DB down | **200** page + `loadFailed` | WARN FQCN pair | none | poor |
| Liquibase fail at start | process stays up | WARN two class names, no stack | none | missed |
| 5xx without throw | app never emits 5xx (`@ResponseStatus` unused) | n/a | none | n/a |

## 4. Systemic root causes

1. **No observability product.** Actuator health/info and stdout are the whole pipeline. There is nothing to open at 3am except a log buffer on a Machine that may already have stopped.
2. **Privacy-first logging without a compensating channel.** Class-name-only WARN is a reasonable local rule (SQL messages carry the login). Tests assert it (`DietGeneratorTest.java:72-93`). The cost is undiagnosable infrastructure failures, including ones with no PII (Ollama timeout, PDF IO, skipped Liquibase).
3. **Soft failure as the domain model.** Expected outcomes are result objects and HTTP 200. That keeps users off a 500 page (good) and also keeps Fly, Actuator-on-status, and any future 5xx alert blind.
4. **Silent branches inside those result objects.** Catch-and-log is not applied to every `unavailable()` / login-failure path. Missing key, null entity, login lookup throw, and ignored `UPDATE` row counts produce the same UX as a handled outage **without** the WARN.
5. **Availability at start over readiness.** Liquibase skip (`LiquibaseConfiguration.java:47-54`) plus start-with-unreachable-DB plus Fly auto-stop and no HTTP check means “the process is running” is not “the product can register, save a profile, or generate.”

## 5. Findings by area

### Plan generate

| # | Location | Category | Severity | What happens in production | Fix direction |
|---|---|---|---|---|---|
| PLAN-01 | `DietGenerator.java:71-72` | swallowed | **critical** | Missing/blank `OLLAMA_API_KEY`: user sees generic “Nie udało się wygenerować planu…”; HTTP 200 `UNAVAILABLE`; **no log**. Responder cannot tell misconfig from model outage. | WARN/ERROR with stable code `OLLAMA_KEY_MISSING`; never log the key. |
| PLAN-02 | `DietGenerator.java:79-80` | swallowed | **critical** | Null parsed `DietPlan` after up to 120 s: same UX and JSON as PLAN-01; **no log**. | WARN with code `MODEL_EMPTY_ENTITY`. |
| PLAN-03 | `PlanController.java:55-56`, `PlanFilesDto.java:34-35`, `PlanResult.java:18-19` | flattened-response | **high** | DB down, Ollama throw, and PDF write all return **200** `UNAVAILABLE`. Access logs look successful. PDF write is the only path that logs at the controller (`PlanController.java:62-65`). | Keep user JSON; log/metric an internal reason enum (`DB`, `MODEL`, `PDF`, `KEY`). |
| PLAN-04 | `plan-download.js:51-67,75-77` | flattened-response | **high** | Network/5xx/HTML `/error` → generic unavailable or **redirect to `/`** (looks like sign-out). No client telemetry. | On non-JSON generate responses, show unavailable; do not assign `HOME`. Optional request id in the UI. |
| PLAN-05 | `DietGenerator.java:75-78`, `OllamaConfiguration.java:24` | missing-context | **medium** | User waits up to two minutes with no server phase log. A stuck vs working call is indistinguishable in `fly logs`. | INFO start/end with duration and outcome code (no prompt/products). |

### Register and sign-in

| # | Location | Category | Severity | What happens in production | Fix direction |
|---|---|---|---|---|---|
| AUTH-01 | `AccountUserDetailsService.java:33-37`, `SecurityConfiguration.java:45-49`, `chrome.html:30`; proven by `SecurityConfigurationTest.java:88-93,165` | flattened-response | **critical** | Database throw on login → **302** `/?error` and “Nieprawidłowy login lub hasło.” Same as a wrong password. **No application log.** | Failure handler or `UserDetailsService` wrapper: log `LOGIN_DB_UNAVAILABLE` vs `LOGIN_FAILED` (hashed login at most); keep user copy generic. |
| AUTH-02 | `RegistrationService.java:55-57`, `register.html:22` | flattened-response | **high** | Any `DataIntegrityViolationException` is `LOGIN_TAKEN` (“Login jest zajęty.”). INFO class names only. Schema/constraint bugs look like a taken login. | Map unique-violation SQLState to `LOGIN_TAKEN`; else `UNAVAILABLE` + distinct WARN. |
| AUTH-03 | `RegisterController.java:58-59`, `AccountSignInService.java:14-48` | coverage-gap | **high** | Account row is saved, then `signIn` runs with **nothing logged**. If session/cookie persistence fails, user lands on `/` signed out; support sees a row and no auth event. | Log hashed-id outcome after `signIn`; do not redirect as success if context save fails. |
| AUTH-04 | `SecurityConfiguration.java:39` | flattened-response | **medium** | Missing CSRF on `/login`, `/register`, `/logout` → bare **403**. Easy to confuse with Secure-cookie loss. No app log. | Access-denied handler: redirect + WARN `CSRF_REJECTED` + path. |
| AUTH-05 | `RegisterController.java:44-59`, `SecurityConfiguration.java:45-49` | coverage-gap | **medium** | Successful register/login emit no audit line. Credential stuffing and sign-up rate are invisible except in raw access logs. | INFO outcome enum + hashed account id; never password or raw login. |
| AUTH-06 | `SecurityConfiguration.java:50` | coverage-gap | **low** | Logout has no audit event. | Optional INFO `logout` with principal hash. |

### Profile and calories

| # | Location | Category | Severity | What happens in production | Fix direction |
|---|---|---|---|---|---|
| PROF-01 | `ProfileService.java:97-101`, `UserProfileRepository.java:21-44` | swallowed | **high** | `replaceConfirmedCalories` / `replaceBodyAndProducts` return row counts that are **ignored**. Zero rows still flash “Profil zapisany”. No log. Plan generate then fails for “no” calories/profile. | Treat `== 0` as failure; log `outcome=noRowUpdated` + hashed login. |
| PROF-02 | `ProfileController.java:58-61`, `profile.html:13,17` | flattened-response | **high** | Calories read failure uses the same `loadFailed` as a full profile outage and **hides the form**. User and responder cannot tell which read failed. | Separate flag/message; keep body form if body load succeeded. |
| PROF-03 | `ConfirmedCaloriesService.java:77-79`, `ProfileController.java:112-114` | identity-lost | **high** | `replaceConfirmedCalories == 0` becomes `noProfile()` with **no log** — same UI as “never saved a profile.” | Log `noRowUpdated`; do not reuse the empty-profile story without a line. |
| PROF-04 | `ProfileController.java:134-140` | swallowed | **high** | On validation/save failure, calorie load failure is replaced with `DailyCaloriesForm.empty()` and no extra flag — looks like “calories not set yet.” | Surface `calorieLoadFailed` when `failed()`. |
| PROF-05 | `ProfileService.java:91-104` (two `@Transactional` repo methods, no service transaction) | missing-context | **medium** | If the second update throws, user sees save failed and a generic WARN; logs do not say calories-vs-body. Partial commit is possible. | One transaction or one UPDATE; log which statement failed. (Correctness plus a monitoring gap.) |

### Platform / plumbing

| # | Location | Category | Severity | What happens in production | Fix direction |
|---|---|---|---|---|---|
| PLUMB-01 | `build.gradle.kts:26-42` | coverage-gap | **critical** | No issue stream. On-call has stdout on a Machine that auto-stops (`fly.toml:10-12`). | Keep using logs first (see fix order). An SDK needs a `tech-stack.md` change before it can be added. |
| PLUMB-02 | All `LOGGER.warn/info` sites above; `DietGeneratorTest.java:72-93` | identity-lost | **high** | When a line exists, it is two FQCNs and **no stack**. Same `Profile storage failed` text for load, body save, calorie save, and recalc. | Pass the throwable as the last SLF4J argument to an ops sink; keep messages free of login/SQL text; add `operation=` tags. |
| PLUMB-03 | `LiquibaseConfiguration.java:47-54` | swallowed | **high** | Failed migration: process starts; WARN two class names; register/profile/plan then fail as generic user errors. Health may still be probed only if someone hits `/actuator/health` by hand. | Do not treat skip as healthy: ERROR + stack to logs; health/readiness DOWN until migrate succeeds. |
| PLUMB-04 | `fly.toml:7-12`, `.github/workflows/ci.yml:41` | config | **high** | No Fly HTTP check, no CI smoke on `/actuator/health`. A bad deploy or DB-down process can sit until a human opens the site. Auto-stop drops log continuity. | `[[http_service.checks]]` on `/actuator/health`; post-deploy curl in CI. |
| PLUMB-05 | `build.gradle.kts:8`, `Dockerfile:10` | missing-context | **medium** | Every image is `0.0.1-SNAPSHOT`. Logs cannot be tied to git sha. | `spring-boot-build-info` or Fly `RELEASE` env from `GITHUB_SHA`. |
| PLUMB-06 | `application.properties:17-18` | config | **medium** | Hibernate SQL helper OFF (justified: login in SQL text). Combined with class-only app logs, JDBC state never appears. | Keep helper OFF; log SQLState / vendor code from `DataAccessException` without the message. |
| PLUMB-07 | No MDC filter in `src/main` | missing-context | **medium** | Cannot join one click to DietGenerator WARN vs PDF WARN vs nothing. Login must not be the join key in plaintext. | `X-Request-Id` / MDC; optional hashed account id. |

## 6. Recommended fix order

Blindness removed per unit of effort. Constraint: do not log login, password, SQL text, age/height/weight/sex/goal, product lists, API key, prompt, or PDF bytes. `tech-stack.md` does not list an error-tracker library; do not add one in the same change as logging hygiene.

1. **Outcome codes + throwable on infrastructure paths** (PLAN-01, PLAN-02, PLAN-03, AUTH-01, AUTH-02, PLUMB-02, PLUMB-06). One small pattern: `operation`, `reason`, exception as last SLF4J arg (or a sanitized stack). Log the currently silent DietGenerator branches. Distinguish login DB failure from bad password **in logs only**.
2. **Readiness that matches reality** (PLUMB-03, PLUMB-04). Fly HTTP check on `/actuator/health`; skipped Liquibase must not look like a good boot.
3. **HTTP-boundary outcome log** for generate and profile flags (PLAN-03, PROF-02, PROF-04) so 200-with-error is countable without scraping bodies.
4. **Honor repository row counts** (PROF-01, PROF-03) — false success is worse than a noisy WARN.
5. **Generate client: do not redirect on non-JSON** (PLAN-04).
6. **Request id** (PLUMB-07, PLAN-05).
7. **Only then** consider an error tracker via `/10x-new` + a `tech-stack.md` amendment. Without (1)–(2), a tracker would still miss the 200/silent paths.

## 7. Changes since last audit

First run. No previous report.

## 8. Method and limits

- **Agents:** four read-only explore subagents (plan-generate, register-and-sign-in, profile-and-calories, plumbing). Findings were merged; duplicate “no tracker” / “FQCN WARN” items collapsed into PLUMB-01/PLUMB-02.
- **Spot-checked by the parent (code, not runtime):**
  1. Blank API key returns `unavailable()` with no log — `DietGenerator.java:71-72`; `DietGeneratorTest.shouldReturnUnavailableWhenTheApiKeyIsMissing` does not assert a log line.
  2. Class-name-only logging is **required by test** — `DietGeneratorTest.java:72-93`.
  3. Login DB throw → `/?error` — `SecurityConfigurationTest.java:88-93,165`.
  4. Liquibase swallow — `LiquibaseConfiguration.java:47-54`.
  5. Ignored update counts — `ProfileService.java:97-101` vs `UserProfileRepository.java:21-44`.
  6. `LOGGER.error` unused in `src/main`; seven WARN/INFO sites listed in §2.
- **Dropped as not observability (or too speculative):** missing `@WebMvcTest` for calorie flags; JS 50-product silent add (UX); `PlanFilesDto` compact-constructor throw (only if a caller builds success wrong).
- **Runtime proof:** not run (no `--runtime`, no ingest SDK). Isolated worktree was not created. Re-run with `--runtime` would still have **no tracker**; probes would record HTTP + stdout only. Local boot needs `DATABASE_URL` / user / password (dummy unreachable URL is enough to start) and `SESSION_COOKIE_SECURE=false` for http.
- **Not verifiable here:** Fly log retention, whether anyone has a dashboard on `fly logs`, production `/actuator/health` JSON on the live app, and whether servlet ERROR stacks actually appear in the Fly buffer after auto-stop.
- **Repo-wide sweep (`src/main`):** ~15 Java `catch` + 1 JS `.catch`; 0 empty catches; 0 `LOGGER.error`; 0 `console.*`; throw-without-cause: `PlanFilesDto.java:26`, `PlanPdfWriter` font-missing path, `plan-download.js:57`.
