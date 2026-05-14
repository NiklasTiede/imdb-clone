import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { describe, expect, test } from "vitest";

const srcRoot = path.resolve(
  path.dirname(fileURLToPath(import.meta.url)),
  "..",
);
const appSource = () => readFileSync(path.join(srcRoot, "App.tsx"), "utf8");
const routeRegistrySource = () =>
  readFileSync(
    path.join(srcRoot, "app", "routes", "routeDefinitions.tsx"),
    "utf8",
  );

describe("app route architecture", () => {
  test("lazy-loads feature route pages", () => {
    const source = appSource();
    const routeRegistry = routeRegistrySource();

    expect(routeRegistry).toContain("lazyRoute");
    expect(routeRegistry).toMatch(/import\("\.\.\/\.\.\/features\//);
    expect(source).toContain("Suspense");
    expect(source).not.toMatch(/from "\.\/features\//);
  });

  test("keeps route declarations in the app route registry", () => {
    const source = appSource();
    const routeRegistryPath = path.join(
      srcRoot,
      "app",
      "routes",
      "routeDefinitions.tsx",
    );

    expect(existsSync(routeRegistryPath)).toBe(true);
    expect(source).toContain("routeDefinitions.map");
  });
});
