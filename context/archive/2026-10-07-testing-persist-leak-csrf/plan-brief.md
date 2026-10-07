# Persist, leak and CSRF — Plan Brief

> Full plan: `context/changes/testing-persist-leak-csrf/plan.md`
> Research: `context/changes/testing-persist-leak-csrf/research.md`

## What & Why

Rollout Phase 3 must protect no-persist / no-leak (risk #5) and form CSRF (risk #6). Research shows the prompt contract and six CSRF-less 403s already exist; remaining cheap signal is generate `never().save`, DietGenerator class-name logs, and recalculate CSRF.

## Starting Point

`DietGeneratorTest` omits personal data from the prompt. CSRF-less 403 covers every form POST except `/profile/recalculate`. Plan package has no ListAppender leak test.

## Desired End State

Generate cannot write `user_profile`. Failure logs omit planted secrets. Recalculate without CSRF is 403. Cookbook §6.6 names those locks.

## Key Decisions Made

| Decision | Choice | Why | Source |
| ------------------------------ | ----------------- | ----------------- | ---------------- |
| Coverage posture | Gap-fill | Prompt and most CSRF already exist | Research |
| Recalculate CSRF | One MockMvc 403 | Only missing form | Research |
| Generate fetch CSRF | Skip (Phase 4) | Browser-only | Test-plan |
| Prompt snapshot | Forbidden | Brittle anti-pattern | Research |

## Scope

**In scope:** generate no-write; DietGenerator log test; prompt omit `MODERATE`; recalculate CSRF; §6.6.

**Out of scope:** Playwright, production, §6.3, Secure-cookie tests.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --------- | ----------------------- | ------------------------- |
| 1. Persist and leak locks | No-write + class-name logs | Stored PDFs or leaked key |
| 2. Recalculate CSRF lock | 403 without token | CSRF-less calorie overwrite |
| 3. Cookbook §6.6 | Phase note | Next agent skips recalculate |

**Prerequisites:** research.md. **Estimated effort:** one session.
