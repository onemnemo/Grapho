import type { HTMLAttributes, KeyboardEvent, ReactNode } from 'react';
import { Icon } from '@/assets/icons';
import { CoverArt, DocArt, CanvasArt, type CoverPreset, type DocKind, type FolderColour, type FolderPaper } from '@/assets/covers';
import { Highlight } from '@/components/Highlight';
import { cx } from '@/lib/cx';
import { FolderMoreButton, type FolderAction, type FolderTarget } from '../FolderMenu';
import { SelectionBadge } from '../SelectionBadge';
import type { CoverTileKind } from '../CoverTile';
import styles from './LibraryRow.module.css';

export interface LibraryRowProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  kind: CoverTileKind | 'folder';
  title: string;
  /** Under the title: where it lives, such as Library / Semester 1. */
  path?: ReactNode;
  cover?: CoverPreset;
  color?: string;
  doc?: DocKind;
  colour?: FolderColour;
  /** Folder rows ignore this; present so a folder and an item can share one call. */
  papers?: FolderPaper[];
  /** Notebook, PDF, Canvas, or "12 items" for a folder. */
  typeLabel: string;
  /** 32 pages, Infinite, or "2 folders". */
  size?: string;
  /** Last edited: 2 h ago, Yesterday, 12 Mar. */
  when?: string;
  selecting?: boolean;
  selected?: boolean;
  lifted?: boolean;
  /** Only the thumbnail and title, for search results and pickers. */
  compact?: boolean;
  /** Compact rows only: something on the right, such as the page a match is on. */
  aside?: ReactNode;
  state?: 'rest' | 'hover';
  query?: string;
  pinned?: boolean;
  starred?: boolean;
  onMenuAction?: (action: FolderAction) => void;
  moveTargets?: FolderTarget[];
  onMove?: (folderId: string) => void;
}

const TYPE_ICON = { notebook: 'notebook', pdf: 'pdf', canvas: 'canvas' } as const;

/** One item or folder in list view. */
export function LibraryRow({
  kind, title, path, cover = 'plain', color, doc = 'plain', colour = 'indigo', typeLabel, size, when,
  selecting = false, selected = false, lifted = false, compact = false, aside, state = 'rest', query, pinned = false, starred = false,
  onMenuAction, moveTargets, onMove, className, onKeyDown, ...rest
}: LibraryRowProps) {
  const key = (e: KeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(e);
    if (!e.defaultPrevented && e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) {
      e.preventDefault();
      e.currentTarget.click();
    }
  };
  const isFolder = kind === 'folder';
  return (
    <div
      role="button"
      tabIndex={0}
      aria-pressed={selecting ? selected : undefined}
      className={cx(styles.row, isFolder && styles.folder, compact && styles.compact, compact && aside != null && styles.aside, isFolder && styles[colour], className)}
      data-state={state}
      data-selecting={selecting}
      data-selected={selected}
      data-lifted={lifted}
      onKeyDown={key}
      {...rest}
    >
      <SelectionBadge size="sm" visible={selecting} selected={selected} />
      {isFolder ? (
        <div className={styles.tab} />
      ) : (
        <div className={styles.thumb}>
          {kind === 'notebook' && <CoverArt preset={cover} color={color} />}
          {kind === 'pdf' && <DocArt kind={doc} />}
          {kind === 'canvas' && <CanvasArt />}
        </div>
      )}
      <div className={styles.text}>
        <span className={styles.name}><Highlight text={title} query={query} /></span>
        {(path || pinned) && <small className={styles.sub}>{path}{pinned ? `${path ? ' · ' : ''}Pinned` : ''}</small>}
      </div>
      {compact && aside != null && <div className={styles.cell}>{aside}</div>}
      {!compact && (
        <>
          <div className={styles.type}>{!isFolder && <Icon name={TYPE_ICON[kind]} size={16} />}{typeLabel}</div>
          <div className={styles.cell}>{size}</div>
          <div className={styles.cell}>{when}</div>
          {onMenuAction && (
            <FolderMoreButton variant="row" kind={kind} pinned={pinned} starred={starred} onAction={onMenuAction} targets={moveTargets} onMove={onMove} />
          )}
        </>
      )}
    </div>
  );
}
