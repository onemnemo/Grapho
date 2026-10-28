import type { CSSProperties } from 'react';
import { EmptyState } from '@/components/EmptyState';
import { GroupHeading } from '@/blocks/GroupHeading';
import { SectionHeader } from '@/blocks/SectionHeader';
import { letterOf, sortFolders, type LibFolder } from './libraryModel';
import { DeletedLink } from './DeletedBody';
import { FolderItem, FolderRowItem } from './tiles';
import type { Library } from './useLibrary';
import styles from './layout.module.css';

/** Every folder in one list: pinned first, then A to Z when sorted by name. */
export function foldersShown(lib: Library): LibFolder[] {
  const q = lib.query.trim().toLowerCase();
  return sortFolders(lib.folders.filter((f) => !q || f.name.toLowerCase().includes(q)), lib.folderSortKey, lib.folderSortDir, lib.items);
}

export function folderGroups(lib: Library, list: LibFolder[]): Array<{ label: string; quiet?: boolean; folders: LibFolder[] }> {
  if (lib.folderSortKey !== 'name' || lib.query.trim()) return [{ label: '', folders: list }];
  const out: Array<{ label: string; quiet?: boolean; folders: LibFolder[] }> = [];
  const pinned = list.filter((f) => f.pin);
  if (pinned.length) out.push({ label: 'Pinned', quiet: true, folders: pinned });
  const m = new Map<string, LibFolder[]>();
  list.filter((f) => !f.pin).forEach((f) => m.set(letterOf(f.name), [...(m.get(letterOf(f.name)) ?? []), f]));
  m.forEach((folders, label) => out.push({ label, folders }));
  return out;
}

export function FoldersBody({ lib, onDeleted }: { lib: Library; onDeleted: () => void }) {
  const q = lib.query.trim();
  const list = foldersShown(lib);
  return (
    <section className={styles.section}>
      <SectionHeader
        title="Folders"
        view={lib.view}
        onView={lib.setView}
        sort={{ scope: 'folders', sortKey: lib.folderSortKey, sortDir: lib.folderSortDir, onSortKey: (k) => lib.setFolderSortKey(k as 'name'), onSortDir: lib.setFolderSortDir }}
      />
      {list.length === 0 ? (
        <div className={styles.center}>
          {q ? (
            <EmptyState anim="worry" bubble="no match" title="No folder with that name" secondary={{ label: 'Clear search', onClick: () => lib.setQuery('') }}>
              Check the spelling or try fewer letters.
            </EmptyState>
          ) : (
            <EmptyState anim="wave" bubble="no folders yet" title="No folders yet" primary={{ label: 'New folder', icon: 'plus', onClick: () => lib.create('folder') }}>
              Folders keep related notebooks together. Make one from New.
            </EmptyState>
          )}
        </div>
      ) : (
        folderGroups(lib, list).map((g) => (
          <div key={g.label || 'all'} data-group={g.quiet ? '*' : g.label || undefined} className={g.label ? styles.group : undefined}>
            {g.label && <GroupHeading label={g.label} tone={g.quiet ? 'quiet' : 'default'} />}
            {lib.view === 'grid' ? (
              <div className={styles.folderGrid} style={{ '--cols': 4 } as CSSProperties}>
                {g.folders.map((f) => <FolderItem key={f.id} lib={lib} folder={f} width="100%" query={q} />)}
              </div>
            ) : (
              <div className={styles.rows}>
                {g.folders.map((f) => <FolderRowItem key={f.id} lib={lib} folder={f} query={q} />)}
              </div>
            )}
          </div>
        ))
      )}
      {!q && <DeletedLink count={lib.bin.length} onOpen={onDeleted} />}
    </section>
  );
}
