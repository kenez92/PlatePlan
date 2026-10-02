# Save User Profile — Plan Brief

> Full plan: `context/changes/save-user-profile/plan.md`

## What & Why

A signed-in user keeps their whole profile on one screen, `/profile`: age, height, weight, sex, goal, activity level, preferred products, and excluded products. One form, one request, saved on the account so a later visit starts with it filled in (US-01, US-02, FR-003). This is roadmap S-02 widened to all of FR-003; the change was renamed from `save-food-preferences` for that reason. Nothing here calculates calories; S-03 builds the calculation on this data.

## Starting Point

Registration and sign-in work (S-01), and every path except the public ones is already behind login, so a new screen is protected by default. The only table is `account`. There is no profile data, no form for it, and no table that could hold it.

## Desired End State

A "Profil" link in the header opens one form with six body fields and two product fields (products separated by `;`) and a single "Zapisz profil" button. A save either stores everything or, if any field is wrong, stores nothing and shows each problem next to its field with everything typed still in place. The profile persists across logins and belongs to the signed-in account only. If the database is down, the page says so and never returns a 500.

## Key Decisions Made

| Decision | Choice | Why (1 sentence) | Source |
| --- | --- | --- | --- |
| Scope | Whole FR-003 on one screen, one form, one POST | The user wants one request; S-03 shrinks to the calculation and accept-or-edit. | Plan |
| Naming | Table `user_profile`, entity `UserProfile`, package `profile`, screen `/profile`, change `save-user-profile` | "Profile" is the word the repository already uses for this data. | Plan |
| Body fields | All six required; age 10–110, height 80–250 cm, weight 20.0–400.0 kg with one decimal, `,` or `.` | Required keeps the row complete; wide ranges avoid blocking real people. | Plan |
| Choices | Sex `MALE`/`FEMALE`; goal `LOSE_WEIGHT`/`MAINTAIN`/`GAIN`; activity four levels | They are the values the PRD and the formula already name. | Plan |
| Key | `login` column, unique index on `upper(login)`, no foreign key | `account.username` has no plain unique constraint, so a foreign key is not possible. | Plan |
| Products | Optional lists, text joined with `;`, name 2–50 characters, at most 50 per list | A comma can be part of a name, a semicolon cannot. | Plan |
| Same product | Equal after strip, NFC, collapsed spaces, and lower-casing | "Mleko" and "mleko " are one product. | Plan |
| Repeat or conflict | Refuse, never merge or move | Conflicting data never reaches the model. | Plan |
| Save semantics | All or nothing, every problem shown together, one per field | The stored state is always predictable. | Plan |
| Row creation | On the first successful save; every column `not null` | Registration stays untouched and the row is never half-filled. | Plan |
| Concurrency | Last write wins | One person, one account; no version column needed. | Plan |
| Enums | `Sex`, `Goal`, `ActivityLevel` created here, names only | F-03 reuses them and adds the multipliers beside them. | Plan |

## Scope

**In scope:** migration `003`, entity and repository, the parser and service with every rule, the profile screen and its form, the header link, styles, tests, and `AGENTS.md`. The roadmap was already brought to the new scope during planning.

**Out of scope:** the calorie calculation, the goal-adjustment size, `confirmed_calories` (S-03); using the products in a prompt (S-04); per-product add and remove buttons; redirecting to `/profile` after registration; sorting; diacritic folding; optimistic locking; JavaScript; any new library.

## Architecture / Approach

A new package `profile` holds the entity, the repository, the enums, the text format and name rules, the parser, the result types, and a `ProfileService` that is the only code touching the table. `ProfileController` in `controller` takes the login from `Principal.getName()` only and routes the result to a view or a redirect. The parser turns the typed strings into a valid profile or a list of problems without touching the database. The service catches database exceptions inside its methods, returns a value, and logs only class names, the way `RegistrationService` does. `ProfileInput.toString()` is redacted.

## Phases at a Glance

| Phase | What it delivers | Key risk |
| --- | --- | --- |
| 1. Storage and rules | Migration, entity, repository, parser, service, unit tests | A boundary or rule (range, repeat, conflict, limit) behaves differently than decided |
| 2. The profile screen | Controller, form view, header link, styles, slice tests, manual walk-through | Typed values reaching the page unescaped, or another account's data being readable |
| 3. Documentation | `AGENTS.md` updated | The next change plans a second table instead of extending `user_profile` |

**Prerequisites:** S-01 done (it is); a reachable PostgreSQL for the manual checks.
**Estimated effort:** about 3–4 sessions; Phase 1 is the largest.

## Open Risks & Assumptions

- Age 10–110 lets a minor save a profile, while the calorie formula is meant for adults; S-03 may narrow the range.
- F-03 must reuse `Sex` and `ActivityLevel` from `profile` and not define a second set; whichever lands first owns them.
- Two browser tabs on the same account can lose one change (last write wins, accepted), and a simultaneous first save shows the retry message.
- There is no foreign key, so a row could in theory outlive its account; accounts cannot be deleted or renamed today.
- A product name cannot contain `;`; "mleko; 3,2%" is two products. The hint under the field says so.

## Success Criteria (Summary)

- A signed-in user fills in the whole profile once, saves it in one request, and finds it filled in after logging out and in.
- A wrong field, a repeated product, or a product on both lists is refused with a message next to the field, nothing is saved, and everything typed stays.
- A signed-out visitor, or another account, can never read or change someone's profile, and `.\gradlew.bat test` is green.
