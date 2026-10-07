---
change_id: testing-persist-leak-csrf
title: Persist, leak and CSRF coverage
status: implemented
created: 2026-10-07
updated: 2026-10-07
archived_at: null
---

## Notes

Open a change folder for rollout Phase 3 of context/foundation/test-plan.md: "Persistence, leak and session contract".
Risks covered: #5 (the plan or shopping list is stored, or a log/response leaks PII, the prompt, or the Ollama key), #6 (a POST without CSRF succeeds on register/login/logout/generate, or a legitimate POST loses the session and gets 403).
Test types planned: integration.
Risk response intent:
- #5: prove generate does not persist files; logs and errors do not carry the key, prompt, lists, calories, or PDF bytes; the model does not receive age/height/weight/sex/activity/login. Challenge that no plan table means nothing is stored. Avoid snapshot of the full prompt; logging the prompt in the test.
- #6: prove form POST without CSRF → 403 on register/login/logout; with a token it succeeds. CSRF on the generate fetch header waits for the Playwright phase. Challenge that CSRF on one controller means CSRF everywhere. Avoid treating local SESSION_COOKIE_SECURE=true as a product bug; e2e for form CSRF.
After creating the folder, follow the downstream continuation rule.
