// Generates the web icon set for every theme from the SVG sources in docs/brand.
// Output: frontend/public/brand/<theme>/ (checked in). Rendering uses Playwright's
// Chromium so no image tooling is needed. Run: yarn brand:assets
import { chromium, type Page } from "@playwright/test";
import {
  mkdirSync,
  mkdtempSync,
  readFileSync,
  rmSync,
  writeFileSync,
} from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import { pathToFileURL } from "node:url";

type ThemeBrand = {
  id: string;
  /** Header mark: transparent, shown next to the app name. */
  mark: string;
  /** App icon: artwork on its own rounded square. Source for favicons and PWA icons. */
  icon: string;
  /** Full-bleed colour behind opaque and maskable icons; matches the theme page colour. */
  background: string;
  /** Scale of the mark inside the maskable safe circle (radius 40 % of the icon). */
  maskableScale: number;
  og: { text: string; secondary: string; accent: string };
};

const docsBrand = path.resolve("../docs/brand");
const publicBrand = path.resolve("public/brand");
const manrope = path.resolve(
  "node_modules/@fontsource-variable/manrope/files/manrope-latin-wght-normal.woff2",
);

// Colours mirror the theme tokens in src/theme/themes; keep them in sync when a theme changes.
const themes: ThemeBrand[] = [
  {
    id: "after-dark",
    mark: "after-dark/logo/reflections-mark.svg",
    icon: "after-dark/logo/reflections-icon.svg",
    background: "#0A0C12",
    maskableScale: 0.56,
    og: { text: "#FFFFFF", secondary: "#BFC5D1", accent: "#FF2D3F" },
  },
  {
    id: "classic",
    mark: "classic/logo/brand-logo.svg",
    icon: "classic/logo/brand-logo.svg",
    background: "#0E1626",
    maskableScale: 0.62,
    og: { text: "#FFFFFF", secondary: "#C4C8D0", accent: "#F5C518" },
  },
];

/** Keep in sync with defaultThemeId in src/theme/themes/index.ts (checked by the theme tests). */
const defaultThemeId = "after-dark";

const workDir = mkdtempSync(path.join(tmpdir(), "brand-assets-"));

const render = async (
  page: Page,
  {
    width,
    height,
    body,
    transparent,
  }: { width: number; height: number; body: string; transparent: boolean },
) => {
  const file = path.join(workDir, "render.html");
  writeFileSync(
    file,
    `<!doctype html><html><head><style>
      @font-face { font-family: "Manrope"; src: url("${pathToFileURL(manrope).href}") format("woff2"); font-weight: 200 800; }
      html, body { margin: 0; width: ${width}px; height: ${height}px; overflow: hidden; background: transparent; }
      img { display: block; }
    </style></head><body>${body}</body></html>`,
  );
  await page.setViewportSize({ width, height });
  await page.goto(pathToFileURL(file).href);
  await page.evaluate(async () => {
    await document.fonts.ready;
    await Promise.all(
      Array.from(document.images).map((image) => image.decode()),
    );
  });
  return page.screenshot({ omitBackground: transparent, type: "png" });
};

const svgImage = (src: string, size: number, extra = "") =>
  `<img src="${pathToFileURL(src).href}" width="${size}" height="${size}" style="${extra}" alt="">`;

/** ICO with PNG-encoded entries (supported by all current browsers). */
const buildIco = (images: Array<{ size: number; png: Buffer }>) => {
  const header = Buffer.alloc(6 + 16 * images.length);
  header.writeUInt16LE(0, 0);
  header.writeUInt16LE(1, 2);
  header.writeUInt16LE(images.length, 4);
  let offset = header.length;
  images.forEach(({ size, png }, index) => {
    const entry = 6 + 16 * index;
    header.writeUInt8(size >= 256 ? 0 : size, entry);
    header.writeUInt8(size >= 256 ? 0 : size, entry + 1);
    header.writeUInt8(0, entry + 2);
    header.writeUInt8(0, entry + 3);
    header.writeUInt16LE(1, entry + 4);
    header.writeUInt16LE(32, entry + 6);
    header.writeUInt32LE(png.length, entry + 8);
    header.writeUInt32LE(offset, entry + 12);
    offset += png.length;
  });
  return Buffer.concat([header, ...images.map(({ png }) => png)]);
};

const browser = await chromium.launch();
const page = await browser.newPage({ deviceScaleFactor: 1 });

try {
  for (const theme of themes) {
    const outDir = path.join(publicBrand, theme.id);
    mkdirSync(outDir, { recursive: true });
    const mark = path.join(docsBrand, theme.mark);
    const icon = path.join(docsBrand, theme.icon);
    const write = (name: string, data: Buffer | string) =>
      writeFileSync(path.join(outDir, name), data);
    const iconPng = (size: number) =>
      render(page, {
        width: size,
        height: size,
        body: svgImage(icon, size),
        transparent: true,
      });

    // Written (not copied) so the served files get normal read permissions.
    write("mark.svg", readFileSync(mark));
    write("favicon.svg", readFileSync(icon));

    // One page renders one image at a time, so the sizes are rendered in sequence.
    const icoImages: Array<{ size: number; png: Buffer }> = [];
    for (const size of [16, 32, 48])
      icoImages.push({ size, png: await iconPng(size) });
    write("favicon.ico", buildIco(icoImages));
    write("favicon-96.png", await iconPng(96));
    write("app-icon-192.png", await iconPng(192));
    write("app-icon-512.png", await iconPng(512));

    // iOS shows transparent corners as black, so the touch icon is opaque and full bleed.
    write(
      "apple-touch-icon.png",
      await render(page, {
        width: 180,
        height: 180,
        transparent: false,
        body: `<div style="width:180px;height:180px;background:${theme.background};display:grid;place-items:center">${svgImage(icon, 180)}</div>`,
      }),
    );

    // Maskable: launchers crop to any shape, so the artwork stays inside the central safe circle.
    const maskableArt = Math.round(512 * theme.maskableScale);
    write(
      "app-icon-maskable-512.png",
      await render(page, {
        width: 512,
        height: 512,
        transparent: false,
        body: `<div style="width:512px;height:512px;background:${theme.background};display:grid;place-items:center">${svgImage(mark, maskableArt)}</div>`,
      }),
    );

    write(
      "og-image.png",
      await render(page, {
        width: 1200,
        height: 630,
        transparent: false,
        body: `<div style="width:1200px;height:630px;box-sizing:border-box;padding:0 110px;background:${theme.background};display:flex;align-items:center;gap:64px;font-family:Manrope,sans-serif">
          ${svgImage(icon, 280)}
          <div>
            <div style="color:${theme.og.text};font-size:84px;font-weight:800;line-height:1;letter-spacing:-1px">Popcorn Society</div>
            <div style="color:${theme.og.secondary};font-size:36px;font-weight:500;margin-top:22px">watch together, after dark</div>
            <div style="width:96px;height:6px;border-radius:3px;background:${theme.og.accent};margin-top:36px"></div>
          </div>
        </div>`,
      }),
    );

    // Browsers, bookmark tools and crawlers still request /favicon.ico directly.
    if (theme.id === defaultThemeId) {
      writeFileSync(
        path.resolve("public/favicon.ico"),
        readFileSync(path.join(outDir, "favicon.ico")),
      );
    }

    console.log(`${theme.id}: 9 files in public/brand/${theme.id}`);
  }
} finally {
  await browser.close();
  rmSync(workDir, { recursive: true, force: true });
}
