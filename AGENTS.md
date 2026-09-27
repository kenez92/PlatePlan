# Repository Guidelines

PlatePlan calculates daily calories and returns a next-day diet plan plus a shopping list as two downloadable PDFs. Requirements: `@context/foundation/prd.md`. Stack: `@context/foundation/tech-stack.md`. Build: `@build.gradle.kts`.

## Hard rules

- Generate the diet plan and shopping list and return them for download. Do not persist either file.
- Store age, height, weight, sex, goal, and product preferences on the account. One account, no roles, own data only. The profile stays between visits.
- Preferences do not change calories (BMR plus goal). They apply only inside the plan. Lose weight lowers the result, maintain leaves it, gain raises it. The user may edit the number.
- Leave open (`@context/foundation/prd.md`): goal-adjustment size, activity, and email instead of PDF. Out of MVP: multiple people, payments, realtime, background jobs.
- Add a library only when `@context/foundation/tech-stack.md` lists it, and declare it in `@build.gradle.kts`.
- Keep secrets out of git (`@.gitignore`). Do not log, commit, or return age, height, weight, sex, goal, or product preferences except for the signed-in account that owns them.
- Edit `context/foundation/` in place (`@context/foundation/README.md`). Record a change in `context/changes/<change-id>/change.md` (`@context/changes/README.md`). `context/archive/` is read-only (`@context/archive/README.md`).

## Commands

- `./gradlew test --tests com.kenez92.plateplan.ApplicationTest` — one class.

## Layout

One Gradle module (`@settings.gradle.kts`, `@build.gradle.kts`). Put new classes in `com.kenez92.plateplan`, beside `@src/main/java/com/kenez92/plateplan/Application.java`. Test conventions: `@.cursor/rules/testing.mdc` (local only, not in git).

`@src/main/resources/application.properties`. There is no `.env`.

## Style

Java 21 is the toolchain in `@build.gradle.kts`. Indent with tabs, as in `Application.java`. Checkstyle, Spotless, and `.editorconfig` are absent. There is no coverage gate.

## Commits and pull requests

No prefix is set — do not add `feat:` or `fix:` on your own. `.github/workflows/` is empty, so a pull request does not wait on CI.

## Account flow

The start screen is a login window with registration. Registration logs the user in; the next visit requires login (FR-002 in `@context/foundation/prd.md`).
