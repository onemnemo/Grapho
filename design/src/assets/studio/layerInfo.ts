import type { IconName } from '../icons';
import { CARDS, FONTS } from './cards';
import { CLIPS } from './clips';
import { PAINT } from './paints';
import { PAT } from './patterns';
import { TEX, type TextureKey } from './textures';
import { patArt, paintArt, texArt, type TileArt } from './panelItems';
import type { Design, Layer, LayerType } from './types';

export const LAYER_NAMES: Record<LayerType, string> = {
  sticker: 'Sticker', image: 'Image', text: 'Text', shape: 'Shape', card: 'Card', icon: 'Icon', tex: 'Texture', pat: 'Pattern', paint: 'Painted fill', video: 'Video',
};
export const LAYER_ICON: Record<LayerType, IconName> = {
  sticker: 'sticker', image: 'image', text: 'text', shape: 'shapes', card: 'card', icon: 'smile', tex: 'texture', pat: 'texture', paint: 'texture', video: 'video',
};

/** The name shown in the layer list and the inspector header. */
export function layerLabel(l: Layer, design: Pick<Design, 'name'>): string {
  if (l.name) return l.name;
  switch (l.type) {
    case 'text': return '“' + (l.bind ? design.name : l.text || '').slice(0, 18) + '”';
    case 'icon': return l.ico ? l.ico[0].toUpperCase() + l.ico.slice(1) + ' icon' : 'Icon';
    case 'card': return (CARDS.find((c) => c[0] === l.kind) || [0, 'Card'])[1] as string;
    case 'tex': return TEX[l.key as TextureKey].name + ' texture';
    case 'pat': return PAT[l.key as keyof typeof PAT].name + ' pattern';
    case 'paint': return PAINT[l.key as keyof typeof PAINT].name + ' fill';
    case 'video': return l.clip && CLIPS[l.clip] ? CLIPS[l.clip].name + ' clip' : 'Video';
    default: return LAYER_NAMES[l.type] || 'Layer';
  }
}

/** The 28 px thumbnail of a layer in the layer list. */
export function layerThumb(l: Layer): TileArt {
  switch (l.type) {
    case 'sticker': return l.src ? { t: 'img', src: l.src, pixel: true } : { t: 'html', html: l.svg || '' };
    case 'image': return { t: 'photo', src: l.src || '' };
    case 'text': return { t: 'aa', font: FONTS[l.font || 'sans'][0], weight: l.weight || 500, colour: l.colour || 'currentColor' };
    case 'shape': return { t: 'shape', key: l.shape || 'rect', colour: l.fill && l.fill !== 'none' ? l.fill : l.stroke };
    case 'card': return { t: 'chip', bg: l.colour === 'transparent' ? 'none' : l.colour || 'none', ink: l.ink || 'currentColor' };
    case 'icon': return { t: 'icon', name: l.ico || 'star', colour: l.colour || 'currentColor', soft: l.soft };
    case 'tex': return texArt(l.key as TextureKey, TEX[l.key as TextureKey].mode === 'relief' ? undefined : l.colour);
    case 'pat': return patArt(l.key as keyof typeof PAT, l.ink, 0.35);
    case 'paint': return paintArt(l.key as keyof typeof PAINT);
    default: return { t: 'poster', bg: l.clip ? CLIPS[l.clip].bg : '' };
  }
}
