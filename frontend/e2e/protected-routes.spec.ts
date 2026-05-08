import { expect, test } from "@playwright/test";

test("redirects anonymous users from protected routes to login", async ({
  page,
}) => {
  await page.goto("/your-watchlist");

  await expect(page).toHaveURL(/\/login$/);
  await expect(page.getByRole("button", { name: "Login" })).toBeVisible();
});

test("renders protected watchlist for authenticated users", async ({ page }) => {
  await page.addInitScript(() => {
    window.localStorage.setItem("jwtExpiresAt", String(Date.now() / 1000 + 60));
    window.localStorage.setItem("rolesFromJwt", "ROLE_USER");
  });

  await page.goto("/your-watchlist");

  await expect(page).toHaveURL(/\/your-watchlist$/);
  await expect(page.getByText("Your Movie WatchList")).toBeVisible();
});
