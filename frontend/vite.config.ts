import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";
import { loadEnv } from "vite";
import { createSeoAssets } from "./src/shared/seo/metadata.js";

export default defineConfig(({ mode }) => ({
  plugins: [
    react({}),
    {
      name: "public-crawl-files",
      apply: "build",
      transformIndexHtml() {
        const env = loadEnv(mode, process.cwd(), "VITE_");
        const origin = env.VITE_SITE_URL || "http://localhost:3000";
        return [
          {
            tag: "meta",
            attrs: {
              property: "og:image",
              content: new URL("/brand/after-dark/og-image.png", origin).href,
            },
            injectTo: "head",
          },
        ];
      },
      generateBundle() {
        const env = loadEnv(mode, process.cwd(), "VITE_");
        const assets = createSeoAssets(
          env.VITE_SITE_URL || "http://localhost:3000",
        );
        for (const [fileName, source] of Object.entries(assets)) {
          this.emitFile({ type: "asset", fileName, source });
        }
      },
    },
  ],
  server: {
    port: 3000,
    strictPort: true,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
      },
      "/concierge-api": {
        target: "http://localhost:8090",
        ws: true,
        rewrite: (path) => path.replace(/^\/concierge-api/, ""),
      },
      "/oauth2": {
        target: "http://localhost:8080",
      },
      "/login/oauth2": {
        target: "http://localhost:8080",
      },
      "/webauthn": {
        target: "http://localhost:8080",
      },
      "/login/webauthn": {
        target: "http://localhost:8080",
      },
    },
  },
  build: {
    outDir: "build",
  },
  test: {
    environment: "jsdom",
    fsModuleCache: true,
    globals: true,
    setupFiles: [
      "./src/test/failOnUnexpectedConsole.ts",
      "./src/test/renderWithAppTheme.tsx",
    ],
    include: ["src/**/*.{test,spec}.{ts,tsx}"],
    exclude: ["e2e/**"],
  },
}));
