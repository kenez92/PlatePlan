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

PlatePlan to jednoosobowa aplikacja webowa budowana po godzinach, na małą skalę. Dla Javy rekomendowany starter to Spring Boot: moduły web, danych i bezpieczeństwa są w zestawie, a konto i logowanie z PRD mieszczą się w tym stosie. Front jest w tym samym projekcie: ekran logowania, formularz i pobieranie PDF to widoki serwowane przez Spring Web (Thymeleaf). Komenda startera dołącza tylko web i devtools, więc warstwa HTML dochodzi w tym projekcie. W zakresie jest AI: Spring AI z lokalnym modelem Ollama. Płatności, czas rzeczywisty i zadania w tle są poza tym MVP. Wdrożenie idzie na Fly.io, a GitHub Actions wdraża automatycznie po merge do głównej gałęzi. Katalog: PlatePlan. Build jest na Gradle (Kotlin DSL), Spring Boot 4.1.1 i Java 21.
