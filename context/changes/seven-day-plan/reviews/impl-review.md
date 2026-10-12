<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Plan na siedem dni

- **Plan**: context/changes/seven-day-plan/plan.md
- **Scope**: Full plan
- **Reviewed phases**: 1, 2, 3, 4
- **Date**: 2026-10-12
- **Verdict**: APPROVED
- **Findings**: 0 critical, 1 warning, 2 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | WARNING |
| Scope Discipline | PASS |
| Safety & Quality | WARNING |
| Architecture | PASS |
| Pattern Consistency | PASS |
| Success Criteria | PASS |

## Findings

### F1 — Kilocalorie sum can wrap past the band

- **Severity**: ⚠️ WARNING
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Safety & Quality
- **Location**: src/main/java/com/kenez92/plateplan/plan/validator/WeekPlanValidator.java:56
- **Detail**: `caloriesInBand` adds four `Meal.kcal` values with `int` arithmetic. A sum past `Integer.MAX_VALUE` wraps. Meals of 2000000000, 2000000000, 294967296, and 2000 have a true total of 4294969296 but an `int` total of 2000, so a 2000 kcal target accepts the day. Jackson will bind any kcal that fits in an `int`. Normal model output is a few thousand kcal and is unaffected.
- **Fix**: Sum the four values in `long` and compare that total to the band.
- **Decision**: DISMISSED — daily target stays in the human range (the user: a normal day does not exceed 4000 kcal, so an int wrap is not a real path)

### F2 — Shopping-list PDF is not given the daily target

- **Severity**: 💡 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Plan Adherence
- **Location**: src/main/java/com/kenez92/plateplan/plan/controller/PlanController.java:80
- **Detail**: Phase 2 says the controller passes the week and the same daily calorie target to both PDFs. `dietPdf` receives `result.dailyCalories()`. `shoppingListPdf` receives only the `WeekPlan`. The shopping PDF prints `Lista zakupów` and the week bullets. It does not print the calorie target, so the missing argument does not change the file.
- **Fix**: Keep `shoppingListPdf(WeekPlan)`. The list does not display the daily target, so an unused parameter would not change the PDF.
- **Decision**: FIXED — kept shoppingListPdf(WeekPlan); the user confirmed the daily target is not needed on the shopping list

### F3 — Written plan still describes a dated day title

- **Severity**: 💡 OBSERVATION
- **Impact**: 🏃 LOW — quick decision; fix is obvious and narrowly scoped
- **Dimension**: Plan Adherence
- **Location**: context/changes/seven-day-plan/plan.md:17
- **Detail**: The plan title is `Dzień N · d.MM.yyyy`, and it says not to squeeze a day onto exactly seven pages. After manual review the title is `Dzień %d` with no date (`PlanPdfWriter.java:37`). Line spacing is tighter so a day with a short temperature-and-time preparation fits on one page. Text is not cut. A very long day still continues on a later page, and a trailing spacer no longer opens a blank page. The prompt also asks for temperature, time, and the vessel in two or three short sentences. Those three changes were requested after the plan was written and are locked by tests. The plan text was not updated.
- **Fix**: Add a short note to the plan that the day title is `Dzień N` without a date, and that a normal day is fitted to one page without cutting text.
- **Decision**: FIXED — note added under Desired End State in plan.md (2026-10-12)
