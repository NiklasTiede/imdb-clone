import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { describe, expect, test } from "vitest";
import ts from "typescript";

const srcRoot = path.dirname(fileURLToPath(import.meta.url));
const featuresRoot = path.join(srcRoot, "features");

const sourceExtensions = new Set([".ts", ".tsx"]);

describe("frontend feature architecture", () => {
  test("uses backend domain vocabulary for top-level feature modules", () => {
    expect(featureDirectoryNames()).toEqual([
      "account",
      "catalog",
      "concierge",
      "engagement",
      "home",
      "identity",
      "media",
      "search",
    ]);
    expect(existsSync(path.join(featuresRoot, "auth"))).toBe(false);
    expect(existsSync(path.join(featuresRoot, "rating"))).toBe(false);
    expect(existsSync(path.join(featuresRoot, "watchlist"))).toBe(false);
    expect(existsSync(path.join(featuresRoot, "engagement", "rating"))).toBe(
      true,
    );
    expect(existsSync(path.join(featuresRoot, "engagement", "watchlist"))).toBe(
      true,
    );
  });

  test("cross-feature imports use public feature interfaces", () => {
    expect(crossFeatureInternalImports()).toEqual([]);
  });

  test("keeps Concierge preview code out of the application and model dependencies inward", () => {
    const conciergeRoot = path.join(featuresRoot, "concierge");
    const violations = sourceFiles(conciergeRoot).flatMap((sourceFile) =>
      importSpecifiers(sourceFile).flatMap((specifier) => {
        const target = resolveSourceImport(sourceFile, specifier);
        if (!target) return [];
        const sourceArea = path
          .relative(conciergeRoot, sourceFile)
          .split(path.sep)[0];
        const targetArea = path
          .relative(conciergeRoot, target)
          .split(path.sep)[0];
        const importsPreview =
          sourceArea !== "preview" && targetArea === "preview";
        const modelImportsImplementation =
          sourceArea === "model" &&
          ["api", "audio", "components", "hooks", "preview"].includes(
            targetArea ?? "",
          );
        return importsPreview || modelImportsImplementation
          ? [`${path.relative(srcRoot, sourceFile)} imports ${specifier}`]
          : [];
      }),
    );
    expect(violations).toEqual([]);
  });

  test("detects lazy imports and re-exports without treating comments or strings as dependencies", () => {
    expect(
      moduleSpecifiers(`
      import type { Movie } from "./model";
      import "./setup";
      export { Card } from "./components/Card";
      export * from "./public";
      const Page = lazy(() => import("./pages/Page"));
      type Result = import("./api").Result;
      // import Decoy from "./comment";
      const example = 'import Decoy from "./string"';
    `),
    ).toEqual([
      "./model",
      "./setup",
      "./components/Card",
      "./public",
      "./pages/Page",
      "./api",
    ]);
  });

  test("keeps domain code out of legacy top-level folders", () => {
    expect(legacyDomainFolders()).toEqual([]);
  });

  test("feature modules use the public auth interface", () => {
    expect(internalAuthImports()).toEqual([]);
  });

  test("watchlist cache keys are not hard-coded outside watchlist api modules", () => {
    expect(hardCodedWatchlistQueryKeys()).toEqual([]);
  });

  test("watchlist page keeps mutation cache behavior behind the api seam", () => {
    const watchlistPage = readFileSync(
      path.join(
        featuresRoot,
        "engagement",
        "watchlist",
        "pages",
        "WatchlistPage.tsx",
      ),
      "utf8",
    );

    expect(watchlistPage).not.toContain("onMutate");
    expect(watchlistPage).not.toContain("PagedResponseWatchedMovieRecord");
  });

  test("feature pages and components use feature-owned model types", () => {
    expect(generatedClientUiImports()).toEqual([]);
  });
});

const featureDirectoryNames = (): string[] =>
  readdirSync(featuresRoot)
    .filter((entry) => {
      const featureDirectory = path.join(featuresRoot, entry);
      return (
        statSync(featureDirectory).isDirectory() &&
        sourceFiles(featureDirectory).length > 0
      );
    })
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

const internalAuthImports = (): string[] =>
  sourceFiles(srcRoot)
    .flatMap((sourceFile) =>
      importSpecifiers(sourceFile).map((specifier) => ({
        sourceFile,
        specifier,
      })),
    )
    .filter(({ sourceFile }) =>
      path.relative(srcRoot, sourceFile).startsWith("features"),
    )
    .filter(({ specifier }) => specifier.includes("shared/auth/"))
    .map(
      ({ sourceFile, specifier }) =>
        `${path.relative(srcRoot, sourceFile)} imports ${specifier}`,
    )
    .sort();

const hardCodedWatchlistQueryKeys = (): string[] =>
  sourceFiles(srcRoot)
    .filter(
      (sourceFile) =>
        !path
          .relative(srcRoot, sourceFile)
          .endsWith(
            path.join(
              "features",
              "engagement",
              "watchlist",
              "api",
              "watchlistQueries.ts",
            ),
          ),
    )
    .filter((sourceFile) =>
      readFileSync(sourceFile, "utf8").includes('queryKey: ["watchlist"]'),
    )
    .map((sourceFile) => path.relative(srcRoot, sourceFile))
    .sort();

const generatedClientUiImports = (): string[] =>
  sourceFiles(srcRoot)
    .flatMap((sourceFile) =>
      importSpecifiers(sourceFile).map((specifier) => ({
        sourceFile,
        specifier,
      })),
    )
    .filter(({ sourceFile }) => {
      const relativePath = path.relative(srcRoot, sourceFile);
      return (
        relativePath.startsWith("features") &&
        (relativePath.includes(`${path.sep}pages${path.sep}`) ||
          relativePath.includes(`${path.sep}components${path.sep}`))
      );
    })
    .filter(({ specifier }) =>
      specifier.includes("client/movies/generator-output"),
    )
    .map(
      ({ sourceFile, specifier }) =>
        `${path.relative(srcRoot, sourceFile)} imports ${specifier}`,
    )
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

const importSpecifiers = (sourceFile: string): string[] =>
  moduleSpecifiers(readFileSync(sourceFile, "utf8"), sourceFile);

const moduleSpecifiers = (
  source: string,
  fileName = "fixture.ts",
): string[] => {
  const specifiers: string[] = [];
  const visit = (node: ts.Node): void => {
    let specifier: ts.Node | undefined;
    if (ts.isImportDeclaration(node) || ts.isExportDeclaration(node)) {
      specifier = node.moduleSpecifier;
    } else if (
      ts.isCallExpression(node) &&
      node.expression.kind === ts.SyntaxKind.ImportKeyword
    ) {
      specifier = node.arguments[0];
    } else if (
      ts.isImportTypeNode(node) &&
      ts.isLiteralTypeNode(node.argument)
    ) {
      specifier = node.argument.literal;
    }
    if (specifier && ts.isStringLiteralLike(specifier))
      specifiers.push(specifier.text);
    ts.forEachChild(node, visit);
  };
  visit(ts.createSourceFile(fileName, source, ts.ScriptTarget.Latest));
  return specifiers;
};

type ResolvedImport = {
  sourceFile: string;
  specifier: string;
  targetFile: string | null;
};

type SourceImport = ResolvedImport & {
  targetFile: string;
};

const hasResolvedTarget = (
  sourceImport: ResolvedImport,
): sourceImport is SourceImport => sourceImport.targetFile !== null;

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
  const [feature, nestedFeature] = relativeToFeatures.split(path.sep);
  if (!feature) {
    return null;
  }
  if (feature !== "engagement") {
    return feature;
  }
  return nestedFeature === undefined ? feature : `${feature}/${nestedFeature}`;
};
