import { test as setup, expect } from '@playwright/test';

const authFile = 'playwright/.auth/user.json';

setup('sign in once and save the session', async ({ page }) => {
  const username = process.env.E2E_USERNAME;
  const password = process.env.E2E_PASSWORD;
  if (!username || !password) {
    throw new Error('Set E2E_USERNAME and E2E_PASSWORD (see context/foundation/test-stack.md, ## E2E)');
  }

  await page.goto('/');
  // Thymeleaf renders the login form in the first HTML. After a successful
  // POST the URL stays `/`, so wait for the signed-in chrome instead of a path change.
  await expect(async () => {
    await page.getByLabel('Login').fill('');
    await page.getByLabel('Login').fill(username);
    await page.getByLabel('Hasło', { exact: true }).fill('');
    await page.getByLabel('Hasło', { exact: true }).fill(password);
    await page.getByRole('button', { name: 'Zaloguj się' }).click();
    await expect(page.getByRole('button', { name: 'Wyloguj' })).toBeVisible({ timeout: 5_000 });
  }).toPass();
  await expect(page.getByRole('link', { name: 'Plan', exact: true })).toBeVisible();

  await page.context().storageState({ path: authFile });
});
