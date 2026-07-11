import { expect, type Page, test } from "@playwright/test";

const mockAnonymousSession = async (page: Page) => {
  await page.route("**/api/auth/me", async (route) => {
    await route.fulfill({ status: 401, body: "" });
  });
};

const mockAvailableIdentity = async (page: Page) => {
  await page.route("**/api/auth/check-*-availability**", async (route) => {
    await route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({ isAvailable: true }),
    });
  });
};

test("desktop auth pane fills the available main region", async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 1000 });
  await mockAnonymousSession(page);

  await page.goto("/login");
  await expect(page.getByRole("heading", { name: "Sign in" })).toBeVisible({
    timeout: 15_000,
  });

  const mainBounds = await page.locator("main").boundingBox();
  const pane = page.getByTestId("auth-visual-pane");
  const paneBounds = await pane.boundingBox();
  expect(mainBounds).not.toBeNull();
  expect(paneBounds).not.toBeNull();
  expect(
    Math.abs((mainBounds?.height ?? 0) - (paneBounds?.height ?? 0)),
  ).toBeLessThanOrEqual(1);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(
    1440,
  );
});

test("mobile auth forms start below a compact non-overlapping header", async ({
  page,
}) => {
  await page.setViewportSize({ width: 320, height: 700 });
  await mockAnonymousSession(page);

  await page.goto("/registration");
  await expect(page.getByTestId("auth-visual-pane")).toBeHidden();
  await expect(page.getByRole("link", { name: "IMDb Clone" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Sign in" })).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Create your account" }),
  ).toBeVisible();

  const headerBounds = await page.locator("header").boundingBox();
  const headingBounds = await page
    .getByRole("heading", { name: "Create your account" })
    .boundingBox();
  expect(
    (headingBounds?.y ?? 0) -
      ((headerBounds?.y ?? 0) + (headerBounds?.height ?? 0)),
  ).toBeGreaterThanOrEqual(24);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(
    320,
  );
});

test("login presents validation and durable server feedback", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await mockAnonymousSession(page);
  await page.route("**/api/auth/login", async (route) => {
    await route.fulfill({
      status: 401,
      contentType: "application/problem+json",
      body: JSON.stringify({ status: 401, title: "Unauthorized" }),
    });
  });

  await page.goto("/login");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(page.getByText("Email or username is required")).toBeVisible();
  await expect(page.getByText("Password is required")).toBeVisible();

  await page.getByLabel("Email or username").fill("movie_fan");
  await page.locator('input[name="password"]').fill("incorrect");
  await page.getByRole("button", { name: "Sign in", exact: true }).click();
  await expect(
    page.getByText("Email/username or password is incorrect."),
  ).toBeVisible();
});

test("registration redirects with persistent completion feedback", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await mockAnonymousSession(page);
  await mockAvailableIdentity(page);
  await page.route("**/api/auth/registration", async (route) => {
    await route.fulfill({
      status: 201,
      contentType: "application/json",
      body: JSON.stringify({
        message: "Account created. You can sign in now.",
      }),
    });
  });

  await page.goto("/registration");
  await page.getByLabel("Username").fill("new_user");
  await page.getByLabel("Email").fill("new@example.com");
  await page.getByLabel("Password", { exact: true }).fill("Movie!12");
  for (const rule of [
    "8-30 characters",
    "One uppercase letter",
    "One lowercase letter",
    "One number",
    "One special character",
  ]) {
    await expect(page.getByText(rule)).toHaveAttribute("data-met", "true");
  }
  await page.getByLabel("Confirm password").fill("Different!12");
  await page.getByLabel("Email").focus();
  await expect(page.getByText("Passwords do not match")).toBeVisible();
  await page.getByLabel("Confirm password").fill("Movie!12");
  const submit = page.getByRole("button", { name: "Create account" });
  await expect(submit).toBeEnabled();
  await submit.click();

  await expect(page).toHaveURL(/\/login$/);
  await expect(
    page.getByText("Account created. You can sign in now."),
  ).toBeVisible();
});

test("social login failure is provider-neutral", async ({ page }) => {
  await mockAnonymousSession(page);

  await page.goto("/login?error=social");

  await expect(
    page.getByText(
      "Social sign-in could not be completed. Try again or choose another method.",
    ),
  ).toBeVisible();
});

test("keyboard navigation reaches every authentication method", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await mockAnonymousSession(page);
  await page.goto("/login");
  await expect(page.getByRole("heading", { name: "Sign in" })).toBeVisible();

  const focusOrder = [
    page.getByRole("link", { name: "IMDb Clone" }),
    page.getByRole("link", { name: "Sign up" }),
    page.getByLabel("Email or username"),
    page.getByRole("link", { name: "Forgot password?" }),
    page.locator('input[name="password"]'),
    page.getByRole("button", { name: "Show password" }),
    page.getByRole("button", { name: "Sign in", exact: true }),
    page.getByRole("button", { name: "Sign in with passkey" }),
    page.getByRole("button", { name: "Continue with Google" }),
    page.getByRole("button", { name: "Continue with GitHub" }),
  ];

  for (const target of focusOrder) {
    await page.keyboard.press("Tab");
    await expect(target).toBeFocused();
  }
});
