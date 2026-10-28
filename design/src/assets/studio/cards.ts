/* Cards and labels, fonts and palettes the studio offers. */
export type CardKind = 'label' | 'sticky' | 'index' | 'tag' | 'ribbon' | 'stamp' | 'polaroid';
export type CardDef = [CardKind, string, string, string, string, FontKey, number, number];
/** kind, name, sample text, card colour, ink, font, width, height */
export const CARDS: CardDef[] = [
  ['label', 'Label', 'Biology', '#FFFFFF', '#17181C', 'hand', 150, 44],
  ['sticky', 'Sticky note', 'Exam on Friday', '#F6E27A', '#3a3208', 'hand', 130, 120],
  ['index', 'Index card', 'Chapter 4', '#F7F4EC', '#23242A', 'hand', 170, 110],
  ['tag', 'Tag', 'SEMESTER 2', '#17181C', '#F5F5F4', 'sans', 160, 56],
  ['ribbon', 'Ribbon', 'Lecture 6', '#E0402E', '#FFFFFF', 'sans', 190, 44],
  ['stamp', 'Stamp', 'DRAFT', 'transparent', '#E0402E', 'sans', 140, 52],
  ['polaroid', 'Photo card', 'Summer 24', '#FFFFFF', '#23242A', 'hand', 150, 178],
];

export type FontKey = 'sans' | 'serif' | 'hand' | 'mono';
export const FONTS: Record<FontKey, [string, string, number[]]> = {
  sans: ["Geist, 'Inter', system-ui, sans-serif", 'Sans', [400, 500, 600]],
  serif: ["'Instrument Serif', Georgia, serif", 'Serif', [400]],
  hand: ["Caveat, 'Bradley Hand', cursive", 'Handwritten', [700]],
  mono: ["'Geist Mono', ui-monospace, monospace", 'Mono', [400, 500]],
};
export const WEIGHT_NAMES: Record<number, string> = { 400: 'Regular', 500: 'Medium', 600: 'Semi', 700: 'Bold' };

export const PALETTE = ['#F5F5F4', '#17181C', '#E0402E', '#F28A2E', '#F6C453', '#3FB27A', '#5C8DF0', '#B9A6F0', '#D9487A', '#F4A9C6'];
export const COVERCOL = ['#F2C14E', '#B7A5F2', '#27275A', '#5B1F2B', '#E9873A', '#0F3328', '#E8A2BE', '#5A8CF0', '#2F7F5E', '#3A3C46', '#E0402E', '#F5F1E6'];
export const TEX_TINTS = ['#F1EBDD', '#E9E7E3', '#C89B63', '#B98B58', '#F5F5F4', '#B7A5F2', '#9ED8F2', '#E8A2BE', '#3A3C46', '#17181C'];
export const FOLDER_BACKS = ['#3B3470', '#7A2E50', '#8A4A16', '#25604A', '#1F5A66', '#8A6A1A', '#2B4E8F', '#3A3C46', '#BDB9B2', '#141a33'];
export const FOLDER_TABS = ['#3B3470', '#7A2E50', '#8A4A16', '#25604A', '#1F5A66', '#8A6A1A', '#2B4E8F', '#3A3C46', '#BDB9B2', '#F6C453'];
export const FOLDER_FRONTS = ['#6A61B4', '#B04D76', '#CC7330', '#3F9A74', '#3A97A8', '#D9A933', '#5588D6', '#6A6D7B', '#E9E7E3', '#1b1936'];
/** Elastic strap colours. The first is the default dark band, stored as true. */
export const STRAP_DARK = '#0E0F12';
export const STRAP_COLOURS = [STRAP_DARK, '#D9B870', '#E0402E', '#F5F1E6', '#27275A', '#2F7F5E', '#5A8CF0', '#E8A2BE'];
/** Quick colours in the colour picker. */
export const SAVED_COLOURS = ['#E0402E', '#F28A2E', '#F6C453', '#3FB27A', '#5C8DF0', '#B9A6F0', '#D9487A', '#17181C'];
export const PAPER_COLOURS = ['#F2C14E', '#E8A2BE', '#F5F1E6'];
/** The grey a custom colour picker opens on when the value has no colour yet. */
export const NEUTRAL_GREY = '#808080';
