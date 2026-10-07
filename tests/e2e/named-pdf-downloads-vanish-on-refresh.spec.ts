import { test, expect } from '@playwright/test';

// risk: test-plan.md #1 — two named PDF downloads that vanish on refresh
// seed: tests/e2e/seed.spec.ts
// facet: signed-in generate offers dieta-na-jutro.pdf and lista-zakupow.pdf as blob links,
//        then refresh drops them; signed-out /plan is covered by seed.spec.ts
// mocked: POST /plan/generate JSON only (Ollama is server-side; no API key in this run)

const GENERATE_FILES = {
  dietPdf: 'QQ==',
  shoppingListPdf: 'QQ=='
};

test.describe('risk #1 named PDF downloads', () => {
  test('two named PDF downloads vanish after refresh', async ({ page }) => {
    // Open the plan screen as the saved session.
    await page.goto('/plan');
    const generate = page.getByRole('button', { name: 'Generuj plan' });
    await expect(generate).toBeVisible();

    // Stub generate JSON so the browser can show downloads without calling Ollama.
    await page.route('**/plan/generate', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(GENERATE_FILES)
      });
    });

    // Click generate and wait for the two named files.
    await generate.click();
    const diet = page.getByRole('link', { name: 'Pobierz plan' });
    const list = page.getByRole('link', { name: 'Pobierz listę zakupów' });
    await expect(diet).toBeVisible();
    await expect(list).toBeVisible();
    expect(await diet.getAttribute('download')).toBe('dieta-na-jutro.pdf');
    expect(await list.getAttribute('download')).toBe('lista-zakupow.pdf');
    expect(await diet.getAttribute('href')).toMatch(/^blob:/);
    expect(await list.getAttribute('href')).toMatch(/^blob:/);

    // Refresh drops the in-page files; they were never stored.
    await page.reload();
    await expect(page.getByRole('button', { name: 'Generuj plan' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Pobierz plan' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Pobierz listę zakupów' })).toHaveCount(0);

    // No data created on the server — generate does not persist files. Nothing to clean up.
  });
});
