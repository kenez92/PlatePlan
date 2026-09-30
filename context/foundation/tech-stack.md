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

PlatePlan is a one-person web app, built after hours, at a small scale. For Java the starter is Spring Boot. Account and login from the PRD fit this stack; Spring Security is the chosen library and is absent from `build.gradle.kts`. The front end is in the same project: the login screen, the form, and the PDF download are Spring Web views (Thymeleaf). The build is Gradle (Kotlin DSL), Spring Boot 4.1.1, and Java 21. Declared dependencies are `spring-boot-starter-webmvc`, `spring-boot-starter-thymeleaf`, `spring-boot-starter-actuator`, `spring-boot-starter-data-jpa` (Hibernate), the PostgreSQL driver (`org.postgresql:postgresql`), and `spring-boot-devtools`. The database is Supabase PostgreSQL, used through Hibernate. Its URL, user name, and password come from Fly secrets (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`), never from a tracked file. Hibernate does not change the schema; schema changes go only through Liquibase (added in the `database-configured` change). AI in scope is Spring AI with a local Ollama model; neither is a dependency yet. Payments, realtime, and background jobs are outside this MVP. Deployment goes to Fly.io, and GitHub Actions deploys after a merge to the main branch. The Gradle project name is PlatePlan. The hand-off name `plate-plan` is the Fly app name.
