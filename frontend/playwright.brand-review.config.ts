import { defineConfig, devices } from "@playwright/test";

// Captures the "After Dark" brand evaluation screenshots and contrast checks
// against the local development stack (docs/brand/after-dark/brand-direction.md).
// Run: yarn playwright test -c playwright.brand-review.config.ts
export default defineConfig({
  testDir: "./e2e/brand-review",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 120_000,
  reporter: [["list"]],
  outputDir: "test-results/brand-review/artifacts",
  tsconfig: "./tsconfig.e2e.json",
  use: {
    ...devices["Desktop Chrome"],
    baseURL: "http://localhost:3000",
    trace: "off",
    video: "off",
    screenshot: "off",
  },
  webServer: {
    command: "yarn start",
    url: "http://localhost:3000",
    reuseExistingServer: true,
  },
});
