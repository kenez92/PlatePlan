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

One Gradle module (`@settings.gradle.kts`, `@build.gradle.kts`). Put new classes in `com.kenez92.plateplan`, beside `@src/main/java/com/kenez92/plateplan/Application.java`. A feature that is more than a few types uses the package split under **Packages**. Test conventions: `@.cursor/rules/testing.mdc` (local only, not in git).

`@src/main/resources/application.properties` sets `spring.application.name` and exposes only the Actuator `health` and `info` endpoints (`management.endpoints.web.exposure.include=health,info`); both answer without sign-in. Heap dump and shutdown stay closed (`access=none`). Every other path is deny-by-default behind form login (`@src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java`), and a new public path must be added there on purpose. There is no `.env`. The session cookie is `Secure` by default (`SESSION_COOKIE_SECURE`, default `true`); for a local run over plain http set `SESSION_COOKIE_SECURE=false`, otherwise register and login posts fail with 403 (the CSRF token is lost with the session). The Fly image sets `SERVER_ADDRESS=0.0.0.0` and `SERVER_PORT=8080` in `@Dockerfile` and `@fly.toml`.

The `DataSource` is created in `@src/main/java/com/kenez92/plateplan/config/DataSourceConfiguration.java`, with `DataSourceAutoConfiguration` excluded in `Application`. `application.properties` reads `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` from the environment (Fly secrets in production). No value is written in the repository and there is no default, so a missing variable stops the start; for a local run without a database, export a dummy unreachable URL such as `jdbc:postgresql://127.0.0.1:1/plateplan` plus any user and password. The application starts when the database is unreachable. Hibernate runs with `ddl-auto=none` and never generates schema. Schema changes go only as Liquibase XML changelogs under `@src/main/resources/db/changelog/`, included from `db.changelog-master.xml`; never edit an applied changeSet. `db.changelog-master.xml` includes every XML file in `@src/main/resources/db/changelog/changes/` with `<includeAll>` (alphabetical order, so prefix new files with a number; never rename or move an applied file); each file holds its whole changeSet, written with Liquibase change types such as `createTable`, not raw SQL. The changes so far are the `SELECT 1` smoke test (`changes/001-test.xml`), the `account` table with its case-insensitive unique login index (`changes/002-create-account.xml`), and the `user_profile` table keyed by `login` (`changes/003-create-user-profile.xml`). The Liquibase bean is `config/LiquibaseConfiguration` (a `SpringLiquibase` subclass); a failed migration at start is logged and skipped until the next start.

## Packages

Name the package after what the class *is*, not after who calls it. `profile` is the pattern for a new feature. `account` stays a flat package until it is split on purpose.

- `controller` — only `@Controller` classes.
- `controller.dto` — the inbound form (`ProfileFormDto`). A form is not a model and not a controller.
- `model` — public data: enums, value objects, results (`Sex`, `ProductLists`, `ProfileDetails`, `ProfileResult`). No formatters, no validators, no services.
- `db` — the JPA entity and its `@Repository`.
- `service` — only the application `@Service`.
- `validator` — a Spring `Validator` or a check that refuses a value (`ProductListsValidator`, `ProductNameValidator`). The rules live in that class; do not extract a `*Rules` helper beside it.
- `format` — encode and decode of a stored representation (`ProductListFormat`).

Do not put a class in `service` or `validator` because those classes use it. Nested packages such as `db.model` do not share `package-private` access in Java; do not nest for encapsulation. Do not invent a package for one class that does not fit; put the behavior on the class that *is* that thing. Collaborators are Spring beans with instance methods and constructor injection. Do not construct them with `new` in production. A class of only static methods is a `*Util` in `utils`; otherwise it is a bean. A type injected from another package is `public`.

## Style

Java 21 is the toolchain in `@build.gradle.kts`. Indent with four spaces and format as IntelliJ does, as in `@src/main/java/com/kenez92/plateplan/config/LiquibaseConfiguration.java` (constructor parameters aligned under the first one, no blank line before the closing brace). Older files such as `Application.java` still use tabs; reformat a file only when you change it. Every method and constructor parameter is `final`, and every Spring Data repository interface carries `@Repository`. Checkstyle, Spotless, and `.editorconfig` are absent. There is no coverage gate.

## Commits and pull requests

No prefix is set — do not add `feat:` or `fix:` on your own. Put each change on a feature branch and open a pull request into `main`. `@.github/workflows/ci.yml` runs `./gradlew test` on that pull request. Merging the pull request into `main` deploys the Fly app `plate-plan`.

## Account flow

The start screen is a login window with registration. Registration logs the user in; the next visit requires login (FR-002 in `@context/foundation/prd.md`). Registration validates the login (3–50 characters, stripped and NFC-normalized; control, format and non-ordinary space characters are refused) and the password (at least 8 characters, at most 72 bytes), reports a taken login explicitly, and signs the new account in by saving the security context, changing the session id and dropping the CSRF token (the principal keeps no password hash); sign-out is `POST /logout` back to `/`. Login attempts are not throttled.

## Profile

A signed-in account opens `/profile` from the "Profil" header link: one form, one POST. The row is `user_profile`, primary key `login` exactly as `Principal.getName()` returns it, with no foreign key and no surrogate id. The first save creates the row; later saves replace every value (last write wins). Age is a whole number 10–110, height 80–250 cm, weight 20.0–400.0 kg with one decimal place. Sex is `MALE` or `FEMALE`, goal is `LOSE_WEIGHT`, `MAINTAIN`, or `GAIN`, activity is `SEDENTARY`, `LIGHT`, `MODERATE`, or `HIGH`. All six body fields are required; a refused save writes nothing. Preferred and excluded products are each a list of names (empty is allowed and stored as null). The form sends `List<String>`; a name is 2–100 characters already stripped and NFC, with no `;`, control, format, or non-ordinary space. Repeats are dropped (first spelling kept, compared case-insensitively). A list may have at most 50 names. A name on both lists is refused on the excluded field. The stored column joins names with `;`. S-03 adds `confirmed_calories` to this table. With the database down the screen says so and never returns a 500.
