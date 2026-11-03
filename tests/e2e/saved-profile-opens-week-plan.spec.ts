import { test, expect } from '@playwright/test';

// risk: test-plan.md #1 — a saved profile reaches a week plan with two named PDF downloads
// seed: tests/e2e/seed.spec.ts
// facet: signed-in save persists the body and the calorie number, /plan lists that person,
//        generate offers dieta-na-tydzien.pdf and lista-zakupow.pdf, refresh drops them
// mocked: POST /plan/generate JSON only (the model is the external boundary)

const GENERATE_FILES = {
  dietPdf: 'QQ==',
  shoppingListPdf: 'QQ=='
};

test('saved profile opens the week plan and the two downloads vanish on refresh', async ({ page }) => {
  await page.goto('/profile');
  // The owner fields come before the "Nowa osoba" copy of the same labels.
  await page.getByRole('textbox', { name: 'Nazwa', exact: true }).first().fill('E2E');
  await page.getByRole('textbox', { name: 'Wiek', exact: true }).first().fill('34');
  await page.getByRole('textbox', { name: 'Wzrost (cm)' }).first().fill('180');
  await page.getByRole('textbox', { name: 'Waga (kg)' }).first().fill('82.5');
  await page.getByRole('combobox', { name: 'Płeć' }).first().selectOption({ label: 'Mężczyzna' });
  await page.getByRole('combobox', { name: 'Cel', exact: true }).first().selectOption({ label: 'Utrzymać wagę' });
  await page.getByRole('combobox', { name: 'Poziom aktywności' }).first().selectOption({ label: 'Umiarkowana aktywność — ćwiczenia kilka dni w tygodniu' });
  await page.getByRole('button', { name: 'Zapisz profil' }).click();

  await expect(page.getByRole('status').filter({ hasText: 'Profil zapisany.' })).toBeVisible();
  const calories = page.getByRole('region', { name: 'Cel kaloryczny' }).getByLabel('Cel kaloryczny (kcal)');
  await expect(calories).toHaveValue(/^[1-9][0-9]{2,3}$/);
  const savedCalories = await calories.inputValue();

  await page.reload();
  await expect(page.getByRole('textbox', { name: 'Nazwa', exact: true }).first()).toHaveValue('E2E');
  await expect(page.getByRole('region', { name: 'Cel kaloryczny' }).getByLabel('Cel kaloryczny (kcal)')).toHaveValue(savedCalories);

  await page.goto('/plan');
  await expect(page.getByRole('checkbox', { name: 'E2E' })).toBeChecked();
  const generate = page.getByRole('button', { name: 'Generuj plan' });
  await expect(generate).toBeVisible();
  await expect(page.getByRole('link', { name: 'Pobierz plan' })).toHaveCount(0);

  await page.route('**/plan/generate**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(GENERATE_FILES)
    });
  });

  await generate.click();
  const diet = page.getByRole('link', { name: 'Pobierz plan' });
  const list = page.getByRole('link', { name: 'Pobierz listę zakupów' });
  await expect(diet).toBeVisible();
  await expect(list).toBeVisible();
  await expect(diet).toHaveAttribute('download', 'dieta-na-tydzien.pdf');
  await expect(list).toHaveAttribute('download', 'lista-zakupow.pdf');
  await expect(diet).toHaveAttribute('href', /^blob:/);
  await expect(list).toHaveAttribute('href', /^blob:/);

  await page.reload();
  await expect(page.getByRole('button', { name: 'Generuj plan' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Pobierz plan' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Pobierz listę zakupów' })).toHaveCount(0);
});
