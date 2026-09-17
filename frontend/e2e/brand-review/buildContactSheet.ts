// Builds test-results/brand-review/sheet/index.html from the brand-review capture:
// a screenshot grid (rows: screen × width, columns: variants) plus a WCAG AA
// contrast report for the After Dark tokens and the axe results per screen.
// Run after the capture: node e2e/brand-review/buildContactSheet.ts
import { existsSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import path from "node:path";

type Manifest = {
  generatedAt: string;
  variants: Array<{ id: string; label: string }>;
  screens: Array<{ id: string; label: string }>;
  widths: Array<{ id: string; width: number }>;
};

type AxeNode = {
  target: string;
  html: string;
  data: {
    fgColor?: string;
    bgColor?: string;
    contrastRatio?: number;
    fontSize?: string;
    fontWeight?: string;
    expectedContrastRatio?: string;
  } | null;
};

type TextContrast = {
  text: string;
  selector: string;
  fg: string;
  bg: string;
  ratio: number;
  required: number;
  fontSize: number;
  fontWeight: number;
  overMedia: boolean;
  mediaRatio?: number;
};

type AxeFile = {
  screen: string;
  width: string;
  variant: string;
  violations: AxeNode[];
  incomplete: number;
  textContrast?: TextContrast[];
};

const sheetDir = path.resolve("test-results/brand-review/sheet");
const manifest = JSON.parse(
  readFileSync(path.join(sheetDir, "manifest.json"), "utf8"),
) as Manifest;

// ---- colour maths (WCAG 2.x) ----
const hexToRgb = (hex: string): [number, number, number] => {
  const value = hex.replace("#", "");
  return [0, 2, 4].map((i) => parseInt(value.slice(i, i + 2), 16)) as [
    number,
    number,
    number,
  ];
};
const rgbToHex = ([r, g, b]: number[]) =>
  `#${[r, g, b]
    .map((c) =>
      Math.round(Math.min(255, Math.max(0, c ?? 0)))
        .toString(16)
        .padStart(2, "0"),
    )
    .join("")
    .toUpperCase()}`;
const luminance = (hex: string) =>
  hexToRgb(hex)
    .map((c) => c / 255)
    .map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4))
    .reduce((sum, c, i) => sum + c * [0.2126, 0.7152, 0.0722][i]!, 0);
const contrast = (a: string, b: string) => {
  const [x, y] = [luminance(a), luminance(b)];
  return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
};
// Lighten towards white (dark UI) in small steps until the target ratio is met,
// keeping hue so the suggestion stays recognisably the same token.
const suggest = (fg: string, bg: string, target: number) => {
  const rgb = hexToRgb(fg);
  for (let t = 0; t <= 1.0001; t += 0.01) {
    const candidate = rgbToHex(rgb.map((c) => c + (255 - c) * t));
    if (contrast(candidate, bg) >= target) return candidate;
  }
  return null;
};

// ---- axe results ----
const axeDir = path.join(sheetDir, "axe");
const axeFiles: AxeFile[] = existsSync(axeDir)
  ? readdirSync(axeDir)
      .filter((f) => f.endsWith(".json"))
      .map(
        (f) =>
          JSON.parse(readFileSync(path.join(axeDir, f), "utf8")) as AxeFile,
      )
  : [];

type Finding = {
  fg: string;
  bg: string;
  ratio: number;
  expected: string;
  size: string;
  screens: Set<string>;
  targets: Set<string>;
  variants: Set<string>;
  source: string;
};
const findings = new Map<string, Finding>();
const addFinding = (
  file: AxeFile,
  finding: Omit<Finding, "screens" | "targets" | "variants">,
  target: string,
) => {
  const key = `${finding.source}|${finding.fg}|${finding.bg}|${finding.expected}|${file.variant}|${finding.source === "image" ? target : ""}`;
  const existing = findings.get(key) ?? {
    ...finding,
    screens: new Set<string>(),
    targets: new Set<string>(),
    variants: new Set<string>(),
  };
  existing.ratio = Math.min(existing.ratio, finding.ratio);
  existing.screens.add(`${file.screen} (${file.width})`);
  existing.variants.add(file.variant);
  if (existing.targets.size < 3) existing.targets.add(target);
  findings.set(key, existing);
};
for (const file of axeFiles) {
  for (const result of file.textContrast ?? []) {
    const size = `${result.fontSize}px ${result.fontWeight}`;
    const target = `“${result.text}” ${result.selector}`;
    if (!result.overMedia && result.ratio < result.required) {
      addFinding(
        file,
        {
          fg: result.fg,
          bg: result.bg,
          ratio: result.ratio,
          expected: `${result.required}:1`,
          size,
          source: "solid",
        },
        target,
      );
    } else if (
      result.overMedia &&
      result.mediaRatio !== undefined &&
      result.mediaRatio < result.required
    ) {
      addFinding(
        file,
        {
          fg: result.fg,
          bg: "image",
          ratio: result.mediaRatio,
          expected: `${result.required}:1`,
          size,
          source: "image",
        },
        target,
      );
    }
  }
}
for (const file of axeFiles) {
  for (const node of file.violations) {
    const data = node.data;
    if (!data?.fgColor || !data.bgColor) continue;
    const key = `${data.fgColor}|${data.bgColor}|${file.variant}`;
    const finding = findings.get(key) ?? {
      fg: data.fgColor,
      bg: data.bgColor,
      ratio: data.contrastRatio ?? contrast(data.fgColor, data.bgColor),
      expected: data.expectedContrastRatio ?? "4.5:1",
      size: `${data.fontSize ?? "?"} ${data.fontWeight ?? ""}`.trim(),
      screens: new Set<string>(),
      targets: new Set<string>(),
      variants: new Set<string>(),
      source: "axe",
    };
    finding.screens.add(`${file.screen} (${file.width})`);
    finding.variants.add(file.variant);
    if (finding.targets.size < 3) finding.targets.add(node.target);
    findings.set(key, finding);
  }
}
const findingRows = Array.from(findings.values()).sort(
  (a, b) => a.ratio - b.ratio,
);
const afterDarkFindings = findingRows.filter(
  (f) => !(f.variants.size === 1 && f.variants.has("current")),
);
const currentFindings = findingRows.filter((f) => f.variants.has("current"));

const escape = (value: string) =>
  value.replace(
    /[&<>"]/g,
    (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" })[c]!,
  );
const swatch = (hex: string) =>
  `<span class="sw" style="background:${hex}"></span><code>${hex}</code>`;

const findingTable = (rows: Finding[]) =>
  rows.length === 0
    ? `<p class="muted">No axe colour-contrast violations.</p>`
    : `<table><thead><tr><th>Text</th><th>Background</th><th>Ratio</th><th>Needs</th><th>Size</th><th>Suggested text colour</th><th>Where</th></tr></thead><tbody>${rows
        .map((f) => {
          const target = parseFloat(f.expected) || 4.5;
          const suggestion =
            /^#[0-9a-f]{6}$/i.test(f.fg) && /^#[0-9a-f]{6}$/i.test(f.bg)
              ? suggest(f.fg, f.bg, target)
              : null;
          return `<tr><td>${swatch(f.fg)}</td><td>${f.bg === "image" ? "image (95th percentile pixel)" : swatch(f.bg)}</td><td class="num">${f.ratio.toFixed(2)}</td><td class="num">${escape(f.expected)}</td><td>${escape(f.size)}</td><td>${suggestion ? swatch(suggestion) : "—"}</td><td class="where">${escape(Array.from(f.screens).join(", "))}<br><span class="muted">${escape(Array.from(f.targets).join(" · "))}</span></td></tr>`;
        })
        .join("")}</tbody></table>`;

const grid = manifest.screens
  .map((screen) =>
    manifest.widths
      .map((width) => {
        const cells = manifest.variants
          .map((variant) => {
            const file = `screenshots/${screen.id}--${width.id}--${variant.id}.png`;
            return existsSync(path.join(sheetDir, file))
              ? `<figure><a href="${file}" target="_blank" rel="noopener"><img loading="lazy" src="${file}" alt="${escape(`${screen.label}, ${width.id}, ${variant.label}`)}"></a></figure>`
              : `<figure class="missing">not captured</figure>`;
          })
          .join("");
        return `<section class="row" id="${screen.id}-${width.id}"><h3>${escape(screen.label)} <span class="muted">· ${width.id} ${width.width}px</span></h3><div class="cells ${width.id}">${cells}</div></section>`;
      })
      .join(""),
  )
  .join("");

const html = `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>Theme Contact Sheet</title>
<style>
:root{--bg:#0A0C12;--panel:#141925;--line:#2A3142;--text:#E9E6EF;--muted:#9AA3B5;--ok:#7BD88F;--bad:#FF8A4C}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:14px/1.5 system-ui,sans-serif}
header{position:sticky;top:0;z-index:2;background:rgba(10,12,18,.96);border-bottom:1px solid var(--line);padding:12px 16px}
h1{font-size:18px;margin:0 0 6px}h2{font-size:17px;margin:40px 0 8px}h3{font-size:14px;margin:0 0 8px;font-weight:600}
.muted{color:var(--muted)}nav a{color:var(--text);margin-right:12px;font-size:13px}
.labels,.cells{display:grid;grid-template-columns:repeat(${manifest.variants.length},minmax(0,1fr));gap:10px}
.labels div{font-size:12px;color:var(--muted)}main{padding:16px}
.row{margin:22px 0;padding-top:8px;border-top:1px solid var(--line)}
figure{margin:0;background:var(--panel);border:1px solid var(--line);border-radius:4px;overflow:hidden}
.cells.desktop figure{max-height:720px;overflow-y:auto}.cells.mobile figure{max-height:900px;overflow-y:auto}
figure img{display:block;width:100%;height:auto}.missing{padding:24px;color:var(--muted)}
table{border-collapse:collapse;width:100%;margin:8px 0 16px;font-size:13px}th,td{border-bottom:1px solid var(--line);padding:6px 8px;text-align:left;vertical-align:top}
th{color:var(--muted);font-weight:500}.num{font-variant-numeric:tabular-nums}.where{font-size:12px;max-width:420px;word-break:break-word}
.sw{display:inline-block;width:14px;height:14px;border-radius:3px;border:1px solid #fff3;vertical-align:-2px;margin-right:6px}
code{font-size:12px}.ok{color:var(--ok)}.bad{color:var(--bad);font-weight:600}.report{max-width:1200px}
</style></head><body>
<header><h1>Theme contact sheet</h1>
<div class="muted">Captured ${escape(manifest.generatedAt)} from the local stack. Rows are screen × width, columns are variants. Click a shot to open it full size; each cell scrolls.</div>
<nav>${manifest.screens.map((s) => `<a href="#${s.id}-desktop">${escape(s.label.split(" · ")[0]!)}</a>`).join("")}<a href="#contrast">Contrast report</a></nav>
<div class="labels" style="margin-top:8px">${manifest.variants.map((v) => `<div>${escape(v.label)}</div>`).join("")}</div>
</header>
<main>${grid}
<section class="report" id="contrast">
<p class="muted">Token pairs (text, accent, controls, stars, chart series) are enforced for every theme by <code>src/theme/theme.test.ts</code>.</p>
<h2>Text contrast in the real app: After Dark</h2>
<p class="muted">Every visible text node is checked against the layers actually rendered beneath it (translucent chips, scrims and overlays composited). Text on images is measured from screenshot pixels with the text hidden, against the brightest 5 % of pixels behind it. Axe colour-contrast violations are included as well (axe left ${axeFiles.filter((f) => f.variant === "after-dark").reduce((n, f) => n + f.incomplete, 0)} nodes undecided, which is why the composited check exists). Rows are deduplicated across both widths.</p>
${findingTable(afterDarkFindings)}
<h2>Text contrast in the real app: Classic</h2>
${findingTable(currentFindings)}
</section>
</main></body></html>`;

writeFileSync(path.join(sheetDir, "index.html"), html);
console.log(
  `Wrote ${path.join(sheetDir, "index.html")} (${afterDarkFindings.length} After Dark and ${currentFindings.length} current contrast findings)`,
);
