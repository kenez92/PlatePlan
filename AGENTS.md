# Repository Guidelines

PlatePlan calculates daily calories and returns a next-day diet plan plus a shopping list as two downloadable PDFs. Requirements: `@context/foundation/prd.md`. Stack: `@context/foundation/tech-stack.md`. Build: `@build.gradle.kts`.

## Hard rules

- Generate the diet plan and shopping list through Spring AI to Ollama Cloud (`OLLAMA_API_KEY`) and return them for download as two PDF fields (`dietPdf`, `shoppingListPdf`) on `POST /plan/generate`. Do not persist either file. Do not call a local Ollama on the Fly Machine and do not ship a ZIP.
- Store age, height, weight, sex, goal, confirmed calories, and product preferences on the account. One account, no roles, own data only. The profile stays between visits.
- Preferences do not change calories. The daily number is BMR times activity, then −250 (`LOSE_WEIGHT`), 0 (`MAINTAIN`), or +250 (`GAIN`), rounded half-up to a whole kilocalorie. They apply only inside the plan. The user may edit the number.
- Leave open (`@context/foundation/prd.md`): email instead of PDF. That option does not replace the two download buttons. Out of MVP: multiple people, payments, realtime, background jobs.
- Add a library only when `@context/foundation/tech-stack.md` lists it, and declare it in `@build.gradle.kts`.
- Keep secrets out of git (`@.gitignore`). Do not log, commit, or return age, height, weight, sex, goal, or product preferences except for the signed-in account that owns them.
- Edit `context/foundation/` in place (`@context/foundation/README.md`). Record a change in `context/changes/<change-id>/change.md` (`@context/changes/README.md`). `context/archive/` is read-only (`@context/archive/README.md`).

## Commands

- `./gradlew test` — the suite CI runs. On Windows: `.\gradlew.bat test`.
- `./gradlew test --tests com.kenez92.plateplan.ApplicationTest` — one class.
- `./gradlew bootRun` — local server. On Windows: `.\gradlew.bat bootRun`.

## Layout

One Gradle module (`@settings.gradle.kts`, `@build.gradle.kts`). Put new classes in `com.kenez92.plateplan`, beside `@src/main/java/com/kenez92/plateplan/Application.java`. A feature that is more than a few types uses the package split under **Packages**. Test conventions: `@.cursor/rules/testing.mdc` (local only, not in git).

`@src/main/resources/application.properties` sets `spring.application.name` and exposes only the Actuator `health` and `info` endpoints (`management.endpoints.web.exposure.include=health,info`); both answer without sign-in. Heap dump and shutdown stay closed (`access=none`). Every other path is deny-by-default behind form login (`@src/main/java/com/kenez92/plateplan/config/SecurityConfiguration.java`), and a new public path must be added there on purpose. There is no `.env`. The session cookie is `Secure` by default (`SESSION_COOKIE_SECURE`, default `true`); for a local run over plain http set `SESSION_COOKIE_SECURE=false`, otherwise register and login posts fail with 403 (the CSRF token is lost with the session). `OLLAMA_API_KEY` is a Fly secret (`fly secrets`), never `fly.toml`; the app starts without it and generation then returns unavailable. The Fly image sets `SERVER_ADDRESS=0.0.0.0` and `SERVER_PORT=8080` in `@Dockerfile` and `@fly.toml`.

The `DataSource` is created in `@src/main/java/com/kenez92/plateplan/config/DataSourceConfiguration.java`, with `DataSourceAutoConfiguration` excluded in `Application`. `application.properties` reads `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` from the environment (Fly secrets in production). No value is written in the repository and there is no default, so a missing variable stops the start; for a local run without a database, export a dummy unreachable URL such as `jdbc:postgresql://127.0.0.1:1/plateplan` plus any user and password. The application starts when the database is unreachable. Hibernate runs with `ddl-auto=none` and never generates schema. Schema changes go only as Liquibase XML changelogs under `@src/main/resources/db/changelog/`, included from `db.changelog-master.xml`; never edit an applied changeSet. `db.changelog-master.xml` includes every XML file in `@src/main/resources/db/changelog/changes/` with `<includeAll>` (alphabetical order, so prefix new files with a number; never rename or move an applied file); each file holds its whole changeSet, written with Liquibase change types such as `createTable`, not raw SQL. The changes so far are the `SELECT 1` smoke test (`changes/001-test.xml`), the `account` table with its case-insensitive unique login index (`changes/002-create-account.xml`), the `user_profile` table keyed by `login` (`changes/003-create-user-profile.xml`), and nullable `confirmed_calories` on that table (`changes/004-add-confirmed-calories.xml`). The Liquibase bean is `config/LiquibaseConfiguration` (a `SpringLiquibase` subclass); a failed migration at start is logged and skipped until the next start.

## Packages

Name the package after what the class *is*, not after who calls it. `profile` is the pattern for a new feature; `account` and `home` follow the same split.

- `controller` — only `@Controller` classes (`HomeController`, `RegisterController`, `ProfileController`, `PlanController`). The shared header login is `CurrentAccountAdvice` in `home.controller`.
- `controller.dto` — the inbound form (`RegistrationForm`, `ProfileFormDto`, `DailyCaloriesForm`) and the outgoing generate body (`PlanFilesDto`). A form is not a model and not a controller.
- `model` — public data: enums, value objects, results (`RegistrationError`, `RegistrationResult`, `Sex`, `ProductLists`, `ProfileDetails`, `ProfileResult`, `ConfirmedCaloriesResult`, `Meal`, `DietPlan`, `PlanResult`). No formatters, no validators, no services.
- `db` — the JPA entity and its `@Repository`.
- `service` — only the application `@Service` (`ProfileService`, `CalorieService`, `ConfirmedCaloriesService`, `DietGenerator`, `PlanService`, `PlanPdfWriter`).
- `validator` — a Spring `Validator` or a check that refuses a value (`RegistrationValidator`, `ProductListsValidator`, `ProductNameValidator`). The rules live in that class; do not extract a `*Rules` helper beside it.
- `format` — encode and decode of a stored representation (`LoginNormalizer`, `ProductListFormat`).

Do not put a class in `service` or `validator` because those classes use it. Nested packages such as `db.model` do not share `package-private` access in Java; do not nest for encapsulation. Do not invent a package for one class that does not fit; put the behavior on the class that *is* that thing. Collaborators are Spring beans with instance methods and constructor injection. Do not construct them with `new` in production. A class of only static methods is a `*Util` in `utils`; otherwise it is a bean. A type injected from another package is `public`.

## Style

Java 21 is the toolchain in `@build.gradle.kts`. Indent with four spaces and format as IntelliJ does, as in `@src/main/java/com/kenez92/plateplan/config/LiquibaseConfiguration.java` (constructor parameters aligned under the first one, no blank line before the closing brace). Older files such as `Application.java` still use tabs; reformat a file only when you change it. Every method and constructor parameter is `final`, and every Spring Data repository interface carries `@Repository`. Do not assign a local variable when the value is used once: return or pass the expression. Checkstyle, Spotless, and `.editorconfig` are absent. There is no coverage gate.

## Commits and pull requests

No prefix is set — do not add `feat:` or `fix:` on your own. Put each change on a feature branch and open a pull request into `main`. `@.github/workflows/ci.yml` runs `./gradlew test` on that pull request. Merging the pull request into `main` deploys the Fly app `plate-plan`.

## Account flow

The start screen is a login window with registration. Registration logs the user in; the next visit requires login (FR-002 in `@context/foundation/prd.md`). Registration validates the login (3–50 characters, stripped and NFC-normalized; control, format and non-ordinary space characters are refused) and the password (at least 8 characters, at most 72 bytes), reports a taken login explicitly, and signs the new account in by saving the security context, changing the session id and dropping the CSRF token (the principal keeps no password hash); sign-out is `POST /logout` back to `/`. Login attempts are not throttled.

## Profile

A signed-in account opens `/profile` from the "Profil" header link. Save is `POST /profile` (body fields and product lists) through `ProfileService`. The client edits the daily calorie target on `POST /profile/calories`, which accepts only `dailyCalories`. Recalculate is `POST /profile/recalculate` and sends no calorie number. Both calorie writes go through `ConfirmedCaloriesService` and update only `confirmed_calories`. GET fills `profileForm` from `ProfileService` and `calorieForm` from `ConfirmedCaloriesService`; `ProfileFormDto` has no calorie field. The row is `user_profile`, primary key `login` exactly as `Principal.getName()` returns it, with no foreign key and no surrogate id. The first save creates the row; later body saves replace every body and product value and keep `confirmed_calories` (last write wins). Age is a whole number 10–110, height 80–250 cm, weight 20.0–400.0 kg with one decimal place. Sex is `MALE` or `FEMALE`, goal is `LOSE_WEIGHT`, `MAINTAIN`, or `GAIN`, activity is `SEDENTARY`, `LIGHT`, `MODERATE`, or `HIGH`. All six body fields are required; a refused save writes nothing. The first body save stores the formula from `profile.service.CalorieService` (BMR × activity, then −250 / 0 / +250, rounded half-up to a whole kilocalorie) as `confirmed_calories`. A later body save does not take a calorie number from the client. `POST /profile/calories` stores a whole number 800–6000 and writes only that column; it requires an existing profile row. Recalculate overwrites `confirmed_calories` with the formula from the stored body and does not change other columns. Preferred and excluded products are each a list of names (empty is allowed and stored as null). The form sends `List<String>`; a name is 2–100 characters already stripped and NFC, with no `;`, control, format, or non-ordinary space. Repeats are dropped (first spelling kept, compared case-insensitively). A list may have at most 50 names. A name on both lists is refused on the excluded field. The stored column joins names with `;`. With the database down the screen says so and never returns a 500.

## Plan

A signed-in account opens `/plan` from the **Plan** header link (next to Profil) or from the profile page. `GET /plan` is the screen. `POST /plan/generate` needs CSRF (`X-CSRF-TOKEN`) and always answers HTTP 200 JSON: success is `{"dietPdf":"<base64>","shoppingListPdf":"<base64>"}`; failure is only `error` as `PROFILE_REQUIRED`, `CALORIES_REQUIRED`, or `UNAVAILABLE` (missing key, timeout, model, database, or PDF write). The page shows **Pobierz plan** (`dieta-na-jutro.pdf`) and **Pobierz listę zakupów** (`lista-zakupow.pdf`). Files stay in the browser; refresh drops them. Generation needs a `user_profile` row and non-null `confirmed_calories`. The prompt is English; the diet text is Polish. Four meals: breakfast, second breakfast (a ready-made shop item), lunch, dinner. Only preferred products, excluded products, and the confirmed calorie number go to the model — not age, height, weight, sex, goal, activity, or login. Do not log the API key, the prompt, product lists, calories, or PDF bytes.
