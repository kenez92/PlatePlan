---
project: PlatePlan
version: 1
status: draft
created: 2026-09-28
updated: 2026-10-07
prd_version: 2
main_goal: low-complexity
top_blocker: decisions
milestone_id: first-downloadable-plan
milestone_seq: 1
milestone_status: done
---

# Roadmap: PlatePlan

> Derived from `context/foundation/prd.md` (v2) + auto-researched codebase baseline.
> Edit-in-place; archive when superseded.
> Slices below are listed in dependency order. The "At a glance" table is the index.

## Milestone

**M-1: First downloadable next-day plan** — Status: done

- **Intent:** One person can create an account, save a profile (body data and food preferences), confirm a calorie number from a formula, receive a next-day diet from the model, and download that diet and a shopping list as two PDF files that are not stored, then generate another pair on a later visit without typing the profile again.
- **Source materials:** `context/foundation/prd.md` (v2)
- **Done when:** every F-NN and S-NN below is `done`
- **Scope anchors:** FR-001, FR-002, FR-003, FR-004, FR-005, FR-006, US-01, US-02

## Vision recap

Every day you decide breakfast, lunch, and dinner while trying to lose weight, maintain it, or gain it. That costs time and often ends in a poor choice. Diets from the internet impose dishes. PlatePlan lets you state what you like and what you do not like, calculates a daily calorie number, and turns the confirmed number into a next-day plan and a shopping list as two PDF files.

## North star

**S-05: user can download the next-day diet plan and the shopping list** — delivered in `generate-diet-with-ollama` together with S-04 (two PDF fields on `/plan`, no ZIP). A separate download change is not needed.

> A north star is the smallest end-to-end slice whose delivery would prove the core product hypothesis — the claim that naming foods you like and do not like, then confirming a calorie number, is enough to download a next-day plan and a shopping list. It is placed as early as its prerequisites allow, because the other slices only matter if this works.

## At a glance

| ID   | Change ID                  | Outcome (user can …)                                       | Prerequisites | PRD refs                          | Status   |
| ---- | -------------------------- | ---------------------------------------------------------- | ------------- | --------------------------------- | -------- |
| F-01 | database-configured        | (foundation) a database is configured                      | —             | Access Control                    | done |
| F-02 | spring-security-sign-in    | (foundation) Spring Security can require a signed-in account | F-01        | Access Control, FR-001, FR-002    | done |
| F-03 | calorie-formula            | (foundation) calories are BMR × activity then −250 / 0 / +250 | —             | FR-004                            | done |
| S-01 | register-and-sign-in       | user can register and log in                               | F-02          | US-01, FR-001, FR-002             | done |
| S-02 | save-user-profile          | user can save the whole profile: body data, goal, activity, products | S-01 | US-01, US-02, FR-003              | done |
| S-03 | calorie-formula            | user can confirm a daily calorie number from the formula (delivered in F-03; no leftover confirm UX) | S-02, F-03    | US-01, FR-004, FR-005             | done |
| S-04 | generate-diet-with-ollama  | user can receive a next-day diet from Ollama Cloud on `/plan` as two PDF JSON fields | S-02, S-03    | US-01, FR-006                     | done |
| S-05 | generate-diet-with-ollama  | user can download the next-day plan and shopping list (delivered in S-04; no separate ZIP change) | S-04          | US-01, FR-006                     | done |
| S-06 | generate-diet-with-ollama  | user can generate another plan without re-entering data (delivered by stored profile + S-04; no separate return-visit change) | S-05          | US-02                             | done |

## Streams

Navigation aid — groups items that share a Prerequisites chain. Canonical ordering still lives in the dependency graph below; this table is the proposed reading order across parallel tracks.

| Stream | Theme                         | Chain                                                    | Note                                                                                          |
| ------ | ----------------------------- | -------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| A      | Account, preferences, diet    | `F-01` → `F-02` → `S-01` → `S-02` → `S-04` → `S-05` → `S-06` | Database, then sign-in, then the profile. Generate on `/plan` (S-04) plus the stored row is also the return visit (S-06). |
| B      | Calorie formula               | `F-03` → `S-03`                                          | Confirm, edit, and store landed in F-03; S-03 has no leftover work. |

## Baseline

What's already in place in the codebase as of 2026-09-28 (auto-researched + user-confirmed).
Foundations below assume these are present and do not re-scaffold them.

- **Frontend:** present — server-rendered home and register screens
- **Backend / API:** present — application entry and read routes for home and register; no API description document
- **Data:** absent — no database driver, schema, or migrations
- **Auth:** absent — no sign-in integration, session issuance, or route protection
- **Deploy / infra:** present — container image, host config, and a workflow that tests then deploys
- **Observability:** partial — management endpoints are exposed, including heap dump and shutdown, with no error tracker

## Foundations

### F-01: Database

- **Outcome:** (foundation) the application has a configured database. Product tables are not created here.
- **Change ID:** database-configured
- **PRD refs:** Access Control
- **Unlocks:** F-02, S-02, S-03
- **Prerequisites:** —
- **Parallel with:** F-03
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Sequenced first because sign-in, food preferences, and the saved calorie number all have to survive a later visit. F-02 adds the sign-in tables. S-02 adds the profile table with the body fields and the product lists. F-03 stores the confirmed number in it.
- **Status:** done

### F-02: Spring Security

- **Outcome:** (foundation) Spring Security can require a signed-in account before that account's data is shown, and the sign-in tables exist for one account with no roles. Public heap dump and shutdown cannot expose account data.
- **Change ID:** spring-security-sign-in
- **PRD refs:** Access Control, FR-001, FR-002
- **Unlocks:** S-01
- **Prerequisites:** F-01
- **Parallel with:** F-03
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Depends on the database because the sign-in tables live there. S-01 still builds registration, the login window, and automatic sign-in on top of this. This does not finish those screens.
- **Status:** done

### F-03: Calorie formula

- **Outcome:** (foundation) a daily calorie number on the profile is BMR times one activity level, then −250 (`LOSE_WEIGHT`), 0 (`MAINTAIN`), or +250 (`GAIN`), rounded half-up to a whole kilocalorie. Weight is in kilograms, height in centimetres, age in years. Sex selects the BMR line. Male: BMR = (10 × weight) + (6.25 × height) − (5 × age) + 5. Female: BMR = (10 × weight) + (6.25 × height) − (5 × age) − 161. Activity is `SEDENTARY` ×1.2 (most of the day sitting), `LIGHT` ×1.375 (walking or light effort on most days), `MODERATE` ×1.55 (exercise several days a week), or `HIGH` ×1.725 (hard training or physical work on most days). `/profile` shows an editable Cel kaloryczny. The first body save stores the formula as `confirmed_calories`. The client edits that number on `POST /profile/calories` (only the calorie amount). Recalculate is `POST /profile/recalculate`.
- **Change ID:** calorie-formula
- **PRD refs:** FR-004
- **Unlocks:** S-03
- **Prerequisites:** —
- **Parallel with:** F-01, S-02
- **Blockers:** —
- **Unknowns:** —
- **Risk:** This is the settled BMR, the four activity levels, and the ±250 goal step in `profile.service.CalorieService`. `/profile` shows Cel kaloryczny, fills it from the formula when empty, and stores `confirmed_calories`. Food preferences do not change the number. Confirm/edit/store (S-03 / FR-005) landed here; do not start `confirm-daily-calories`.
- **Status:** done

## Slices

### S-01: Register and log in

- **Outcome:** user can create an account from the login window and land signed in, without a second login step, and can log in again from that window on a later visit.
- **Change ID:** register-and-sign-in
- **PRD refs:** US-01, FR-001, FR-002
- **Prerequisites:** F-02
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** Settled in this slice: the login is capped at 50 characters and the password at 72 bytes (done); an existing login is checked before insert and the exception message is not logged (done). Deferred: login throttling or lockout. No rate-limit library is in `tech-stack.md`, the MVP serves one person, and BCrypt cost already slows guessing. `/actuator/health` is public and probes the database on every call.
- **Risk:** The login and register screens are already a shell. This slice makes account creation, automatic sign-in, and a later login real on the Spring Security boundary from F-02. Only that account can see its own data.
- **Status:** done

### S-02: Save the user profile

- **Outcome:** user can enter age, height, weight, sex, goal, activity level, preferred products, and excluded products on one screen, in one form and one request, and the profile is stored in the table `user_profile` on the account and shown again on a later visit.
- **Change ID:** save-user-profile
- **PRD refs:** US-01, US-02, FR-003
- **Prerequisites:** S-01
- **Parallel with:** F-03
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Next after sign-in. All of FR-003 lands in one slice because it is one requirement and one request. The products are included or excluded in the diet and do not change the calorie number. The confirmed number landed in F-03 (`confirmed_calories`).
- **Status:** done

### S-03: Confirm daily calories

- **Outcome:** user can see the number from the F-03 formula, computed from the profile saved in S-02, after the goal adjustment, then accept or edit it. The confirmed number stays on the account in `user_profile`. Preferred and excluded products do not change the number.
- **Change ID:** calorie-formula
- **PRD refs:** US-01, FR-004, FR-005
- **Prerequisites:** S-02, F-03
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Delivered in `calorie-formula` (F-03): the first body save stores the formula as `confirmed_calories`, `POST /profile/calories` edits it, `POST /profile/recalculate` overwrites it from the stored body. No leftover confirm UX. Do not start `confirm-daily-calories`.
- **Status:** done

### S-04: Generate the diet with the model

- **Outcome:** user can open `/plan`, call Ollama Cloud, and receive a next-day diet plus shopping list as two PDF fields (`dietPdf`, `shoppingListPdf`) on `POST /plan/generate`. The text and files are not stored.
- **Change ID:** generate-diet-with-ollama
- **PRD refs:** US-01, FR-006
- **Prerequisites:** S-02, S-03
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Spring AI talks to Ollama Cloud (`https://ollama.com`, `OLLAMA_API_KEY`). Nothing is placed on the 1 GB Fly Machine. S-05's two PDF downloads are produced in this same change; archive S-04 and S-05 together.
- **Status:** done

### S-05: Download the next-day plan

- **Outcome:** user can download the diet and the shopping list as two PDF files that are not stored.
- **Change ID:** generate-diet-with-ollama
- **PRD refs:** US-01, FR-006
- **Prerequisites:** S-04
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:**
  - Does the result stay two PDF files, or become an email with the full content? — Owner: user. Block: no.
- **Risk:** Delivered in `generate-diet-with-ollama`; a separate ZIP or `download-next-day-plan` change is not needed. The email question stays open and does not replace the two download buttons. Archive with S-04.
- **Status:** done

### S-06: Return and generate again

- **Outcome:** user can return later and generate another next-day plan and shopping list without entering age, height, weight, sex, goal, activity, or preferences again.
- **Change ID:** generate-diet-with-ollama
- **PRD refs:** US-02
- **Prerequisites:** S-05
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Delivered by the stored profile (`save-user-profile`, `calorie-formula`) plus generate on `/plan` (`generate-diet-with-ollama`). `PlanService` reads the account row; PDFs are not stored. A separate `return-visit-plan` change is not needed.
- **Status:** done

## Backlog Handoff

| Roadmap ID | Change ID                 | Suggested issue title                                         | Ready for `/10x-plan` | Notes                                                        |
| ---------- | ------------------------- | ------------------------------------------------------------- | --------------------- | ------------------------------------------------------------ |
| F-01       | database-configured       | Configure the database                                        | yes                   | Run `/10x-plan database-configured`                          |
| F-02       | spring-security-sign-in   | Require sign-in with Spring Security and sign-in tables       | no                    | After F-01. Parallel with F-03                               |
| F-03       | calorie-formula           | Set BMR times the four activity levels                        | yes                   | Run `/10x-plan calorie-formula`. Parallel with F-01          |
| S-01       | register-and-sign-in      | Register and log in                                           | no                    | After F-02                                                   |
| S-02       | save-user-profile         | Save the whole profile on one screen and one table            | no                    | After S-01. Parallel with F-03                               |
| S-03       | calorie-formula           | Confirm or edit the daily calorie number on the profile   | no                    | Delivered in F-03; do not start `confirm-daily-calories` |
| S-04       | generate-diet-with-ollama | Generate the full diet with Spring AI and Ollama Cloud on `/plan` | no                    | After S-02 and S-03; two PDF JSON fields live here           |
| S-05       | generate-diet-with-ollama | Download the two PDFs (delivered in S-04; no ZIP change)      | no                    | Do not start `download-next-day-plan`                        |
| S-06       | generate-diet-with-ollama | Generate another plan on a return visit                       | no                    | Delivered by stored profile + S-04; do not start `return-visit-plan` |

## Open Roadmap Questions

1. **By how much does lose weight lower the result, and by how much does gain raise it?** — Settled: −250 kcal / +250 kcal. Owner: user. Block: no.
2. **Does the result stay two PDF files, or become an email with the full content?** — Owner: user. Block: does not gate S-05. Two PDF download buttons are already in `generate-diet-with-ollama`; email stays open and does not replace them.
3. **How many weeks is the MVP?** — Owner: user. Block: does not gate a slice. This roadmap does not use a week count.
4. **Should this milestone publish a Swagger description of the API?** — Owner: user. Block: does not gate a slice. Raised while framing the roadmap. The PRD does not mention it, so it is not a slice until it has a source anchor.

## Parked

- **Support for multiple people** — Why parked: PRD Non-Goals. This version is one account.
- **Storing diet plans and shopping lists** — Why parked: PRD Non-Goals. Both files are generated and downloaded, not kept.
- **Advanced user roles** — Why parked: PRD Non-Goals. Access stays flat, with no roles. F-02 creates sign-in tables for one account.

## Milestone History

- **M-1: First downloadable next-day plan** — closed 2026-10-07. S-03 delivered in `calorie-formula` (F-03). S-06 delivered by stored profile (S-02, F-03) plus generate on `/plan` (S-04); no separate `confirm-daily-calories` or `return-visit-plan` change.

## Done

- **F-01: (foundation) the application has a configured database. Product tables are not created here.** — Archived 2026-10-01 → `context/archive/2026-09-29-database-configured/`. Lesson: —.
- **F-02: (foundation) Spring Security can require a signed-in account before that account's data is shown, and the sign-in tables exist for one account with no roles. Public heap dump and shutdown cannot expose account data.** — Archived 2026-10-02 → `context/archive/2026-10-01-spring-security-sign-in/`. Lesson: —.
- **S-01: user can create an account from the login window and land signed in, without a second login step, and can log in again from that window on a later visit.** — Archived 2026-10-02 → `context/archive/2026-10-02-register-and-sign-in/`. Lesson: —.
- **S-02: user can enter age, height, weight, sex, goal, activity level, preferred products, and excluded products on one screen, in one form and one request, and the profile is stored in the table `user_profile` on the account and shown again on a later visit.** — Archived 2026-10-05 → `context/archive/2026-10-02-save-user-profile/`. Lesson: —.
- **F-03: (foundation) a daily calorie number on the profile is BMR times one activity level, then −250 (`LOSE_WEIGHT`), 0 (`MAINTAIN`), or +250 (`GAIN`), rounded half-up to a whole kilocalorie. Weight is in kilograms, height in centimetres, age in years. Sex selects the BMR line. Male: BMR = (10 × weight) + (6.25 × height) − (5 × age) + 5. Female: BMR = (10 × weight) + (6.25 × height) − (5 × age) − 161. Activity is `SEDENTARY` ×1.2 (most of the day sitting), `LIGHT` ×1.375 (walking or light effort on most days), `MODERATE` ×1.55 (exercise several days a week), or `HIGH` ×1.725 (hard training or physical work on most days). `/profile` shows an editable Cel kaloryczny. The first body save stores the formula as `confirmed_calories`. The client edits that number on `POST /profile/calories` (only the calorie amount). Recalculate is `POST /profile/recalculate`.** — Archived 2026-10-05 → `context/archive/2026-10-05-calorie-formula/`. Lesson: —.
- **S-04: user can open `/plan`, call Ollama Cloud, and receive a next-day diet plus shopping list as two PDF fields (`dietPdf`, `shoppingListPdf`) on `POST /plan/generate`. The text and files are not stored.** — Archived 2026-10-06 → `context/archive/2026-10-05-generate-diet-with-ollama/`. Lesson: —.
- **S-05: user can download the diet and the shopping list as two PDF files that are not stored.** — Archived 2026-10-06 → `context/archive/2026-10-05-generate-diet-with-ollama/`. Lesson: —.
- **S-03: user can see the number from the F-03 formula, computed from the profile saved in S-02, after the goal adjustment, then accept or edit it. The confirmed number stays on the account in `user_profile`. Preferred and excluded products do not change the number.** — Closed 2026-10-07 (delivered in F-03) → `context/archive/2026-10-05-calorie-formula/`. Lesson: —.
- **S-06: user can return later and generate another next-day plan and shopping list without entering age, height, weight, sex, goal, activity, or preferences again.** — Closed 2026-10-07 (delivered by stored profile + S-04; no separate change) → `context/archive/2026-10-05-generate-diet-with-ollama/`. Lesson: —.
