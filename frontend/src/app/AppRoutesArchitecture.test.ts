import { readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { describe, expect, test } from "vitest";

const srcRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
const appSource = () => readFileSync(path.join(srcRoot, "App.tsx"), "utf8");

describe("app route architecture", () => {
  test("lazy-loads feature route pages", () => {
    const source = appSource();

    expect(source).toContain("lazyRoute");
    expect(source).toContain("Suspense");
    expect(source).not.toMatch(/from "\.\/features\//);
  });
});
