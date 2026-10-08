# Test stack

## E2E

<!-- Written by /10x-e2e-setup. Re-run it to change this section; other skills only read it. -->

- runner: Playwright Test, @playwright/test 1.63.0
- config: playwright.config.ts
- single-spec command: npx playwright test tests/e2e/seed.spec.ts
- full-suite command: npx playwright test
- base URL: http://localhost:8080
- port: 8080 (detected from Dockerfile / fly.toml SERVER_PORT; detected default 8080, override with E2E_PORT)
- web server command: `gradlew.bat bootJar --no-daemon && java -jar build/libs/PlatePlan-0.0.1-SNAPSHOT.jar --server.port=$E2E_PORT` (`./gradlew` on non-Windows); reuseExistingServer outside CI
- auth setup project: setup (tests/e2e/auth.setup.ts), credentials from E2E_USERNAME / E2E_PASSWORD in .env
- storageState: playwright/.auth/user.json (gitignored)
- seed: tests/e2e/seed.spec.ts — protects #1 After calories are confirmed the user does not get two PDFs (signed-out `/plan` shows the login window, not download buttons)
- browser CLI: playwright-cli, command skill at .cursor/skills/playwright-cli/SKILL.md
- updated: 2026-10-07
