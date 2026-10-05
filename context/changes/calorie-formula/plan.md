# Calorie Formula Implementation Plan

## Overview

F-03 adds the daily calorie number from the settled BMR formula, the four activity multipliers, and the goal adjustment (−500 / 0 / +500 kcal). The calculator belongs to the profile: `/profile` is where the proposed daily calories will be shown. This slice still does not persist the number. S-03 later lets the user accept or edit it (FR-005) and stores `confirmed_calories`. This change is roadmap F-03 (FR-004). Planning moved the goal step into F-03 so the open size question no longer blocks S-03.

## Current State Analysis

- The formula lives only in `context/foundation/prd.md` (Business Logic) and `context/foundation/roadmap.md` (F-03). No Java calculator exists.
- S-02 already stores the inputs on `user_profile` and owns `Sex`, `Goal`, and `ActivityLevel` as names only (`src/main/java/com/kenez92/plateplan/profile/model/Sex.java:7-9`, `Goal.java:7-9`, `ActivityLevel.java:7-10`). Those enums must be reused; a second set of values is out of scope.
- `ProfileDetails` also carries `Goal` and `ProductLists` (`profile/model/ProfileDetails.java:8-14`). Products never change the number (FR-004). The calculator takes the six formula fields, not that record, so the profile cannot feed products into the number by passing the whole profile.
- `ProfileService` only reads and writes the table (`profile/service/ProfileService.java:18-27`). It must not grow a formula. The calculator is a second `@Service` beside it in `profile.service`.
- Weight is `BigDecimal` scale 1; age and height are `int`. There is no `MathContext` anywhere. The only production rounding is weight `setScale(1, HALF_UP)` on load (`ProfileService.java:33`, `90-94`).
- `AGENTS.md` still lists activity and the goal-adjustment size as open, and says preferences do not change calories via “BMR plus goal” without the activity multipliers. `context/foundation/lessons.md` does not exist.
- Roadmap F-03 originally stopped before the goal adjustment; S-03 was blocked on the size. This plan applies −500 / +500 inside F-03.

## Desired End State

`profile.service.CalorieService` is a `@Service` that returns one `int`: BMR × activity, then −500 (`LOSE_WEIGHT`), 0 (`MAINTAIN`), or +500 (`GAIN`), then a single `HALF_UP` to a whole kcal. For 34 years, 180 cm, 82.5 kg, `MALE`, `MODERATE` the results are 2767 / 2267 / 3267. `/profile` shows that number as the proposed daily calories when the body fields are filled. Enums stay names-only. Verify with `.\gradlew.bat test` and the locked examples in `CalorieServiceTest`.

### Key Discoveries:

- Scope (decided): the full pre-confirm number, including the goal step. S-03 is accept/edit and `confirmed_calories` on the profile, not a second formula.
- Rounding (decided): one `RoundingMode.HALF_UP` to whole kilocalories after the goal step (`WHOLE_KILOCALORIE_SCALE`). `MAINTAIN` for the locked male example is 2767 (from 2766.75).
- Goal size (decided): −500 kcal lose, +500 kcal gain, applied to the exact BMR × activity product, not to the already-rounded maintain number.
- Input (decided): `age`, `heightCm`, `weightKg`, `Sex`, `ActivityLevel`, `Goal`. Not `ProfileDetails`. Products are not parameters.
- Result (decided): `int`.
- Invalid input (decided): no range checks. `null` sex, activity, goal, or weight fails as a normal `NullPointerException`. Age 9 still returns a number. Extreme legal profile values may go negative; do not clamp.
- Package (decided, corrected in implementation): `com.kenez92.plateplan.profile.service`. The calculator is part of the profile, because `/profile` is where the proposed daily calories will be shown. No separate `calorie` package.
- Bean (decided, corrected in implementation): `@Service`, not `@Component`.
- Multipliers (derived): map inside the calculator. Do not add methods or constants onto `profile.model` enums.
- Arithmetic (derived): `BigDecimal` from decimal strings. `1.2` and `1.55` are not exact in binary `double`; a second rounding path would disagree with the locked 2767.
- Wiring (derived): this slice does not call the calculator from `ProfileController` yet. Tests are the proof. The profile screen later shows the proposed number.
- Docs (derived): close PRD Open Question 1, drop activity and goal size from the `AGENTS.md` open list, and unblock S-03 on the roadmap.

## What We're NOT Doing

- No accept/edit of a confirmed number and no `confirmed_calories` column (FR-005 / S-03).
- No separate calorie package or calorie screen.
- No second `Sex` / `ActivityLevel` / `Goal` enums.
- No range clamp, adult-age narrowing, or minimum daily calories (S-02 left adult narrowing to S-03; this change does not take it).
- No logging of age, height, weight, sex, goal, activity, or the resulting number.
- No new library, no `double` arithmetic, no `*Util`.

## Implementation Approach

Two phases, each ending with a green `.\gradlew.bat test`. Phase 1 is the `@Service` and unit tests in `profile.service`. Phase 2 aligns `AGENTS.md`, the PRD, and the roadmap with the settled ±500 rule. New production code follows `config/LiquibaseConfiguration.java` (four spaces, `final` parameters). Tests follow `.cursor/rules/testing.mdc`: `should…` names, every local `final`, no `private static` constants in the test class, `new CalorieService()`, no Spring context. An `int` result is `assertThat(actual).isEqualTo(expected)`, not recursive comparison.

## Critical Implementation Details

- **Arithmetic order** — BMR, then × activity, then −500 / 0 / +500, then one `setScale(WHOLE_KILOCALORIE_SCALE, RoundingMode.HALF_UP)` and `intValueExact()`. Rounding before the goal, or using `double`, is a different number than the locked examples.
- **Debug & observability** — The calculator has no `Logger`. Inputs are the same personal fields `AGENTS.md` forbids logging.

## Phase 1: Calculator

### Overview

Add `CalorieService` in `profile.service` and lock the formula with unit tests. After this phase the number exists in code; the profile does not yet show it.

### Changes Required:

#### 1. Calculator service

**File**: `src/main/java/com/kenez92/plateplan/profile/service/CalorieService.java` (new)

**Intent**: Own the daily calorie number on the profile so `/profile` can later show the proposed amount without copying the formula.

**Contract**: `@Service` with a public instance method. Reuse `profile.model.Sex`, `ActivityLevel`, and `Goal`. Constants are `private static final` `BigDecimal` values from decimal strings, plus `WHOLE_KILOCALORIE_SCALE` for the whole-kcal `setScale`. Male BMR offset `+5`, female `−161`. Activity: `SEDENTARY` ×1.2, `LIGHT` ×1.375, `MODERATE` ×1.55, `HIGH` ×1.725. Goal: `LOSE_WEIGHT` −500, `MAINTAIN` 0, `GAIN` +500. No range checks, no logging, no products parameter.

```java
public int dailyCalories(final int age,
                         final int heightCm,
                         final BigDecimal weightKg,
                         final Sex sex,
                         final ActivityLevel activityLevel,
                         final Goal goal)
```

#### 2. Unit tests

**File**: `src/test/java/com/kenez92/plateplan/profile/service/CalorieServiceTest.java` (new)

**Intent**: Pin the locked examples and the four activity lines so the profile cannot drift the number.

**Contract**: construct with `new CalorieService()`. Weight literals are decimal strings (e.g. `new BigDecimal("82.5")`). Gold values are local `final` ints in each test, not class constants. Required cases:
- Locked male `MODERATE`: age 34, height 180, weight 82.5, `MALE` → `MAINTAIN` 2767, `LOSE_WEIGHT` 2267, `GAIN` 3267 (same body, only `Goal` changes).
- Same male BMR × each other activity, `MAINTAIN`: `SEDENTARY` 2142, `LIGHT` 2454, `HIGH` 3079.
- One female line: age 30, height 165, weight 60.0, `FEMALE`, `LIGHT`, `MAINTAIN` → 1815 (BMR 1320.25 × 1.375 = 1815.34375).
- A case that goes negative, to prove there is no floor: age 110, height 80, weight 20.0, `FEMALE`, `SEDENTARY`, `LOSE_WEIGHT` → −513.

### Success Criteria:

#### Automated Verification:

- `CalorieServiceTest` passes: `.\gradlew.bat test --tests com.kenez92.plateplan.profile.service.CalorieServiceTest`
- The full suite still passes: `.\gradlew.bat test`

#### Manual Verification:

- The test class contains the locked triple 2767 / 2267 / 3267 for 34 / 180 / 82.5 / `MALE` / `MODERATE`

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Phase 2: Documentation

### Overview

Write the settled formula into the living docs so S-03 is not planned against an open size question or a stale “activity is open” bullet.

### Changes Required:

#### 1. Repository guidelines

**File**: `AGENTS.md`

**Intent**: Agents must not treat activity or the goal delta as still open, and must reuse `profile.service.CalorieService` instead of inventing a second formula or a `calorie` package.

**Contract**: In **Hard rules**, replace the “BMR plus goal” / “Leave open: goal-adjustment size, activity” wording so calories are BMR × activity, then −500 / 0 / +500, preferences still do not change the number, and the remaining open PRD item in that bullet is email instead of PDF. In **Packages**, list `CalorieService` under `profile.service` with `ProfileService`. In **Profile**, keep storage as it is and add that F-03 computes the daily `int` in `profile.service.CalorieService` and `/profile` shows the proposed daily calories; S-03 still adds `confirmed_calories`. Do not reformat unrelated paragraphs.

#### 2. PRD

**File**: `context/foundation/prd.md`

**Intent**: Close the size question that blocked S-03, in the same document that left it open.

**Contract**: In **Business Logic**, replace “The size of the goal change is not settled” with: lose weight subtracts 500 kcal, maintain adds 0, gain adds 500, applied to BMR × activity, then one round half-up to a whole kcal. In **Open Questions**, mark item 1 settled (−500 / +500) rather than deleting the history; leave items 2 and 3 untouched.

#### 3. Roadmap

**File**: `context/foundation/roadmap.md`

**Intent**: F-03’s outcome must include the goal step this plan added, and S-03 must not stay blocked on a size that is now known.

**Contract**: Update the F-03 outcome (At a glance + Foundations body) so the number is BMR × activity then −500 / 0 / +500, rounded half-up to `int`, and belongs to the profile. Remove S-03’s unknown about the goal-adjustment size and the `blocked` status that depended on it (S-03 becomes ready to plan: confirm/edit/store on `/profile`). Refresh Open Roadmap Question 1 to settled. Change only those Status / outcome / unknown lines; do not rewrite unrelated slices.

### Success Criteria:

#### Automated Verification:

- The full suite still passes: `.\gradlew.bat test`

#### Manual Verification:

- `AGENTS.md` Hard rules name BMR × activity then −500 / 0 / +500 and do not list activity or goal size as open
- PRD Business Logic and Open Question 1 record the same ±500 rule
- Roadmap F-03 outcome includes the goal step; S-03 is not blocked on size

**Implementation Note**: After completing this phase and all automated verification passes, pause here for manual confirmation from the human that the manual testing was successful before proceeding to the next phase. Phase blocks use plain bullets — the corresponding `- [ ]` checkboxes for these items live in the `## Progress` section at the bottom of the plan.

---

## Testing Strategy

### Unit Tests:

- Locked male `MODERATE` × three goals (2767 / 2267 / 3267)
- Same male × the other three activity levels at `MAINTAIN`
- One female `LIGHT` `MAINTAIN` (1815)
- Negative result at the small-body extreme (no floor)

### Integration Tests:

- None. F-03 has no new HTTP surface in this slice. `ApplicationTest.shouldLoadContext` already boots the context and will pick up the new `@Service`.

### Manual Testing Steps:

1. Open `CalorieServiceTest` and confirm the 2767 / 2267 / 3267 triple matches the planning examples.
2. After Phase 2, read `AGENTS.md` Hard rules, PRD Open Question 1, and roadmap F-03 / S-03 and confirm they agree on ±500.

## Performance Considerations

One arithmetic call per invocation. No cache.

## Migration Notes

No schema change. Saved profiles are unchanged. No calorie is stored until S-03.

## References

- PRD Business Logic and FR-004: `context/foundation/prd.md`
- Roadmap F-03 / S-03: `context/foundation/roadmap.md`
- Neighbor service: `src/main/java/com/kenez92/plateplan/profile/service/ProfileService.java`
- Enums to reuse: `src/main/java/com/kenez92/plateplan/profile/model/Sex.java`, `ActivityLevel.java`, `Goal.java`
- Prior slice (inputs only): `context/archive/2026-10-02-save-user-profile/plan.md`

## Progress

> Convention: `- [ ]` pending, `- [x]` done. Append ` — <commit sha>` when a step lands. Do not rename step titles. See `references/progress-format.md`.

### Phase 1: Calculator

#### Automated

- [x] 1.1 CalorieServiceTest passes: `.\gradlew.bat test --tests com.kenez92.plateplan.profile.service.CalorieServiceTest` — 73194fd
- [x] 1.2 The full suite still passes: `.\gradlew.bat test` — 73194fd

#### Manual

- [x] 1.3 The test class contains the locked triple 2767 / 2267 / 3267 for 34 / 180 / 82.5 / `MALE` / `MODERATE` — 73194fd

### Phase 2: Documentation

#### Automated

- [x] 2.1 The full suite still passes: `.\gradlew.bat test`

#### Manual

- [ ] 2.2 `AGENTS.md` Hard rules name BMR × activity then −500 / 0 / +500 and do not list activity or goal size as open
- [ ] 2.3 PRD Business Logic and Open Question 1 record the same ±500 rule
- [ ] 2.4 Roadmap F-03 outcome includes the goal step; S-03 is not blocked on size
