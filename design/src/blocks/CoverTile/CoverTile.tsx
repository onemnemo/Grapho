import { useRef, type HTMLAttributes, type KeyboardEvent, type PointerEvent } from 'react';
import { Icon } from '@/assets/icons';
import { CoverArt, DocArt, CanvasArt, type CoverPreset, type DocKind } from '@/assets/covers';
import { Highlight } from '@/components/Highlight';
import { cx } from '@/lib/cx';
import { FolderMoreButton, type FolderAction, type FolderTarget } from '../FolderMenu';
import { SelectionBadge } from '../SelectionBadge';
import styles from './CoverTile.module.css';

export type CoverTileKind = 'notebook' | 'pdf' | 'canvas';
export type CoverTileState = 'rest' | 'hover' | 'press' | 'hold';

export interface CoverTileProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  kind?: CoverTileKind;
  /** Notebook cover preset. */
  cover?: CoverPreset;
  /** A cover colour picked in the notebook sheet, over the preset's. */
  color?: string;
  /** PDF page look. */
  doc?: DocKind;
  title: string;
  /** The quiet line under the title: Notebook, 24 pages. */
  meta?: string;
  /** Replaces the meta line with a warning, such as "3 days left". */
  warning?: string;
  starred?: boolean;
  /** Selection mode is on: the badge shows and unpicked tiles step back. */
  selecting?: boolean;
  selected?: boolean;
  /** Being dragged: the tile left behind fades. */
  lifted?: boolean;
  /** Pins a pose for stories. Real hover, press and long press work without it. */
  state?: CoverTileState;
  /** Highlight the first match of a search in the title. */
  query?: string;
  /** The ••• menu. Shows when this is given. */
  onMenuAction?: (action: FolderAction) => void;
  moveTargets?: FolderTarget[];
  onMove?: (folderId: string) => void;
}

/** A notebook, PDF or canvas in the library grid. */
export function CoverTile({
  kind = 'notebook',
  cover = 'plain',
  color,
  doc = 'plain',
  title,
  meta,
  warning,
  starred = false,
  selecting = false,
  selected = false,
  lifted = false,
  state = 'rest',
  query,
  onMenuAction,
  moveTargets,
  onMove,
  className,
  onKeyDown,
  onClick,
  ...rest
}: CoverTileProps) {
  const art = useRef<HTMLDivElement>(null);
  const tilt = (e: PointerEvent<HTMLDivElement>) => {
    const el = art.current;
    if (!el || e.pointerType !== 'mouse') return;
    const r = el.getBoundingClientRect();
    const x = (e.clientX - r.left) / r.width;
    const y = (e.clientY - r.top) / r.height;
    el.style.setProperty('--ry', `${((x - 0.5) * 9).toFixed(2)}deg`);
    el.style.setProperty('--rx', `${((0.5 - y) * 9).toFixed(2)}deg`);
    el.style.setProperty('--mx', `${x * 100}%`);
    el.style.setProperty('--my', `${y * 100}%`);
  };
  const level = () => {
    art.current?.style.setProperty('--rx', '0deg');
    art.current?.style.setProperty('--ry', '0deg');
  };
  const key = (e: KeyboardEvent<HTMLDivElement>) => {
    onKeyDown?.(e);
    if (!e.defaultPrevented && (e.key === 'Enter' || e.key === ' ')) {
      e.preventDefault();
      e.currentTarget.click();
    }
  };
  return (
    <div
      role="button"
      tabIndex={0}
      aria-pressed={selecting ? selected : undefined}
      className={cx(styles.tile, className)}
      data-state={state}
      data-selected={selected}
      data-dim={selecting && !selected}
      data-lifted={lifted}
      onPointerMove={tilt}
      onPointerLeave={level}
      onKeyDown={key}
      onClick={onClick}
      {...rest}
    >
      <div ref={art} className={styles.art}>
        {kind === 'notebook' && <CoverArt preset={cover} color={color} label={title} />}
        {kind === 'pdf' && <DocArt kind={doc} />}
        {kind === 'canvas' && <CanvasArt />}
        <SelectionBadge visible={selecting} selected={selected} />
        {starred && <span className={styles.star}><Icon name="star" size={16} active /></span>}
        {onMenuAction && !selecting && (
          <FolderMoreButton className={styles.more} kind={kind} starred={starred} show={state === 'hover'} small onAction={onMenuAction} targets={moveTargets} onMove={onMove} />
        )}
      </div>
      <div className={styles.name}><Highlight text={title} query={query} /></div>
      {warning ? <div className={cx(styles.meta, styles.warn)}>{warning}</div> : <div className={styles.meta}>{meta ?? ''}</div>}
    </div>
  );
}
