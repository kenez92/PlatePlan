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

    // A POST without the token is refused immediately. The model is not called.
    const csrfHeader = await generate.getAttribute('data-csrf-header');
    const csrfToken = await generate.getAttribute('data-csrf-token');
    const generateUrl = await generate.getAttribute('data-generate-url');
    expect(csrfHeader).toBeTruthy();
    expect(csrfToken).toBeTruthy();
    expect(generateUrl).toBeTruthy();
    const forbidden = await page.evaluate(async (url) => {
      const response = await fetch(url, {
        method: 'POST',
        headers: { Accept: 'application/json' }
      });
      return response.status;
    }, generateUrl);
    expect(forbidden).toBe(403);

    // The button's fetch carries that same token. Answer locally so the model stays out of the run.
    await page.route('**/plan/generate**', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ dietPdf: 'QQ==', shoppingListPdf: 'QQ==' })
      });
    });
    const posted = page.waitForRequest(
      (request) => request.method() === 'POST' && request.url().includes('/plan/generate')
    );
    await generate.click();

    const request = await posted;
    expect(await request.headerValue(csrfHeader!)).toBe(csrfToken);

    // A 403 HTML response would bounce the page to the login window.
    await expect(page).toHaveURL(/\/plan/);
    await expect(page.getByRole('button', { name: 'Wyloguj' })).toBeVisible();

    // No data created — generate does not persist files. Nothing to clean up.
  });
});
