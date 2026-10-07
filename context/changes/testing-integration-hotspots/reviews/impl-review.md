<!-- IMPL-REVIEW-REPORT -->
# Implementation Review: Calorie invariants and refused-save coverage

- **Plan**: `context/changes/testing-integration-hotspots/plan.md`
- **Scope**: Full plan
- **Reviewed phases**: 1, 2, 3
- **Date**: 2026-10-07
- **Verdict**: APPROVED
- **Findings**: 0 critical 0 warnings 0 observations

## Verdicts

| Dimension | Verdict |
|-----------|---------|
| Plan Adherence | PASS |
| Scope Discipline | PASS |
| Safety & Quality | PASS |
| Architecture | PASS |
| Pattern Consistency | PASS |
| Success Criteria | PASS |

## Findings

None. Gap-fill matches the plan: 2767 is written in expected rows (no `CalorieService` call in `shouldStoreTheSameFormulaCaloriesWhenProductListsDiffer`); calorie writes `never().replaceBodyAndProducts`; body POST `never().update` / `never().recalculate`; illegal-name and cross-list MVC `never().save`; cookbook §6.1 / §6.5 filled, §6.3 still TBD. No production code.

## Success criteria

Targeted suites passed (`eb07105`). Manual checks: PRD fixture oracle; cross-list test uses imported `ProductListsValidator`.

## Decision

No triage. Overall APPROVED.
