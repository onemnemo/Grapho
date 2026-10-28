/* Cover art for packs: a diagonal gradient and a pixel glyph. Artwork, so raw colours are allowed here. */
import type { PixelIconName } from '../pixel';

export interface PackArt {
  gradient: string;
  glyph: PixelIconName;
}

const g = (a: string, b: string) => `linear-gradient(135deg, ${a}, ${b})`;

export const packArt = {
  amber: { gradient: g('#F6C453', '#E9873A'), glyph: 'star' },
  forest: { gradient: g('#3FB27A', '#27275A'), glyph: 'pin' },
  blush: { gradient: g('#E8A2BE', '#B7A5F2'), glyph: 'book' },
  azure: { gradient: g('#5A8CF0', '#27275A'), glyph: 'doc' },
  graphite: { gradient: g('#3A3C46', '#0E0F12'), glyph: 'pen' },
  midnight: { gradient: g('#27275A', '#5A8CF0'), glyph: 'eye' },
  moon: { gradient: g('#27275A', '#B7A5F2'), glyph: 'eye' },
  lab: { gradient: g('#3FB27A', '#0E0F12'), glyph: 'doc' },
  paper: { gradient: g('#F2C14E', '#E8A2BE'), glyph: 'book' },
  nib: { gradient: g('#5A8CF0', '#27275A'), glyph: 'pen' },
  tabs: { gradient: g('#E9873A', '#E0402E'), glyph: 'tag' },
  dusk: { gradient: g('#B7A5F2', '#27275A'), glyph: 'star' },
  botanica: { gradient: g('#3FB27A', '#27275A'), glyph: 'tag' },
} satisfies Record<string, PackArt>;

export type PackArtName = keyof typeof packArt;
