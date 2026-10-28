/* Shapes: the first seven are drawn with CSS, the rest with these paths on a 100 grid. */
export const SHAPE_SVG: Record<string, string> = {
  triangle: 'M50 10L92 88H8z', diamond: 'M50 6L94 50L50 94L6 50z', hexagon: 'M27 8H73L96 50L73 92H27L4 50z',
  star: 'M50 6l12 29 31 3-24 21 8 31-27-17-27 17 8-31L7 38l31-3z',
  heart: 'M50 90C20 68 8 50 8 33 8 19 19 10 31 10c8 0 15 4 19 11 4-7 11-11 19-11 12 0 23 9 23 23 0 17-12 35-42 57z',
  blob: 'M50 8C70 6 94 22 92 46 90 68 78 94 52 92 28 90 6 76 8 50 10 28 30 10 50 8z',
  squiggle: 'M5 50Q16 18 28 50T52 50T76 50T96 50', arrow: 'M8 50H88 M62 22L90 50 62 78',
};
export const SHAPES: Array<[string, string]> = [
  ['rect', 'Square'], ['rounded', 'Rounded'], ['pill', 'Pill'], ['circle', 'Circle'], ['arch', 'Arch'], ['ring', 'Ring'],
  ['triangle', 'Triangle'], ['diamond', 'Diamond'], ['hexagon', 'Hexagon'], ['star', 'Star'], ['heart', 'Heart'], ['blob', 'Blob'],
  ['squiggle', 'Squiggle'], ['arrow', 'Arrow'], ['line', 'Line'],
];
export const SHAPE_STROKEONLY: Record<string, number> = { squiggle: 1, arrow: 1 };
export const SHAPE_CSS = ['rect', 'rounded', 'pill', 'circle', 'arch', 'ring', 'line'];
export const SHAPE_CATS: Record<string, string[]> = {
  geometric: ['rect', 'rounded', 'pill', 'circle', 'arch', 'ring', 'triangle', 'diamond', 'hexagon'],
  symbols: ['star', 'heart', 'blob'],
  lines: ['arrow', 'line', 'squiggle'],
};
/** The CSS drawn by the first seven shapes, for a thumbnail inside a 100 percent box. */
export const SHAPE_THUMB_CSS: Record<string, { radius?: string; ring?: boolean; pill?: boolean; line?: boolean }> = {
  rect: { radius: '2px' }, rounded: { radius: '9px' }, pill: { radius: '99px', pill: true }, circle: { radius: '50%' },
  arch: { radius: '99px 99px 0 0' }, ring: { radius: '50%', ring: true }, line: { radius: '2px', line: true },
};
