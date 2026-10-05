<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Calorie formula

- **Plan**: context/changes/calorie-formula/plan.md
- **Scope**: Full plan
- **Reviewed phases**: 1, 2
- **Date**: 2026-10-05
- **Verdict**: NEEDS ATTENTION
- **Findings**: 0 critical 3 warnings 3 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | WARNING |
| Scope Discipline | WARNING |
| Safety & Quality | WARNING |
| Architecture | PASS |
| Pattern Consistency | PASS |
| Success Criteria | PASS |

## Findings

### F1 — Goal step is ±250 while the plan still specifies ±500

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Plan Adherence
- **Location**: src/main/java/com/kenez92/plateplan/profile/service/CalorieService.java:27
- **Detail**: Plan Phase 1 contracted LOSE −500 / GAIN +500 and golds 2267 / 3267 / −513. Code uses −250 / +250. Tests lock 2517 / 3017 / −263. Living AGENTS, PRD Open Question 1, and roadmap F-03 match −250. The plan body and Progress row titles were left at ±500 (phase blocks are read-only during implement). This was a product decision after a sedentary lose-weight example looked too steep, not an accidental drift.
- **Fix A ⭐ Recommended**: Add a plan addendum that records −250 / +250 as the settled rule and notes that Progress titles stay historical.
  - Strength: Future archive and S-03 planning stop treating ±500 as live.
  - Tradeoff: The original plan text stays mixed with the addendum.
  - Confidence: HIGH — living docs and tests already agree on −250.
  - Blind spot: None significant.
- **Fix B**: Restore ±500 in CalorieService and tests to match the original plan.
  - Strength: Plan Adherence becomes MATCH.
  - Tradeoff: Reverts the light-deficit product decision the user already accepted.
  - Confidence: HIGH — mechanical, but wrong product-wise.
  - Blind spot: `/profile` UX would show the steeper cut again.
- **Decision**: FIXED via Fix A

### F2 — F-03 shipped S-03 persist, edit, and recalculate

- **Severity**: ⚠️ WARNING
- **Impact**: 🔬 HIGH — architectural stakes; think carefully before deciding
- **Dimension**: Scope Discipline
- **Location**: src/main/resources/db/changelog/changes/004-add-confirmed-calories.xml:1
- **Detail**: Plan “What We're NOT Doing” forbade accept/edit, `confirmed_calories`, schema change, and ProfileController calling the formula. The branch added Liquibase 004, `ConfirmedCaloriesService`, `DailyCaloriesForm`, `POST /profile/calories`, `POST /profile/recalculate`, first-save formula persistence, and the Cel kaloryczny UI. Roadmap S-03 is still `ready` with leftover-UX wording. The extra work is coherent (formula vs profile vs confirmed calories) and was requested during implementation.
- **Fix A ⭐ Recommended**: Keep the code. In the plan addendum and roadmap, treat confirm/edit/store as done in F-03; shrink or skip S-03 to leftover UX only.
  - Strength: Matches what is in production code and what the user confirmed.
  - Tradeoff: F-03 is larger than the written plan; S-03 may become a no-op.
  - Confidence: HIGH — user asked for persist/edit/split endpoints.
  - Blind spot: Whether any confirm-UX from FR-005 is still missing on `/profile`.
- **Fix B**: Revert the persist/edit/recalculate surface and leave it for `confirm-daily-calories`.
  - Strength: Restores the original F-03 boundary.
  - Tradeoff: Throws away the committed feature the user already accepted.
  - Confidence: HIGH — large revert, not a small patch.
  - Blind spot: How much of ad08591 would need to be undone.
- **Decision**: FIXED via Fix A

### F3 — Body save can overwrite a concurrent calorie UPDATE

- **Severity**: ⚠️ WARNING
- **Impact**: 🔎 MEDIUM — real tradeoff; pause to reason through it
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/profile/service/ProfileService.java:85
- **Detail**: Calorie writes use JPQL `UPDATE` of `confirmed_calories` only. A later body save still `findById`, copies `confirmedCalories` onto the entity, and `save()`s the full row. A `POST /profile/calories` that commits between that find and save is lost. Recalculate has a smaller stale-body window (read body, then UPDATE calories). Last-write-wins is already the profile rule; two writers on one row make it real for two tabs.
- **Fix A ⭐ Recommended**: Body `UPDATE` that does not set `confirmed_calories` (mirror `replaceConfirmedCalories`).
  - Strength: Same last-write-wins contract the calorie path already uses.
  - Tradeoff: Two write styles on `UserProfileRepository`; first-save still uses entity `save`.
  - Confidence: HIGH — the calorie path already proved the pattern.
  - Blind spot: Hibernate dirty flush if a later change loads the same entity in one persistence context.
- **Fix B**: Accept last-write-wins for MVP (one account, one browser).
  - Strength: No extra repository methods.
  - Tradeoff: Two-tab overwrite stays.
  - Confidence: MEDIUM — acceptable for one-person MVP, easy to forget later.
  - Blind spot: Unverified how often a user would save profile and calories at once.
- **Decision**: FIXED via Fix A

### F4 — Progress manual titles still say ±500 after living docs moved to −250

- **Severity**: ℹ OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Success Criteria
- **Location**: context/changes/calorie-formula/plan.md:211
- **Detail**: Rows 2.2–2.4 are `[x]` with SHA 68e8644. Their titles still name −500 / +500. AGENTS Hard rules, PRD Business Logic / Open Question 1, and roadmap F-03 actually record −250 / +250. Automated gates passed (`CalorieServiceTest` and `.\gradlew.bat test`).
- **Fix**: Mention in the F1 addendum that those Progress titles are historical and must not be renamed.
- **Decision**: FIXED via F1 addendum

### F5 — GET /profile selects the same row twice

- **Severity**: ℹ OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Architecture
- **Location**: src/main/java/com/kenez92/plateplan/profile/controller/ProfileController.java:53
- **Detail**: `show()` calls `profileService.load` then `confirmedCaloriesService.load`; both `findById(login)` with `open-in-view=false`. Cost of keeping profile and calories independent. Fine at one row per account.
- **Fix**: Leave it; do not fold calories back into `ProfileFormDto`.
- **Decision**: FIXED (left as two loads)

### F6 — Formula persist is not bounded by 800–6000

- **Severity**: ℹ OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/profile/service/CalorieService.java:41
- **Detail**: `DailyCaloriesForm` refuses 800–6000. First body save and recalculate store the raw formula, including the locked negative −263. The original plan forbade a floor. The two writers disagree on range by design unless FR-005 is read as applying to every stored target.
- **Fix**: Leave the formula unbounded unless S-03 explicitly wants a stored-target clamp.
- **Decision**: FIXED (left unbounded; clamp stays on the client edit form only)
