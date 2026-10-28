import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { ToastProvider } from '@/components/Toast';
import { FOLDER_COLOURS, type FolderColour } from '@/assets/covers';
import { QUICK_FOLDERS } from '@/assets/studio';
import { CanvasSheet } from '@/blocks/CanvasSheet';
import { FolderSheet } from '@/blocks/FolderSheet';
import { NotebookSheet } from '@/blocks/NotebookSheet';
import { AlphabetRail } from '@/blocks/AlphabetRail';
import { FilterSheet } from '@/blocks/FilterSheet';
import { LibraryHeader, type Crumb } from '@/blocks/LibraryHeader';
import { SelectionBar } from '@/blocks/SelectionBar';
import { AppFrame } from '@/components/AppFrame';
import { DeletedBody } from './DeletedBody';
import { FoldersBody, foldersShown } from './FoldersBody';
import { HomeBody } from './HomeBody';
import { SearchBody } from './SearchBody';
import { folderIds, groupItems } from './libraryModel';
import { targetsFor } from './tiles';
import { useLibrary, type LibraryInit } from './useLibrary';
import styles from './layout.module.css';

export interface LibraryRootProps extends LibraryInit {
  /** Start on the list of every folder instead of Home. */
  startOnFolders?: boolean;
  /** Start on Recently deleted. */
  startOnDeleted?: boolean;
  /** Folders as one pinned row of small tiles, for a big library. */
  pinnedRow?: boolean;
  /** Show "148 items" after the title and "3 of 40" in the Recent heading. */
  recentSearches?: string[];
  /** Give the frame rounded corners, for a screen shown inside a page. */
  framed?: boolean;
}

function Root({ startOnFolders = false, startOnDeleted = false, pinnedRow = false, recentSearches = ['Calculus', 'Lecture 6', 'exam'], framed, ...init }: LibraryRootProps) {
  const lib = useLibrary(init);
  const [onFolders, setOnFolders] = useState(startOnFolders || startOnDeleted);
  const [onDeleted, setOnDeleted] = useState(startOnDeleted);
  const sheet = lib.newSheet;
  const setSheet = lib.setNewSheet;
  const here = lib.current ? lib.folders.find((f) => f.id === lib.current)?.name : undefined;
  const locations = ['Library', ...lib.folders.map((f) => f.name)];
  const [scrolled, setScrolled] = useState(false);
  const [letter, setLetter] = useState<string | undefined>();
  const scroller = useRef<HTMLElement>(null);

  const home = !onFolders;
  const sub = home && lib.path.length > 0;
  const q = lib.query.trim();

  // letters that have a group, for the rail
  const letters = useMemo(() => {
    if (home) return lib.group === 'letter' ? groupItems(lib.sortedVisible, 'letter', lib.sortKey).map(([l]) => l ?? '') : [];
    return lib.folders.length > 24 && lib.folderSortKey === 'name' && !q ? foldersShown(lib).map((f) => (/[a-z]/i.test(f.name[0]) ? f.name[0].toUpperCase() : '#')) : [];
  }, [home, lib.group, lib.sortedVisible, lib.sortKey, lib.folders, lib.folderSortKey, q]);
  const showRail = letters.length > 0 && !(home && lib.searching);

  const syncLetter = useCallback(() => {
    const el = scroller.current;
    if (!el) return;
    setScrolled(el.scrollTop > 2);
    let act: string | undefined;
    el.querySelectorAll<HTMLElement>('[data-group]').forEach((g, i) => {
      if (i === 0 || g.offsetTop <= el.scrollTop + 60) act = g.dataset.group;
    });
    setLetter(act);
  }, []);
  useEffect(syncLetter, [syncLetter, letters.length, onFolders]);

  const jump = (l: string) => {
    const el = scroller.current;
    const target = el?.querySelector<HTMLElement>(`[data-group="${l}"]`);
    if (el && target) el.scrollTo({ top: target.offsetTop - 6 });
  };

  const crumbs: Crumb[] = [];
  let current: string | undefined;
  if (onDeleted) {
    crumbs.push({ label: 'Library', onClick: () => { setOnDeleted(false); setOnFolders(false); } }, { label: 'All folders', onClick: () => setOnDeleted(false) });
    current = 'Recently deleted';
  } else if (onFolders) {
    crumbs.push({ label: 'Library', onClick: () => setOnFolders(false) });
    current = 'All folders';
  } else if (sub) {
    crumbs.push({ label: 'Library', onClick: () => lib.setPath([]) });
    lib.path.forEach((id, i) => {
      const f = lib.folders.find((x) => x.id === id);
      if (!f) return;
      if (i < lib.path.length - 1) crumbs.push({ label: f.name, onClick: () => lib.setPath(lib.path.slice(0, i + 1)) });
      else current = f.name;
    });
  }

  const everyId = onFolders ? foldersShown(lib).map((f) => folderIds(f.id)) : [...lib.here.map((f) => folderIds(f.id)), ...lib.visible.map((i) => i.id)];
  const typeCounts = {
    notebook: lib.inScope.filter((i) => i.kind === 'notebook').length,
    pdf: lib.inScope.filter((i) => i.kind === 'pdf').length,
    canvas: lib.inScope.filter((i) => i.kind === 'canvas').length,
  };
  const tagMap = new Map<string, number>();
  lib.inScope.forEach((i) => i.tags.forEach((t) => tagMap.set(t, (tagMap.get(t) ?? 0) + 1)));
  const tags = [...tagMap.entries()].map(([name, count]) => ({ name, count }));
  const total = home ? (lib.sortedVisible.length) : 0;

  const back = () => {
    if (onDeleted) setOnDeleted(false);
    else if (onFolders) setOnFolders(false);
    else lib.setPath(lib.path.slice(0, -1));
  };

  return (
      <AppFrame framed={framed}>
        <LibraryHeader
          title="Library"
          path={crumbs}
          current={current}
          onBack={back}
          selecting={lib.selecting}
          selectedCount={lib.selected.length}
          allSelected={everyId.length > 0 && lib.selected.length >= everyId.length}
          onSelectAll={() => lib.select.all(everyId)}
          onDone={lib.select.end}
          searching={lib.searching}
          query={lib.query}
          searchPlaceholder={onFolders ? 'Search folders' : 'Search notebooks, folders and text'}
          onQueryChange={lib.setQuery}
          onSearchOpen={lib.openSearch}
          onSearchClose={lib.closeSearch}
          onFilter={home ? () => lib.setFilterOpen(true) : undefined}
          filterActive={lib.filterActive}
          onSettings={() => {}}
          onNew={lib.create}
          scrolled={scrolled}
        />
        <main ref={scroller} className={styles.scroll} onScroll={syncLetter}>
          {onDeleted ? <DeletedBody lib={lib} /> : onFolders ? <FoldersBody lib={lib} onDeleted={() => { lib.closeSearch(); setOnDeleted(true); }} /> : lib.searching ? <SearchBody lib={lib} recent={recentSearches} /> : (
            <HomeBody lib={lib} onSeeAll={() => setOnFolders(true)} pinnedRow={pinnedRow} />
          )}
        </main>
        {showRail && <AlphabetRail available={letters} active={letter} onSelect={jump} />}
        <SelectionBar
          visible={lib.selecting}
          count={lib.selected.length}
          moveTargets={targetsFor(lib).filter((t) => !lib.selected.includes(folderIds(t.id)))}
          onMoveTo={(to) => lib.moveTo(lib.selected, to)}
          onDelete={() => lib.remove(lib.selected)}
          onShare={() => {}}
          onCopy={() => {}}
          onTag={() => {}}
        />
        <NotebookSheet
          key={`nb${sheet === 'notebook'}`} open={sheet === 'notebook'} onClose={() => setSheet(null)} mode="new" placement="container" locations={locations}
          defaultWhere={here}
          onCreate={(c) => { lib.createFrom('notebook', { name: c.name, where: c.where, color: c.hex }); setSheet(null); }}
        />
        <CanvasSheet
          key={`cv${sheet === 'canvas'}`} open={sheet === 'canvas'} onClose={() => setSheet(null)} mode="new" placement="container" locations={locations} defaultWhere={here}
          onCreate={(c) => { lib.createFrom('canvas', { name: c.name, where: c.where }); setSheet(null); }}
        />
        <FolderSheet
          key={`fo${sheet === 'folder'}`} open={sheet === 'folder'} onClose={() => setSheet(null)} mode="new" placement="container" locations={locations}
          defaultWhere={here}
          onCreate={(c) => { lib.createFrom('folder', { name: c.name, where: c.where, folderColour: nearestFolderColour(c.hex) }); setSheet(null); }}
        />
        <FilterSheet
          open={lib.filterOpen}
          onClose={() => lib.setFilterOpen(false)}
          placement="container"
          value={lib.filters}
          onChange={lib.setFilters}
          typeCounts={typeCounts}
          tags={tags}
          resultCount={total}
        />
      </AppFrame>
  );
}

/** The library's folders come in eight named colours, so a colour from the sheet lands on the closest of them. */
function nearestFolderColour(hex: string): FolderColour {
  const channels = (h: string) => [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16));
  const [r, g, b] = channels(hex);
  let best: FolderColour = 'indigo';
  let d = Infinity;
  QUICK_FOLDERS.forEach(([name, h]) => {
    const [r2, g2, b2] = channels(h);
    const e = (r - r2) ** 2 + (g - g2) ** 2 + (b - b2) ** 2;
    const c = name.toLowerCase() as FolderColour;
    if (e < d && FOLDER_COLOURS.includes(c)) { d = e; best = c; }
  });
  return best;
}

/** The library, with its toast region inside the frame. */
export function LibraryRoot(props: LibraryRootProps) {
  return (
    <div className={styles.root}>
      <ToastProvider placement="container" offset="above-bar">
        <Root {...props} />
      </ToastProvider>
    </div>
  );
}
