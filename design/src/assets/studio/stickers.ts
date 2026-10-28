/* Die-cut stickers, 100 by 100. The outline and shadow are drawn by the renderer, not here. */
const S = (inner: string) => `<svg viewBox="0 0 100 100" xmlns="http://www.w3.org/2000/svg">${inner}</svg>`;
const INK = '#17181C';

export const DOODLES = {
  star: S(`<path d="M50 8l12.5 28 30 3.2-22.5 20 6.5 30L50 74.5 23.5 89.2 30 59.2 7.5 39.200l30-3.200z" fill="#F6C453" stroke="${INK}" stroke-width="3.5" stroke-linejoin="round"/>`),
  heart: S(`<path d="M50 88C20 66 8 48 8 32 8 18 19 10 31 10c8 0 15 4 19 11 4-7 11-11 19-11 12 0 23 8 23 22 0 16-12 34-42 56z" fill="#E0402E" stroke="${INK}" stroke-width="3.5" stroke-linejoin="round"/><path d="M24 26c2-4 6-6 10-5" stroke="#fff" stroke-width="4" stroke-linecap="round" fill="none" opacity=".8"/>`),
  sparkle: S(`<path d="M50 6c3 26 18 41 44 44-26 3-41 18-44 44-3-26-18-41-44-44 26-3 41-18 44-44z" fill="#fff" stroke="${INK}" stroke-width="3.5" stroke-linejoin="round"/>`),
  flower: S(`<g fill="#F4A9C6" stroke="${INK}" stroke-width="3"><circle cx="50" cy="24" r="17"/><circle cx="76" cy="43" r="17"/><circle cx="66" cy="73" r="17"/><circle cx="34" cy="73" r="17"/><circle cx="24" cy="43" r="17"/></g><circle cx="50" cy="52" r="15" fill="#F6C453" stroke="${INK}" stroke-width="3"/>`),
  cloud: S(`<path d="M26 74a18 18 0 0 1-2-35.900A24 24 0 0 1 70 34a20 20 0 0 1 6 40z" fill="#fff" stroke="${INK}" stroke-width="3.5" stroke-linejoin="round"/>`),
  smile: S(`<circle cx="50" cy="50" r="42" fill="#F6C453" stroke="${INK}" stroke-width="3.5"/><circle cx="36" cy="42" r="5" fill="${INK}"/><circle cx="64" cy="42" r="5" fill="${INK}"/><path d="M30 60c8 14 32 14 40 0" stroke="${INK}" stroke-width="4" stroke-linecap="round" fill="none"/>`),
  bolt: S(`<path d="M58 6L22 56h24l-6 38 38-54H54z" fill="#F6C453" stroke="${INK}" stroke-width="3.5" stroke-linejoin="round"/>`),
  sun: S(`<g stroke="${INK}" stroke-width="3.5" stroke-linecap="round"><path d="M50 6v12 M50 82v12 M6 50h12 M82 50h12 M19 19l8 8 M73 73l8 8 M19 81l8-8 M73 27l8-8" fill="none"/></g><circle cx="50" cy="50" r="22" fill="#F28A2E" stroke="${INK}" stroke-width="3.5"/>`),
  moon: S(`<path d="M72 78A38 38 0 0 1 38 14a38 38 0 1 0 34 64z" fill="#B9A6F0" stroke="${INK}" stroke-width="3.5" stroke-linejoin="round"/>`),
  leaf: S(`<path d="M14 86C12 44 38 14 88 12c0 46-26 74-72 74z" fill="#3FB27A" stroke="${INK}" stroke-width="3.5" stroke-linejoin="round"/><path d="M14 86C36 62 52 48 70 34" stroke="${INK}" stroke-width="3.5" stroke-linecap="round" fill="none"/>`),
  bubble: S(`<path d="M12 18a8 8 0 0 1 8-8h60a8 8 0 0 1 8 8v42a8 8 0 0 1-8 8H44L26 88V68h-6a8 8 0 0 1-8-8z" fill="#fff" stroke="${INK}" stroke-width="3.5" stroke-linejoin="round"/><path d="M28 32h44M28 46h28" stroke="${INK}" stroke-width="4" stroke-linecap="round"/>`),
  check: S(`<circle cx="50" cy="50" r="42" fill="#3FB27A" stroke="${INK}" stroke-width="3.5"/><path d="M30 52l14 14 28-30" stroke="#fff" stroke-width="9" stroke-linecap="round" stroke-linejoin="round" fill="none"/>`),
  pushpin: S(`<path d="M36 10h28l-4 30 14 12v8H26v-8l14-12z" fill="#E0402E" stroke="${INK}" stroke-width="3.5" stroke-linejoin="round"/><path d="M50 60v32" stroke="${INK}" stroke-width="4" stroke-linecap="round"/>`),
  arrow: S(`<path d="M10 70C26 30 54 22 84 34" stroke="${INK}" stroke-width="6" stroke-linecap="round" fill="none"/><path d="M66 18l20 16-22 14" stroke="${INK}" stroke-width="6" stroke-linecap="round" stroke-linejoin="round" fill="none"/>`),
  bookmark: S(`<path d="M26 8h48v86L50 72 26 94z" fill="#D9487A" stroke="${INK}" stroke-width="3.5" stroke-linejoin="round"/>`),
} as const;
export type DoodleKey = keyof typeof DOODLES;
export const DOODLEKEYS = Object.keys(DOODLES) as DoodleKey[];

const LABEL = (t: string, col = INK, fill = '#fff', dash = false) =>
  S(`<rect x="4" y="26" width="92" height="48" rx="${dash ? 24 : 10}" fill="${fill}" stroke="${col}" stroke-width="3"/><rect x="9" y="31" width="82" height="38" rx="${dash ? 19 : 6}" fill="none" stroke="${col}" stroke-width="1.1"/><text x="50" y="57" text-anchor="middle" font-family="Instrument Serif, Georgia, serif" font-size="${t.length > 8 ? 15 : 19}" fill="${col}">${t}</text>`);
export const LABELS = {
  exam: LABEL('Exam week'), due: LABEL('Due Friday'), revise: LABEL('Revise'), lab: LABEL('Lab notes'),
  read: LABEL('To read', '#fff', '#17181C'), focus: LABEL('Focus', INK, '#F6C453', true),
} as const;
export type LabelKey = keyof typeof LABELS;

const TAPE = (a: string, b: string, dots = false) =>
  S(`<defs><pattern id="tp" width="12" height="12" patternUnits="userSpaceOnUse" patternTransform="rotate(${dots ? 0 : 45})">${dots ? `<rect width="12" height="12" fill="${a}"/><circle cx="6" cy="6" r="2.2" fill="${b}"/>` : `<rect width="12" height="12" fill="${a}"/><rect width="6" height="12" fill="${b}"/>`}</pattern></defs><path d="M6 34l4 4-4 4 4 4-4 4 4 4-4 4v0h84l-4-4 4-4-4-4 4-4-4-4 4-4-4-4z" transform="translate(0 -4)" fill="url(#tp)" opacity=".92"/>`);
export const TAPES = { tape1: TAPE('#F4A9C6', '#fff'), tape2: TAPE('#9ED8F2', '#5C8DF0'), tape3: TAPE('#F6C453', '#fff4cc', true), tape4: TAPE('#3FB27A', '#bfe9d3') } as const;
export type TapeKey = keyof typeof TAPES;

/* pixel props: 7 by 7 bitmaps, brand moments only */
const BITMAPS: Record<string, string[]> = {
  star: ['...#...', '..###..', '#######', '.#####.', '..###..', '.#...#.', '.......'],
  heart: ['.##.##.', '#######', '#######', '#######', '.#####.', '..###..', '...#...'],
  pen: ['.....##', '....###', '...###.', '..###..', '.###...', '.##....', '#......'],
  book: ['.#####.', '##...##', '##...##', '##...##', '##...##', '##...##', '.#####.'],
  folder: ['.......', '###....', '#######', '#.....#', '#.....#', '#.....#', '#######'],
  image: ['#######', '#.....#', '#..#..#', '#.###.#', '#######', '.......', '.......'],
  tag: ['#####..', '#...##.', '#.#..##', '#...##.', '#####..', '.......', '.......'],
  calendar: ['#.#.#.#', '#######', '#.....#', '#.....#', '#.....#', '#.....#', '#######'],
};
const bitmapPath = (n: string) => {
  let d = '';
  BITMAPS[n].forEach((row, y) => {
    for (const r of row.matchAll(/#+/g)) d += `M${r.index} ${y}h${r[0].length}v1h-${r[0].length}z`;
  });
  return d;
};
export const PIXELPROPS: Array<[string, string]> = [
  ['star', '#F6C453'], ['heart', '#E0402E'], ['pen', '#D9487A'], ['book', '#F28A2E'],
  ['folder', '#E5A92B'], ['image', '#5C8DF0'], ['tag', '#3FB27A'], ['calendar', '#B9A6F0'],
];
export const pixSVG = (n: string, c: string) => `<svg viewBox="0 0 7 7" xmlns="http://www.w3.org/2000/svg" shape-rendering="crispEdges"><path d="${bitmapPath(n)}" fill="${c}"/></svg>`;
