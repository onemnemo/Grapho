import type { CardKind, FontKey } from './cards';
import type { ClipKey } from './clips';
import type { PaintKey } from './paints';
import type { PatternKey } from './patterns';
import type { PhotoKey } from './photos';
import type { TextureKey } from './textures';

export type LayerType = 'sticker' | 'image' | 'text' | 'shape' | 'card' | 'icon' | 'tex' | 'pat' | 'paint' | 'video';
export type Placement = false | 'above' | 'behind';

/** One thing on a cover or folder front. Position is the centre, in design units. */
export interface Layer {
  id: string;
  type: LayerType;
  x: number; y: number; w: number; h: number; r: number;
  op: number; blend: string;
  fx: number; fy: number;
  lock: boolean; hide: boolean;
  /** Shadow step: 0 none, 1 soft, 2 lifted, 3 hard. */
  sh: number;
  rad: number;
  pop: Placement;
  name?: string;
  bind?: 'name';
  /* sticker */
  svg?: string; src?: string; pix?: boolean; cut?: number;
  /* image */
  crop?: { z: number; px: number; py: number }; photo?: PhotoKey;
  /* text and card */
  text?: string; font?: FontKey; size?: number; weight?: number; colour?: string; align?: 'left' | 'center' | 'right';
  ls?: number; lh?: number; caps?: boolean; italic?: boolean; outline?: number; outlineC?: string;
  kind?: CardKind;
  /** Card text colour, and the mark colour of a pattern. */
  ink?: string;
  /* shape */
  shape?: string; fill?: string; stroke?: string; sw?: number; dash?: boolean; radius?: number;
  /* icon */
  ico?: string; soft?: boolean; mask?: string;
  /* tex, pat, paint */
  key?: TextureKey | PatternKey | PaintKey; scale?: number;
  /* video */
  clip?: ClipKey; poster?: number;
}

export type DesignKind = 'cover' | 'folder';
export interface Design {
  kind: DesignKind;
  name: string;
  meta: string;
  base: { c1: string; c2?: string | null; ang?: number };
  cover?: { rad?: number; spine?: boolean; strap?: boolean | string };
  folder?: { back: string; tab?: string | null; papers?: string[]; name?: 'left' | 'centre' | 'below' | 'hidden'; ink?: 'light' | 'dark' };
  layers: Layer[];
  /* remembered by the quick sheets, so the preset tile can show what it was made from */
  hex?: string; finish?: string; icon?: string | null;
}

/** Design units. The cover is 300 by 400. The folder is 480 by 300 with its front starting at y 92. */
export const DIM = { cover: { W: 300, H: 400 }, folder: { W: 480, H: 300, fy: 92, fr: 28 } } as const;
export const SHADOWS = ['', 'drop-shadow(0 2px 4px rgba(0,0,0,.3))', 'drop-shadow(0 8px 14px rgba(0,0,0,.38))', 'drop-shadow(3px 3px 0 rgba(0,0,0,.85))'];
export const SHADOW_NAMES = ['None', 'Soft', 'Lifted', 'Hard'];
export const BLENDS: Array<[string, string]> = [['normal', 'Normal'], ['multiply', 'Multiply'], ['screen', 'Screen'], ['overlay', 'Overlay'], ['soft-light', 'Soft light']];
