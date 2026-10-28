/* Colour maths for the studio: hex, rgb and hsv, mixing and luminance. */
export const clamp = (v: number, a: number, b: number) => Math.min(b, Math.max(a, v));
export const h2rgb = (h: string): [number, number, number] => {
  const m = /^#?([0-9a-f]{6})$/i.exec(h || '');
  if (!m) return [128, 128, 128];
  const n = parseInt(m[1], 16);
  return [n >> 16, (n >> 8) & 255, n & 255];
};
export const rgb2h = (r: number, g: number, b: number) => '#' + [r, g, b].map((v) => Math.round(clamp(v, 0, 255)).toString(16).padStart(2, '0')).join('');
export const lum = (hex: string) => {
  const m = /^#?([0-9a-f]{6})$/i.exec(hex || '');
  if (!m) return 0.5;
  const n = parseInt(m[1], 16);
  return (0.2126 * (n >> 16) + 0.7152 * ((n >> 8) & 255) + 0.0722 * (n & 255)) / 255;
};
export const mixHex = (a: string, b: string, t: number) => {
  const A = h2rgb(a), B = h2rgb(b);
  return rgb2h(A[0] + (B[0] - A[0]) * t, A[1] + (B[1] - A[1]) * t, A[2] + (B[2] - A[2]) * t);
};
export const lighten = (h: string, t: number) => mixHex(h, '#ffffff', t);
export const darken = (h: string, t: number) => mixHex(h, '#000000', t);
export const shade = (hex: string, amt: number) => {
  const [r, g, b] = h2rgb(hex);
  return rgb2h(r + amt, g + amt, b + amt);
};
export function hex2hsv(h: string): [number, number, number] {
  const [r, g, b] = h2rgb(h).map((v) => v / 255);
  const mx = Math.max(r, g, b), mn = Math.min(r, g, b), d = mx - mn;
  let hh = 0;
  if (d) {
    if (mx === r) hh = ((g - b) / d) % 6;
    else if (mx === g) hh = (b - r) / d + 2;
    else hh = (r - g) / d + 4;
    hh *= 60;
    if (hh < 0) hh += 360;
  }
  return [hh, mx ? d / mx : 0, mx];
}
export function hsv2hex(h: number, s: number, v: number) {
  const c = v * s, x = c * (1 - Math.abs(((h / 60) % 2) - 1)), m = v - c;
  let r = 0, g = 0, b = 0;
  if (h < 60) [r, g, b] = [c, x, 0];
  else if (h < 120) [r, g, b] = [x, c, 0];
  else if (h < 180) [r, g, b] = [0, c, x];
  else if (h < 240) [r, g, b] = [0, x, c];
  else if (h < 300) [r, g, b] = [x, 0, c];
  else [r, g, b] = [c, 0, x];
  return rgb2h((r + m) * 255, (g + m) * 255, (b + m) * 255);
}
