import type { CardDef } from './cards';
import type { ClipKey } from './clips';
import { CLIPKEYS } from './clips';
import { mascotPack } from './mascotPoses';
import { PAINT, PAINTKEYS, paintCSS } from './paints';
import { PAT, PATKEYS, patURL, type PatternKey } from './patterns';
import { PHOTOKEYS, PHOTOS } from './photos';
import { PACKDEF, STICKER_CATS, BOTANICAL, type PackedTool } from './packs';
import { SHAPES, SHAPE_CATS } from './shapes';
import { DOODLES, LABELS, TAPES, PIXELPROPS, pixSVG, DOODLEKEYS } from './stickers';
import { HOLO, TEX, TEXKEYS, texMapURL, TEX_BASE, type TextureKey } from './textures';

/** What a tile in the studio panel (or a thumbnail in the layer list) shows. */
export type TileArt =
  | { t: 'html'; html: string }
  | { t: 'pixel'; html: string }
  | { t: 'img'; src: string; pixel?: boolean }
  | { t: 'shape'; key: string; colour?: string }
  | { t: 'swatch'; base: string; image?: string; size?: string; map?: { image: string; blend: string } }
  | { t: 'photo'; src: string }
  | { t: 'card'; def: CardDef }
  | { t: 'video'; clip: ClipKey }
  | { t: 'aa'; font: string; weight: number; colour: string }
  | { t: 'chip'; bg: string; ink: string }
  | { t: 'icon'; name: string; colour: string; soft?: boolean }
  | { t: 'poster'; bg: string };

export type TileSize = 'normal' | 'wide' | 'sm' | 'swatch';
export interface PanelItem { spec: string; art: TileArt; size: TileSize; name: string; /** A caption under the tile. */ cell?: boolean }

const INK = '#F5F5F4';
const PAT_BASE = '#3B3470';

export const texArt = (k: TextureKey, base?: string): TileArt => {
  const t = TEX[k];
  if (k === 'holo') return { t: 'swatch', base: HOLO };
  return { t: 'swatch', base: base ?? (t.mode === 'relief' ? TEX_BASE[k] : (t.c as string)), map: { image: texMapURL(k), blend: t.mode === 'relief' ? t.blend : (t.inner as string) } };
};
export const patArt = (k: PatternKey, ink = INK, s = 0.5): TileArt => ({ t: 'swatch', base: PAT_BASE, image: patURL(k, ink, 1), size: `${PAT[k].w * s}px ${PAT[k].h * s}px` });
export const paintArt = (k: (typeof PAINTKEYS)[number]): TileArt => ({ t: 'swatch', base: paintCSS(k) });

const I = (spec: string, art: TileArt, size: TileSize, name: string, cell = false): PanelItem => ({ spec, art, size, name, cell });

/** Everything in one tab of one pack. */
export function packItems(tool: PackedTool, pack: string, tab: string): PanelItem[] {
  if (tool === 'stickers') {
    if (pack === 'grapho') {
      if (tab === 'pixel') {
        return [
          ...mascotPack().map((m) => I('st:mascot:' + m.key, { t: 'img', src: m.src, pixel: true }, 'normal', 'Mascot ' + m.anim)),
          ...PIXELPROPS.map(([n, c]) => I('st:prop:' + n, { t: 'pixel', html: pixSVG(n, c) }, 'normal', 'Pixel ' + n)),
        ];
      }
      if (STICKER_CATS[tab]) return STICKER_CATS[tab].map((k) => I('st:doodle:' + k, { t: 'html', html: DOODLES[k as keyof typeof DOODLES] }, 'normal', k));
      return Object.keys(LABELS).map((k) => I('st:study:' + k, { t: 'html', html: LABELS[k as keyof typeof LABELS] }, 'wide', k));
    }
    if (pack === 'tape') return Object.keys(TAPES).filter((_, i) => (i < 2) === (tab === 'pastel')).map((k) => I('st:tape:' + k, { t: 'html', html: TAPES[k as keyof typeof TAPES] }, 'wide', k));
    return BOTANICAL.filter((_, i) => (i < 2) === (tab === 'plants')).map((k) => I('st:botanical:' + k, { t: 'html', html: DOODLES[k as keyof typeof DOODLES] }, 'normal', k));
  }
  if (tool === 'shapes') return SHAPES.filter(([k]) => SHAPE_CATS[tab].includes(k)).map(([k, n]) => I('sh:' + k, { t: 'shape', key: k }, 'sm', n));
  if (tab === 'textures') return TEXKEYS.map((k) => I('tex:' + k, texArt(k), 'swatch', TEX[k].name, true));
  if (tab === 'patterns') return PATKEYS.map((k) => I('pt:' + k, patArt(k), 'swatch', PAT[k].name, true));
  return PAINTKEYS.map((k) => I('pn:' + k, paintArt(k), 'swatch', PAINT[k].name, true));
}

export const packDef = (tool: PackedTool, id: string) => PACKDEF[tool].find((p) => p.id === id) ?? PACKDEF[tool][0];
export const allPackItems = (tool: PackedTool, id: string) => packDef(tool, id).tabs.flatMap(([tab]) => packItems(tool, id, tab));
export const photoItems = (): PanelItem[] => PHOTOKEYS.map((k) => I('im:' + k, { t: 'photo', src: PHOTOS[k].src }, 'normal', PHOTOS[k].name));
export const clipItems = (): PanelItem[] => CLIPKEYS.map((k) => I('vd:' + k, { t: 'video', clip: k }, 'swatch', k, true));

/** The three small pictures on a pack tile. */
export function packArt(tool: PackedTool, id: string): TileArt[] {
  if (tool === 'stickers') {
    if (id === 'grapho') return [{ t: 'img', src: mascotPack()[0]?.src ?? '', pixel: true }, { t: 'html', html: DOODLES[DOODLEKEYS[0]] }, { t: 'html', html: LABELS[Object.keys(LABELS)[0] as keyof typeof LABELS] }];
    if (id === 'tape') return Object.values(TAPES).slice(0, 3).map((html) => ({ t: 'html', html }) as TileArt);
    return BOTANICAL.slice(0, 3).map((k) => ({ t: 'html', html: DOODLES[k as keyof typeof DOODLES] }) as TileArt);
  }
  if (tool === 'shapes') return ['heart', 'star', 'hexagon'].map((key) => ({ t: 'shape', key }) as TileArt);
  return [texArt('linen'), texArt('leather'), texArt('marble')];
}
