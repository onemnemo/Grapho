/* Library data model and the pure functions the library screens share: sort, filter, group, labels. */
import type { CoverPreset, DocKind, FolderColour } from '@/assets/covers';
import type { CoverTileKind } from '@/blocks/CoverTile';
import type { FilterState } from '@/blocks/FilterSheet';
import type { FolderSortKey, GroupBy, ItemSortKey, SortDir, SortKey } from '@/blocks/SortMenu';

export interface LibItem {
  id: string;
  kind: CoverTileKind;
  cover: CoverPreset;
  /** A cover colour of its own, from the notebook sheet. */
  color?: string;
  doc: DocKind;
  title: string;
  pages: number;
  /** Hours since last opened, edited and created. */
  op: number;
  ed: number;
  cr: number;
  folder: string | null;
  star: boolean;
  tags: string[];
  notes: Array<{ p: number; t: string }>;
}

export interface LibFolder {
  id: string;
  colour: FolderColour;
  name: string;
  parent?: string | null;
  pin?: boolean;
  /** Item count to show when the items themselves are not loaded. */
  n?: number;
  ed?: number;
}

/** One thing in Recently deleted: an item, or a folder with everything that was inside it. */
export interface BinEntry {
  /** The id it had in the library: an item id, or a folder id with its prefix. */
  id: string;
  items: LibItem[];
  folders: LibFolder[];
  /** Days before it goes for good. */
  left: number;
}

export const binName = (e: BinEntry) => (e.folders[0] && e.id === folderIds(e.folders[0].id) ? e.folders[0].name : e.items[0]?.title ?? '');

export const folderIds = (id: string) => `f:${id}`;
export const isFolderId = (id: string) => id.startsWith('f:');
export const plural = (n: number, word: string) => `${n} ${word}${n === 1 ? '' : 's'}`;

const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
/** Hours ago, as a short phrase. The calendar is fixed so stories look the same every day. */
export function when(h: number): string {
  if (h < 1) return `${Math.max(1, Math.round(h * 60))} min ago`;
  if (h < 24) return `${Math.round(h)} h ago`;
  if (h < 48) return 'Yesterday';
  const dt = new Date(new Date(2026, 9, 26, 12).getTime() - h * 36e5);
  return h < 168 ? DAYS[dt.getDay()] : dt.toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
}

export const KIND_LABEL: Record<CoverTileKind, string> = { notebook: 'Notebook', pdf: 'PDF', canvas: 'Canvas' };
export const itemMeta = (it: LibItem, short = false) =>
  it.kind === 'canvas' ? 'Canvas' : short ? plural(it.pages, 'page') : `${KIND_LABEL[it.kind]} · ${plural(it.pages, 'page')}`;

export function folderCount(f: LibFolder, items: LibItem[]): number {
  return f.n ?? items.filter((x) => x.folder === f.id).length;
}
export function folderMeta(f: LibFolder, items: LibItem[], folders: LibFolder[]): string {
  const n = folderCount(f, items);
  const sub = folders.filter((x) => x.parent === f.id).length;
  return (n ? plural(n, 'item') : 'Empty') + (sub ? ` · ${plural(sub, 'folder')}` : '');
}
export function folderEdited(f: LibFolder, items: LibItem[]): number {
  if (f.ed != null) return f.ed;
  const own = items.filter((x) => x.folder === f.id);
  return own.length ? Math.min(...own.map((i) => i.ed)) : 900;
}
export const pathOf = (it: LibItem, folders: LibFolder[]): string => {
  const out: string[] = [];
  let f = it.folder;
  while (f) {
    const cur = folders.find((x) => x.id === f);
    if (!cur) break;
    out.unshift(cur.name);
    f = cur.parent ?? null;
  }
  return ['Library', ...out].join(' / ');
};

export function passesFilters(it: LibItem, f: FilterState): boolean {
  if (f.types.length && !f.types.includes(it.kind)) return false;
  if (f.tags.length && !it.tags.some((t) => f.tags.includes(t))) return false;
  if (f.edited === 'week' && it.ed > 168) return false;
  if (f.edited === 'month' && it.ed > 720) return false;
  if (f.edited === 'year' && it.ed > 8760) return false;
  if (f.starred && !it.star) return false;
  return true;
}

const TIME_KEY = { opened: 'op', edited: 'ed', created: 'cr' } as const;

export function sortItems(list: LibItem[], key: ItemSortKey, dir: SortDir): LibItem[] {
  if (key === 'custom') return list.slice();
  const sgn = dir === 'asc' ? 1 : -1;
  const val = (i: LibItem) => (key === 'name' ? i.title.toLowerCase() : key === 'size' ? i.pages : -i[TIME_KEY[key]]);
  return list.slice().sort((a, b) => {
    const x = val(a), y = val(b);
    return (x < y ? -1 : x > y ? 1 : a.title.localeCompare(b.title)) * sgn;
  });
}

export function sortFolders(list: LibFolder[], key: FolderSortKey, dir: SortDir, items: LibItem[]): LibFolder[] {
  const sgn = dir === 'asc' ? 1 : -1;
  const val = (f: LibFolder) => (key === 'items' ? folderCount(f, items) : key === 'edited' ? -folderEdited(f, items) : f.name.toLowerCase());
  return list.slice().sort((a, b) => {
    const x = val(a), y = val(b);
    return (x < y ? -1 : x > y ? 1 : a.name.localeCompare(b.name)) * sgn;
  });
}

export const letterOf = (s: string) => (/[a-z]/i.test(s[0]) ? s[0].toUpperCase() : '#');

export function groupItems(list: LibItem[], group: GroupBy, key: SortKey): Array<[string | null, LibItem[]]> {
  if (group === 'none') return [[null, list]];
  const m = new Map<string, LibItem[]>();
  const tk = key === 'opened' || key === 'edited' || key === 'created' ? TIME_KEY[key] : 'op';
  list.forEach((i) => {
    let g: string;
    if (group === 'type') g = { notebook: 'Notebooks', pdf: 'PDFs', canvas: 'Canvases' }[i.kind];
    else if (group === 'letter') g = letterOf(i.title);
    else {
      const h = i[tk];
      g = h < 24 ? 'Today' : h < 48 ? 'Yesterday' : h < 168 ? 'This week' : 'Earlier';
    }
    m.set(g, [...(m.get(g) ?? []), i]);
  });
  return [...m.entries()];
}

/** Papers peeking out of a folder: the covers of the first notebooks inside it. */
export function paperPresets(f: LibFolder, items: LibItem[]) {
  const own = items.filter((x) => x.folder === f.id);
  const n = Math.min(3, folderCount(f, items));
  const fallback = ['biology', 'calculus', 'sketches', 'history', 'plain-paper'] as const;
  return [0, 1, 2].filter((i) => i < n).map((i) => {
    const it = own[i];
    if (it) return it.kind === 'notebook' ? it.cover : 'plain-paper';
    return fallback[(f.name.length + i * 2) % 5];
  });
}
