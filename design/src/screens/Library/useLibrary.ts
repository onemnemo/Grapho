import { useCallback, useMemo, useRef, useState } from 'react';
import { useToast } from '@/components/Toast';
import { NO_FILTERS, filtersActive, type FilterState } from '@/blocks/FilterSheet';
import type { NewKind } from '@/blocks/NewMenu';
import type { FolderColour } from '@/assets/covers';
import type { FolderSortKey, GroupBy, ItemSortKey, SortDir, SortKey, ViewMode } from '@/blocks/SortMenu';
import type { FolderAction } from '@/blocks/FolderMenu';
import { binName, folderIds, isFolderId, passesFilters, plural, sortItems, type BinEntry, type LibFolder, type LibItem } from './libraryModel';

export interface BannerInfo {
  title: string;
  text: string;
  action?: string;
}

export interface LibraryInit {
  items: LibItem[];
  folders: LibFolder[];
  /** Folders you are inside, outermost first. */
  path?: string[];
  view?: ViewMode;
  sortKey?: SortKey;
  sortDir?: SortDir;
  group?: GroupBy;
  size?: number;
  query?: string;
  searching?: boolean;
  selecting?: boolean;
  selected?: string[];
  filters?: FilterState;
  banner?: BannerInfo | null;
  /** Show every note at the top level, not only those outside folders. */
  scopeAll?: boolean;
  /** What is in Recently deleted. */
  deleted?: BinEntry[];
}

/** The state of a library and what you can do to it. Needs a ToastProvider above it for Undo. */
export function useLibrary(init: LibraryInit) {
  const toast = useToast();
  const [items, setItems] = useState(init.items);
  const [folders, setFolders] = useState(init.folders);
  const [path, setPath] = useState<string[]>(init.path ?? []);
  const [view, setView] = useState<ViewMode>(init.view ?? 'grid');
  const [sortKey, setSortKey] = useState<SortKey>(init.sortKey ?? 'opened');
  const [sortDir, setSortDir] = useState<SortDir>(init.sortDir ?? 'desc');
  const [folderSortKey, setFolderSortKey] = useState<FolderSortKey>('name');
  const [folderSortDir, setFolderSortDir] = useState<SortDir>('asc');
  const [group, setGroup] = useState<GroupBy>(init.group ?? 'none');
  const [size, setSize] = useState(init.size ?? 50);
  const [query, setQuery] = useState(init.query ?? '');
  const [searching, setSearching] = useState(init.searching ?? false);
  const [selecting, setSelecting] = useState(init.selecting ?? false);
  const [selected, setSelected] = useState<string[]>(init.selected ?? []);
  const [filters, setFilters] = useState<FilterState>(init.filters ?? NO_FILTERS);
  const [banner, setBanner] = useState<BannerInfo | null>(init.banner ?? null);
  const [filterOpen, setFilterOpen] = useState(false);
  const [newSheet, setNewSheet] = useState<'notebook' | 'canvas' | 'folder' | null>(null);
  const [bin, setBin] = useState<BinEntry[]>(init.deleted ?? []);
  const scopeAll = init.scopeAll ?? false;
  // Undo and Restore can run long after the delete, so they read the library as it is now.
  const live = useRef({ folders, bin });
  live.current = { folders, bin };

  const current = path[path.length - 1] ?? null;

  /** The notes in view: this folder (or the top level), before filters. */
  const inScope = useMemo(
    () => items.filter((i) => (path.length ? i.folder === current : scopeAll ? true : !i.folder)),
    [items, path, current, scopeAll],
  );
  const visible = useMemo(() => inScope.filter((i) => passesFilters(i, filters)), [inScope, filters]);
  const sortedVisible = useMemo(() => sortItems(visible, sortKey as ItemSortKey, sortDir), [visible, sortKey, sortDir]);
  const here = useMemo(() => folders.filter((f) => (f.parent ?? null) === current), [folders, current]);

  const select = {
    toggle: (id: string) => setSelected((s) => (s.includes(id) ? s.filter((x) => x !== id) : [...s, id])),
    begin: (id?: string) => { setSelecting(true); if (id) setSelected((s) => (s.includes(id) ? s : [...s, id])); },
    end: () => { setSelecting(false); setSelected([]); },
    all: (ids: string[]) => setSelected((s) => (s.length >= ids.length ? [] : ids)),
  };

  const openSearch = useCallback(() => { setSearching(true); setSelecting(false); setSelected([]); }, []);
  const closeSearch = useCallback(() => { setSearching(false); setQuery(''); }, []);

  /** Put entries back. An item whose folder is gone goes back to the library. */
  const restore = useCallback((ids: string[]) => {
    const back = live.current.bin.filter((e) => ids.includes(e.id));
    if (!back.length) return [];
    const folderBack = back.flatMap((e) => e.folders);
    const known = new Set([...live.current.folders, ...folderBack].map((f) => f.id));
    setFolders((l) => [...l, ...folderBack.filter((f) => !l.some((x) => x.id === f.id))].map((f) => (f.parent && !known.has(f.parent) ? { ...f, parent: null } : f)));
    setItems((l) => [...l, ...back.flatMap((e) => e.items).filter((i) => !l.some((x) => x.id === i.id)).map((i) => (i.folder && !known.has(i.folder) ? { ...i, folder: null } : i))]);
    setBin((b) => b.filter((e) => !ids.includes(e.id)));
    return back;
  }, []);

  const remove = useCallback((ids: string[]) => {
    const kids = (id: string): string[] => folders.filter((f) => f.parent === id).flatMap((f) => [f.id, ...kids(f.id)]);
    const topFolders = ids.filter(isFolderId).map((id) => id.slice(2));
    const folderIdsGone = [...new Set(topFolders.flatMap((id) => [id, ...kids(id)]))];
    const inside = (fids: string[]) => items.filter((i) => i.folder && fids.includes(i.folder));
    const entries: BinEntry[] = [
      ...topFolders.filter((id) => !folderIdsGone.some((g) => g !== id && kids(g).includes(id))).map((id) => {
        const fids = [id, ...kids(id)];
        return { id: folderIds(id), folders: folders.filter((f) => fids.includes(f.id)), items: inside(fids), left: 30 };
      }),
      ...items.filter((i) => ids.includes(i.id) && !(i.folder && folderIdsGone.includes(i.folder))).map((i) => ({ id: i.id, items: [i], folders: [], left: 30 })),
    ];
    if (!entries.length) return;
    const goneItems = new Set(entries.flatMap((e) => e.items.map((i) => i.id)));
    setItems((l) => l.filter((i) => !goneItems.has(i.id)));
    setFolders((l) => l.filter((f) => !folderIdsGone.includes(f.id)));
    setBin((b) => [...entries, ...b]);
    setSelected([]);
    setSelecting(false);
    if (path.some((id) => folderIdsGone.includes(id))) setPath([]);
    const n = entries.reduce((sum, e) => sum + e.items.length + e.folders.length, 0);
    const name = entries.length === 1 && n === 1 ? binName(entries[0])
      : entries.length === 1 && entries[0].folders.length ? `${binName(entries[0])} and ${plural(n - 1, 'item')}`
      : plural(entries.length, 'item');
    live.current.bin = [...entries, ...live.current.bin];
    toast.deleted(name, () => restore(entries.map((e) => e.id)));
  }, [folders, items, path, toast, restore]);

  const restoreFromBin = useCallback((ids: string[]) => {
    const back = restore(ids);
    if (back.length) toast.show(back.length === 1 ? `Restored ${binName(back[0])}` : `Restored ${plural(back.length, 'item')}`);
  }, [restore, toast]);

  /** Delete for good. Asked first, in the screen. */
  const purge = useCallback((ids?: string[]) => setBin((b) => (ids ? b.filter((e) => !ids.includes(e.id)) : [])), []);

  const moveTo = useCallback((ids: string[], folderId: string) => {
    const target = folders.find((f) => f.id === folderId);
    if (!target) return;
    const before = ids.map((id) => [id, isFolderId(id) ? folders.find((f) => f.id === id.slice(2))?.parent ?? null : items.find((i) => i.id === id)?.folder ?? null] as const);
    const apply = (to: (id: string) => string | null) => {
      setItems((l) => l.map((i) => (ids.includes(i.id) ? { ...i, folder: to(i.id) } : i)));
      setFolders((l) => l.map((f) => (ids.includes(folderIds(f.id)) && f.id !== folderId ? { ...f, parent: to(folderIds(f.id)) } : f)));
    };
    apply(() => folderId);
    setSelected([]);
    setSelecting(false);
    const first = ids[0];
    const label = ids.length === 1 ? (isFolderId(first) ? folders.find((f) => f.id === first.slice(2))?.name : items.find((i) => i.id === first)?.title) : plural(ids.length, 'item');
    toast.show(`Moved ${label} to ${target.name}`, {
      undo: () => apply((id) => before.find(([b]) => b === id)?.[1] ?? null),
    });
  }, [folders, items, toast]);

  const folderAction = useCallback((id: string, action: FolderAction) => {
    if (action === 'open') { setPath((p) => [...p, id]); setQuery(''); setSearching(false); }
    else if (action === 'pin') setFolders((l) => l.map((f) => (f.id === id ? { ...f, pin: !f.pin } : f)));
    else if (action === 'select') select.begin(folderIds(id));
    else if (action === 'delete') remove([folderIds(id)]);
    else if (action === 'edit') toast.show('Folder editing opens in the studio');
  }, [remove, toast]);

  const itemAction = useCallback((id: string, action: FolderAction) => {
    const it = items.find((i) => i.id === id);
    if (!it) return;
    if (action === 'open') toast.show(`Opens ${it.title}`);
    else if (action === 'edit') toast.show(it.kind === 'notebook' ? 'Opens Edit notebook' : `Rename ${it.title}`);
    else if (action === 'star') setItems((l) => l.map((i) => (i.id === id ? { ...i, star: !i.star } : i)));
    else if (action === 'duplicate') {
      const copy: LibItem = { ...it, id: `${id}-copy-${Date.now()}`, title: `${it.title} copy`, star: false, op: 0, ed: 0, cr: 0 };
      setItems((l) => [...l.slice(0, l.indexOf(it) + 1), copy, ...l.slice(l.indexOf(it) + 1)]);
      toast.show(`Duplicated ${it.title}`, { undo: () => setItems((l) => l.filter((x) => x.id !== copy.id)) });
    }
    else if (action === 'select') select.begin(id);
    else if (action === 'delete') remove([id]);
  }, [items, remove, toast]); // eslint-disable-line react-hooks/exhaustive-deps

  /** Make what a sheet described: a name, where it goes, and for a notebook or folder its colour. */
  const createFrom = useCallback((kind: 'notebook' | 'canvas' | 'folder', o: { name: string; where?: string; color?: string; folderColour?: FolderColour; pages?: number }) => {
    const id = `n${items.length}-${folders.length}-${Date.now()}`;
    const parent = o.where && o.where !== 'Library' ? folders.find((f) => f.name === o.where)?.id ?? null : null;
    const name = o.name.trim() || (kind === 'folder' ? 'New folder' : kind === 'canvas' ? 'New canvas' : 'New notebook');
    if (kind === 'folder') {
      setFolders((l) => [...l, { id, colour: o.folderColour ?? 'indigo', name, parent }]);
      toast.show(`Created ${name}`, { undo: () => setFolders((l) => l.filter((x) => x.id !== id)) });
      return;
    }
    const it: LibItem = { id, kind, cover: 'plain', color: o.color, doc: 'plain', title: name, pages: kind === 'canvas' ? 0 : o.pages ?? 1, op: 0, ed: 0, cr: 0, folder: parent, star: false, tags: [], notes: [] };
    setItems((l) => [...l, it]);
    if (searching) closeSearch();
    toast.show(`Created ${name}`, { undo: () => setItems((l) => l.filter((x) => x.id !== id)) });
  }, [items.length, folders, searching, closeSearch, toast]);

  /** New notebook, canvas and folder open their sheet; imports start their own flow. */
  const create = useCallback((kind: NewKind) => {
    if (kind === 'notebook' || kind === 'canvas' || kind === 'folder') { setNewSheet(kind); return; }
    toast.show(kind === 'image' ? 'Choose an image to import' : kind === 'pack' ? 'Choose a .graphopack file' : 'Choose a PDF to import');
  }, [toast]);

  return {
    items, folders, path, setPath, current, inScope, visible, sortedVisible, here, folderSortKey, setFolderSortKey, folderSortDir, setFolderSortDir,
    view, setView, sortKey, setSortKey, sortDir, setSortDir, group, setGroup, size, setSize,
    query, setQuery, searching, openSearch, closeSearch,
    selecting, selected, select,
    filters, setFilters, filterOpen, setFilterOpen, filterActive: filtersActive(filters),
    banner, dismissBanner: () => setBanner(null),
    remove, moveTo, folderAction, itemAction, create, createFrom, newSheet, setNewSheet, scopeAll,
    bin, restore: restoreFromBin, purge,
  };
}

export type Library = ReturnType<typeof useLibrary>;
