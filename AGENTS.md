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

- `./gradlew test` — the suite CI runs. On Windows: `.\gradlew.bat test`.
- `./gradlew test --tests com.kenez92.plateplan.ApplicationTest` — one class.
- `./gradlew bootRun` — local server. On Windows: `.\gradlew.bat bootRun`.

## Layout

One Gradle module (`@settings.gradle.kts`, `@build.gradle.kts`). Put new classes in `com.kenez92.plateplan`, beside `@src/main/java/com/kenez92/plateplan/Application.java`. Test conventions: `@.cursor/rules/testing.mdc` (local only, not in git).

`@src/main/resources/application.properties` sets `spring.application.name` and exposes every Actuator endpoint, with heap dump and shutdown unrestricted. There is no `.env`. The Fly image sets `SERVER_ADDRESS=0.0.0.0` and `SERVER_PORT=8080` in `@Dockerfile` and `@fly.toml`.

The `DataSource` is created in `@src/main/java/com/kenez92/plateplan/config/DataSourceConfiguration.java`, with `DataSourceAutoConfiguration` excluded in `Application`. `application.properties` reads `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` from the environment (Fly secrets in production). No value is written in the repository and there is no default, so a missing variable stops the start; for a local run without a database, export a dummy unreachable URL such as `jdbc:postgresql://127.0.0.1:1/plateplan` plus any user and password. The application starts when the database is unreachable. Hibernate runs with `ddl-auto=none` and never generates schema. Schema changes go only as Liquibase XML changelogs under `@src/main/resources/db/changelog/`, included from `db.changelog-master.xml`; never edit an applied changeSet. SQL scripts live in `@src/main/resources/db/scripts/` and are referenced with `<sqlFile>`; the only script so far is the `SELECT 1` smoke test (`001-select-1.xml`). The Liquibase bean is `config/LiquibaseConfiguration` (a `SpringLiquibase` subclass); a failed migration at start is logged and skipped until the next start.

## Style

Java 21 is the toolchain in `@build.gradle.kts`. Indent with tabs, as in `Application.java`. Checkstyle, Spotless, and `.editorconfig` are absent. There is no coverage gate.

## Commits and pull requests

No prefix is set — do not add `feat:` or `fix:` on your own. Put each change on a feature branch and open a pull request into `main`. `@.github/workflows/ci.yml` runs `./gradlew test` on that pull request. Merging the pull request into `main` deploys the Fly app `plate-plan`.

## Account flow

The start screen is a login window with registration. Registration logs the user in; the next visit requires login (FR-002 in `@context/foundation/prd.md`).
