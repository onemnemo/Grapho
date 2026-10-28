/* Canvases are drawn straight from their settings, so the preview is the real thing. */
export type CanvasPattern = 'blank' | 'dots' | 'grid' | 'lines';
export type CanvasGap = 'S' | 'M' | 'L';
export type CanvasPaper = 'A4' | 'A3' | 'Screen' | 'Square';
export interface CanvasSettings {
  name: string;
  bg: string;
  tpl: CanvasPattern;
  gap: CanvasGap;
  size: 'inf' | 'fixed';
  fixed: CanvasPaper;
  orient: 'land' | 'port';
  where?: string;
  meta?: string;
}
export const CV_GAP: Record<CanvasGap, number> = { S: 12, M: 20, L: 30 };
export const CV_FIXED: Record<CanvasPaper, [string, number]> = { A4: ['A4', 1.414], A3: ['A3', 1.414], Screen: ['Screen 16:9', 1.778], Square: ['Square', 1] };
export const CV_PRESETS: Array<[string, string, CanvasPattern]> = [
  ['Whiteboard', '#F7F6F2', 'blank'], ['Dot grid', '#1B1C21', 'dots'], ['Blueprint', '#1F3A6B', 'grid'], ['Graph paper', '#F2EEDF', 'grid'],
  ['Chalkboard', '#24382F', 'blank'], ['Midnight lines', '#17181C', 'lines'], ['Sand dots', '#E9DFC9', 'dots'], ['Sky', '#DCEBFA', 'dots'],
];
export const CV_COLS: Array<[string, string[]]> = [
  ['Light', ['#FFFFFF', '#F7F6F2', '#F2EEDF', '#E9DFC9', '#DCEBFA', '#F6E3EA', '#E2F0E6']],
  ['Dark', ['#17181C', '#1B1C21', '#1F3A6B', '#24382F', '#3B2A4F', '#5B1F2B', '#2B2B30']],
];
export const CV_TPL: Array<[CanvasPattern, string]> = [['blank', 'Blank'], ['dots', 'Dots'], ['grid', 'Grid'], ['lines', 'Lines']];
export const CV_GAPS: Array<[CanvasGap, string]> = [['S', 'Close'], ['M', 'Medium'], ['L', 'Wide']];
export const cvMeta = (c: Pick<CanvasSettings, 'size' | 'fixed' | 'orient'>) =>
  c.size === 'fixed' ? `${CV_FIXED[c.fixed][0]} · ${c.orient === 'port' ? 'portrait' : 'landscape'}` : 'Infinite';
/** Width and height of the thumbnail for a canvas, given the width of a landscape one. */
export const cvDims = (c: Pick<CanvasSettings, 'size' | 'fixed' | 'orient'>, w: number): [number, number] => {
  if (c.size !== 'fixed') return [w, Math.round(w * 0.7)];
  const r = CV_FIXED[c.fixed][1];
  return c.orient === 'port' ? [Math.round(((w * 0.7) / r) * 1.1), Math.round(w * 0.78)] : [w, Math.round(w / r)];
};
export const CV_NEW: CanvasSettings = { name: 'Untitled canvas', bg: '#1B1C21', tpl: 'dots', gap: 'M', size: 'inf', fixed: 'A4', orient: 'land', where: 'Library', meta: 'Infinite' };
export const CV_CURRENT: CanvasSettings = { name: 'Lecture map', bg: '#1B1C21', tpl: 'dots', gap: 'M', size: 'inf', fixed: 'A4', orient: 'land', where: 'Library', meta: 'Infinite' };
export const CV_LIB: CanvasSettings[] = [
  { name: 'Lecture map', bg: '#1B1C21', tpl: 'dots', gap: 'M', size: 'inf', fixed: 'A4', orient: 'land', meta: 'Infinite' },
  { name: 'Project plan', bg: '#F2EEDF', tpl: 'grid', gap: 'M', size: 'fixed', fixed: 'A3', orient: 'land', meta: 'A3 · landscape' },
  { name: 'Brainstorm', bg: '#1F3A6B', tpl: 'grid', gap: 'L', size: 'inf', fixed: 'A4', orient: 'land', meta: 'Infinite' },
];
