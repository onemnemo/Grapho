import { CARDS, type CardKind } from './cards';
import { mascotPack } from './mascotPoses';
import { TEX, type TextureKey } from './textures';
import type { PatternKey } from './patterns';
import type { ClipKey } from './clips';
import { DIM, type Design, type Layer, type LayerType } from './types';

/* Layer shorthands used by the example designs and the studio's Add actions. */
let n = 0;
export const uid = () => 'L' + ++n + Math.random().toString(36).slice(2, 5);
export const deep = <T,>(o: T): T => JSON.parse(JSON.stringify(o));

export function L(type: LayerType, o: Partial<Layer> = {}): Layer {
  return { id: uid(), type, x: 150, y: 200, w: 100, h: 100, r: 0, op: 1, blend: 'normal', fx: 0, fy: 0, lock: false, hide: false, sh: 0, rad: 0, pop: false, ...o };
}

const CW = DIM.cover.W, CH = DIM.cover.H, FW = DIM.folder.W, FH = DIM.folder.H, FY = DIM.folder.fy;
export const covFull = (o: Partial<Layer> = {}): Partial<Layer> => ({ x: CW / 2, y: CH / 2, w: CW, h: CH, ...o });
export const froFull = (o: Partial<Layer> = {}): Partial<Layer> => ({ x: FW / 2, y: FY + (FH - FY) / 2, w: FW, h: FH - FY, ...o });

export const tT = (text: string, x: number, y: number, w: number, o: Partial<Layer> = {}) =>
  L('text', { text, x, y, w, h: 40, size: 28, weight: 500, font: 'sans', colour: '#F5F5F4', align: 'center', ls: 0, lh: 1.1, ...o });
export const tShape = (shape: string, x: number, y: number, w: number, h: number, fill: string, o: Partial<Layer> = {}) =>
  L('shape', { shape, x, y, w, h, fill, stroke: 'none', sw: 0, ...o });
export const tCard = (kind: CardKind, x: number, y: number, o: Partial<Layer> = {}) => {
  const c = CARDS.find((k) => k[0] === kind)!;
  const size = kind === 'polaroid' || kind === 'label' || kind === 'index' ? 30 : kind === 'sticky' ? 26 : 18;
  return L('card', { kind, x, y, w: c[6], h: c[7], text: c[2], colour: c[3], ink: c[4], font: c[5], size, ...o });
};
export const tIcon = (ico: string, x: number, y: number, w: number, colour: string, o: Partial<Layer> = {}) =>
  L('icon', { ico, x, y, w, h: w, colour, sw: 1.75, soft: false, ...o });
export const tTex = (key: TextureKey, full: Partial<Layer>, o: Partial<Layer> = {}) => {
  const t = TEX[key];
  return L('tex', { key, ...full, colour: t.c || '#fff', scale: 1, blend: t.mode === 'relief' ? t.blend : 'normal', op: t.op ?? 1, ...o });
};
export const tPat = (key: PatternKey, full: Partial<Layer>, ink: string, o: Partial<Layer> = {}) =>
  L('pat', { key, ...full, colour: 'transparent', ink, scale: 1, ...o });
export const tSt = (svg: string, x: number, y: number, w: number, o: Partial<Layer> = {}) => L('sticker', { svg, x, y, w, h: w, cut: 2, sh: 1, ...o });
export const tPx = (anim: string, x: number, y: number, k: number, o: Partial<Layer> = {}) => {
  const m = mascotPack().find((p) => p.anim === anim);
  return L('sticker', { src: m?.src ?? '', pix: true, x, y, w: 13 * k, h: 14 * k, cut: 0, sh: 0, name: 'Mascot', ...o });
};
export const tVid = (clip: ClipKey, full: Partial<Layer>, poster = 0, o: Partial<Layer> = {}) => L('video', { clip, ...full, poster, ...o });

export const dCover = (name: string, base: Design['base'], layers: Layer[], cover: Design['cover'] = {}): Design =>
  ({ kind: 'cover', name, meta: 'Notebook · 24 pages', base, cover: { rad: 14, spine: true, strap: true, ...cover }, layers });
export const dFolder = (name: string, meta: string, base: Design['base'], folder: Partial<NonNullable<Design['folder']>>, layers: Layer[]): Design =>
  ({ kind: 'folder', name, meta, base, folder: { back: '#3B3470', name: 'left', ink: 'light', papers: ['#F2C14E', '#E8A2BE', '#F5F1E6'], ...folder }, layers });
