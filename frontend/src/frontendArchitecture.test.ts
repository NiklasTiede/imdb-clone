import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { describe, expect, test } from "vitest";

const srcRoot = path.dirname(fileURLToPath(import.meta.url));
const featuresRoot = path.join(srcRoot, "features");

const sourceExtensions = new Set([".ts", ".tsx"]);

describe("frontend feature architecture", () => {
  test("uses backend domain vocabulary for top-level feature modules", () => {
    expect(featureDirectoryNames()).toEqual([
      "account",
      "catalog",
      "engagement",
      "home",
      "identity",
      "media",
      "notification",
      "search",
    ]);
    expect(existsSync(path.join(featuresRoot, "auth"))).toBe(false);
    expect(existsSync(path.join(featuresRoot, "rating"))).toBe(false);
    expect(existsSync(path.join(featuresRoot, "watchlist"))).toBe(false);
    expect(existsSync(path.join(featuresRoot, "engagement", "rating"))).toBe(
      true,
    );
    expect(
      existsSync(path.join(featuresRoot, "engagement", "watchlist")),
    ).toBe(true);
  });

  test("cross-feature imports use public feature interfaces", () => {
    expect(crossFeatureInternalImports()).toEqual([]);
  });

  test("keeps domain code out of legacy top-level folders", () => {
    expect(legacyDomainFolders()).toEqual([]);
  });
});

const featureDirectoryNames = (): string[] =>
  readdirSync(featuresRoot)
    .filter((entry) => statSync(path.join(featuresRoot, entry)).isDirectory())
    .sort();

const crossFeatureInternalImports = (): string[] =>
  sourceFiles(srcRoot)
    .flatMap((sourceFile) =>
      importSpecifiers(sourceFile).map((specifier) => ({
        sourceFile,
        specifier,
        targetFile: resolveSourceImport(sourceFile, specifier),
      })),
    )
    .filter(hasResolvedTarget)
    .filter(({ sourceFile, targetFile }) => {
      const sourceFeature = featurePath(sourceFile);
      const targetFeature = featurePath(targetFile);
      return targetFeature !== null && sourceFeature !== targetFeature;
    })
    .filter(({ targetFile }) => {
      const relativeTarget = path.relative(featuresRoot, targetFile);
      return !relativeTarget.endsWith("index.ts");
    })
    .map(
      ({ sourceFile, specifier }) =>
        `${path.relative(srcRoot, sourceFile)} imports ${specifier}`,
    )
    .sort();

const legacyDomainFolders = (): string[] =>
  ["components", "hooks", "pages", "types", "utils"]
    .filter((entry) => existsSync(path.join(srcRoot, entry)))
    .sort();

const sourceFiles = (directory: string): string[] =>
  readdirSync(directory).flatMap((entry) => {
    const absolutePath = path.join(directory, entry);
    if (entry === "generator-output" || entry === "build") {
      return [];
    }
    if (statSync(absolutePath).isDirectory()) {
      return sourceFiles(absolutePath);
    }
    if (
      !sourceExtensions.has(path.extname(entry)) ||
      entry.endsWith(".test.ts") ||
      entry.endsWith(".test.tsx") ||
      entry.endsWith(".spec.ts") ||
      entry.endsWith(".spec.tsx")
    ) {
      return [];
    }
    return [absolutePath];
  });

const importSpecifiers = (sourceFile: string): string[] => {
  const source = readFileSync(sourceFile, "utf8");
  return Array.from(
    source.matchAll(/import(?:\s+type)?[\s\S]*?from\s+["']([^"']+)["']/g),
    (match) => match[1],
  );
};

type ResolvedImport = {
  sourceFile: string;
  specifier: string;
  targetFile: string | null;
};

type SourceImport = ResolvedImport & {
  targetFile: string;
};

const hasResolvedTarget = (sourceImport: ResolvedImport): sourceImport is SourceImport =>
  sourceImport.targetFile !== null;

const resolveSourceImport = (
  sourceFile: string,
  specifier: string,
): string | null => {
  if (!specifier.startsWith(".")) {
    return null;
  }

  const absoluteBase = path.resolve(path.dirname(sourceFile), specifier);
  for (const candidate of sourceImportCandidates(absoluteBase)) {
    if (existsSync(candidate) && statSync(candidate).isFile()) {
      return candidate;
    }
  }
  return null;
};

const sourceImportCandidates = (absoluteBase: string): string[] => [
  absoluteBase,
  `${absoluteBase}.ts`,
  `${absoluteBase}.tsx`,
  path.join(absoluteBase, "index.ts"),
  path.join(absoluteBase, "index.tsx"),
];

const featurePath = (sourceFile: string): string | null => {
  const relativeToFeatures = path.relative(featuresRoot, sourceFile);
  if (relativeToFeatures.startsWith("..")) {
    return null;
  }
  const parts = relativeToFeatures.split(path.sep);
  if (parts[0] !== "engagement") {
    return parts[0];
  }
  return parts.length > 2 ? `${parts[0]}/${parts[1]}` : parts[0];
};
