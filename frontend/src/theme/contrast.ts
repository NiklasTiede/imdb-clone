type Rgba = [number, number, number, number];

const parseColor = (value: string): Rgba => {
  const hex = /^#([0-9a-f]{6})$/i.exec(value);
  if (hex) {
    const digits = hex[1]!;
    return [0, 2, 4]
      .map((i) => parseInt(digits.slice(i, i + 2), 16))
      .concat(1) as Rgba;
  }
  const rgba = /^rgba?\(([^)]+)\)$/i.exec(value);
  if (rgba) {
    const [r = 0, g = 0, b = 0, a = 1] = rgba[1]!
      .split(",")
      .map((part) => Number(part.trim()));
    return [r, g, b, a];
  }
  throw new Error(`Unsupported colour: ${value}`);
};

const over = (top: Rgba, bottom: Rgba): Rgba => {
  const alpha = top[3];
  return [0, 1, 2]
    .map((i) => top[i]! * alpha + bottom[i]! * (1 - alpha))
    .concat(1) as Rgba;
};

const luminance = ([r, g, b]: Rgba) =>
  [r, g, b]
    .map((channel) => channel / 255)
    .map((channel) =>
      channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4,
    )
    .reduce(
      (sum, channel, index) => sum + channel * [0.2126, 0.7152, 0.0722][index]!,
      0,
    );

/** WCAG 2.x contrast ratio; a translucent foreground is composited onto the (opaque) background. */
export const contrastRatio = (foreground: string, background: string) => {
  const bg = parseColor(background);
  const fg = over(parseColor(foreground), bg);
  const [a, b] = [luminance(fg), luminance(bg)];
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
};
