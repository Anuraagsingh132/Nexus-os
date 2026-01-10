import { test, expect } from '@playwright/test';

test.describe('Authentication Smoke Tests', () => {
  test('login page should have a submit button', async ({ page }) => {
    await page.goto('/login');
    const submitButton = page.locator('button[type="submit"]');
    await expect(submitButton).toBeVisible();
  });

  test('signup page should have a submit button', async ({ page }) => {
    await page.goto('/signup');
    const submitButton = page.locator('button[type="submit"]');
    await expect(submitButton).toBeVisible();
  });
});
