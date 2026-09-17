import AxeBuilder from "@axe-core/playwright";
import { expect, type Page, test } from "@playwright/test";
import { mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";

// Brand evaluation capture for docs/brand/after-dark/brand-direction.md ("After Dark").
// Needs the local stack (backend on :8080, object storage on :9000, Vite on :3000).
// Screens use real catalog data; only the signed-in ratings library is mocked,
// and the home feed is recorded once and replayed so every variant shows the same movies.

const backendUrl =
  process.env.BRAND_REVIEW_BACKEND_URL ?? "http://localhost:8080";
const outputRoot = path.resolve("test-results/brand-review/sheet");
const screenshotDir = path.join(outputRoot, "screenshots");
const axeDir = path.join(outputRoot, "axe");

type Variant = { id: string; label: string };

// One column per theme in src/theme/themes.
const variants: Variant[] = [
  { id: "after-dark", label: "After Dark (default)" },
  { id: "classic", label: "Classic" },
];

type Screen = {
  id: string;
  label: string;
  path: string;
  ready: (page: Page) => Promise<void>;
  maxHeight: number;
  signedIn?: boolean;
};

const screens: Screen[] = [
  {
    id: "home",
    label: "Home feed",
    path: "/",
    maxHeight: 2600,
    ready: async (page) => {
      await expect(page.getByTestId("featured-movie-card").first()).toBeVisible(
        { timeout: 30_000 },
      );
    },
  },
  {
    id: "movie-detail",
    label: "Movie detail · The Lord of the Rings: The Return of the King",
    path: "/movie?id=11",
    maxHeight: 2400,
    ready: async (page) => {
      await expect(page.getByTestId("movie-detail-hero")).toBeVisible({
        timeout: 30_000,
      });
    },
  },
  {
    id: "your-ratings",
    label: "Your ratings (mocked signed-in library)",
    path: "/your-ratings",
    maxHeight: 2400,
    signedIn: true,
    ready: async (page) => {
      await expect(page.getByLabel("Your rating distribution")).toBeVisible({
        timeout: 30_000,
      });
    },
  },
  {
    id: "sign-in",
    label: "Sign in",
    path: "/login",
    maxHeight: 1400,
    ready: async (page) => {
      await expect(
        page.getByRole("button", { name: "Sign in", exact: true }),
      ).toBeVisible({ timeout: 30_000 });
    },
  },
];

const widths = [
  { id: "desktop", width: 1440, height: 900 },
  { id: "mobile", width: 390, height: 844 },
] as const;

const homeFeedCache = new Map<string, string>();

const replayHomeFeed = async (page: Page) => {
  await page.route("**/api/v1/recommendations/home-feed**", async (route) => {
    const body = route.request().postDataJSON() as { cursor?: string } | null;
    const key = body?.cursor ?? "first";
    const cached = homeFeedCache.get(key);
    if (cached) {
      await route.fulfill({ body: cached, contentType: "application/json" });
      return;
    }
    const response = await route.fetch();
    const text = await response.text();
    if (response.ok()) homeFeedCache.set(key, text);
    await route.fulfill({
      body: text,
      contentType: "application/json",
      status: response.status(),
    });
  });
};

const ratedMovieIds = [11, 41, 10, 14, 1, 2, 3, 4, 5, 6, 7, 8];
const ratingsByIndex = [10, 9, 9, 8, 8, 7, 9, 6, 8, 7, 5, 9];

const mockSignedInLibrary = async (page: Page) => {
  const movies = (
    await Promise.all(
      ratedMovieIds.map(async (id) => {
        const response = await page.request.get(
          `${backendUrl}/api/v1/movies/${id}`,
        );
        return response.ok()
          ? ((await response.json()) as { id: number })
          : null;
      }),
    )
  ).filter((movie): movie is { id: number } => movie !== null);

  await page.route("**/api/v1/auth/me", (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        id: 7,
        username: "niklas",
        email: "niklas@example.com",
        roles: ["ROLE_USER"],
      }),
    }),
  );
  await page.route("**/api/v1/accounts/me/profile", (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({ username: "niklas", email: "niklas@example.com" }),
    }),
  );
  await page.route("**/api/v1/accounts/niklas/library/ratings**", (route) =>
    route.fulfill({
      contentType: "application/json",
      body: JSON.stringify({
        items: {
          content: movies.map((movie, index) => ({
            accountId: 7,
            movieId: movie.id,
            rating: ratingsByIndex[index] ?? 8,
            movie,
          })),
          page: 0,
          size: 30,
          totalElements: movies.length,
          totalPages: 1,
          last: true,
        },
        insights: {
          totalRatings: 212,
          averageUserRating: 7.6,
          distribution: [
            { label: "0–3.9", count: 4 },
            { label: "4–5.9", count: 11 },
            { label: "6–6.9", count: 29 },
            { label: "7–7.9", count: 63 },
            { label: "8–8.9", count: 71 },
            { label: "9–10", count: 34 },
          ],
          favoriteGenres: [
            { label: "Drama", movieCount: 88, averageUserRating: 8.1 },
          ],
          favoriteDecades: [
            { label: "2010s", movieCount: 64, averageUserRating: 7.9 },
          ],
          averageImdbDifference: 0.4,
          definingMovies: movies.slice(0, 6).map((movie, index) => ({
            movie,
            userRating: ratingsByIndex[index] ?? 8,
            imdbDifference: 1.1,
          })),
        },
      }),
    }),
  );
};

export type TextContrastResult = {
  text: string;
  selector: string;
  fg: string;
  bg: string;
  ratio: number;
  required: number;
  fontSize: number;
  fontWeight: number;
  overMedia: boolean;
  /** For text on images: contrast against the brightest 5 % of pixels behind it. */
  mediaRatio?: number;
  probeId?: string;
  rect?: { x: number; y: number; width: number; height: number };
};

// Composites the real layers under each text element (elementsFromPoint), so
// translucent chips, scrims and overlays are measured the way they render.
// Text sitting on images is flagged as overMedia for visual review.
const auditTextContrast = (page: Page) =>
  page.evaluate((): TextContrastResult[] => {
    type Rgba = [number, number, number, number];
    const parse = (value: string): Rgba | null => {
      const match = /rgba?\(([^)]+)\)/.exec(value);
      if (!match) return null;
      const parts = match[1]!
        .split(/[\s,/]+/)
        .filter(Boolean)
        .map(Number);
      return [parts[0]!, parts[1]!, parts[2]!, parts[3] ?? 1];
    };
    const over = (top: Rgba, bottom: Rgba): Rgba => {
      const a = top[3] + bottom[3] * (1 - top[3]);
      if (a === 0) return [0, 0, 0, 0];
      const mix = (i: number) =>
        (top[i]! * top[3] + bottom[i]! * bottom[3] * (1 - top[3])) / a;
      return [mix(0), mix(1), mix(2), a];
    };
    const lum = ([r, g, b]: Rgba) =>
      [r, g, b]
        .map((c) => c / 255)
        .map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4))
        .reduce((sum, c, i) => sum + c * [0.2126, 0.7152, 0.0722][i]!, 0);
    const hex = ([r, g, b]: Rgba) =>
      `#${[r, g, b].map((c) => Math.round(c).toString(16).padStart(2, "0")).join("")}`.toUpperCase();
    const describe = (el: Element) => {
      const testId = el.closest("[data-testid]")?.getAttribute("data-testid");
      const tag = el.tagName.toLowerCase();
      return testId ? `[data-testid=${testId}] ${tag}` : tag;
    };

    const pageBg = parse(getComputedStyle(document.body).backgroundColor) ?? [
      0, 0, 0, 1,
    ];
    const results: TextContrastResult[] = [];
    const seen = new Set<Element>();
    const walker = document.createTreeWalker(
      document.body,
      NodeFilter.SHOW_TEXT,
    );
    for (let node = walker.nextNode(); node; node = walker.nextNode()) {
      const el = node.parentElement;
      const text = node.textContent?.trim() ?? "";
      if (
        !el ||
        !text ||
        seen.has(el) ||
        el.closest("[data-dev-toolbar],svg,[aria-hidden=true]")
      )
        continue;
      seen.add(el);
      const style = getComputedStyle(el);
      if (
        style.visibility === "hidden" ||
        style.display === "none" ||
        Number(style.opacity) === 0
      )
        continue;
      const range = document.createRange();
      range.selectNodeContents(node);
      // Clip to the element box so line-clamped text is measured where it is visible.
      const textRect = range.getBoundingClientRect();
      const box = el.getBoundingClientRect();
      const left = Math.max(textRect.left, box.left);
      const top = Math.max(textRect.top, box.top);
      const rect = {
        left,
        top,
        width: Math.min(textRect.right, box.right) - left,
        height: Math.min(textRect.bottom, box.bottom) - top,
      };
      if (rect.width < 2 || rect.height < 2) continue;
      const x = rect.left + rect.width / 2;
      const y = rect.top + rect.height / 2;
      if (x < 0 || y < 0 || x > window.innerWidth || y > window.innerHeight)
        continue;
      const stack = document.elementsFromPoint(x, y);
      const index = stack.findIndex(
        (candidate) =>
          candidate === el || el.contains(candidate) || candidate.contains(el),
      );
      if (index === -1) continue; // covered by a fixed overlay
      let bg: Rgba = [0, 0, 0, 0];
      let overMedia = false;
      for (const layer of stack.slice(index)) {
        const layerStyle = getComputedStyle(layer);
        // MUI dark Paper uses a translucent gradient overlay; only real media counts.
        if (
          ["IMG", "VIDEO", "CANVAS", "IFRAME"].includes(layer.tagName) ||
          layerStyle.backgroundImage.includes("url(")
        ) {
          overMedia = true;
        }
        const color = parse(layerStyle.backgroundColor);
        if (color && color[3] > 0) {
          bg = over(bg, color);
          if (bg[3] >= 0.999) break;
        }
        if (overMedia && bg[3] < 0.95) break;
      }
      bg = over(bg, pageBg);
      const fgRaw = parse(style.color) ?? [0, 0, 0, 1];
      const fg = over(
        [fgRaw[0], fgRaw[1], fgRaw[2], fgRaw[3] * Number(style.opacity)],
        bg,
      );
      const [l1, l2] = [lum(fg), lum(bg)];
      const ratio = (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
      const fontSize = parseFloat(style.fontSize);
      const fontWeight = Number(style.fontWeight) || 400;
      const large = fontSize >= 24 || (fontSize >= 18.66 && fontWeight >= 700);
      const required = large ? 3 : 4.5;
      if (ratio < required || overMedia) {
        const probeId = overMedia ? String(results.length) : undefined;
        if (probeId) el.setAttribute("data-contrast-probe", probeId);
        results.push({
          ...(probeId && {
            probeId,
            rect: {
              x: rect.left,
              y: rect.top,
              width: rect.width,
              height: rect.height,
            },
          }),
          text: text.slice(0, 60),
          selector: describe(el),
          fg: hex(fg),
          bg: hex(bg),
          ratio: Math.round(ratio * 100) / 100,
          required,
          fontSize,
          fontWeight,
          overMedia,
        });
      }
    }
    return results;
  });

// Measures text on images from real pixels: hide the probed text, screenshot,
// then read the pixels behind each text box in a canvas.
const measureMediaContrast = async (
  page: Page,
  results: TextContrastResult[],
) => {
  const probes = results.filter((result) => result.probeId && result.rect);
  if (probes.length === 0) return results;
  const style = await page.addStyleTag({
    content:
      "[data-contrast-probe]{color:transparent!important;text-shadow:none!important}",
  });
  const image = (await page.screenshot({ animations: "disabled" })).toString(
    "base64",
  );
  await style.evaluate((node) => (node as HTMLElement).remove());
  const ratios = await page.evaluate(
    async ({ image, probes }) => {
      const bitmap = new Image();
      bitmap.src = `data:image/png;base64,${image}`;
      await bitmap.decode();
      const canvas = document.createElement("canvas");
      canvas.width = bitmap.width;
      canvas.height = bitmap.height;
      const context = canvas.getContext("2d", { willReadFrequently: true })!;
      context.drawImage(bitmap, 0, 0);
      const lum = (r: number, g: number, b: number) =>
        [r, g, b]
          .map((c) => c / 255)
          .map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4))
          .reduce((sum, c, i) => sum + c * [0.2126, 0.7152, 0.0722][i]!, 0);
      const hexLum = (hex: string) =>
        lum(
          parseInt(hex.slice(1, 3), 16),
          parseInt(hex.slice(3, 5), 16),
          parseInt(hex.slice(5, 7), 16),
        );
      return probes.map(({ fg, rect }) => {
        const x = Math.max(0, Math.floor(rect!.x));
        const y = Math.max(0, Math.floor(rect!.y));
        const width = Math.max(
          1,
          Math.min(canvas.width - x, Math.ceil(rect!.width)),
        );
        const height = Math.max(
          1,
          Math.min(canvas.height - y, Math.ceil(rect!.height)),
        );
        const { data } = context.getImageData(x, y, width, height);
        const values: number[] = [];
        for (let i = 0; i < data.length; i += 4)
          values.push(lum(data[i]!, data[i + 1]!, data[i + 2]!));
        values.sort((a, b) => a - b);
        const fgLum = hexLum(fg);
        // Light text: the brightest background pixels are the worst case, dark text the darkest.
        const worst =
          fgLum > 0.18
            ? values[Math.floor(values.length * 0.95)]!
            : values[Math.floor(values.length * 0.05)]!;
        return (
          Math.round(
            ((Math.max(fgLum, worst) + 0.05) /
              (Math.min(fgLum, worst) + 0.05)) *
              100,
          ) / 100
        );
      });
    },
    { image, probes },
  );
  probes.forEach((probe, index) => {
    probe.mediaRatio = ratios[index]!;
  });
  return results;
};

const settle = async (page: Page, maxHeight: number) => {
  await page.evaluate(() => document.fonts.ready);
  // Scroll through the capture area so lazy posters load, then return to top.
  await page.evaluate(async (limit) => {
    const step = Math.max(300, Math.floor(window.innerHeight * 0.8));
    for (
      let y = 0;
      y < Math.min(limit, document.body.scrollHeight);
      y += step
    ) {
      window.scrollTo(0, y);
      await new Promise((resolve) => setTimeout(resolve, 180));
    }
    window.scrollTo(0, 0);
  }, maxHeight);
  await page
    .waitForFunction(
      () => Array.from(document.images).every((image) => image.complete),
      undefined,
      { timeout: 10_000 },
    )
    .catch(() => undefined);
  await page.addStyleTag({
    content: "[data-dev-toolbar]{display:none!important}",
  });
  await page.waitForTimeout(400);
};

mkdirSync(screenshotDir, { recursive: true });
mkdirSync(axeDir, { recursive: true });

for (const screen of screens) {
  for (const width of widths) {
    for (const variant of variants) {
      test(`${screen.id} · ${width.id} · ${variant.id}`, async ({ page }) => {
        await page.setViewportSize({
          width: width.width,
          height: width.height,
        });
        await replayHomeFeed(page);
        if (screen.signedIn) await mockSignedInLibrary(page);

        const separator = screen.path.includes("?") ? "&" : "?";
        await page.goto(`${screen.path}${separator}theme=${variant.id}`);
        await screen.ready(page);
        await settle(page, screen.maxHeight);

        const pageHeight = await page.evaluate(
          () => document.documentElement.scrollHeight,
        );
        const file = `${screen.id}--${width.id}--${variant.id}.png`;
        await page.screenshot({
          animations: "disabled",
          clip: {
            x: 0,
            y: 0,
            width: width.width,
            height: Math.min(pageHeight, screen.maxHeight),
          },
          fullPage: true,
          path: path.join(screenshotDir, file),
        });

        // Expand the viewport over the captured area so every text node can be hit-tested.
        await page.setViewportSize({
          width: width.width,
          height: Math.min(pageHeight, screen.maxHeight),
        });
        await page.evaluate(() => window.scrollTo(0, 0));
        await page.waitForTimeout(300);
        const textContrast = await measureMediaContrast(
          page,
          await auditTextContrast(page),
        );

        const axe = await new AxeBuilder({ page })
          .withRules(["color-contrast"])
          .exclude("[data-dev-toolbar]")
          .analyze();
        writeFileSync(
          path.join(axeDir, `${screen.id}--${width.id}--${variant.id}.json`),
          JSON.stringify(
            {
              screen: screen.id,
              width: width.id,
              variant: variant.id,
              textContrast,
              violations: axe.violations.flatMap((violation) =>
                violation.nodes.map((node) => ({
                  target: node.target.join(" "),
                  html: node.html.slice(0, 200),
                  data: (node.any[0]?.data ?? null) as unknown,
                })),
              ),
              incomplete: axe.incomplete.flatMap((result) =>
                result.nodes.map((node) => ({
                  target: node.target.join(" "),
                  data: (node.any[0]?.data ?? null) as unknown,
                })),
              ).length,
            },
            null,
            2,
          ),
        );
      });
    }
  }
}

test.afterAll(() => {
  writeFileSync(
    path.join(outputRoot, "manifest.json"),
    JSON.stringify(
      {
        generatedAt: new Date().toISOString(),
        variants,
        screens: screens.map(({ id, label }) => ({ id, label })),
        widths,
      },
      null,
      2,
    ),
  );
});
