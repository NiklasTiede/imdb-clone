import eslint from "@eslint/js";
import { defineConfig, globalIgnores } from "eslint/config";
import globals from "globals";
import reactHooks from "eslint-plugin-react-hooks";
import testingLibrary from "eslint-plugin-testing-library";
import tseslint from "typescript-eslint";

export default defineConfig(
  globalIgnores([
    "build/**",
    "coverage/**",
    "node_modules/**",
    "playwright-report/**",
    "test-results/**",
    "src/client/movies/generator-output/**",
  ]),
  {
    files: ["**/*.{ts,tsx}"],
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommended,
    ],
    languageOptions: {
      ecmaVersion: "latest",
      sourceType: "module",
      parser: tseslint.parser,
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
    rules: {
      "no-console": "off",
      "@typescript-eslint/no-explicit-any": "off",
      "react-hooks/rules-of-hooks": "error",
      "react-hooks/exhaustive-deps": "warn",
    },
    plugins: {
      "react-hooks": reactHooks,
    },
  },
  {
    files: ["src/**/*.{test,spec}.{ts,tsx}"],
    extends: [testingLibrary.configs["flat/react"]],
    languageOptions: {
      globals: {
        ...globals.browser,
      },
    },
  },
  {
    files: ["e2e/**/*.ts", "playwright.config.ts"],
    rules: {
      "testing-library/prefer-screen-queries": "off",
    },
  },
);
