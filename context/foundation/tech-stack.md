---
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
---

## Why this stack

PlatePlan is a one-person web app, built after hours, at a small scale. For Java the starter is Spring Boot. Account and login from the PRD fit this stack; Spring Security is the chosen library and is declared in `build.gradle.kts`. The front end is in the same project: the login screen, the form, and the PDF download are Spring Web views (Thymeleaf). The build is Gradle (Kotlin DSL), Spring Boot 4.1.1, and Java 21. Declared dependencies are `spring-boot-starter-webmvc`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-actuator`, `spring-boot-starter-security`, `spring-boot-starter-validation` (Bean Validation with Hibernate Validator: form fields are checked by annotations while Spring binds them, and the errors go to `BindingResult`), `spring-boot-starter-data-jpa` (Hibernate), the PostgreSQL driver (`org.postgresql:postgresql`), `spring-boot-starter-liquibase`, `org.apache.commons:commons-lang3` (string constants such as `StringUtils.EMPTY`; its version comes from the Spring Boot dependency management), and `spring-boot-devtools`. The database is Supabase PostgreSQL, used through Hibernate. Its URL, user name, and password come from Fly secrets (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`), never from a tracked file. Hibernate does not change the schema; schema changes go only through Liquibase XML changelogs under `src/main/resources/db/changelog/`, picked up by `db.changelog-master.xml` from `src/main/resources/db/changelog/changes/` with `includeAll`. Liquibase runs at start; a failed attempt is logged and skipped so the application still starts. AI in scope is Spring AI 2.0.1 (`org.springframework.ai:spring-ai-bom:2.0.1`) and the Ollama model starter `org.springframework.ai:spring-ai-starter-model-ollama`, which call Ollama Cloud. Apache PDFBox is not a dependency yet. Payments, realtime, and background jobs are outside this MVP. Deployment goes to Fly.io, and GitHub Actions deploys after a merge to the main branch. The Gradle project name is PlatePlan. The hand-off name `plate-plan` is the Fly app name.
