/* Starting content for the pen wheel: the colour sets and pens a new notebook ships with.
   This is user content (artwork data), so the hex values live here and not in components. */
export interface DefaultSet { id: string; name: string; colours: string[] }
export interface DefaultPen { name: string; kind: 'pen' | 'highlighter'; color: string; w: number; alpha: number; line: 'solid' | 'dashed' | 'dotted' }

export const DEFAULT_SETS: DefaultSet[] = [
  { id: 'basic', name: 'Basic', colours: ['#F5F5F4', '#5C8DF0', '#FF5A4A', '#3FB27A', '#F28A2E', '#B9A6F0', '#8B8B94'] },
  { id: 'hl', name: 'Highlighters', colours: ['#F5D33F', '#86EFAC', '#F9A8D4', '#7DD3FC'] },
  { id: 'more', name: 'More', colours: ['#2DD4BF', '#EC4899', '#C08457', '#EAB308', '#6D83F2', '#38BDF8', '#A3E635', '#F87171', '#D4D4D8', '#A78BFA'] },
];

export const DEFAULT_RECENT = ['#E0402E', '#F6C453', '#3FB27A', '#5C8DF0', '#D9487A'];

export const DEFAULT_PENS: DefaultPen[] = [
  { name: 'Fountain pen', kind: 'pen', color: '#F5F5F4', w: 1.6, alpha: 255, line: 'solid' },
  { name: 'Ballpoint', kind: 'pen', color: '#5C8DF0', w: 0.8, alpha: 255, line: 'solid' },
  { name: 'Pencil', kind: 'pen', color: '#8B8B94', w: 1.0, alpha: 255, line: 'solid' },
  { name: 'Brush pen', kind: 'pen', color: '#F5F5F4', w: 5, alpha: 255, line: 'solid' },
  { name: 'Marker', kind: 'pen', color: '#F28A2E', w: 4, alpha: 255, line: 'solid' },
  { name: 'Highlighter', kind: 'highlighter', color: '#F5D33F', w: 12, alpha: 115, line: 'solid' },
  { name: 'Red pen', kind: 'pen', color: '#FF5A4A', w: 1.4, alpha: 255, line: 'solid' },
  { name: 'Green highlighter', kind: 'highlighter', color: '#86EFAC', w: 14, alpha: 115, line: 'solid' },
];

export const DEFAULT_PEN_INK = '#F5F5F4';
export const DEFAULT_HIGHLIGHTER_INK = '#F5D33F';

/** Ink used by the specimen stories. */
export const SAMPLE_RED = '#FF5A4A';
