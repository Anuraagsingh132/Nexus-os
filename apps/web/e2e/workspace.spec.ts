import { test, expect } from '@playwright/test';

test.describe('Workspace and Projects', () => {
  let testEmail: string;
  let testPassword: string;
  let workspaceName: string;
  let projectName: string;

  test.beforeEach(async ({ page }) => {
    // Generate unique identifiers for each test run
    const ts = Date.now();
    testEmail = `workspace-${ts}@example.com`;
    testPassword = 'Password123!';
    workspaceName = `Test Workspace ${ts}`;
    projectName = `Test Project ${ts}`;

    // Sign up a new user before each test to have a clean state
    await page.goto('/signup');
    await page.waitForLoadState('networkidle');
    await page.fill('input[name="fullName"]', 'Workspace User');
    await page.fill('input[name="email"]', testEmail);
    await page.fill('input[name="password"]', testPassword);
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/.*\/login(\?registered=true)?/, { timeout: 15000 });
    
    // Log in with the newly created user
    await page.waitForLoadState('networkidle');
    await page.fill('input[name="email"]', testEmail);
    await page.fill('input[name="password"]', testPassword);
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL(/\/dashboard|\/workspaces/, { timeout: 15000 });
  });

  test('should create a workspace, project, and task', async ({ page }) => {
    // 1. Create Workspace
    await page.goto('/workspaces/new');
    await page.waitForLoadState('networkidle');
    await page.fill('input[name="name"]', workspaceName);
    await page.click('button[type="submit"]');
    
    // Ensure we're in the new workspace dashboard
    await expect(page.getByText(workspaceName)).toBeVisible({ timeout: 15000 });

    // 2. Create Project
    await page.getByRole('link', { name: /projects/i }).click();
    await page.waitForLoadState('networkidle');
    await page.getByRole('button', { name: /new project/i }).click();
    await page.waitForLoadState('networkidle');
    await page.fill('input[name="name"]', projectName);
    await page.click('button[type="submit"]');
    
    await expect(page.getByText(projectName)).toBeVisible({ timeout: 15000 });

    // 3. Create Task
    await page.getByRole('button', { name: /add task/i }).click();
    await page.fill('input[name="title"]', 'New Test Task');
    await page.click('button[type="submit"]:has-text("Create Task")');

    // Verify task is visible
    await expect(page.getByText('New Test Task')).toBeVisible({ timeout: 15000 });
  });
});
