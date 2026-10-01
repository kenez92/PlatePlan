---
project: PlatePlan
version: 1
status: draft
created: 2026-09-28
updated: 2026-10-01
prd_version: 2
main_goal: low-complexity
top_blocker: decisions
milestone_id: first-downloadable-plan
milestone_seq: 1
milestone_status: open
---

# Roadmap: PlatePlan

> Derived from `context/foundation/prd.md` (v2) + auto-researched codebase baseline.
> Edit-in-place; archive when superseded.
> Slices below are listed in dependency order. The "At a glance" table is the index.

## Milestone

**M-1: First downloadable next-day plan** — Status: open

- **Intent:** One person can create an account, save food preferences, confirm a calorie number from a formula, receive a next-day diet from the model, and download that diet and a shopping list as two PDF files that are not stored, then generate another pair on a later visit without typing the profile again.
- **Source materials:** `context/foundation/prd.md` (v2)
- **Done when:** every F-NN and S-NN below is `done`
- **Scope anchors:** FR-001, FR-002, FR-003, FR-004, FR-005, FR-006, US-01, US-02

## Vision recap

Every day you decide breakfast, lunch, and dinner while trying to lose weight, maintain it, or gain it. That costs time and often ends in a poor choice. Diets from the internet impose dishes. PlatePlan lets you state what you like and what you do not like, calculates a daily calorie number, and turns the confirmed number into a next-day plan and a shopping list as two PDF files.

## North star

**S-05: user can download the next-day diet plan and the shopping list** — placed as soon as the model can return the diet, because that download is the smallest flow that shows the product works, and the sequencing goal is to keep each slice small.

> A north star is the smallest end-to-end slice whose delivery would prove the core product hypothesis — the claim that naming foods you like and do not like, then confirming a calorie number, is enough to download a next-day plan and a shopping list. It is placed as early as its prerequisites allow, because the other slices only matter if this works.

## At a glance

| ID   | Change ID                  | Outcome (user can …)                                       | Prerequisites | PRD refs                          | Status   |
| ---- | -------------------------- | ---------------------------------------------------------- | ------------- | --------------------------------- | -------- |
| F-01 | database-configured        | (foundation) a database is configured                      | —             | Access Control                    | done |
| F-02 | spring-security-sign-in    | (foundation) Spring Security can require a signed-in account | F-01        | Access Control, FR-001, FR-002    | planning |
| F-03 | calorie-formula            | (foundation) calories are BMR times an activity level      | —             | FR-004                            | ready    |
| S-01 | register-and-sign-in       | user can register and log in                               | F-02          | US-01, FR-001, FR-002             | proposed |
| S-02 | save-food-preferences      | user can save preferred and excluded products              | S-01          | US-01, FR-003                     | proposed |
| S-03 | confirm-daily-calories     | user can confirm a daily calorie number from the formula   | S-01, F-03    | US-01, FR-003, FR-004, FR-005     | blocked  |
| S-04 | generate-diet-with-ollama  | user can receive a full next-day diet from the model       | S-02, S-03    | US-01, FR-006                     | proposed |
| S-05 | download-next-day-plan     | user can download the next-day plan and shopping list      | S-04          | US-01, FR-006                     | proposed |
| S-06 | return-visit-plan          | user can generate another plan without re-entering data    | S-05          | US-02                             | proposed |

## Streams

Navigation aid — groups items that share a Prerequisites chain. Canonical ordering still lives in the dependency graph below; this table is the proposed reading order across parallel tracks.

| Stream | Theme                         | Chain                                                    | Note                                                                                          |
| ------ | ----------------------------- | -------------------------------------------------------- | --------------------------------------------------------------------------------------------- |
| A      | Account, preferences, diet    | `F-01` → `F-02` → `S-01` → `S-02` → `S-04` → `S-05` → `S-06` | Database, then sign-in. The model joins the calorie number at `S-04`, then the PDFs follow. |
| B      | Calorie formula               | `F-03` → `S-03`                                          | Joins Stream A at `S-04`. The formula does not need the database, so it sits beside `F-01`. |

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
- **Risk:** Sequenced first because sign-in, food preferences, and the saved calorie number all have to survive a later visit. F-02 adds the sign-in tables. S-02 adds the preferences table. S-03 stores the body fields and the confirmed number.
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
- **Status:** planning

### F-03: Calorie formula

- **Outcome:** (foundation) a daily calorie number before the goal adjustment is BMR times one activity level. Weight is in kilograms, height in centimetres, age in years. Sex selects the BMR line. Male: BMR = (10 × weight) + (6.25 × height) − (5 × age) + 5. Female: BMR = (10 × weight) + (6.25 × height) − (5 × age) − 161. Activity is `SEDENTARY` ×1.2 (most of the day sitting), `LIGHT` ×1.375 (walking or light effort on most days), `MODERATE` ×1.55 (exercise several days a week), or `HIGH` ×1.725 (hard training or physical work on most days).
- **Change ID:** calorie-formula
- **PRD refs:** FR-004
- **Unlocks:** S-03
- **Prerequisites:** —
- **Parallel with:** F-01
- **Blockers:** —
- **Unknowns:** —
- **Risk:** This is the settled BMR and the four activity levels. It has no screen and no database. S-03 collects the inputs, applies the goal adjustment, and lets the user accept or edit the result. Food preferences do not change the number.
- **Status:** ready

## Slices

### S-01: Register and log in

- **Outcome:** user can create an account from the login window and land signed in, without a second login step, and can log in again from that window on a later visit.
- **Change ID:** register-and-sign-in
- **PRD refs:** US-01, FR-001, FR-002
- **Prerequisites:** F-02
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** The login and register screens are already a shell. This slice makes account creation, automatic sign-in, and a later login real on the Spring Security boundary from F-02. Only that account can see its own data.
- **Status:** proposed

### S-02: Save food preferences

- **Outcome:** user can enter preferred products and excluded products on a screen, and those lists are stored in their own table on the account.
- **Change ID:** save-food-preferences
- **PRD refs:** US-01, FR-003
- **Prerequisites:** S-01
- **Parallel with:** S-03
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Next after sign-in. These products are included or excluded in the diet and do not change the calorie number, so this slice can proceed beside S-03. The screen and the preferences table land here, on the database from F-01.
- **Status:** proposed

### S-03: Confirm daily calories

- **Outcome:** user can enter age, height, weight, sex, goal, and an activity level, see the number from the F-03 formula after the goal adjustment, then accept or edit it. The confirmed number and the activity level stay on the account. Preferred and excluded products do not change the number.
- **Change ID:** confirm-daily-calories
- **PRD refs:** US-01, FR-003, FR-004, FR-005
- **Prerequisites:** S-01, F-03
- **Parallel with:** S-02
- **Blockers:** —
- **Unknowns:**
  - By how much does lose weight lower the result, and by how much does gain raise it? — Owner: user. Block: yes.
- **Risk:** BMR and the four activity levels are settled in F-03. The goal direction is settled (lose lowers, maintain leaves the result, gain raises). The size of that goal change is not. Planning the last step of the number before that size is known would invent the remaining figure the PRD left open.
- **Status:** blocked

### S-04: Generate the diet with the model

- **Outcome:** user can receive a full next-day diet and shopping list from the model, using the saved food preferences and the confirmed calorie number. The text is not stored.
- **Change ID:** generate-diet-with-ollama
- **PRD refs:** US-01, FR-006
- **Prerequisites:** S-02, S-03
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Spring AI with Ollama produces the diet before any PDF exists. The model is called by the application; it is not placed on the small deploy host. S-05 turns this result into files.
- **Status:** proposed

### S-05: Download the next-day plan

- **Outcome:** user can download the diet and the shopping list from S-04 as two PDF files that are not stored.
- **Change ID:** download-next-day-plan
- **PRD refs:** US-01, FR-006
- **Prerequisites:** S-04
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:**
  - Does the result stay two PDF files, or become an email with the full content? — Owner: user. Block: no.
- **Risk:** This is the north star. It starts only after the model has returned the diet. The email question stays open and does not move this slice off the two PDF files already written in the PRD. The files are produced for download and are not kept.
- **Status:** proposed

### S-06: Return and generate again

- **Outcome:** user can return later and generate another next-day plan and shopping list without entering age, height, weight, sex, goal, activity, or preferences again.
- **Change ID:** return-visit-plan
- **PRD refs:** US-02
- **Prerequisites:** S-05
- **Parallel with:** —
- **Blockers:** —
- **Unknowns:** —
- **Risk:** Sequenced after the first download because this is the other success criterion: the account data is still there, and a later visit can call the model and download again immediately.
- **Status:** proposed

## Backlog Handoff

| Roadmap ID | Change ID                 | Suggested issue title                                         | Ready for `/10x-plan` | Notes                                                        |
| ---------- | ------------------------- | ------------------------------------------------------------- | --------------------- | ------------------------------------------------------------ |
| F-01       | database-configured       | Configure the database                                        | yes                   | Run `/10x-plan database-configured`                          |
| F-02       | spring-security-sign-in   | Require sign-in with Spring Security and sign-in tables       | no                    | After F-01. Parallel with F-03                               |
| F-03       | calorie-formula           | Set BMR times the four activity levels                        | yes                   | Run `/10x-plan calorie-formula`. Parallel with F-01          |
| S-01       | register-and-sign-in      | Register and log in                                           | no                    | After F-02                                                   |
| S-02       | save-food-preferences     | Save preferred and excluded products on a screen and a table  | no                    | After S-01. Parallel with S-03                               |
| S-03       | confirm-daily-calories    | Enter body data, apply the formula, accept or edit            | no                    | Blocked on goal-adjustment size. After S-01 and F-03         |
| S-04       | generate-diet-with-ollama | Generate the full diet with Spring AI and Ollama              | no                    | After S-02 and S-03                                          |
| S-05       | download-next-day-plan    | Download the model diet and shopping list as two PDFs         | no                    | After S-04                                                   |
| S-06       | return-visit-plan         | Generate another plan on a return visit                       | no                    | After S-05                                                   |

## Open Roadmap Questions

1. **By how much does lose weight lower the result, and by how much does gain raise it?** — Owner: user. Block: S-03.
2. **Does the result stay two PDF files, or become an email with the full content?** — Owner: user. Block: does not gate S-05. That slice follows the two PDF files written in the PRD until email is chosen.
3. **How many weeks is the MVP?** — Owner: user. Block: does not gate a slice. This roadmap does not use a week count.
4. **Should this milestone publish a Swagger description of the API?** — Owner: user. Block: does not gate a slice. Raised while framing the roadmap. The PRD does not mention it, so it is not a slice until it has a source anchor.

## Parked

- **Support for multiple people** — Why parked: PRD Non-Goals. This version is one account.
- **Storing diet plans and shopping lists** — Why parked: PRD Non-Goals. Both files are generated and downloaded, not kept.
- **Advanced user roles** — Why parked: PRD Non-Goals. Access stays flat, with no roles. F-02 creates sign-in tables for one account.

## Milestone History

No closed milestone yet.

## Done

- **F-01: (foundation) the application has a configured database. Product tables are not created here.** — Archived 2026-10-01 → `context/archive/2026-09-29-database-configured/`. Lesson: —.
