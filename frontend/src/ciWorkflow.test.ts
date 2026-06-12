import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { describe, expect, test } from "vitest";

const srcRoot = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(srcRoot, "..", "..");

describe("CI workflow configuration", () => {
  const e2eWorkflow = (): string =>
    readFileSync(
      path.join(repositoryRoot, ".github", "workflows", "e2e.yaml"),
      "utf8",
    );

  test("RustFS readiness accepts the authenticated health endpoint response", () => {
    const workflow = e2eWorkflow();

    const waitForRustFsStep = workflow.match(
      /- name: Wait for RustFS[\s\S]*?(?=\n {6}- name: )/,
    )?.[0];

    expect(waitForRustFsStep).toBeDefined();
    expect(waitForRustFsStep).toContain('status_code="$(curl');
    expect(waitForRustFsStep).toContain('[ "$status_code" = "200" ]');
    expect(waitForRustFsStep).toContain('[ "$status_code" = "403" ]');
    expect(waitForRustFsStep).not.toContain("curl -fsS");
  });

  test("Playwright install is cached and has enough job time", () => {
    const workflow = e2eWorkflow();

    const timeout = workflow.match(/timeout-minutes: (?<minutes>\d+)/)?.groups
      ?.minutes;
    const browserInstallStep = workflow.match(
      /- name: Install Playwright browser[\s\S]*?(?=\n {6}- name: )/,
    )?.[0];

    expect(Number(timeout)).toBeGreaterThanOrEqual(45);
    expect(workflow).toContain("path: ~/.cache/ms-playwright");
    expect(workflow).toContain("${{ runner.os }}-playwright-");
    expect(browserInstallStep).toBeDefined();
    expect(browserInstallStep).toContain("yarn playwright install chromium");
    expect(browserInstallStep).not.toContain("--with-deps");
  });
});
