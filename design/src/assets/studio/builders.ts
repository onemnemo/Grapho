import { mixHex, lum, lighten, darken } from './colour';
import { PAPER_COLOURS } from './cards';
import { TEX, type TextureKey } from './textures';
import { covFull, froFull, tCard, tIcon, tShape, tTex, dCover, dFolder } from './layers';
import type { Design } from './types';

/* Folder and cover designs built from a few choices: a colour, a finish or texture, an icon. */
export interface FolderChoice { name?: string; meta?: string; finish?: string | null; icon?: string | null; papers?: string[] }
export function folderFrom(hex: string, o: FolderChoice = {}): Design {
  const dark = lum(hex) > 0.6;
  const ink = dark ? '#17181C' : 'rgba(255,255,255,.92)';
  const layers = [];
  if (o.finish && o.finish !== 'plain') layers.push(tTex(o.finish as TextureKey, froFull(), { op: o.finish === 'grain' ? 0.55 : 0.95, name: TEX[o.finish as TextureKey].name }));
  if (o.icon) layers.push(tIcon(o.icon, 428, 142, 46, ink, { name: 'Folder icon' }));
  const d = dFolder(o.name || 'Semester 2', o.meta || 'Empty', { c1: lighten(hex, 0.05), c2: darken(hex, 0.14) }, { back: darken(hex, 0.36), ink: dark ? 'dark' : 'light', papers: o.papers || PAPER_COLOURS }, layers);
  d.hex = hex; d.finish = o.finish || 'plain'; d.icon = o.icon || null;
  return d;
}
export interface CoverChoice { name?: string; tex?: string | null; band?: string; icon?: string | null; cover?: Design['cover'] }
export function coverFrom(hex: string, o: CoverChoice = {}): Design {
  const layers = [];
  if (o.tex) layers.push(tTex(o.tex as TextureKey, covFull(), { op: o.tex === 'grain' ? 0.45 : 0.9 }));
  if (o.band) layers.push(tShape('rect', 150, 296, 300, 70, o.band, { name: 'Band' }));
  const dark = lum(hex) > 0.6;
  if (o.icon) layers.push(tIcon(o.icon, 150, 250, 92, dark ? '#17181C' : 'rgba(255,255,255,.9)', { name: 'Cover icon', sw: 1.5 }));
  layers.push(tCard('label', 150, o.band ? 296 : 76, { text: o.name || 'Untitled', colour: '#fff', ink: '#17181C', sh: 1, r: o.band ? 0 : -2, bind: 'name' }));
  const d = dCover(o.name || 'Untitled', { c1: hex }, layers, o.cover || {});
  d.hex = hex;
  return d;
}

/** name, colour, finish or texture */
export const QUICK_FOLDERS: Array<[string, string, string]> = [
  ['Indigo', '#6A61B4', 'linen'], ['Rose', '#B04D76', 'felt'], ['Ember', '#CC7330', 'grain'], ['Forest', '#3F9A74', 'grain'],
  ['Sea', '#3C8FB5', 'linen'], ['Gold', '#D9A933', 'grain'], ['Sky', '#5C8DF0', 'felt'], ['Slate', '#5C6070', 'grain'],
];
export const QUICK_COVERS: Array<[string, string, string | null]> = [
  ['Yellow', '#F2C14E', 'grain'], ['Lilac', '#B7A5F2', 'grain'], ['Navy', '#27275A', null], ['Wine', '#5B1F2B', 'leather'],
  ['Orange', '#E9873A', 'grain'], ['Forest', '#0F3328', 'linen'], ['Pink', '#E8A2BE', 'grain'], ['Sky', '#5A8CF0', null],
];

export const HUES: Array<[string, string]> = [
  ['Rose', '#D9487A'], ['Red', '#E0402E'], ['Orange', '#F28A2E'], ['Gold', '#F6C453'], ['Green', '#3FB27A'],
  ['Teal', '#2FA3A0'], ['Blue', '#5C8DF0'], ['Indigo', '#6A61B4'], ['Lilac', '#B9A6F0'],
];
export const SHADES: Array<['light' | 'base' | 'deep', (h: string) => string]> = [
  ['light', (h) => mixHex(h, '#ffffff', 0.32)], ['base', (h) => h], ['deep', (h) => mixHex(h, '#000000', 0.34)],
];
export const FINISHES: Array<[string, string]> = [['plain', 'Plain'], ['grain', 'Grain'], ['linen', 'Linen'], ['felt', 'Felt'], ['leather', 'Leather']];
export const COVER_TEX: Array<[string, string]> = [['plain', 'Plain'], ['grain', 'Grain'], ['linen', 'Linen'], ['leather', 'Leather'], ['felt', 'Felt'], ['paper', 'Paper']];
export const COVER_COLS = ['#F2C14E', '#E9873A', '#E0402E', '#E8A2BE', '#B7A5F2', '#5A8CF0', '#3C8FB5', '#2F7F5E', '#0F3328', '#27275A', '#5B1F2B', '#B98B58', '#17181C', '#ECE9E4'];
