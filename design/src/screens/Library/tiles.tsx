import type { CSSProperties } from 'react';
import { CoverTile } from '@/blocks/CoverTile';
import { FolderTile } from '@/blocks/FolderTile';
import { LibraryRow } from '@/blocks/LibraryRow';
import type { FolderTarget } from '@/blocks/FolderMenu';
import { GroupHeading } from '@/blocks/GroupHeading';
import {
  KIND_LABEL, folderCount, folderEdited, folderIds, folderMeta, groupItems, itemMeta, paperPresets, pathOf, plural, when,
  type LibFolder, type LibItem,
} from './libraryModel';
import type { Library } from './useLibrary';
import { useLongPress } from './useLongPress';
import styles from './layout.module.css';

/** Click, long press and selection behaviour shared by every tile and row. */
export function useTileProps(lib: Library) {
  const press = useLongPress((id) => lib.select.begin(id), !lib.selecting);
  return (id: string, open: () => void) => {
    const lp = press(id);
    return {
      onPointerDown: lp.onPointerDown,
      onPointerMove: lp.onPointerMove,
      onPointerUp: lp.onPointerUp,
      onPointerLeave: lp.onPointerLeave,
      onPointerCancel: lp.onPointerCancel,
      onClick: () => {
        if (lp.wasLong()) return;
        if (lib.selecting) lib.select.toggle(id);
        else open();
      },
    };
  };
}

export const gridColumns = (size: number) => Math.round(8 - (size / 100) * 4);

export function targetsFor(lib: Library, skip?: string): FolderTarget[] {
  return lib.folders
    .filter((f) => f.id !== skip && f.id !== lib.current)
    .map((f) => ({ id: f.id, name: f.name, colour: f.colour, parent: f.parent ? lib.folders.find((p) => p.id === f.parent)?.name : undefined }));
}

interface FolderProps { lib: Library; folder: LibFolder; width: number | string; index?: number; query?: string; compactMeta?: boolean }

export function FolderItem({ lib, folder, width, query, compactMeta }: FolderProps) {
  const tile = useTileProps(lib);
  const id = folderIds(folder.id);
  return (
    <FolderTile
      name={folder.name}
      meta={compactMeta ? undefined : folderMeta(folder, lib.items, lib.folders)}
      colour={folder.colour}
      papers={paperPresets(folder, lib.items)}
      pinned={folder.pin && !compactMeta}
      width={width}
      query={query}
      selecting={lib.selecting}
      selected={lib.selected.includes(id)}
      smallMenuButton={typeof width === 'number' && width < 200}
      moveTargets={targetsFor(lib, folder.id)}
      onMove={(to) => lib.moveTo([id], to)}
      onMenuAction={(a) => lib.folderAction(folder.id, a)}
      {...tile(id, () => lib.folderAction(folder.id, 'open'))}
    />
  );
}

export function FolderRowItem({ lib, folder, query }: { lib: Library; folder: LibFolder; query?: string }) {
  const tile = useTileProps(lib);
  const id = folderIds(folder.id);
  const parent = folder.parent ? lib.folders.find((f) => f.id === folder.parent) : null;
  const sub = lib.folders.filter((f) => f.parent === folder.id).length;
  const n = folderCount(folder, lib.items);
  return (
    <LibraryRow
      kind="folder" colour={folder.colour} title={folder.name} query={query} pinned={folder.pin} path={parent ? parent.name : 'Library'}
      typeLabel={n ? plural(n, 'item') : 'Empty'} size={sub ? plural(sub, 'folder') : ''} when={when(folderEdited(folder, lib.items))}
      selecting={lib.selecting} selected={lib.selected.includes(id)}
      moveTargets={targetsFor(lib, folder.id)} onMove={(to) => lib.moveTo([id], to)} onMenuAction={(a) => lib.folderAction(folder.id, a)}
      {...tile(id, () => lib.folderAction(folder.id, 'open'))}
    />
  );
}

export function ItemTile({ lib, item, short, query }: { lib: Library; item: LibItem; short?: boolean; query?: string }) {
  const tile = useTileProps(lib);
  return (
    <CoverTile
      kind={item.kind} cover={item.cover} color={item.color} doc={item.doc} title={item.title} meta={itemMeta(item, short)} starred={item.star} query={query}
      selecting={lib.selecting} selected={lib.selected.includes(item.id)}
      moveTargets={targetsFor(lib)} onMove={(to) => lib.moveTo([item.id], to)} onMenuAction={(a) => lib.itemAction(item.id, a)}
      {...tile(item.id, () => {})}
    />
  );
}

export function ItemRow({ lib, item, query, hours }: { lib: Library; item: LibItem; query?: string; hours: number }) {
  const tile = useTileProps(lib);
  return (
    <LibraryRow
      kind={item.kind} cover={item.cover} color={item.color} doc={item.doc} title={item.title} query={query} path={pathOf(item, lib.folders)}
      typeLabel={KIND_LABEL[item.kind]} size={item.kind === 'canvas' ? 'Infinite' : plural(item.pages, 'page')} when={when(hours)} starred={item.star}
      selecting={lib.selecting} selected={lib.selected.includes(item.id)}
      moveTargets={targetsFor(lib)} onMove={(to) => lib.moveTo([item.id], to)} onMenuAction={(a) => lib.itemAction(item.id, a)}
      {...tile(item.id, () => {})}
    />
  );
}

/** Notes as a grid or a list, grouped when the sort menu says so. */
export function ItemsView({ lib, items }: { lib: Library; items: LibItem[] }) {
  const cols = gridColumns(lib.size);
  const timeKey = lib.sortKey === 'created' ? 'cr' : lib.sortKey === 'opened' ? 'op' : 'ed';
  return (
    <>
      {groupItems(items, lib.group, lib.sortKey).map(([label, list]) => (
        <div key={label ?? 'all'} data-group={label ?? undefined} className={label ? styles.group : undefined}>
          {label && <GroupHeading label={label} />}
          {lib.view === 'grid' ? (
            <div className={styles.grid} style={{ '--cols': cols } as CSSProperties}>
              {list.map((it) => <ItemTile key={it.id} lib={lib} item={it} short={cols >= 7} />)}
            </div>
          ) : (
            <div className={styles.rows}>
              {list.map((it) => <ItemRow key={it.id} lib={lib} item={it} hours={it[timeKey]} />)}
            </div>
          )}
        </div>
      ))}
    </>
  );
}
