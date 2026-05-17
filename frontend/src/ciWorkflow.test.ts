import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { describe, expect, test } from "vitest";

const srcRoot = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(srcRoot, "..", "..");

describe("CI workflow configuration", () => {
  test("RustFS readiness accepts the authenticated health endpoint response", () => {
    const workflow = readFileSync(
      path.join(repositoryRoot, ".github", "workflows", "e2e.yaml"),
      "utf8",
    );

    const waitForRustFsStep = workflow.match(
      /- name: Wait for RustFS[\s\S]*?(?=\n      - name: )/,
    )?.[0];

    expect(waitForRustFsStep).toBeDefined();
    expect(waitForRustFsStep).toContain('status_code="$(curl');
    expect(waitForRustFsStep).toContain('[ "$status_code" = "200" ]');
    expect(waitForRustFsStep).toContain('[ "$status_code" = "403" ]');
    expect(waitForRustFsStep).not.toContain("curl -fsS");
  });
});
