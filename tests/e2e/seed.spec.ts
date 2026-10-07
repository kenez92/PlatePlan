import { test, expect } from '@playwright/test';

// Origin: test-plan.md risk #1 — anonymous visitors must not get the two PDFs.

test.use({ storageState: { cookies: [], origins: [] } });

test('signed-out visitor at /plan sees the login window, not PDF downloads', async ({ page }) => {
  await page.goto('/plan');

  await expect(page).toHaveURL('/');
  await expect(page.getByRole('form', { name: 'Logowanie' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Zaloguj się' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Plan', exact: true })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Generuj plan' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Pobierz plan' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Pobierz listę zakupów' })).toHaveCount(0);

  // No data created — nothing to clean up.
});
