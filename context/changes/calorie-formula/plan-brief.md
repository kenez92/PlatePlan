# Calorie Formula — Plan Brief

> Full plan: `context/changes/calorie-formula/plan.md`

## What & Why

PlatePlan needs a daily calorie integer on the profile before anyone can confirm it or send it to the model (FR-004, roadmap F-03). This change is that formula: BMR × activity, then the goal step, then one round to a whole kcal. `/profile` is where the proposed daily calories will be shown. This slice does not yet render or store the number.

## Starting Point

S-02 already saves age, height, weight, sex, goal, and activity on `user_profile`, with `Sex` / `Goal` / `ActivityLevel` as names only. There is no calculator. `ProfileService` only persists. `AGENTS.md` and the PRD still treat activity and the goal-adjustment size as open; S-03 is blocked on that size.

## Desired End State

`profile.service.CalorieService.dailyCalories(...)` returns an `int`. For 34 years, 180 cm, 82.5 kg, male, moderate: maintain 2767, lose 2267, gain 3267. `/profile` shows that number as the proposed daily calories when the body fields are filled. S-03 can later let the user edit it and save `confirmed_calories`. Docs no longer list the delta as unknown.

## Key Decisions Made

| Decision | Choice | Why (1 sentence) |
| -------- | ------ | ---------------- |
| Rounding | One `HALF_UP` to whole kcal after the goal step | 2766.75 must become 2767; a second rounding path would drift the profile |
| Goal size | −500 lose / +500 gain | Simple constant; FR-005 still lets the user edit |
| Goal in F-03 | Apply it here, not in S-03 | Unblocks S-03; that slice becomes confirm/edit/store on `/profile` |
| Input | age, height, weight, `Sex`, `ActivityLevel`, `Goal` | Six formula fields; products must not be a parameter |
| Result | `int` | One whole number, compared with `isEqualTo` |
| Invalid input | No range checks; `null` NPEs | Validation stays on the profile form |
| Package | `profile.service` | The calculator is part of the profile, where the proposed number will be shown |
| Bean | `@Service` | It is a profile application service, beside `ProfileService` |
| Multipliers | Inside the calculator | Profile enums stay names-only |

## Scope

**In scope:** `CalorieService` `@Service` in `profile.service`, unit tests for the locked examples, docs (`AGENTS.md`, PRD Open Question 1, roadmap F-03/S-03).

**Out of scope:** accept/edit of a confirmed number, `confirmed_calories`, a `calorie` package, a second enum set, calorie floor, adult-age narrowing, logging body data, new libraries.

## Architecture / Approach

A stateless Spring `@Service` in `com.kenez92.plateplan.profile.service`, beside `ProfileService`. Arithmetic is `BigDecimal` from decimal strings: BMR → × activity → −500/0/+500 → `setScale(WHOLE_KILOCALORIE_SCALE, HALF_UP)` → `int`. Tests `new` the bean. The profile screen later shows the proposed number.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| ----- | ---------------- | -------- |
| 1. Calculator | `@Service` + locked unit tests | `double` or round-then-adjust disagrees with 2767 |
| 2. Documentation | PRD / AGENTS / roadmap match ±500 | S-03 stays “blocked” if the roadmap text is skipped |

**Prerequisites:** S-02 enums and profile fields (done).
**Estimated effort:** ~1 session across 2 phases.

## Open Risks & Assumptions

- Legal profile extremes can yield a negative `int`; S-03 must still allow edit (FR-005), not a hidden clamp here.
- This slice does not yet call the calculator from the profile controller; tests are the only caller until the screen shows the proposed number.

## Success Criteria (Summary)

- The locked male moderate example is 2767 / 2267 / 3267 in tests that CI runs.
- Docs state BMR × activity then −500 / 0 / +500; S-03 is not blocked on size.
- The calculator lives on the profile; this slice does not yet show or store the number.
