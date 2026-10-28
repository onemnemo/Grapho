import type { ContentItem, ContentSource } from '@/assets/content';

export type ContentSort = 'recent' | 'name' | 'newest';
export const SORT_OPTIONS: Array<[ContentSort, string]> = [['recent', 'Recently used'], ['name', 'Name'], ['newest', 'Newest']];

/** All, Built in, Your own, or a pack id. */
export type SourceFilter = string;

/** Items of one type from the chosen source, in the chosen order. Items of packs that are off are left out. */
export function viewItems(items: ContentItem[], sources: ContentSource[], o: { type: ContentItem['type']; source: SourceFilter; sort: ContentSort; query: string }): ContentItem[] {
  const off = new Set(sources.filter((s) => !s.enabled).map((s) => s.id));
  const q = o.query.trim().toLowerCase();
  const list = items.filter((i) => i.type === o.type && !off.has(i.source) && (o.source === 'all' || i.source === o.source) && (!q || i.name.toLowerCase().includes(q)));
  const by: Record<ContentSort, (a: ContentItem, b: ContentItem) => number> = {
    recent: (a, b) => b.usedAt - a.usedAt,
    name: (a, b) => a.name.localeCompare(b.name),
    newest: (a, b) => b.added - a.added,
  };
  return list.sort(by[o.sort]);
}

/** How many items of a type a source holds, for the menu. */
export const countFrom = (items: ContentItem[], type: ContentItem['type'], source: string) => items.filter((i) => i.type === type && (source === 'all' || i.source === source)).length;
