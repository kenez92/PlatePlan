---
bootstrapped_at: 2026-09-26T11:44:36Z
starter_id: spring
starter_name: Spring Boot
project_name: plate-plan
language_family: java
package_manager: gradle
cwd_strategy: subdir-then-move
bootstrapper_confidence: verified
path_taken: standard
quality_override: false
self_check_answers: null
phase_3_status: ok
audit_command: "null"
---

## Hand-off

Matches `context/foundation/tech-stack.md` as committed in `3aa30d0` and on disk.
The build in that file is Gradle. The `project_name` field is `plate-plan`. The
Gradle project name is `PlatePlan` (`settings.gradle.kts`).

```yaml
starter_id: spring
package_manager: gradle
project_name: plate-plan
hints:
  language_family: java
  team_size: solo
  deployment_target: fly
  ci_provider: github-actions
  ci_default_flow: auto-deploy-on-merge
  bootstrapper_confidence: verified
  path_taken: standard
  quality_override: false
  self_check_answers: null
  has_auth: true
  has_payments: false
  has_realtime: false
  has_ai: true
  has_background_jobs: false
```

## Why this stack

PlatePlan is a one-person web app, built after hours, at a small scale. For Java the starter is Spring Boot. Account and login from the PRD fit this stack; Spring Security is the chosen library and is absent from `build.gradle.kts`. The front end is in the same project: the login screen, the form, and the PDF download are Spring Web views (Thymeleaf). The build is Gradle (Kotlin DSL), Spring Boot 4.1.1, and Java 21. Declared dependencies are `spring-boot-starter-webmvc`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-actuator`, and `spring-boot-devtools`. AI in scope is Spring AI with a local Ollama model; neither is a dependency yet. Payments, realtime, and background jobs are outside this MVP. Deployment goes to Fly.io, and GitHub Actions deploys after a merge to the main branch. The Gradle project name is PlatePlan. The hand-off name `plate-plan` is the Fly app name.

## Pre-scaffold verification

| Signal      | Value   | Severity | Notes                                                                                     |
| ----------- | ------- | -------- | ----------------------------------------------------------------------------------------- |
| npm package | not run | n/a      | Java starter. The command is a `curl` to start.spring.io, not an `npm create` CLI.       |
| GitHub repo | not run | n/a      | The card `docs_url` is `https://docs.spring.io/spring-boot/`, not a github.com URL. No push date. |

This starter has no freshness signal: start.spring.io publishes no version to date-check, and the card points at documentation.

## Scaffold log

The project in git since `3aa30d0` is Gradle (Kotlin DSL), Spring Boot 4.1.1, Java 21.

| Field            | Value                                      |
| ---------------- | ------------------------------------------ |
| Group            | `com.kenez92` (`build.gradle.kts`)         |
| Version          | `0.0.1-SNAPSHOT`                           |
| Gradle name      | `PlatePlan` (`settings.gradle.kts`)        |
| Package          | `com.kenez92.plateplan`                    |
| Fly app          | `plate-plan` (`fly.toml`)                  |

Build files: `build.gradle.kts`, `settings.gradle.kts`, `gradlew`, `gradlew.bat`, `gradle/wrapper/`.

Dependencies in `build.gradle.kts`:

- `spring-boot-starter-webmvc` and `spring-boot-devtools` — since `3aa30d0`
- `spring-boot-starter-actuator` — since `6176656`
- `spring-boot-starter-thymeleaf` — since `892790c`
- test: `spring-boot-starter-webmvc-test`, `junit-platform-launcher`

`.gitignore` ignores `.gradle` and `build/`, and keeps the wrapper with `!gradle/wrapper/gradle-wrapper.jar` plus the exceptions `!**/src/main/**/build/` and `!**/src/test/**/build/`. The file also lists `HELP.md`; that file is absent from the tree.

`cwd_strategy` for this run is `subdir-then-move`: the starter name was substituted only as the `artifactId` `.bootstrap-scaffold`. The start.spring.io archive has no wrapper directory, so the files landed in the working directory. There is no `.bootstrap-scaffold/` directory. `tech-stack.md` has no `cwd_strategy` field.

An earlier draft of this log recorded a command with `type=maven-project` and a Maven file list (`pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`, package `com.example.bootstrap_scaffold`). Those files are absent from `3aa30d0` and from disk. The tree that remains is the Gradle project above.

Kept beside the scaffold: `context/**`, `.cursor/**`, `.agents/**`, `.10x-cli.json`, `skills-lock.json`. There are no `.scaffold` files.

## Post-scaffold audit

**Tool**: skipped — the bootstrapper has no built-in audit for Java (`audit_command` is empty).
**External tool**: OWASP Dependency-Check or Snyk.

No CVE scan has been run. Actuator and Thymeleaf were added after the starter and have not been scanned either.

`src/main/resources/application.properties` exposes every Actuator endpoint, and heap dump and shutdown are unrestricted:

```
management.endpoints.web.exposure.include=*
management.endpoint.heapdump.access=unrestricted
management.endpoint.shutdown.access=unrestricted
```

The dependencies include no Spring Security, so these endpoints have no authentication. `Dockerfile` and `fly.toml` set `SERVER_ADDRESS=0.0.0.0` and `SERVER_PORT=8080`.

## Hints

| Hint                    | Value                | State in the repository                                                                                       |
| ----------------------- | -------------------- | ------------------------------------------------------------------------------------------------------------- |
| bootstrapper_confidence | verified             | Covers the scaffold step. It does not cover the build, the dependency set, or a scan.                        |
| quality_override        | false                | Recorded only.                                                                                                |
| path_taken              | standard             | Recorded only.                                                                                                |
| self_check_answers      | null                 | Recorded only.                                                                                                |
| team_size               | solo                 | Recorded only.                                                                                                |
| deployment_target       | fly                  | `fly.toml`, `Dockerfile`, and the deploy job in `.github/workflows/ci.yml` (`a1a958a`).                       |
| ci_provider             | github-actions       | `.github/workflows/ci.yml` runs the tests.                                                                    |
| ci_default_flow         | auto-deploy-on-merge | The deploy job runs after tests, on a push to `main`.                                                         |
| has_auth                | true                 | No Spring Security and no account handling. GET `/` and GET `/register` exist. POST `/login` and POST `/register` do not. |
| has_payments            | false                | Absent from the build, as the hand-off says.                                                                  |
| has_realtime            | false                | Absent from the build, as the hand-off says.                                                                  |
| has_ai                  | true                 | No Spring AI and no Ollama.                                                                                   |
| has_background_jobs     | false                | Absent from the build, as the hand-off says.                                                                  |

Thymeleaf has been in `build.gradle.kts` since `892790c`. The login form is in `src/main/resources/templates/fragments/chrome.html` and posts to `/login`. The registration form is in `src/main/resources/templates/register.html` and posts to `/register`. The visible copy on those pages is Polish.

## Next steps

`AGENTS.md` is the guide for agents. Git history starts at `3aa30d0`.

Still open:

- Run an external dependency scan. This log has no CVE results.
- Restrict Actuator in `src/main/resources/application.properties` before deploy: heap dump and shutdown are exposed with no authentication.
- Finish the account (`has_auth`) and the AI layer (`has_ai`). They are absent from the build.
