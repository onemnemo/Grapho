import { uri } from './svgUri';

export interface Pattern { name: string; w: number; h: number; svg: (ink: string) => string }
export type PatternKey = 'dots' | 'grid' | 'stripes' | 'diag' | 'checker' | 'waves' | 'plaid' | 'terrazzo' | 'scallop' | 'pixel';

export const PAT: Record<PatternKey, Pattern> = {
  dots: { name: 'Dots', w: 24, h: 24, svg: (i) => `<circle cx="0" cy="0" r="2.6" fill="${i}"/><circle cx="24" cy="0" r="2.6" fill="${i}"/><circle cx="0" cy="24" r="2.6" fill="${i}"/><circle cx="24" cy="24" r="2.6" fill="${i}"/><circle cx="12" cy="12" r="2.6" fill="${i}"/>` },
  grid: { name: 'Grid', w: 24, h: 24, svg: (i) => `<path d="M0 .6H24M.6 0V24" stroke="${i}" stroke-width="1.2" fill="none"/>` },
  stripes: { name: 'Stripes', w: 24, h: 24, svg: (i) => `<rect y="0" width="24" height="9" fill="${i}"/>` },
  diag: { name: 'Diagonal', w: 24, h: 24, svg: (i) => `<path d="M-12 12L12-12 M0 24L24 0 M12 36L36 12" stroke="${i}" stroke-width="7" fill="none"/>` },
  checker: { name: 'Checker', w: 32, h: 32, svg: (i) => `<rect width="16" height="16" fill="${i}"/><rect x="16" y="16" width="16" height="16" fill="${i}"/>` },
  waves: { name: 'Waves', w: 40, h: 20, svg: (i) => `<path d="M0 10Q10 0 20 10T40 10" stroke="${i}" stroke-width="2.4" fill="none"/>` },
  plaid: { name: 'Plaid', w: 48, h: 48, svg: (i) => `<rect width="14" height="48" fill="${i}" opacity=".42"/><rect width="48" height="14" fill="${i}" opacity=".42"/><rect x="30" width="3" height="48" fill="${i}" opacity=".5"/><rect y="30" width="48" height="3" fill="${i}" opacity=".5"/>` },
  terrazzo: { name: 'Terrazzo', w: 80, h: 80, svg: (i) => `<path d="M10 12l9-4 4 8-8 5z M52 10l8 2-2 8-8-3z M30 40l10-3 3 9-9 4z M64 46l8-4 4 9-8 3z M8 62l8-3 3 7-8 3z M44 68l7 1-1 8-8-2z" fill="${i}"/><circle cx="68" cy="22" r="3" fill="${i}"/><circle cx="22" cy="30" r="2.4" fill="${i}"/><circle cx="40" cy="14" r="2" fill="${i}"/>` },
  scallop: { name: 'Scallops', w: 28, h: 28, svg: (i) => `<path d="M0 14a14 14 0 0 1 28 0 M-14 28a14 14 0 0 1 28 0 M14 28a14 14 0 0 1 28 0" stroke="${i}" stroke-width="2" fill="none"/>` },
  pixel: { name: 'Pixel', w: 24, h: 24, svg: (i) => `<rect width="6" height="6" fill="${i}"/><rect x="12" y="12" width="6" height="6" fill="${i}"/><rect x="18" y="0" width="6" height="6" fill="${i}" opacity=".5"/><rect x="6" y="18" width="6" height="6" fill="${i}" opacity=".5"/>` },
};
export const PATKEYS = Object.keys(PAT) as PatternKey[];
/** CSS background-image of a pattern tile drawn in `ink`. Ink can be any CSS colour. */
export const patURL = (k: PatternKey, ink: string, s = 1) => {
  const p = PAT[k];
  return uri(`<svg xmlns="http://www.w3.org/2000/svg" width="${p.w * s}" height="${p.h * s}" viewBox="0 0 ${p.w} ${p.h}">${p.svg(ink)}</svg>`);
};
