---
change_id: testing-quality-gates
title: Wire Playwright in CI and tech-stack
status: implemented
created: 2026-10-07
updated: 2026-10-07
archived_at: null
---

## Notes

Open a change folder for rollout Phase 5 of context/foundation/test-plan.md: "Quality-gates wiring".
Risks covered: cross-cutting (the Phase 4 Playwright test must run on PR; Playwright must be declared on the stack).
Test types planned: gates.
Risk response intent:
- Wire the one Playwright test in CI (`./gradlew test` already includes it; CI must install Chromium and OS deps).
- Fill remaining cookbook notes for gates.
- Add Playwright Java to context/foundation/tech-stack.md so a later library addition follows AGENTS.md.
Do not invent extra gates (lint, coverage, agent hooks).
After creating the folder, follow the downstream continuation rule.
