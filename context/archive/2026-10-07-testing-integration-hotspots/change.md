---
change_id: testing-integration-hotspots
title: Calorie invariants and refused-save coverage
status: archived
created: 2026-10-07
updated: 2026-10-07
archived_at: 2026-10-07T08:33:14Z
---

## Notes

Open a change folder for rollout Phase 2 of context/foundation/test-plan.md: "Integration around hot-spots".
Risks covered: #3 (the calorie number is wrong, preferences change it, or edit/recalculate corrupts other columns), #4 (a refused profile save still writes a row; a name on both lists is accepted).
Test types planned: unit + integration.
Risk response intent:
- #3: prove first body save stores the formula; a later body save does not take calories from the client; POST .../calories writes only that column (800–6000); recalculate overwrites from the stored body; preferences do not change the number. Challenge that a unit test of the formula equals the whole calorie write path. Avoid oracle copied from the calculator implementation.
- #4: prove a refusal writes nothing; a name on both lists fails on the excluded field; an illegal name fails on the server. Challenge that DTO validation is enough — the client is trusted. Avoid annotation-only DTO tests.
After creating the folder, follow the downstream continuation rule.
