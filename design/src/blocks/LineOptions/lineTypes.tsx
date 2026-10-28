import type { ReactNode } from 'react';
import type { OutlineStyle } from '../ShapeOptions';

export type LinePath = 'straight' | 'curved' | 'elbow';
export type LineEnding = 'none' | 'arrow' | 'open' | 'dot' | 'square' | 'diamond' | 'bar';

export interface LineValues {
  path: LinePath;
  start: LineEnding;
  end: LineEnding;
  outline: OutlineStyle;
  width: number;
}

/** A line with two plain ends is a line, with one arrow end it is an arrow: one tool, no decision. */
export const defaultLine: LineValues = { path: 'straight', start: 'none', end: 'arrow', outline: 'solid', width: 4 };

/** Arrowhead size in dp for a line width. Heads grow with the line but slower than it, so a thin line keeps a clear head and a thick one stays in proportion. */
export const headSize = (width: number) => Math.round(7 + width * 2.6);

export const endings: Array<{ value: LineEnding; label: string }> = [
  { value: 'none', label: 'None' },
  { value: 'arrow', label: 'Arrow' },
  { value: 'open', label: 'Open arrow' },
  { value: 'dot', label: 'Dot' },
  { value: 'square', label: 'Square' },
  { value: 'diamond', label: 'Diamond' },
  { value: 'bar', label: 'Bar' },
];

const drawings: Record<LineEnding, ReactNode> = {
  none: <path d="M2 9h22" />,
  arrow: (<><path d="M2 9h18" /><path d="M18 3l8 6-8 6z" fill="currentColor" /></>),
  open: <path d="M2 9h24M19 3l7 6-7 6" />,
  dot: (<><path d="M2 9h18" /><circle cx="24" cy="9" r="4" fill="currentColor" /></>),
  square: (<><path d="M2 9h18" /><rect x="20" y="5" width="8" height="8" fill="currentColor" /></>),
  diamond: (<><path d="M2 9h16" /><path d="M18 9l5-5 5 5-5 5z" fill="currentColor" /></>),
  bar: <path d="M2 9h24M26 2v14" />,
};

/** The small drawing for a line ending. The start picker mirrors it so it points the way it will on the page. */
export function EndingGlyph({ ending, flip = false }: { ending: LineEnding; flip?: boolean }) {
  return (
    <svg width="30" height="18" viewBox="0 0 30 18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={flip ? { transform: 'scaleX(-1)' } : undefined}>
      {drawings[ending]}
    </svg>
  );
}
