import { EmptyState } from '@/components/EmptyState';
import type { AnimName } from '@/assets/mascot';
import { LibraryHeader } from '@/blocks/LibraryHeader';
import { AppFrame } from '@/components/AppFrame';
import styles from './LibraryEmpty.module.css';

export type LibraryEmptyKind = 'library' | 'folder' | 'folders' | 'search' | 'folderSearch' | 'filters' | 'deleted' | 'select';

interface Copy { anim: AnimName; bubble: string; title: string; text?: string; primary?: string; secondary?: string }

export const EMPTY_COPY: Record<LibraryEmptyKind, Copy> = {
  library: { anim: 'wave', bubble: 'start here', title: 'Your library is empty', primary: 'New notebook', secondary: 'Import PDF' },
  folder: { anim: 'sit', bubble: 'nothing yet', title: 'This folder is empty', primary: 'New notebook' },
  folders: { anim: 'wave', bubble: 'no folders yet', title: 'No folders yet', primary: 'New folder' },
  search: { anim: 'worry', bubble: 'no match', title: 'Nothing found', secondary: 'Clear search' },
  folderSearch: { anim: 'worry', bubble: 'no match', title: 'No folder with that name', secondary: 'Clear search' },
  filters: { anim: 'lookat', bubble: 'no match', title: 'Nothing matches these filters', secondary: 'Reset filters' },
  deleted: { anim: 'sleep', bubble: 'all clear', title: 'Nothing deleted', text: 'Deleted notebooks and files stay here for 30 days.' },
  select: { anim: 'lookat', bubble: 'pick a few', title: 'Nothing selected', secondary: 'Select all' },
};

export interface LibraryEmptyProps {
  kind: LibraryEmptyKind;
  /** Show it inside the library header (true) or on its own (false). */
  withHeader?: boolean;
  onPrimary?: () => void;
  onSecondary?: () => void;
}

/** The empty states of the library: the mascot, one tiny line, a title, a sentence and at most two actions. */
export function LibraryEmpty({ kind, withHeader = true, onPrimary, onSecondary }: LibraryEmptyProps) {
  const c = EMPTY_COPY[kind];
  const inFolder = kind === 'folder';
  const state = (
    <EmptyState
      anim={c.anim}
      seed={kind.length * 7 + 3}
      bubble={c.bubble}
      title={c.title}
      primary={c.primary ? { label: c.primary, icon: kind === 'library' || kind === 'folder' || kind === 'folders' ? 'plus' : undefined, onClick: onPrimary } : undefined}
      secondary={c.secondary ? { label: c.secondary, onClick: onSecondary } : undefined}
    >
      {c.text}
    </EmptyState>
  );
  if (!withHeader) return <AppFrame><div className={styles.body}>{state}</div></AppFrame>;
  return (
    <AppFrame>
      <LibraryHeader
        title="Library"
        path={inFolder ? [{ label: 'Library', onClick: () => {} }] : kind === 'folders' || kind === 'folderSearch' ? [{ label: 'Library', onClick: () => {} }] : []}
        current={inFolder ? 'Semester 1' : kind === 'folders' || kind === 'folderSearch' ? 'All folders' : undefined}
        onBack={() => {}}
        onFilter={kind === 'library' || kind === 'filters' ? () => {} : undefined}
        filterActive={kind === 'filters'}
        onSettings={() => {}}
        onNew={() => {}}
        searching={kind === 'search' || kind === 'folderSearch'}
        query={kind === 'search' ? 'zzz' : kind === 'folderSearch' ? 'zzz' : ''}
        onSearchClose={() => {}}
      />
      <div className={styles.body}>{state}</div>
    </AppFrame>
  );
}
