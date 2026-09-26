# Repository Guidelines

PlatePlan (Java 21, Spring Boot 4.1.1, Gradle) calculates daily calories and returns a next-day diet plan plus a shopping list as two downloadable PDFs. Requirements: `@context/foundation/prd.md`. Stack: `@context/foundation/tech-stack.md`.

## Hard rules

- Generate the diet plan and shopping list and return them for download. Do not persist either file.
- Store age, height, weight, sex, goal, and product preferences on the account. One account, no roles, own data only. The profile stays between visits.
- Preferences do not change calories (BMR plus goal). They apply only inside the plan. Lose weight lowers the result, maintain leaves it, gain raises it. The user may edit the number.
- Leave open (`@context/foundation/prd.md`): goal-adjustment size, activity, and email instead of PDF. Out of MVP: multiple people, payments, realtime, background jobs.
- Thymeleaf, Spring Security, Spring AI, and Ollama are planned in `@context/foundation/tech-stack.md` and are not dependencies yet. Declare a dependency in `@build.gradle.kts` before you use it.
- `@.gitignore` ignores `*.json`, `.cursor/`, and `.agents/`. Keep secrets out of git. Account data must not leak.
- Edit `context/foundation/` in place (`@context/foundation/README.md`). Record a change in `context/changes/<change-id>/change.md` (`@context/changes/README.md`). `context/archive/` is read-only (`@context/archive/README.md`).

## Commands

- `./gradlew test` — JUnit. On Windows: `.\gradlew.bat test`.
- `./gradlew test --tests com.kenez92.plateplan.ApplicationTests` — one class.
- `./gradlew bootRun` — dev server.
- `./gradlew build` — compile and test.

## Layout

One Gradle module (`@settings.gradle.kts`, `@build.gradle.kts`). Put new classes in `com.kenez92.plateplan`, beside `@src/main/java/com/kenez92/plateplan/Application.java`. Mirror tests in `src/test/java/com/kenez92/plateplan/` with a `Tests` suffix. The only test is `contextLoads` in `@src/test/java/com/kenez92/plateplan/ApplicationTests.java`.

`@src/main/resources/application.properties` sets only `spring.application.name=PlatePlan`. There is no `.env`.

## Style

Java 21 is the toolchain in `@build.gradle.kts`. Indent with tabs, as in `Application.java`. Checkstyle, Spotless, and `.editorconfig` are absent. Tests use JUnit Platform and `@SpringBootTest`. There is no coverage gate.

## Commits and pull requests

History is the commit `Init`. No prefix is set — do not add `feat:` or `fix:` on your own. `.github/workflows/` is empty, so a pull request does not wait on CI.

## Account flow

The start screen is a login window with registration. Registration logs the user in; the next visit requires login (FR-002 in `@context/foundation/prd.md`).
