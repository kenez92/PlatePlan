---
date: 2026-10-01T15:25:13+02:00
researcher: unknown
git_commit: 3340061ed5290245ade827b16a0a508da8793c2b
branch: main
repository: PlatePlan
topic: "F-02 spring-security-sign-in: what exists today and what Spring Security must change"
tags: [research, codebase, spring-security, sign-in, actuator, liquibase, thymeleaf, tests]
status: complete
last_updated: 2026-10-01
last_updated_by: unknown
---

# Research: F-02 spring-security-sign-in — what exists today and what Spring Security must change

**Date**: 2026-10-01T15:25:13+02:00
**Researcher**: unknown
**Git Commit**: 3340061ed5290245ade827b16a0a508da8793c2b (working tree clean except the new `context/changes/spring-security-sign-in/` folder)
**Branch**: main
**Repository**: PlatePlan

## Research Question

`/10x-research spring-security-sign-in`. `change.md` has no question, so the scope comes from roadmap item F-02 (`context/foundation/roadmap.md:91-102`): "Spring Security can require a signed-in account before that account's data is shown, and the sign-in tables exist for one account with no roles. Public heap dump and shutdown cannot expose account data." This research records what the repository already contains for that outcome, which existing files a security layer touches, and which choices the plan has to settle. It does not propose an implementation.

## Summary

- **Nothing security-related exists yet.** A search of `src/` for `security`, `UserDetails`, `PasswordEncoder`, `@EnableWebSecurity`, and `csrf` (case-insensitive) returned no matches, and `build.gradle.kts` has no `security` dependency (`build.gradle.kts:21-29`). `tech-stack.md` already lists Spring Security as the chosen library and says it is absent from the build (`context/foundation/tech-stack.md:24`), so adding it satisfies the "library must be listed" rule in `AGENTS.md`.
- **The UI already assumes Spring Security's default form-login contract.** The header form on every page posts `username` and `password` to `/login` (`src/main/resources/templates/fragments/chrome.html:22-31`), and the register form posts the same two fields to `/register` (`templates/register.html:18-25`). No controller handles either POST: `HomeController` and `RegisterController` each map only `GET` (`controller/HomeController.java:11-14`, `controller/RegisterController.java:11-14`), and no `GET /login` or `POST /login` mapping exists in `src/main/java`.
- **Actuator is the part F-02 is meant to settle.** `application.properties:17-20` exposes every web endpoint (`include=*`), closes `heapdump` and `shutdown` (`access=none`), and leaves `loggers` read-only. Every other exposed endpoint (for example `beans`, which `ActuatorEndpointsTest` asserts returns 200) is unauthenticated because there is no security on the classpath (`context/foundation/infrastructure.md:98`). Two already-accepted review findings explicitly defer this to F-02 (`context/archive/2026-09-29-database-configured/reviews/impl-review.md:32-38`).
- **No sign-in tables or account entity exist.** The only changelog is the `SELECT 1` smoke test (`src/main/resources/db/changelog/changes/001-test.xml:9-11`); the master changelog `includeAll`s `changes/` alphabetically (`db.changelog-master.xml:11`). A failed Liquibase run at start is logged and skipped (`config/LiquibaseConfiguration.java:46-53`), so a new table is not guaranteed to exist after a deploy. The archived review marked "revisit at the first real migration (F-02)" (`impl-review.md:62`).
- **Three existing test classes will behave differently once security is on the classpath** (inference from the Spring Boot testing documentation, not run here): `HomeControllerTest`, `RegisterControllerTest` (both `@WebMvcTest` on one controller) and `ActuatorEndpointsTest` (`useDefaultFilters = false`). `ApplicationTest` boots the whole context with an unreachable database, so any new bean must not open a database connection at start.
- **The F-02 / S-01 boundary is stated but leaves one gap.** The roadmap says S-01 still builds registration, the login window, and automatic sign-in, and that F-02 "does not finish those screens" (`roadmap.md:100`, `roadmap.md:120-129`). Whether F-02 also supplies the lookup of an account by username (needed for any login to succeed) is not stated. See Open Questions.

## Detailed Findings

### Build and library status

- Declared dependencies on this commit (inspected set: `build.gradle.kts:20-30`): `spring-boot-starter-webmvc`, `-thymeleaf`, `-actuator`, `-data-jpa`, `-liquibase`, `postgresql` (runtime), `spring-boot-devtools` (development), and test: `spring-boot-starter-webmvc-test`, `junit-platform-launcher`. There is no Spring Security and no security test starter.
- Spring Boot is 4.1.1 on Java 21 (`build.gradle.kts:3, 13`). For Spring Boot 4 the Boot migration guide and the "Modularizing Spring Boot" post state that each main starter has a matching `-test` starter, and that `spring-boot-starter-security-test` is the one that makes `@WithMockUser`/`@WithUserDetails` work ([Boot 4.0 migration guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide), [Modularizing Spring Boot](https://spring.io/blog/2025/10/28/modularizing-spring-boot), [boot issue 47810](https://github.com/spring-projects/spring-boot/issues/47810)). These are external sources; the artifact names were not resolved against the project's Gradle cache (not inspected).
- `tech-stack.md` front matter sets `has_auth: true` (`tech-stack.md:19`).

### Existing web layer a security boundary has to fit

- `HomeController` serves `GET /` and `RegisterController` serves `GET /register`; both return views only (`controller/HomeController.java:11-14`, `controller/RegisterController.java:11-14`). Both are meant to be reachable by a signed-out visitor: the PRD says "on entering the site there is a login window" and registration ends in an automatic login (`context/foundation/prd.md:108-110`, FR-001/FR-002 at `prd.md:62-67`).
- The login form lives in the shared header fragment that every page includes, not on its own page (`templates/fragments/chrome.html:21-33`), and the page copy says login happens in the top bar ("w pasku u góry strony" at `templates/register.html:11` and `:28`; "u góry strony" at `templates/home.html:82`). Spring Security's default failed-login redirect and its generated login page would therefore point at a page the app does not render. Whether to keep the header-only login (and where a failure message shows) or add a login view is a plan decision.
- Both forms use `th:action="@{...}"` (`chrome.html:22`, `register.html:18`). With Thymeleaf these forms are the usual place a CSRF token is added automatically when Spring Security's CSRF protection is on; this was not run here (inference).
- Static CSS is served at `/css/site.css` and referenced from the head fragment (`chrome.html:7`); a security rule set has to leave `/css/**` readable for the signed-out pages to look right (inference from Spring Security's default of protecting every request).

### Actuator exposure

- `management.endpoints.web.exposure.include=*`, `heapdump.access=none`, `shutdown.access=none`, `loggers.access=read-only` (`application.properties:17-20`).
- `ActuatorEndpointsTest.shouldAnswerHealthAndBeansButNotHeapDumpOrShutdown` asserts `/actuator`, `/actuator/health`, and `/actuator/beans` return 200 and `heapdump`/`shutdown` return 404 (`src/test/java/.../controller/ActuatorEndpointsTest.java:50-65`). Before `database-configured`, this test asserted public exposure of heap dump and shutdown (`context/archive/2026-09-29-database-configured/plan.md:13`).
- The previous change's review lists other still-public read endpoints (`threaddump`, `logfile`, `metrics`, `beans`) as leaking internals "until F-02" (`impl-review.md:37`) and offered "replace `include=*` with an explicit list (`health,info`)" as an alternative (`impl-review.md:38`); the chosen fix was `loggers.access=read-only` and deferral (`impl-review.md:33`).
- `fly.toml` configures no health check (`http_service` block has `internal_port` and `force_https` only, `fly.toml:6-10`), and CI only runs `./gradlew test` and `flyctl deploy` (`.github/workflows/ci.yml:18-40`). On this inspected configuration, nothing in the repo calls `/actuator/health` from outside, so requiring authentication on Actuator would not by itself fail a Fly health check. Checking the Fly dashboard or machine checks was not done.

### Data layer for sign-in tables

- Schema changes go only through Liquibase XML files in `db/changelog/changes/`, numbered so alphabetical order is run order, never editing an applied changeSet (`AGENTS.md` "Layout", `db.changelog-master.xml:7-11`). The only file is `001-test.xml` (`SELECT 1`).
- Hibernate is `ddl-auto=none`, with an explicit PostgreSQL dialect and no JDBC metadata read at start (`application.properties:10-13`). No JPA entity or repository exists in `src/main/java` (the Java files are `Application`, two config classes, two controllers).
- The `DataSource` is an application-owned lazy Hikari bean; `DataSourceAutoConfiguration` is excluded (`Application.java:7`, `config/DataSourceConfiguration.java:13-28`). Any new bean that queries the database at start would break the "starts when the database is unreachable" contract; `ApplicationTest` boots the full context against `jdbc:postgresql://127.0.0.1:1/plateplan` and asserts the context loads (`src/test/java/.../ApplicationTest.java:11-39`).
- `LiquibaseConfiguration` swallows a migration failure with a log line and continues (`config/LiquibaseConfiguration.java:46-53`). The archived review accepted this as a residual risk "with only the `SELECT 1` changeSet" and flagged the first real migration as the moment to revisit it (`impl-review.md:62`).
- Roadmap wording for the tables: "the sign-in tables exist for one account with no roles" (`roadmap.md:93`), and the Parked section repeats "F-02 creates sign-in tables for one account" (`roadmap.md:214`). The account's later fields (age, height, weight, sex, goal, activity level, confirmed calories, preferences) are assigned to S-02 and S-03, not F-02 (`roadmap.md:132-152`).

### Tests that a security layer touches

- `HomeControllerTest` and `RegisterControllerTest` are `@WebMvcTest(<Controller>.class)` with `SpringExtension`, `GET` the page, and assert HTML text and that `class="login"` appears before the main content (`controller/HomeControllerTest.java:14-41`, `controller/RegisterControllerTest.java:14-34`). The Spring Boot testing docs state that a `SecurityFilterChain` declared in an ordinary `@Configuration` class is not picked up by a `@WebMvcTest` slice's component scan and must be imported explicitly ([Testing :: Spring Boot](https://docs.spring.io/spring-boot/4.0/how-to/testing.html)). Whether these existing tests would get 401/302 under Boot's default security without an import was not run here.
- `ActuatorEndpointsTest` uses `@WebMvcTest(useDefaultFilters = false)` plus an explicit list of endpoint auto-configurations (`ActuatorEndpointsTest.java:25-40`); the testing rule in `.cursor/rules/testing.mdc` permits exactly that shape for Actuator HTTP.
- Test conventions from `.cursor/rules/testing.mdc` that apply to any new test: names start with `should`, all locals and fields `final`, constructor injection, no `@SpringBootTest` outside `ApplicationTest`, public methods only, whole-object comparison.
- Existing config classes use `@Configuration(proxyBeanMethods = false)` in package `config` (`config/DataSourceConfiguration.java:12`, `config/LiquibaseConfiguration.java:20`) with IntelliJ-style four-space formatting (`AGENTS.md` "Style"). `Application.java` still uses tabs (`AGENTS.md` "Style").

## Code References

- `build.gradle.kts:20-30` - full dependency block; no security starter.
- `src/main/resources/application.properties:17-20` - Actuator exposure and access settings.
- `src/main/resources/templates/fragments/chrome.html:21-33` - header login form (`POST /login`, fields `username`, `password`) and the Register link.
- `src/main/resources/templates/register.html:18-25` - register form (`POST /register`, fields `username`, `password`).
- `src/main/java/com/kenez92/plateplan/controller/HomeController.java:11-14`, `RegisterController.java:11-14` - GET-only controllers.
- `src/main/java/com/kenez92/plateplan/Application.java:7` - `DataSourceAutoConfiguration` excluded.
- `src/main/java/com/kenez92/plateplan/config/DataSourceConfiguration.java:13-28` - application-owned lazy Hikari data source.
- `src/main/java/com/kenez92/plateplan/config/LiquibaseConfiguration.java:46-53` - tolerant migration at start.
- `src/main/resources/db/changelog/db.changelog-master.xml:11` and `changes/001-test.xml:9-11` - `includeAll` and the only changeSet.
- `src/test/java/com/kenez92/plateplan/controller/ActuatorEndpointsTest.java:50-65` - current Actuator assertions.
- `src/test/java/com/kenez92/plateplan/ApplicationTest.java:11-39` - full-context test with an unreachable database.
- `context/foundation/roadmap.md:91-102` (F-02), `:120-129` (S-01), `:196` (backlog row, "Ready for /10x-plan: no — After F-01").
- `context/foundation/tech-stack.md:19, 24` - `has_auth: true`; Spring Security named and noted absent.
- `context/foundation/prd.md:62-67, 108-110` - FR-001/FR-002 and Access Control.
- `context/foundation/infrastructure.md:65, 98` - Actuator remains unauthenticated until F-02.

## Architecture Insights

- The application is a server-rendered Spring MVC + Thymeleaf app with no JSON API (`controller/` has two view controllers); a form-login session model matches the existing forms, whereas a token/API model would not fit what the templates already post.
- The project's pattern for infrastructure is one small explicit `@Configuration(proxyBeanMethods = false)` class per concern in `config/` (DataSource, Liquibase). A security configuration would sit beside them.
- The previous change deliberately kept start-up independent of the database. The same constraint applies to any user-lookup bean: it may query only on a request, never at start.
- F-01's pattern for a risky default was "close it now, defer the rest" (heap dump/shutdown/loggers); F-02 is the change that finishes the rest.

## Historical Context (from prior changes)

- `context/archive/2026-09-29-database-configured/plan.md:41` - "No Spring Security and no authentication on Actuator. F-02 owns that"; "F-02's sign-in tables will be added as Liquibase changelogs." Still accurate on this commit (no security, only `001-test.xml`).
- `context/archive/2026-09-29-database-configured/plan.md:215` - "Other Actuator endpoints stay as they are; F-02 decides on them." Still accurate.
- `context/archive/2026-09-29-database-configured/reviews/impl-review.md:32-38` - the `loggers` finding and Fix A/B. Partially superseded: `loggers.access=read-only` is applied (`application.properties:20`); the broader exposure question remains open.
- `context/archive/2026-09-29-database-configured/reviews/impl-review.md:62` - tolerant Liquibase accepted; "revisit at the first real migration (F-02)". Still open.
- `context/foundation/infrastructure.md:98` - risk-register row says remaining endpoints "are still unauthenticated … F-02 decides on them". Still accurate.
- No `lessons.md` exists (`context/foundation/lessons.md` not found).

## Related Research

None. `context/changes/**/research.md` and `context/archive/**/research.md` contain no other research file for this topic.

## Open Questions

1. **Does F-02 include loading an account by username (a `UserDetailsService`) and a password encoder, or only the filter chain and tables?** Without a lookup, a login cannot succeed and the "signed-in account" gate cannot be demonstrated; with it, the boundary with S-01 (registration, automatic sign-in) needs to be drawn explicitly. Owner: user/plan.
2. **Table and entity shape for "one account, no roles".** Spring Security's stock JDBC schema assumes an `authorities` table; "no roles" and the later profile fields (S-03) point to a single account table that later changes extend. Not decided anywhere in the repo.
3. **Login failure and logout UX with a header-only login form.** Default Spring Security redirects to a generated `/login` page; no template exists for it. Choose between keeping the header form with an error flag on the current page or adding a login view.
4. **Which Actuator endpoints stay public.** Options on record: authenticate all but `health`/`info`, shrink `include=*` to an explicit list, or both (`impl-review.md:38`). With one account and no roles, "signed-in" would be the only gate, which means any registered user could read `beans`/`env`; whether that is acceptable or Actuator should be closed to web users entirely is undecided.
5. **CSRF and session settings.** Not examined beyond the notes above; the default (CSRF on, server session) is assumed unless the plan says otherwise. Fly stops idle machines (`fly.toml:8-10`), so in-memory sessions are lost on a stop; whether that matters for "automatic login" is not decided.
6. **Exact artifact names against the Gradle cache.** `spring-boot-starter-security` and `spring-boot-starter-security-test` come from external documentation and were not resolved in this repository's build.
