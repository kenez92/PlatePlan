import { existsSync } from 'node:fs';
import { defineConfig, devices } from '@playwright/test';

// Local secrets (E2E_USERNAME, E2E_PASSWORD, app env) come from the gitignored
// env file. In CI the file is absent and the variables come from the job.
if (existsSync('.env')) process.loadEnvFile('.env');

// 8080 was detected from Dockerfile and fly.toml SERVER_PORT.
// E2E_PORT overrides it when that port is taken on this machine.
const PORT = Number(process.env.E2E_PORT ?? 8080);
const baseURL = `http://localhost:${PORT}`;
const gradle = process.platform === 'win32' ? 'gradlew.bat' : './gradlew';

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: 'html',
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'setup', testMatch: /.*\.setup\.ts/ },
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'], storageState: 'playwright/.auth/user.json' },
      dependencies: ['setup'],
    },
  ],
  webServer: {
    // Production-like bootJar + jar, on the port above.
    command: `${gradle} bootJar --no-daemon && java -jar build/libs/PlatePlan-0.0.1-SNAPSHOT.jar --server.port=${PORT}`,
    url: baseURL,
    reuseExistingServer: !process.env.CI,
    timeout: 180_000,
  },
});
