import { CARDS } from './cards';
import { CLIPS, type ClipKey } from './clips';
import { DOODLES, LABELS, PIXELPROPS, TAPES, pixSVG } from './stickers';
import { L, tCard, tIcon, tPat, tTex, tVid } from './layers';
import { mascotPack } from './mascotPoses';
import { PAINT, type PaintKey } from './paints';
import { PAT, type PatternKey } from './patterns';
import { PHOTOS, type PhotoKey } from './photos';
import { SHAPES, SHAPE_STROKEONLY } from './shapes';
import { TEX, type TextureKey } from './textures';
import type { Design, Layer } from './types';

/** The area a full-bleed layer fills: the whole cover, or the front of the folder. */
export const designArea = (d: Pick<Design, 'kind'>) =>
  d.kind === 'cover' ? { x: 150, y: 200, w: 300, h: 400 } : { x: 240, y: 196, w: 480, h: 208 };
const isBgKind = (l: Pick<Layer, 'type' | 'key'>) => ['tex', 'pat', 'paint', 'video'].includes(l.type) && l.key !== 'grain';
export const isBackground = isBgKind;

/**
 * A new layer from the spec a panel tile carries, such as "st:doodle:star", "tx:head" or "tex:linen".
 * The kinds are: st sticker, im image, tx text, sh shape, cd card, ic icon, tex texture, pt pattern, pn paint, vd video.
 */
export function newLayer(spec: string, d: Pick<Design, 'kind'>, o: { iconStyle?: 'line' | 'soft' } = {}): Layer {
  const isC = d.kind === 'cover', A = designArea(d), [t, a, b] = spec.split(':');
  let l: Layer;
  const sz = isC ? 110 : 96;
  if (t === 'st') {
    if (a === 'mascot') {
      const m = mascotPack().find((p) => p.key === b), n = isC ? 8 : 6;
      l = L('sticker', { src: m?.src ?? '', pix: true, w: 13 * n, h: 14 * n, cut: 0, name: 'Mascot' });
    } else if (a === 'prop') {
      const pp = PIXELPROPS.find((p) => p[0] === b)!, s = isC ? 56 : 48;
      l = L('sticker', { svg: pixSVG(pp[0], pp[1]), w: s, h: s, cut: 0, name: 'Pixel ' + pp[0] });
    } else {
      const src = ({ doodle: DOODLES, study: LABELS, tape: TAPES, botanical: DOODLES } as Record<string, Record<string, string>>)[a][b];
      const w = a === 'study' || a === 'tape' ? (isC ? 150 : 130) : sz;
      l = L('sticker', { svg: src, w, h: w, cut: a === 'tape' ? 0 : 2, sh: 1, name: a === 'study' ? 'Label' : a === 'tape' ? 'Tape' : b[0].toUpperCase() + b.slice(1) });
    }
  } else if (t === 'im') {
    const p = PHOTOS[a as PhotoKey];
    l = L('image', { src: p.src, w: isC ? 220 : 240, h: isC ? 150 : 160, crop: { z: 1, px: 0, py: 0 }, rad: 6, sh: 1, photo: a as PhotoKey, name: p.name });
  } else if (t === 'tx') {
    const P = {
      head: { text: 'Title', font: 'sans', size: isC ? 36 : 40, weight: 500 },
      serif: { text: 'Title', font: 'serif', size: isC ? 46 : 50, weight: 400 },
      hand: { text: 'Notes', font: 'hand', size: isC ? 48 : 52, weight: 700 },
      mono: { text: 'LABEL', font: 'mono', size: isC ? 15 : 16, weight: 500, ls: 0.2, caps: true },
      body: { text: 'Write something here', font: 'sans', size: isC ? 16 : 18, weight: 400 },
    }[a as 'head'] as Partial<Layer>;
    l = L('text', { ...P, w: isC ? 240 : 330, h: 40, colour: '#F5F5F4', align: 'center', ls: P.ls || 0, lh: 1.1, caps: !!P.caps });
  } else if (t === 'sh') {
    const so = SHAPE_STROKEONLY[a];
    const dim = ({ rect: [120, 120], rounded: [130, 100], pill: [170, 64], circle: [110, 110], arch: [100, 130], ring: [110, 110], line: [180, 6] } as Record<string, number[]>)[a] || [110, 110];
    l = L('shape', { shape: a, w: dim[0], h: dim[1], fill: '#F5F5F4', stroke: 'none', sw: so ? 6 : 0, radius: a === 'rounded' ? 18 : 0, name: SHAPES.find((s) => s[0] === a)![1] });
  } else if (t === 'cd') {
    const c = CARDS.find((k) => k[0] === a)!;
    l = tCard(c[0], 150, 200, { name: c[1], w: isC ? c[6] : Math.min(c[6] + 20, 200), r: a === 'sticky' ? 3 : 0, sh: a === 'label' || a === 'sticky' || a === 'polaroid' ? 1 : 0 });
    if (a === 'polaroid') l.photo = 'hills';
  } else if (t === 'ic') {
    l = tIcon(a, 150, 200, isC ? 72 : 60, '#F5F5F4', { soft: o.iconStyle === 'soft', name: a[0].toUpperCase() + a.slice(1) + ' icon' });
  } else if (t === 'tex') {
    const tx = TEX[a as TextureKey];
    l = tTex(a as TextureKey, A, { name: tx.name, op: tx.op ?? 1 });
  } else if (t === 'pt') l = tPat(a as PatternKey, A, '#F5F5F4', { op: 0.35, name: PAT[a as PatternKey].name + ' pattern' });
  else if (t === 'pn') l = L('paint', { key: a as PaintKey, ...A, name: PAINT[a as PaintKey].name + ' fill' });
  else if (t === 'vd') l = tVid(a as ClipKey, A, Math.min(2, CLIPS[a as ClipKey].len / 2), { name: CLIPS[a as ClipKey].name + ' clip' });
  else l = L('sticker');
  if (!['tex', 'pt', 'pn', 'vd'].includes(t)) {
    l.x = A.x + (Math.random() - 0.5) * 20;
    l.y = A.y + (Math.random() - 0.5) * 20;
  }
  return l;
}
