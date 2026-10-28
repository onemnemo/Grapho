import type { HTMLAttributes, KeyboardEvent } from 'react';
import { FolderArt, type FolderColour, type FolderPaper } from '@/assets/covers';
import type { IconName } from '@/assets/icons';
import { Highlight } from '@/components/Highlight';
import { cx } from '@/lib/cx';
import { FolderMoreButton, type FolderAction, type FolderTarget } from '../FolderMenu';
import { SelectionBadge } from '../SelectionBadge';
import styles from './FolderTile.module.css';

export interface FolderTileProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  name: string;
  /** 12 items, 2 folders. */
  meta?: string;
  colour?: FolderColour;
  papers?: FolderPaper[];
  icon?: IconName;
  pinned?: boolean;
  /** Width in px or any CSS length. The folder keeps its ratio. */
  width?: number | string;
  /** Selection mode is on: the badge shows, the dots hide and unpicked folders step back. */
  selecting?: boolean;
  selected?: boolean;
  /** Something is being dragged over it. */
  dropTarget?: boolean;
  /** Being dragged itself: the one left behind fades. */
  lifted?: boolean;
  /** Pins the hover pose for stories. */
  state?: 'rest' | 'hover';
  /** Label above the folder while it is a drop target. */
  hint?: string;
  query?: string;
  /** Show the ••• menu button. */
  menu?: boolean;
  /** Keep the dots showing, as on a touch screen. */
  showMenuButton?: boolean;
  smallMenuButton?: boolean;
  onMenuAction?: (action: FolderAction) => void;
  moveTargets?: FolderTarget[];
  onMove?: (folderId: string) => void;
}

/** A folder in the library: lifts on hover, shrinks with a white ring when picked. */
export function FolderTile({
  name,
  meta,
  colour = 'indigo',
  papers,
  icon,
  pinned = false,
  width = 250,
  selecting = false,
  selected = false,
  dropTarget = false,
  lifted = false,
  state = 'rest',
  hint = 'Move here',
  query,
  menu = true,
  showMenuButton = false,
  smallMenuButton = false,
  onMenuAction,
  moveTargets,
  onMove,
  className,
  style,
  onKeyDown,
  ...rest
}: FolderTileProps) {
  const key = (e: KeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(e);
    if (!e.defaultPrevented && e.target === e.currentTarget && (e.key === 'Enter' || e.key === ' ')) {
      e.preventDefault();
      e.currentTarget.click();
    }
  };
  return (
    <div
      role="button"
      tabIndex={0}
      aria-label={`${name}${meta ? `, ${meta}` : ''}`}
      aria-pressed={selecting ? selected : undefined}
      className={cx(styles.tile, className)}
      style={{ ['--w' as string]: typeof width === 'number' ? `${width}px` : width, ...style }}
      data-state={state}
      data-selecting={selecting}
      data-selected={selected}
      data-dim={selecting && !selected}
      data-over={dropTarget}
      data-lifted={lifted}
      onKeyDown={key}
      {...rest}
    >
      <div className={styles.inner}>
        <FolderArt
          colour={colour}
          papers={papers}
          icon={icon}
          pinned={pinned}
          name={<Highlight text={name} query={query} />}
          meta={meta}
          interactive
          state={dropTarget ? 'over' : state === 'hover' ? 'hover' : 'rest'}
        >
          <SelectionBadge visible={selecting} selected={selected} />
          {menu && !selecting && (
            <FolderMoreButton
              pinned={pinned}
              show={showMenuButton || state === 'hover'}
              small={smallMenuButton}
              onAction={(a) => onMenuAction?.(a)}
              targets={moveTargets}
              onMove={onMove}
            />
          )}
        </FolderArt>
        <div className={styles.ring} />
      </div>
      <div className={styles.hint}>{hint}</div>
    </div>
  );
}
