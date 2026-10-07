---
change_id: testing-browser-critical-path
title: Browser US-01 PDF downloads
status: archived
created: 2026-10-07
updated: 2026-10-07
archived_at: 2026-10-07T10:00:00Z
---

## Notes

Open a change folder for rollout Phase 4 of context/foundation/test-plan.md: "Browser critical path".
Risks covered: #1 (two named PDF downloads after generate), #6 (CSRF on the generate fetch header).
Test types planned: e2e (Playwright Java).
Risk response intent:
- #1: prove US-01 in a browser: generate, two named PDFs (dieta-na-jutro.pdf, lista-zakupow.pdf), refresh drops the files. Do not move the JSON error matrix to Playwright.
- #6: prove the generate fetch sends the CSRF header from the page. Form CSRF stays on MockMvc.
Stack: Playwright Java + JUnit, @SpringBootTest(RANDOM_PORT). Do not use Node @playwright/test. Do not call live Ollama.
After creating the folder, follow the downstream continuation rule.
