/* The pen wheel's state: tool, ink settings, pens, colour sets. Plain TypeScript with a tiny
   subscribe API so the engine can read it every frame and React can read it through useWheelModel. */
import type { IconName } from '@/assets/icons';
import { DEFAULT_HIGHLIGHTER_INK, DEFAULT_PEN_INK, DEFAULT_PENS, DEFAULT_RECENT, DEFAULT_SETS } from '@/assets/wheel/defaults';
import { hexOf } from './math';
import { nearestDot, numberOfWidth } from './geometry';

export type ToolId = 'pen' | 'highlighter' | 'eraser' | 'fill' | 'lasso' | 'shape' | 'text' | 'ruler';
export type Line = 'solid' | 'dashed' | 'dotted';
export type PenKind = 'pen' | 'highlighter';

export interface ToolDef {
  id: ToolId;
  label: string;
  icon: IconName;
}
export const TOOLS: Record<ToolId, ToolDef> = {
  pen: { id: 'pen', label: 'Pen', icon: 'pen' },
  highlighter: { id: 'highlighter', label: 'Highlighter', icon: 'highlighter' },
  eraser: { id: 'eraser', label: 'Eraser', icon: 'eraser' },
  fill: { id: 'fill', label: 'Fill', icon: 'bucket' },
  lasso: { id: 'lasso', label: 'Lasso', icon: 'lasso' },
  shape: { id: 'shape', label: 'Shapes', icon: 'shapes' },
  text: { id: 'text', label: 'Text', icon: 'text' },
  ruler: { id: 'ruler', label: 'Ruler', icon: 'ruler' },
};
export const TOOL_ORDER: ToolId[] = ['pen', 'highlighter', 'eraser', 'fill', 'lasso', 'shape', 'text', 'ruler'];

export interface InkSetting {
  color: string;
  w: number;
  line: Line;
}
export interface PenPreset extends InkSetting {
  name: string;
  kind: PenKind;
  alpha: number;
}
export interface ColourSet {
  id: string;
  name: string;
  colours: string[];
}

/** A gap in the colour ring between two sets. */
export const STRIP = null;
export type RimItem = string | null;

export const rimOf = (sets: ColourSet[]): RimItem[] => {
  const filled = sets.filter((s) => s.colours.length);
  const out: RimItem[] = [];
  filled.forEach((s, i) => {
    if (i > 0) out.push(STRIP);
    out.push(...s.colours);
  });
  if (filled.length > 1) out.push(STRIP);
  return out;
};
export const rimIndexOf = (sets: ColourSet[], setIdx: number, colIdx: number) => {
  let slot = 0;
  let seen = 0;
  for (let i = 0; i < sets.length; i++) {
    if (!sets[i].colours.length) continue;
    if (seen > 0) slot++;
    if (i === setIdx) return slot + colIdx;
    slot += sets[i].colours.length;
    seen++;
  }
  return 0;
};

export type ModelEvent = 'mode' | 'colour' | 'size' | 'alpha' | 'line' | 'sizeMode' | 'eraser' | 'rim' | 'tools' | 'pens' | 'recent' | 'pin';

export interface ModelOptions {
  mode?: ToolId;
  sizeMode?: boolean;
  pinned?: boolean;
  pen?: Partial<InkSetting>;
  penAlpha?: number;
  eraserArea?: boolean;
  eraserPt?: number;
  tools?: ToolId[];
}

export class WheelModel {
  mode: ToolId;
  drawMode: PenKind = 'pen';
  pen: InkSetting = { color: DEFAULT_PEN_INK, w: 1.6, line: 'solid' };
  penAlpha = 255;
  hl: InkSetting = { color: DEFAULT_HIGHLIGHTER_INK, w: 12, line: 'solid' };
  hlAlpha = 115;
  eraserPt = 16;
  eraserArea = false;
  sizeMode: boolean;
  pinned: boolean;
  sets: ColourSet[] = DEFAULT_SETS.map((s) => ({ ...s, colours: s.colours.slice() }));
  recent: string[] = DEFAULT_RECENT.slice();
  tools: ToolId[] = TOOL_ORDER.slice();
  pens: PenPreset[] = DEFAULT_PENS.map((p) => ({ ...p }));
  rim: RimItem[] = rimOf(this.sets);
  /** Bumps on every change, for React. */
  version = 0;
  /** Bumps when the ring layout or tool list changes, for the face markup. */
  structVersion = 0;
  rimVersion = 0;
  toolsVersion = 0;
  private subs: Array<(what: ModelEvent) => void> = [];

  constructor(o: ModelOptions = {}) {
    this.mode = o.mode ?? 'pen';
    if (this.mode === 'highlighter') this.drawMode = 'highlighter';
    this.sizeMode = !!o.sizeMode;
    this.pinned = !!o.pinned;
    if (o.pen) Object.assign(this.pen, o.pen);
    if (o.penAlpha != null) this.penAlpha = o.penAlpha;
    if (o.eraserArea != null) this.eraserArea = o.eraserArea;
    if (o.eraserPt != null) this.eraserPt = o.eraserPt;
    if (o.tools) this.tools = o.tools.slice();
  }

  subscribe = (fn: (what: ModelEvent) => void) => {
    this.subs.push(fn);
    return () => {
      this.subs = this.subs.filter((f) => f !== fn);
    };
  };
  emit(what: ModelEvent) {
    this.version++;
    if (what === 'rim') { this.rimVersion++; this.structVersion++; }
    if (what === 'tools') { this.toolsVersion++; this.structVersion++; }
    this.subs.slice().forEach((f) => f(what));
  }

  get hlMode() {
    return this.mode === 'highlighter';
  }
  get setting() {
    return this.hlMode ? this.hl : this.pen;
  }
  get alpha() {
    return this.hlMode ? this.hlAlpha : this.penAlpha;
  }
  get sizeNumber() {
    return numberOfWidth(this.hlMode, this.setting.w);
  }
  get nibKind(): PenKind {
    return this.hlMode ? 'highlighter' : 'pen';
  }
  /** The nearest of the eight size dots. */
  get sizeDot() {
    return nearestDot(this.sizeNumber);
  }
  private inDraw() {
    if (this.mode === 'eraser') this.mode = this.drawMode;
  }
  applyColor(hex: string) {
    this.setting.color = hexOf(hex);
    this.inDraw();
    this.emit('colour');
  }
  applySize(pt: number) {
    this.setting.w = pt;
    this.inDraw();
    this.emit('size');
  }
  applyAlpha(a: number) {
    this.inDraw();
    if (this.hlMode) this.hlAlpha = a;
    else this.penAlpha = a;
    this.emit('alpha');
  }
  applyLine(l: Line) {
    this.setting.line = l;
    this.inDraw();
    this.emit('line');
  }
  toggleTool(mode: ToolId) {
    this.sizeMode = false;
    this.mode = this.mode === mode ? this.drawMode : mode;
    if (this.mode === 'pen' || this.mode === 'highlighter') this.drawMode = this.mode;
    this.emit('mode');
  }
  setMode(mode: ToolId) {
    if (mode !== 'pen' && mode !== 'highlighter') this.sizeMode = false;
    this.mode = mode;
    if (mode === 'pen' || mode === 'highlighter') this.drawMode = mode;
    this.emit('mode');
  }
  setSizeMode(on: boolean) {
    this.sizeMode = on;
    this.emit('sizeMode');
  }
  setEraser(pt: number | null, area?: boolean) {
    if (pt != null) this.eraserPt = pt;
    if (area != null) this.eraserArea = area;
    this.emit('eraser');
  }
  applyPreset(p: PenPreset) {
    const s = p.kind === 'highlighter' ? this.hl : this.pen;
    s.color = hexOf(p.color);
    s.w = p.w;
    s.line = p.line;
    if (p.kind === 'highlighter') this.hlAlpha = p.alpha;
    else this.penAlpha = p.alpha;
    this.setMode(p.kind === 'highlighter' ? 'highlighter' : 'pen');
    this.emit('colour');
  }
  setPinned(v: boolean) {
    this.pinned = v;
    this.emit('pin');
  }
  setSets(sets: ColourSet[]) {
    this.sets = sets;
    this.rim = rimOf(sets);
    this.emit('rim');
  }
  renameSet(index: number, name: string) {
    this.sets[index] = { ...this.sets[index], name };
    this.emit('recent');
  }
  setTools(tools: ToolId[]) {
    this.tools = tools;
    this.emit('tools');
  }
  setPens(pens: PenPreset[]) {
    this.pens = pens;
    this.emit('pens');
  }
  /** Save the live ink as a new pen at the end of the list. */
  saveCurrentPen() {
    const s = this.setting;
    const kind = this.nibKind;
    const n = this.pens.filter((x) => x.kind === kind).length + 1;
    this.setPens([...this.pens, { name: (kind === 'highlighter' ? 'Highlighter ' : 'Pen ') + n, kind, color: s.color, w: s.w, alpha: this.alpha, line: s.line }]);
  }
  addRecent(c: string) {
    c = hexOf(c);
    this.recent = [c, ...this.recent.filter((x) => x !== c)].slice(0, 7);
    this.emit('recent');
  }
  /** Index of the pen preset that matches the live ink, or -1. */
  currentPen() {
    const s = this.setting;
    const hl = this.hlMode;
    return this.pens.findIndex((p) => (p.kind === 'highlighter') === hl && p.color.toUpperCase() === s.color.toUpperCase() && Math.abs(p.w - s.w) < 0.05 && p.line === s.line);
  }
}
