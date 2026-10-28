/* Colour maths and the picker's square. Artwork-level data: it deals in raw colour. */

export function hsvToHex(h: number, s: number, v: number) {
  const f = (n: number) => {
    const k = (n + h / 60) % 6;
    return v - v * s * Math.max(0, Math.min(k, 4 - k, 1));
  };
  const x = (n: number) => Math.round(f(n) * 255).toString(16).padStart(2, '0');
  return ('#' + x(5) + x(3) + x(1)).toUpperCase();
}

export function hexToHsv(hex: string): [number, number, number] {
  const n = parseInt(hex.slice(1), 16);
  const [r, g, b] = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map((v) => v / 255);
  const mx = Math.max(r, g, b);
  const mn = Math.min(r, g, b);
  const d = mx - mn;
  let h = 0;
  if (d) {
    if (mx === r) h = ((g - b) / d) % 6;
    else if (mx === g) h = (b - r) / d + 2;
    else h = (r - g) / d + 4;
    h *= 60;
    if (h < 0) h += 360;
  }
  return [h, mx ? d / mx : 0, mx];
}

/** Paint the saturation and brightness square for a hue. */
export function paintSquare(cv: HTMLCanvasElement, w: number, h: number, hue: number) {
  const c = cv.getContext('2d');
  if (!c) return;
  c.setTransform(2, 0, 0, 2, 0, 0);
  c.fillStyle = hsvToHex(hue, 1, 1);
  c.fillRect(0, 0, w, h);
  let g = c.createLinearGradient(0, 0, w, 0);
  g.addColorStop(0, '#fff');
  g.addColorStop(1, 'rgba(255,255,255,0)');
  c.fillStyle = g;
  c.fillRect(0, 0, w, h);
  g = c.createLinearGradient(0, 0, 0, h);
  g.addColorStop(0, 'rgba(0,0,0,0)');
  g.addColorStop(1, '#000');
  c.fillStyle = g;
  c.fillRect(0, 0, w, h);
}

export function hexToRgb(hex: string): [number, number, number] {
  const n = parseInt(hex.slice(1), 16);
  return [(n >> 16) & 255, (n >> 8) & 255, n & 255];
}


export const hexToRgba = (hex: string, a: number) => inkAt(hex, a);

/** A colour with an alpha from 0 to 1, as a CSS string. */
export function inkAt(hex: string, a: number) {
  const [r, g, b] = hexToRgb(hex);
  return `rgba(${r},${g},${b},${a.toFixed(3)})`;
}
