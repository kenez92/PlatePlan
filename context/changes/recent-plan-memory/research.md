---
date: 2026-11-04T15:36:58+01:00
researcher: Daniel
git_commit: ab513f9ccd590e8f32258c98ee9ad2e1f361a9a4
branch: feature/front-views
repository: PlatePlan
topic: "Where a later generate would read the last three plans, and which accepted rules forbid storing them"
tags: [research, codebase, plan, diet-generator, week-plan-validator, liquibase, prd]
status: complete
last_updated: 2026-11-04
last_updated_by: Daniel
---

# Research: Last three plans in the prompt

**Date**: 2026-11-04T15:36:58+01:00
**Researcher**: Daniel
**Git Commit**: ab513f9ccd590e8f32258c98ee9ad2e1f361a9a4
**Branch**: feature/front-views
**Repository**: PlatePlan

Source anchors below are in that commit, except `context/changes/recent-plan-memory/change.md` and `context/foundation/roadmap.md`, which were read from the working tree. No test run and no live database query.

## Research Question

The note in `context/changes/recent-plan-memory/change.md` (status `new`, 2026-11-03) asks whether to save the last three generated plans and change the prompt so the model does not repeat those plans. What does generation do today, what is stored, and which accepted texts allow or forbid that memory?

## Summary

On this commit, `DietGenerator.generate` sends one user prompt and does not call the model again when the week is empty, the call throws, repair returns null, or `WeekPlanValidator` rejects the week (`DietGenerator.java:124-149`). The three substitutions in that prompt are the current person lines and the two merged product lists (`DietGenerator.java:132-133`). The rest of `PROMPT` is fixed text (`DietGenerator.java:43-107`). A search of `src/main` Java for `ChatMemory` and `advisors(` returned no matches. Inside one accepted week, breakfast, lunch, and dinner names share one case-insensitive set of 21 keys (`WeekPlanValidator.java:99-106`). Second breakfast is outside that set (`WeekPlanValidator.java:101-104`).

The changelog XML under `src/main/resources/db/changelog/changes/` declares three application tables: `account` (`002-create-account.xml:10`), `user_profile` (`003-create-user-profile.xml:16`), and `household_member` (`006-household-members.xml:18`). A search of `src/main/java/com/kenez92/plateplan/plan` for `repository.save` in `*Service.java` returned no matches. Accepted product text forbids keeping the plan and the shopping list (`prd.md:44`, `prd.md:118`). `AGENTS.md:7` forbids persisting either PDF file and does not, in that sentence, name plan text. No accepted sentence in the files checked below allows storing dish names. The draft roadmap asks the user whether to update that PRD ban, and says the question does not block this slice (`roadmap.md:102`).

## Detailed Findings

### One generate, no prior dishes

`POST /plan/generate` calls `PlanService.generate` with the principal name and the `person` list (`PlanController.java:63-72`). `DietGenerator` is the class whose comment says it is the only owner of `ChatClient` (`DietGenerator.java:17-22`). The call site is one `.user(...).call().entity(WeekPlan.class)` (`DietGenerator.java:132-135`). On rejection it logs `Diet generation failed: week plan rejected` and returns `PlanResult.unavailable()` (`DietGenerator.java:141-143`). The class comment says it does not retry (`DietGenerator.java:21`).

The formatted prompt receives three substitutions: person lines, preferred names, excluded names (`DietGenerator.java:132-133`). A person line is `Name: %s. Goal: %s. Daily calorie target: %d kcal.` (`DietGenerator.java:37`). Goal text is the three English instructions in `DietGenerator.java:38-42`, not the enum name. Empty product lists become the word `none` (`DietGenerator.java:34`, `DietGenerator.java:168-170`). The same template tells the model it is a dietitian, to reply in Polish, and not to assign calendar dates (`DietGenerator.java:43-45`). The PDF day title in this writer is `Dzień %d` (`PlanPdfWriter.java:42`). `prd.md:34` and `AGENTS.md:7` still say the plan starts tomorrow. This inspected generator does not put a calendar date into the prompt.

`PlanPerson` is filled from the selected people. This research did not re-read `PlanService` line by line. `AGENTS.md:61` states the model receives each selected person's name, goal as an English instruction, confirmed calories, and product lists merged only from the selected people, and does not receive age, height, weight, sex, activity, login, or the delivery email. That withhold list matches the fields absent from `PERSON_LINE` (`DietGenerator.java:37`).

### Uniqueness inside one week

The prompt says: `Breakfast, lunch, and dinner names must be unique across the seven days.` (`DietGenerator.java:95`). It also says not to repeat the same second breakfast without a break (`DietGenerator.java:96-97`). It does not define the break, and it does not say that a breakfast name may not equal a lunch name.

`WeekPlanValidator.isValid` requires seven days (`WeekPlanValidator.java:26`, `WeekPlanValidator.java:41-43`) and then `cookedNamesAreUnique` (`WeekPlanValidator.java:37-38`). That method collects breakfast, lunch, and dinner only (`WeekPlanValidator.java:101-104`). The key is `strip` plus `toLowerCase(Locale.ROOT)` (`WeekPlanValidator.java:109-110`). `distinct().count() == keys.size()` (`WeekPlanValidator.java:106`) means those 21 keys differ from each other when the week has seven days. A repeated second breakfast is not in that list. The class comment says a repeated second breakfast is accepted and the shopping list is not checked (`WeekPlanValidator.java:19-21`).

`AGENTS.md:7` states the same split: breakfast, lunch, and dinner names are unique across the week; a second breakfast may repeat and Java does not reject that. No sentence in `prd.md` states that uniqueness rule. `prd.md:18` names breakfast, lunch, and dinner and does not name second breakfast.

Nothing in this generator reads a previous week. A later `POST /plan/generate` on this path has no dish names from an earlier success to avoid.

### What the database holds

`db.changelog-master.xml:7-11` includes every XML file in `db/changelog/changes/` in alphabetical order and says never to edit an applied changeSet or rename its file. `AGENTS.md:27` says the same and says Hibernate does not generate schema. The `createTable` hits in that `changes/` directory are `account`, `user_profile`, and `household_member` (grep of `createTable` in `src/main/resources/db/changelog`). Later files add columns on `user_profile`: `confirmed_calories` (`004-add-confirmed-calories.xml`), `email` (`005-add-profile-email.xml`), `display_name` (`006-household-members.xml:15`). This research did not query a live `databasechangelog` table, so it does not say which of those files the deployed database has applied.

A grep of `interface *Repository` under `src/main/java` returned three interfaces: `AccountRepository`, `UserProfileRepository`, and `HouseholdMemberRepository`. `WeekPlan` is the model type returned by the generator (`DietGenerator.java:135`). This pass did not search every file for `@Entity`.

`test-plan.md:31` lists “the plan or shopping list is stored” as risk #5, sourced to the PRD guardrail and to `AGENTS.md` “do not persist / do not log”. `test-plan.md:44` says the claim “No plan table means nothing is stored” must be challenged. That row does not authorize a plan table. It says absence of a table is not, by itself, proof that nothing is stored.

### Accepted ban, and the draft exception

Current rules, read in this pass:

- `prd.md:44` — the diet plan and the shopping list are not stored; they are only generated and downloaded.
- `prd.md:53` and `prd.md:59` — both PDFs are not stored (US-01, US-02).
- `prd.md:78` and `prd.md:87` — the plan and the shopping list are not stored.
- `prd.md:109` — neither PDF file is stored. Account data stays so the next visit can generate again.
- `prd.md:118` — non-goal: storing diet plans and shopping lists.
- `AGENTS.md:7` — do not persist either file. Files in that sentence are the two PDF fields.
- `AGENTS.md:61` — files stay in the browser; refresh drops them.

`prd.md:35` says the next visit generates another plan without re-entering body data. It does not say the next plan must use different dishes.

The draft roadmap (`roadmap.md:23-30`, status `draft`, milestone open) describes remembering three plans’ dish content, then likes, while PDF files stay download-only. `roadmap.md:102` asks whether to update the PRD that forbids storing plans and shopping lists, owner user, and says that question does not block S-01 or S-02. `roadmap.md:106` parks storing the PDF files. That split is written as an open question, not as a change to `prd.md`.

`context/changes/dish-likes/change.md` is a separate note, also status `new`, about saving dishes with like and unlike. It is not part of this question. `roadmap.md:84` lists it as S-02 after S-01.

## Code References

- `src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java:43-107` — the single prompt template, including uniqueness sentences at lines 95–97.
- `src/main/java/com/kenez92/plateplan/plan/service/DietGenerator.java:124-149` — one model call, then repair and validate, no second call.
- `src/main/java/com/kenez92/plateplan/plan/validator/WeekPlanValidator.java:99-111` — 21-name uniqueness for breakfast, lunch, and dinner.
- `src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java:63-72` — generate takes `person` and `sendEmail`, not a previous plan.
- `src/main/resources/db/changelog/db.changelog-master.xml:7-11` — includeAll and the no-edit rule.
- `context/foundation/prd.md:44` — plan and shopping list are not stored.
- `context/foundation/roadmap.md:102` — open question on updating that ban.

## Architecture Insights

A new stored week would be a new numbered Liquibase file under `src/main/resources/db/changelog/changes/`. The last file on disk in this pass is `006-household-members.xml`. Account scope in the existing tables is the login string: `user_profile.login` is the primary key (`003-create-user-profile.xml:16` and the entity key described in `AGENTS.md:57`), and `household_member.login` is the owner login (`006-household-members.xml`). This research did not design a table.

The prompt has no slot for prior dish names. Any memory would be new text inside `PROMPT` or a new argument to `generate` (`DietGenerator.java:124-133`). The validator would still accept a week that repeats a name from an older week, because `cookedNamesAreUnique` reads only `weekPlan.days()` (`WeekPlanValidator.java:99-106`).

Who the week is for is the selection on that request. `AGENTS.md:61` says person checkboxes start checked, no `person` parameter means everyone on the account, and an unselected person adds no portion and no product list. Selection is not a column in the three tables listed above.

## Historical Context (from prior changes)

Each sentence is scored against the current tree.

- Supported for the no-write rule of that slice: `context/archive/2026-10-05-generate-diet-with-ollama/plan.md:5` says the text and the bytes are not saved. Current `AGENTS.md:7` still forbids persisting either PDF. The same overview describes a next-day plan, which the current product no longer is (`prd.md:34` says seven days). The no-save sentence is not invalidated by the day count.
- Contradicted as a description of the current prompt: `context/archive/2026-10-05-generate-diet-with-ollama/plan.md:25` says the model receives only preferred products, excluded products, and the calorie number. Current `DietGenerator.java:37` and `AGENTS.md:61` also send the person's name and an English goal instruction. The archive sentence records what that plan required then.
- Supported by current validator code: `context/archive/2026-10-12-seven-day-plan/plan.md:51` defines the name key as `strip` plus `toLowerCase(Locale.ROOT)`, puts breakfast, lunch, and dinner in one set, allows a repeated second breakfast, and says the prompt asks for a break that the validator does not count. `WeekPlanValidator.java:99-111` and `DietGenerator.java:95-97` match that paragraph. The same plan’s “one person” scope (`plan.md` later in that file, not re-quoted here) is the earlier decision. Current `prd.md:24` and `AGENTS.md:7` are the owner plus at most three added people.
- Supported as a closed milestone record, not as permission to store later text: `roadmap.md:121` says the archived generate slice did not store the text and files.
- Supported for the two paragraphs read in this pass, not for the rest of that file: `context/changes/household-members/plan.md:35` says this change does not save PDFs and does not add a second model attempt. `plan.md:176` says breakfast, lunch, and dinner name keys stay unique across the week and a repeated second breakfast is accepted. Those two sentences match `AGENTS.md:7` and `WeekPlanValidator.java:19-21`. `plan.md:32` says the screen does not remember who was unchecked: opening `/plan` selects everyone. Unread paragraphs of that plan were not scored.

## Related Research

`context/changes/front-views/research.md` is about screen copy and CSS. It does not decide plan storage. `context/changes/dish-likes/` has no `research.md`. The persist risk is written in `context/foundation/test-plan.md:31` and `:44`, not re-derived from `context/archive/2026-10-07-testing-persist-leak-csrf/research.md` in this pass.

## Open Questions

These are product choices. This document does not pick them.

1. Whether remembering dish text is an exception to `prd.md:44` and `prd.md:118`, or whether those sentences stay and this slice cannot store a plan. `roadmap.md:102` leaves that to the user and says it does not block the slice. `AGENTS.md:7` already separates “do not persist either file” from a ban on plan text.
2. What “three plans” means if storage is allowed: dish names only, full week text, or the shopping list too. The PRD ban names both the plan and the shopping list (`prd.md:118`). The roadmap parks the PDF files (`roadmap.md:106`).
3. Whether memory is one list per account, or one list per selected subset. A generate can omit people (`AGENTS.md:61`). The three existing tables do not store that selection.
4. Whether a repeated second breakfast counts as a dish the next plan must avoid. Inside one week, Java accepts that repeat (`WeekPlanValidator.java:101-104`).
5. `dish-likes` stays a later slice (`roadmap.md:84`). This research does not define like or unlike.
