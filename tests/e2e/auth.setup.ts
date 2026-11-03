import { test as setup, expect } from '@playwright/test';

const authFile = 'playwright/.auth/user.json';

setup('sign in once and save the session', async ({ page }) => {
  const username = process.env.E2E_USERNAME;
  const password = process.env.E2E_PASSWORD;
  if (!username || !password) {
    throw new Error('Set E2E_USERNAME and E2E_PASSWORD (see context/foundation/test-stack.md, ## E2E)');
  }

  await page.goto('/');
  await page.getByLabel('Login').fill(username);
  await page.getByLabel('Hasło', { exact: true }).fill(password);
  await page.getByRole('button', { name: 'Zaloguj się' }).click();

  const signedIn = page.getByRole('button', { name: 'Wyloguj' });
  const rejected = page.getByRole('alert').filter({ hasText: 'Nieprawidłowy login lub hasło' });
  await expect(signedIn.or(rejected)).toBeVisible();

  if (await rejected.isVisible()) {
    await page.getByRole('link', { name: 'Zarejestruj się' }).click();
    await page.getByRole('main').getByLabel('Login').fill(username);
    await page.getByRole('main').getByLabel('Hasło').fill(password);
    await page.getByRole('button', { name: 'Załóż konto' }).click();
    await expect(page.getByRole('alert').filter({ hasText: 'Login jest zajęty' })).toHaveCount(0);
    await expect(signedIn).toBeVisible();
  }

  await expect(page.getByRole('link', { name: 'Plan', exact: true })).toBeVisible();
  await page.context().storageState({ path: authFile });
});
