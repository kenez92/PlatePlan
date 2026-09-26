---
bootstrapped_at: 2026-09-26T11:44:36Z
starter_id: spring
starter_name: Spring Boot
project_name: plate-plan
language_family: java
package_manager: maven
cwd_strategy: subdir-then-move
bootstrapper_confidence: verified
phase_3_status: ok
audit_command: "null"
---

## Hand-off

```yaml
starter_id: spring
package_manager: maven
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

PlatePlan to jednoosobowa aplikacja webowa budowana po godzinach, na małą skalę. Dla Javy rekomendowany starter to Spring Boot: moduły web, danych i bezpieczeństwa są w zestawie, a konto i logowanie z PRD mieszczą się w tym stosie. Front jest w tym samym projekcie: ekran logowania, formularz i pobieranie PDF to widoki serwowane przez Spring Web (Thymeleaf). Komenda startera dołącza tylko web i devtools, więc warstwa HTML dochodzi w tym projekcie Maven. W zakresie jest AI: Spring AI z lokalnym modelem Ollama. Płatności, czas rzeczywisty i zadania w tle są poza tym MVP. Wdrożenie idzie na Fly.io, a GitHub Actions wdraża automatycznie po merge do głównej gałęzi. Generowanie szkieletu jest zweryfikowane od początku do końca. Katalog: plate-plan. Karta startera uruchamia Maven przez start.spring.io (type=maven-project); preferencja Gradle nie wchodzi do pola package_manager, bo bootstrapper bierze menedżer pakietów z karty.

## Pre-scaffold verification

| Signal             | Value     | Severity | Notes                                                                 |
| ------------------ | --------- | -------- | --------------------------------------------------------------------- |
| npm package        | not run   | n/a      | Non-JS starter. `cmd_template` is a curl to start.spring.io, not an npm create CLI. |
| GitHub repo        | not run   | n/a      | Card `docs_url` is `https://docs.spring.io/spring-boot/`, not a github.com URL. No recency signal available. |

## Scaffold log

**Resolved invocation**: `curl -s https://start.spring.io/starter.tgz -d dependencies=web,devtools -d type=maven-project -d javaVersion=21 -d groupId=com.example -d artifactId=.bootstrap-scaffold | tar -xzf -`
**Strategy**: subdir-then-move
**Exit code**: 0
**Files moved**: 10
**Conflicts (.scaffold siblings)**: none
**.gitignore handling**: moved silently
**.bootstrap-scaffold cleanup**: not created — the start.spring.io tarball has no wrapper directory, so archive members extracted directly into cwd. Nothing to delete.

The intended flow was: generate into `.bootstrap-scaffold/`, apply the conflict matrix, then move files up. `{name}` was substituted as `.bootstrap-scaffold` into `artifactId` only. The archive root is the project files themselves, so they landed in cwd. No pre-existing scaffold fingerprint (`pom.xml`, `build.gradle`, etc.) was present, and no path collided.

`context/` was not in the archive and was left untouched.

File-by-file (all new; no `.scaffold` siblings):

| Path | Resolution |
| ---- | ---------- |
| `.gitattributes` | written (new) |
| `.gitignore` | written (new) |
| `HELP.md` | written (new) |
| `mvnw` | written (new) |
| `mvnw.cmd` | written (new) |
| `pom.xml` | written (new) |
| `.mvn/wrapper/maven-wrapper.properties` | written (new) |
| `src/main/java/com/example/bootstrap_scaffold/Application.java` | written (new) |
| `src/main/resources/application.properties` | written (new) |
| `src/test/java/com/example/bootstrap_scaffold/ApplicationTests.java` | written (new) |

Preserved as-is: `context/**`, `.cursor/**`, `.agents/**`, `.10x-cli.json`, `skills-lock.json`.

Note: `pom.xml` `artifactId` is `.bootstrap-scaffold` and the Java package is `com.example.bootstrap_scaffold`, because the starter command binds `{name}` to `artifactId` and that placeholder is the temporary directory name, not `project_name` (`plate-plan`).

## Post-scaffold audit

**Tool**: skipped — no built-in audit tool for java
**Recommended external tool**: OWASP Dependency-Check or Snyk

## Hints recorded but not acted on

| Hint                       | Value                              |
| -------------------------- | ---------------------------------- |
| bootstrapper_confidence    | verified                           |
| quality_override           | false                              |
| path_taken                 | standard                           |
| self_check_answers         | null                               |
| team_size                  | solo                               |
| deployment_target          | fly                                |
| ci_provider                | github-actions                     |
| ci_default_flow            | auto-deploy-on-merge               |
| has_auth                   | true                               |
| has_payments               | false                              |
| has_realtime               | false                              |
| has_ai                     | true                               |
| has_background_jobs        | false                              |

## Next steps

Next: a future skill will set up agent context (CLAUDE.md, AGENTS.md). For now, your project is scaffolded and verified — happy hacking.

Useful manual steps in the meantime:
- `git init` (if you have not already) to start your own repo history.
- Review any `.scaffold` siblings the conflict policy created and decide which version of each file to keep.
- Address audit findings per your project's risk tolerance — the full breakdown is in this log.
