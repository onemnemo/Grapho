import type { IconName } from '../icons';

/* The studio's tools and the packs behind stickers, shapes and textures. Inside a pack, items sit in categories. */
export type ToolKey = 'stickers' | 'images' | 'text' | 'shapes' | 'cards' | 'icons' | 'textures' | 'video';
export const TOOLS: Array<[ToolKey, string, IconName]> = [
  ['stickers', 'Stickers', 'sticker'], ['images', 'Images', 'image'], ['text', 'Text', 'text'], ['shapes', 'Shapes', 'shapes'],
  ['cards', 'Cards', 'card'], ['icons', 'Icons', 'smile'], ['textures', 'Textures', 'texture'], ['video', 'Video', 'video'],
];
export type PackedTool = 'stickers' | 'shapes' | 'textures';
export interface PackDef { id: string; name: string; by: string; tabs: Array<[string, string]> }
export const PACKDEF: Record<PackedTool, PackDef[]> = {
  stickers: [
    { id: 'grapho', name: 'Grapho', by: 'Built in', tabs: [['pixel', 'Pixel'], ['marks', 'Marks'], ['nature', 'Nature'], ['notes', 'Notes'], ['study', 'Study labels']] },
    { id: 'tape', name: 'Washi tape', by: 'Built in', tabs: [['pastel', 'Pastel'], ['bold', 'Bold']] },
    { id: 'botanical', name: 'Botanical', by: 'Mira L.', tabs: [['plants', 'Plants'], ['sky', 'Sky']] },
  ],
  shapes: [{ id: 'grapho', name: 'Grapho', by: 'Built in', tabs: [['geometric', 'Geometric'], ['symbols', 'Symbols'], ['lines', 'Lines']] }],
  textures: [{ id: 'grapho', name: 'Grapho', by: 'Built in', tabs: [['textures', 'Textures'], ['patterns', 'Patterns'], ['painted', 'Painted']] }],
};
/** What a pack's contents are called in its count: "41 stickers". */
export const PACK_NOUN: Record<PackedTool, string> = { stickers: 'stickers', shapes: 'shapes', textures: 'materials' };
export const STICKER_CATS: Record<string, string[]> = {
  marks: ['star', 'heart', 'sparkle', 'smile', 'check', 'bolt'],
  nature: ['flower', 'cloud', 'sun', 'moon', 'leaf'],
  notes: ['bubble', 'pushpin', 'arrow', 'bookmark'],
};
export const BOTANICAL = ['leaf', 'flower', 'sun', 'cloud', 'moon', 'sparkle'];
/** Text styles offered in the Text panel: key, label, CSS for the sample. */
export const TEXT_STYLES: Array<[string, string, { font: 'sans' | 'serif' | 'hand' | 'mono'; size: number; weight: number; ls?: string }]> = [
  ['head', 'Heading', { font: 'sans', size: 24, weight: 500, ls: '-.02em' }],
  ['serif', 'Serif title', { font: 'serif', size: 28, weight: 400 }],
  ['hand', 'Handwritten', { font: 'hand', size: 28, weight: 700 }],
  ['mono', 'LABEL', { font: 'mono', size: 13, weight: 500, ls: '.2em' }],
  ['body', 'Body text', { font: 'sans', size: 15, weight: 400 }],
];
/** Folder icons: the subject icons offered in sheets, the studio and the icon grid. */
export const SUBJECT_ICONS = [
  'book', 'atom', 'flask', 'calculator', 'globe', 'pencil', 'palette', 'music', 'code', 'heart', 'star', 'bolt', 'leaf', 'moon', 'sun', 'cloud',
  'camera', 'flag', 'bell', 'bulb', 'key', 'pin', 'cap', 'dumbbell', 'plane', 'coffee', 'briefcase', 'home', 'calendar', 'chart', 'clock', 'mail',
  'paw', 'tag', 'target', 'gamepad', 'mountain', 'ruler', 'dna', 'wave', 'puzzle', 'umbrella', 'anchor', 'smile',
] as IconName[];
