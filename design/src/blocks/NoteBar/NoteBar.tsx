import type { HTMLAttributes, MouseEvent, ReactNode } from 'react';
import { Button } from '@/components/Button';
import { IconButton } from '@/components/IconButton';
import { Tooltip } from '@/components/Tooltip';
import type { IconName } from '@/assets/icons';
import { cx } from '@/lib/cx';
import { tipSide, type BarMode, type BarPosition, type BarVariant, type NoteToolItem } from './noteTools';
import styles from './NoteBar.module.css';

export interface NoteBarProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  /** pill floats and hugs its tools; line is the flat full bar along the edge. */
  variant?: BarVariant;
  position?: BarPosition;
  /** edit: the tools. read: back, title and Edit. */
  mode?: BarMode;
  tools?: NoteToolItem[];
  activeTool?: string;
  title?: string;
  subtitle?: string;
  /** Layers sits before Undo on a horizontal bar. A vertical bar folds it into the menu. */
  showLayers?: boolean;
  /** Auto-hide: the bar has left toward its edge. */
  hidden?: boolean;
  onTool?: (id: string) => void;
  onAction?: (id: string, event: MouseEvent<HTMLButtonElement>) => void;
  onBack?: () => void;
  onLayers?: () => void;
  /** The layers popover is open, so the button shows as on. */
  layersOpen?: boolean;
  onUndo?: () => void;
  onRedo?: () => void;
  onMore?: (event: MouseEvent<HTMLButtonElement>) => void;
  onEdit?: () => void;
  /** Read mode: the page count on the right. */
  readCount?: ReactNode;
}

/** The note screen's bar. One component in two styles and four positions, with an edit and a read mode. */
export function NoteBar({
  variant = 'pill',
  position = 'top',
  mode = 'edit',
  tools = [],
  activeTool,
  title,
  subtitle,
  showLayers = true,
  hidden = false,
  onTool,
  onAction,
  onBack,
  onLayers,
  layersOpen,
  onUndo,
  onRedo,
  onMore,
  onEdit,
  readCount,
  className,
  ...rest
}: NoteBarProps) {
  const vertical = position === 'left' || position === 'right';
  const side = tipSide(position);
  const btn = (icon: IconName, label: string, props: { active?: boolean; onClick?: (e: MouseEvent<HTMLButtonElement>) => void; 'data-tool'?: string } = {}) => (
    <Tooltip content={label} side={side} key={label}>
      <IconButton icon={icon} label={label} size="sm" iconSize={18} {...props} />
    </Tooltip>
  );

  let left: ReactNode;
  let centre: ReactNode = null;
  let right: ReactNode;
  if (mode === 'read') {
    left = (
      <>
        {btn('back', 'Back', { onClick: onBack })}
        <span className={styles.title}>
          {title}
          {subtitle && <small>{subtitle}</small>}
        </span>
      </>
    );
    right = (
      <>
        {readCount != null && <span className={styles.count}>{readCount}</span>}
        <Button variant="primary" size="sm" icon="edit" onClick={onEdit}>Edit</Button>
      </>
    );
  } else {
    left = (
      <>
        {btn('back', 'Back', { onClick: onBack })}
        {title && !vertical && (
          <span className={styles.title}>
            {title}
            {subtitle && <small>{subtitle}</small>}
          </span>
        )}
      </>
    );
    centre = tools.map((t, i) =>
      t === 'separator' ? (
        <i key={`s${i}`} className={styles.sep} />
      ) : (
        btn(t.icon, t.label, { active: t.id === activeTool, onClick: () => onTool?.(t.id), 'data-tool': t.id })
      ),
    );
    right = (
      <>
        {showLayers && !vertical && btn('layers', 'Layers', { onClick: onLayers, active: layersOpen })}
        {btn('undo', 'Undo', { onClick: onUndo })}
        {btn('redo', 'Redo', { onClick: onRedo })}
        {btn('more', 'More', { onClick: onMore })}
      </>
    );
  }

  return (
    <div
      role="toolbar"
      aria-label="Note toolbar"
      data-variant={variant}
      data-pos={position}
      data-hidden={hidden || undefined}
      inert={hidden || undefined}
      className={cx(styles.bar, className)}
      {...rest}
    >
      <div className={cx(styles.grp, styles.l)}>{left}</div>
      {centre && <div className={styles.grp}>{centre}</div>}
      <div className={cx(styles.grp, styles.r)}>{right}</div>
    </div>
  );
}
