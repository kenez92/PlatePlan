import { test, expect } from '@playwright/test';

// risk: test-plan.md #6 — CSRF on the generate fetch header
// seed: tests/e2e/seed.spec.ts
// facet: signed-in generate fetch sends CSRF so a legitimate POST is not 403;
//        form CSRF is covered by @WebMvcTest

test.describe('risk #6 CSRF on generate fetch', () => {
  test('generate fetch sends a CSRF header so a signed-in POST is not forbidden', async ({
    page
  }) => {
    // Open the plan screen as the saved session.
    await page.goto('/plan');
    const generate = page.getByRole('button', { name: 'Generuj plan' });
    await expect(generate).toBeVisible();

    // Click generate and capture the POST the page's fetch actually sends.
    const csrfHeader = await generate.getAttribute('data-csrf-header');
    expect(csrfHeader).toBeTruthy();
    const posted = page.waitForRequest(
      (request) => request.method() === 'POST' && request.url().includes('/plan/generate')
    );
    const answered = page.waitForResponse(
      (response) =>
        response.request().method() === 'POST' && response.url().includes('/plan/generate')
    );
    await generate.click();

    const request = await posted;
    expect(await request.headerValue(csrfHeader!)).toBeTruthy();

    const response = await answered;
    expect(response.status()).not.toBe(403);
    expect(response.ok()).toBe(true);

    // A 403 HTML response would bounce the page to the login window.
    await expect(page).toHaveURL(/\/plan/);
    await expect(page.getByRole('button', { name: 'Wyloguj' })).toBeVisible();

    // No data created — generate does not persist files. Nothing to clean up.
  });
});
