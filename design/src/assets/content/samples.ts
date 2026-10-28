import { packArt } from '../packs';
import { allPackItems, buildPresetItems, type PanelItem } from '../studio';
import type { ContentItem, ContentSource, ContentType, ItemArt, PageLines } from './types';

/* Sample content for stories and the Settings screen: a believable amount, spread over built in, your own and nine packs. */

export const sampleSources: ContentSource[] = [
  { id: 'built', name: 'Built in', kind: 'built', enabled: true },
  { id: 'own', name: 'Your own', kind: 'own', enabled: true },
  { id: 'pixel', name: 'Pixel friends', kind: 'pack', art: packArt.amber, enabled: true },
  { id: 'botan', name: 'Botanical', kind: 'pack', art: packArt.forest, enabled: true },
  { id: 'tape', name: 'Washi tape', kind: 'pack', art: packArt.tabs, enabled: true },
  { id: 'study', name: 'Study labels', kind: 'pack', art: packArt.azure, enabled: true },
  { id: 'moon', name: 'Moon phases', kind: 'pack', art: packArt.moon, enabled: true },
  { id: 'soft', name: 'Soft covers', kind: 'pack', art: packArt.blush, enabled: true },
  { id: 'ink', name: 'Ink and brush', kind: 'pack', art: packArt.graphite, enabled: true },
  { id: 'mid', name: 'Midnight', kind: 'pack', art: packArt.midnight, enabled: false },
  { id: 'plan', name: 'Study planner', kind: 'pack', art: packArt.paper, enabled: true },
];

let seq = 0;
const make = (type: ContentType, name: string, source: string, art: ItemArt): ContentItem => {
  seq += 1;
  return { id: `${type}-${seq}`, type, name, source, art, added: seq, usedAt: (seq * 7919) % 97 };
};
const title = (s: string) => s.replace(/[-_]/g, ' ').replace(/^./, (c) => c.toUpperCase());

const pool: PanelItem[] = [...allPackItems('stickers', 'grapho'), ...allPackItems('stickers', 'tape'), ...allPackItems('stickers', 'botanical')];
const STICKERS: Array<[string, number]> = [['built', 30], ['own', 4], ['pixel', 24], ['botan', 22], ['tape', 12], ['study', 16], ['moon', 12]];
function stickers(): ContentItem[] {
  let at = 0;
  return STICKERS.flatMap(([source, n]) =>
    Array.from({ length: n }, (_, i) => {
      const p = pool[(at + i * 3) % pool.length];
      const round = Math.floor((at + i * 3) / pool.length);
      at += 1;
      return make('stickers', `${title(p.name)}${round ? ` ${round + 1}` : ''}`, source, { kind: 'tile', art: p.art, wide: p.size === 'wide' });
    }),
  );
}

const PENS: Array<[string, string, ItemArt]> = [
  ['Fine liner', 'built', { kind: 'pen', width: 1.6 }], ['Ballpoint', 'built', { kind: 'pen', width: 2.2 }], ['Fountain pen', 'built', { kind: 'pen', width: 3.2 }],
  ['Felt tip', 'built', { kind: 'pen', width: 4.4 }], ['Brush', 'built', { kind: 'pen', width: 6.5 }], ['Pencil', 'built', { kind: 'pen', width: 2.8, dash: true }],
  ['Marker', 'built', { kind: 'pen', width: 7 }], ['Highlighter', 'built', { kind: 'pen', width: 11, soft: true }],
  ['Sumi brush', 'ink', { kind: 'pen', width: 8 }], ['Dip pen', 'ink', { kind: 'pen', width: 2 }], ['Wash brush', 'ink', { kind: 'pen', width: 12 }],
  ['Dry brush', 'ink', { kind: 'pen', width: 6, dash: true }], ['Reed pen', 'ink', { kind: 'pen', width: 5 }], ['Charcoal', 'ink', { kind: 'pen', width: 9, dash: true }],
  ['Sketch 0.3', 'own', { kind: 'pen', width: 1.2 }], ['Margin note', 'own', { kind: 'pen', width: 2.6 }], ['Heading', 'own', { kind: 'pen', width: 5.2 }], ['Soft pastel', 'own', { kind: 'pen', width: 9, soft: true }],
];

const SETS: Array<[string, string, string[]]> = [
  ['Basic', 'built', ['#F5F5F4', '#E0402E', '#F28A2E', '#F6C453', '#3FB27A', '#5C8DF0', '#B9A6F0', '#D9487A']],
  ['Highlighters', 'built', ['#F6E36B', '#8CF0A8', '#7FD8F5', '#F5A3C7', '#FFB36B', '#C3A8FF']],
  ['Pastels', 'built', ['#F7C6D4', '#FBD9B5', '#F8EAB0', '#C8E8C2', '#BCDDF3', '#D9CCF4']],
  ['Earth', 'built', ['#7A4B2A', '#A9703C', '#C9A26B', '#6C7A3F', '#3E4F34', '#2B2B2A']],
  ['Neon', 'built', ['#FF2E88', '#FF6A00', '#E8FF1F', '#18FF8B', '#1FE3FF', '#8A3FFF']],
  ['Botanical', 'botan', ['#2F7F5E', '#3F9A74', '#9BC9A6', '#C9D9A3', '#E8C3D0', '#6B4A3A']],
  ['Ink wash', 'ink', ['#0E0F12', '#2A2D36', '#4B4F5C', '#7C8090', '#B5B8C4', '#E3E4EA']],
  ['Sunset', 'own', ['#2A1B4A', '#7A2E50', '#D9487A', '#F28A2E', '#F6C453']],
  ['Exam week', 'own', ['#E0402E', '#F6C453', '#3FB27A', '#5C8DF0', '#F5F5F4']],
  ['Midnight', 'mid', ['#0B1030', '#27275A', '#3E3A6D', '#5C8DF0', '#B9A6F0']],
];

const THEMES: Array<[string, string, ItemArt]> = [
  ['Grapho dark', 'built', { kind: 'theme', bg: '#08080A', panel: '#18181C', ink: '#F5F5F4', accent: '#F5F5F4' }],
  ['Black', 'built', { kind: 'theme', bg: '#000000', panel: '#101012', ink: '#F5F5F4', accent: '#A6A6AE' }],
  ['Paper', 'built', { kind: 'theme', bg: '#F3F1EC', panel: '#E5E2DA', ink: '#1B1D24', accent: '#1B1D24' }],
  ['Midnight blue', 'mid', { kind: 'theme', bg: '#0B1030', panel: '#1A2152', ink: '#E7EBFF', accent: '#5C8DF0' }],
  ['Dusk', 'mid', { kind: 'theme', bg: '#1D1633', panel: '#2E2350', ink: '#F1E8FF', accent: '#B9A6F0' }],
  ['Ink', 'mid', { kind: 'theme', bg: '#0C0D10', panel: '#1A1C22', ink: '#E7E8EC', accent: '#7C8090' }],
  ['Moon dust', 'moon', { kind: 'theme', bg: '#14141C', panel: '#23232F', ink: '#EDEDF3', accent: '#D7D3F2' }],
  ['Greenhouse', 'own', { kind: 'theme', bg: '#0A1712', panel: '#14281F', ink: '#E5F2EA', accent: '#3FB27A' }],
];

const PAGES: Array<[string, string, PageLines]> = [
  ['Blank', 'built', 'blank'], ['Ruled', 'built', 'ruled'], ['Grid', 'built', 'grid'], ['Dotted', 'built', 'dots'],
  ['Cornell', 'built', 'cornell'], ['Music staff', 'built', 'music'], ['Checklist', 'built', 'checklist'],
  ['Weekly plan', 'plan', 'plan'], ['Daily plan', 'plan', 'plan'], ['Month grid', 'plan', 'grid'], ['Habit tracker', 'plan', 'checklist'], ['Reading log', 'plan', 'ruled'], ['Goals', 'plan', 'checklist'],
  ['Lecture notes', 'study', 'cornell'], ['Flashcard sheet', 'study', 'grid'], ['Formula page', 'study', 'dots'],
  ['Lab report', 'own', 'ruled'], ['Sketch dots', 'own', 'dots'],
];

const PACK_OF: Record<string, string> = { Botanical: 'botan', 'Pastel set': 'soft', Lecture: 'study' };
function designs(kind: 'cover' | 'folder'): ContentItem[] {
  const type: ContentType = kind === 'cover' ? 'covers' : 'folders';
  return buildPresetItems(kind)
    .filter((p) => p.src !== 'library')
    .map((p) => make(type, p.name, p.src === 'yours' ? 'own' : p.src === 'built' ? 'built' : PACK_OF[p.pack ?? ''] ?? 'soft', { kind: 'design', design: p.design }));
}

/** Everything on the Content pages. Built once. */
export function buildSampleContent(): ContentItem[] {
  seq = 0;
  return [
    ...PENS.map(([n, s, a]) => make('pens', n, s, a)),
    ...SETS.map(([n, s, colours]) => make('colours', n, s, { kind: 'colours', colours })),
    ...designs('cover'),
    ...designs('folder'),
    ...stickers(),
    ...PAGES.map(([n, s, lines]) => make('templates', n, s, { kind: 'page', lines })),
    ...THEMES.map(([n, s, a]) => make('themes', n, s, a)),
  ];
}
