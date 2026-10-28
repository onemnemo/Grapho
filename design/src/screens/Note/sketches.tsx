import type { ReactNode } from 'react';

export type SampleName = 'circle' | 'rect' | 'tri' | 'arrow';
type Pt = [number, number];

// A tiny seeded generator so the same sample always wobbles the same way.
let seed = 11;
const rnd = () => {
  seed = (seed * 16807) % 2147483647;
  return seed / 2147483647;
};
const seg = (a: Pt, b: Pt, n: number): Pt[] => [...Array(n)].map((_, i) => [a[0] + ((b[0] - a[0]) * i) / n, a[1] + ((b[1] - a[1]) * i) / n]);
const poly = (vs: Pt[]): Pt[] => [...vs.flatMap((v, i) => seg(v, vs[(i + 1) % vs.length], 7)), vs[0]];
const circle = (cx: number, cy: number, r: number): Pt[] =>
  [...Array(35)].map((_, i) => {
    const a = (i / 30) * Math.PI * 2 - 1.2;
    const rr = r * (1 + 0.05 * Math.sin(a * 2 + 1));
    return [cx + rr * Math.cos(a), cy + rr * 0.93 * Math.sin(a)];
  });

const GEO: Record<SampleName, () => Pt[]> = {
  circle: () => circle(400, 190, 90),
  rect: () => poly([[310, 120], [490, 114], [494, 250], [306, 256]]),
  tri: () => poly([[400, 100], [500, 270], [298, 262]]),
  arrow: () => [...seg([300, 200], [500, 190], 14), ...seg([500, 190], [462, 160], 4), ...seg([500, 190], [466, 226], 4)],
};

/** The hand drawn points for a sample shape. */
export function roughPoints(name: SampleName): Pt[] {
  seed = 11;
  return GEO[name]().map(([x, y]) => [x + (rnd() - 0.5) * 3.2, y + (rnd() - 0.5) * 3.2]);
}
export const toPath = (pts: Pt[]) => 'M' + pts.map((p) => `${p[0].toFixed(1)} ${p[1].toFixed(1)}`).join(' L');

/** The clean shape for each suggestion. */
export const CLEAN: Record<string, ReactNode> = {
  circle: <circle cx="400" cy="190" r="90" />,
  rect: <rect x="308" y="118" width="184" height="136" />,
  tri: <path d="M400 100L500 270H300z" />,
  arrow: <path d="M300 195H500M464 162L500 195L464 228" />,
  ellipse: <ellipse cx="400" cy="190" rx="105" ry="80" />,
  rrect: <rect x="308" y="118" width="184" height="136" rx="30" />,
  poly: <path d="M400 96L496 164L462 270H338L304 164z" />,
  line: <path d="M300 220L500 160" />,
};
