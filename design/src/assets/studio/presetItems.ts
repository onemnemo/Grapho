import { coverFrom, folderFrom, QUICK_COVERS, QUICK_FOLDERS } from './builders';
import { deep } from './layers';
import { buildTemplates } from './templates';
import type { Design, DesignKind } from './types';

export type PresetSource = 'yours' | 'built' | 'pack' | 'library';
export type PresetType = 'Plain' | 'Textured' | 'Illustrated' | 'Video';
export type PresetSort = 'new' | 'name' | 'used';

export interface PresetItem {
  id: string;
  design: Design;
  src: PresetSource;
  name: string;
  type: PresetType;
  /** Position in the suggested order. */
  ord: number;
  used: number;
  pack?: string;
}

export const PRESET_SOURCES: Array<[PresetSource | 'all', string]> = [['all', 'All'], ['yours', 'Yours'], ['built', 'Built in'], ['pack', 'Packs'], ['library', 'Your library']];
export const PRESET_TYPES: Array<PresetType | 'all'> = ['all', 'Plain', 'Textured', 'Illustrated', 'Video'];
export const PRESET_SORTS: Array<[PresetSort, string]> = [['new', 'Suggested'], ['name', 'Name A to Z'], ['used', 'Most used']];

/** What a look is made of: it decides which Type chip finds it. */
export const presetTypeOf = (d: Design): PresetType =>
  d.layers.some((l) => l.type === 'video') ? 'Video'
    : d.layers.some((l) => ['sticker', 'icon', 'image'].includes(l.type)) ? 'Illustrated'
      : d.layers.some((l) => ['tex', 'pat', 'paint'].includes(l.type)) ? 'Textured' : 'Plain';

const PACKS: Array<[string, string[]]> = [
  ['Botanical', ['#3F9A74', '#2F7F5E', '#9BC9A6', '#C9D9A3']],
  ['Pastel set', ['#E8A2BE', '#B7A5F2', '#9ED8F2', '#F6E3A1']],
  ['Lecture', ['#27275A', '#1F3A6B', '#5B1F2B', '#24382F']],
];
const FINISH_OF = [['grain', 'linen', 'felt', 'grain'], ['grain', 'linen', null, 'leather']];
const LIB_META = {
  cover: ['24 pages', '12 pages', '40 pages', '8 pages', '60 pages', '16 pages', '32 pages', '20 pages'],
  folder: ['3 items', '5 items', '2 items', '9 items', '4 items', '1 item', '6 items', '7 items'],
};

/** A big, mixed set so the picker has to scale: yours, built in, packs and the library. */
export function buildPresetItems(kind: DesignKind, yours: Design[] = []): PresetItem[] {
  const isC = kind === 'cover';
  const T = buildTemplates();
  const base = isC ? T.covers : T.folders;
  const out: PresetItem[] = [];
  let n = 0;
  const add = (d: Design, src: PresetSource, o: { meta?: string; used?: number; pack?: string } = {}) => {
    const design = deep(d);
    design.meta = o.meta ?? (design.meta || '').replace('Notebook · ', '');
    out.push({ id: `${src}-${n}`, design, src, name: design.name, type: presetTypeOf(design), ord: n, used: o.used ?? ((n + 1) * 7) % 11, pack: o.pack });
    n += 1;
  };
  yours.forEach((y) => add(y, 'yours', { meta: 'Saved just now', used: 9 }));
  base.forEach((d) => add(d, 'built'));
  (isC ? QUICK_COVERS : QUICK_FOLDERS).forEach((p) =>
    add(isC ? coverFrom(p[1], { tex: p[2], name: p[0], cover: { spine: true } }) : folderFrom(p[1], { finish: p[2], name: p[0] }), 'built', { meta: 'Built in' }));
  PACKS.forEach(([pn, cols]) => cols.forEach((h, i) => {
    const nm = `${pn} ${i + 1}`;
    add(isC ? coverFrom(h, { tex: FINISH_OF[1][i], name: nm, cover: { spine: true } }) : folderFrom(h, { finish: FINISH_OF[0][i], name: nm }), 'pack', { meta: `${pn} pack`, pack: pn });
  }));
  base.slice().reverse().forEach((d, i) => { const c = deep(d); add(c, 'library', { meta: LIB_META[kind][i % 8] }); });
  return out;
}

export interface PresetFilter { q: string; src: PresetSource | 'all'; type: PresetType | 'all'; sort: PresetSort }

export function filterPresets(all: PresetItem[], f: PresetFilter): PresetItem[] {
  const q = f.q.trim().toLowerCase();
  const l = all.filter((i) => (f.src === 'all' || i.src === f.src) && (f.type === 'all' || i.type === f.type) && (!q || i.name.toLowerCase().includes(q) || (i.pack ?? '').toLowerCase().includes(q)));
  if (f.sort === 'name') l.sort((a, b) => a.name.localeCompare(b.name));
  else if (f.sort === 'used') l.sort((a, b) => b.used - a.used);
  else l.sort((a, b) => a.ord - b.ord);
  return l;
}
