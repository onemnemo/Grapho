import type { ReactNode } from 'react';

const glyph = (body: ReactNode) => (
  <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" strokeLinecap="round" aria-hidden="true">{body}</svg>
);

/** 16px drawings for the shapes a stroke can be matched to. */
export const shapeGlyphs = {
  circle: glyph(<circle cx="8" cy="8" r="6" />),
  ellipse: glyph(<ellipse cx="8" cy="8" rx="6.5" ry="4.5" />),
  rect: glyph(<rect x="2" y="3" width="12" height="10" />),
  rrect: glyph(<rect x="2" y="3" width="12" height="10" rx="3" />),
  tri: glyph(<path d="M8 2.5l6 10.5H2z" />),
  poly: glyph(<path d="M8 2l5.5 4-2 6.5h-7l-2-6.5z" />),
  arrow: glyph(<path d="M2 8h11M9 4l4 4-4 4" />),
  line: glyph(<path d="M2 13L14 3" />),
};
export type ShapeGlyphName = keyof typeof shapeGlyphs;
