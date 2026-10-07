# Calorie invariants and refused-save — Plan Brief

> Full plan: `context/changes/testing-integration-hotspots/plan.md`
> Research: `context/changes/testing-integration-hotspots/research.md`

## What & Why

Rollout Phase 2 must protect calorie write-path invariants (risk #3) and refused-save (risk #4). Research shows most of the matrix exists; remaining cheap signal is preferences isolation, `never().replaceBodyAndProducts`, body POST not calling calorie services, and MVC `never().save` on illegal/cross-list products.

## Starting Point

`CalorieServiceTest` locks 2767 from the PRD fixture. Profile/calorie service tests cover first save, later keep, update, recalculate. Validators cover cross-list in unit tests. MVC `never().save` exists only for age.

## Desired End State

Product lists cannot change stored formula calories. Calorie writes cannot replace body columns. Refused product POSTs never persist. Cookbook §6.1 / §6.5 describe those patterns.

## Key Decisions Made

| Decision | Choice | Why | Source |
| ------------------------------ | ----------------- | ----------------- | ---------------- |
| Coverage posture | Gap-fill | Write paths and validators already exist | Research |
| Gold 2767 | PRD arithmetic on 34/180/82.5/MALE/MODERATE | Avoid calculator oracle | Research / Plan |
| Layer | Existing unit + `@WebMvcTest` | No `@SpringBootTest` | Research |
| Recalculate CSRF | Skip (Phase 3) | Risk #6 | Research |

## Scope

**In scope:** three calorie-path assertions; two refused-save MVC locks; cookbook §6.1 / §6.5.

**Out of scope:** new formula unit methods, real DB, Playwright, production code.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --------- | ----------------------- | ------------------------- |
| 1. Calorie write-path locks | Preferences / column / service isolation | Wrong kcal or corrupted columns |
| 2. Refused-save MVC locks | `never().save` on illegal name and cross-list | Persist on refuse |
| 3. Cookbook §6.1 / §6.5 | Recipes for the next unit/validation test | Next agent copies `CalorieService` as oracle |

**Prerequisites:** research.md. **Estimated effort:** one session.
