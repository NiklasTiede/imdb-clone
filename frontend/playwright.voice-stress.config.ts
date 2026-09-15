import { defineConfig, devices } from "@playwright/test";

const runId =
  process.env.VOICE_STRESS_RUN_ID ??
  new Date().toISOString().replaceAll(/[:.]/g, "-");
if (!/^[a-zA-Z0-9_-]+$/.test(runId))
  throw new Error("Invalid stress run identifier");
process.env.VOICE_STRESS_RUN_ID = runId;

export default defineConfig({
  testDir: "./e2e/voice-stress",
  timeout: 340_000,
  globalTimeout: 800_000,
  workers: 1,
  retries: 0,
  reporter: [
    ["list"],
    ["html", { open: "never", outputFolder: "playwright-report/voice-stress" }],
  ],
  outputDir: `test-results/voice-stress/${runId}`,
  use: {
    ...devices["Desktop Chrome"],
    baseURL: "http://localhost:3000",
    actionTimeout: 10_000,
    navigationTimeout: 20_000,
    trace: "off",
    video: "off",
    screenshot: "off",
    launchOptions: { args: ["--autoplay-policy=no-user-gesture-required"] },
  },
  webServer: {
    command: "yarn start",
    url: "http://localhost:3000",
    reuseExistingServer: true,
  },
});
