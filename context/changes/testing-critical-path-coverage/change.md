---
change_id: testing-critical-path-coverage
title: Critical-path coverage for generate errors and own-data
status: implemented
created: 2026-10-07
updated: 2026-10-07
archived_at: null
---

## Notes

Open a change folder for rollout Phase 1 of context/foundation/test-plan.md: "Critical-path coverage".
Risks covered: #1 (after calories are confirmed the user does not get two PDFs, or gets a 500 instead of JSON with only error), #2 (a signed-in account reads or generates from another login's data).
Test types planned: unit + integration.
Risk response intent:
- #1: prove signed-in generate with a profile and calories returns two PDF fields and no error; missing preconditions return only PROFILE_REQUIRED / CALORIES_REQUIRED / UNAVAILABLE at HTTP 200; anonymous and CSRF-less POST do not return PDFs. Challenge that JSON with two fields means download works. Avoid Playwright on the JSON contract and an assertion copied from the DTO without the error matrix.
- #2: prove the request cannot choose whose row to read and that another login never appears in profile or generate. Challenge that signed-in means they see their own data. Avoid happy-path-only @WithMockUser.
After creating the folder, follow the downstream continuation rule.
