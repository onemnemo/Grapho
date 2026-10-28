import type { CSSProperties, HTMLAttributes, ReactNode } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { cx } from '@/lib/cx';
import { Button } from '../Button';
import styles from './Toast.module.css';

export type ToastKind = 'info' | 'ok' | 'error';
export type ToastVariant = 'card' | 'pill';

export interface ToastProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children' | 'title'> {
  /** The one line that says what happened. */
  children: ReactNode;
  /** A quieter second line. */
  detail?: ReactNode;
  kind?: ToastKind;
  /** card has an icon tile and a button; pill is one line with a quiet Undo and a small ring, for a canvas. */
  variant?: ToastVariant;
  /** Glyph in the tile. Defaults to the kind's own. */
  icon?: IconName;
  /** Shows the Undo button and the draining ring around the tile. */
  onUndo?: () => void;
  undoLabel?: string;
  /** How long the ring takes to drain, in ms. Defaults to the --d-toast token. */
  duration?: number;
  /** Pause the ring, for hover and focus. */
  paused?: boolean;
  leaving?: boolean;
}

const KIND_ICON: Record<ToastKind, IconName> = { info: 'info', ok: 'check', error: 'alert' };

/** A dark card at the bottom centre. With Undo, a ring drains around its tile for the length of --d-toast. */
export function Toast({
  children,
  detail,
  kind = 'info',
  variant = 'card',
  icon,
  onUndo,
  undoLabel = 'Undo',
  duration,
  paused = false,
  leaving = false,
  className,
  style,
  ...rest
}: ToastProps) {
  const dur = duration === undefined ? undefined : { '--dur': `${duration}ms` };
  if (variant === 'pill') {
    return (
      <div
        className={cx(styles.toast, styles.pill, className)}
        data-state={leaving ? 'leave' : 'open'}
        data-paused={paused}
        style={{ ...dur, ...style } as CSSProperties}
        {...rest}
      >
        <span>{children}</span>
        {onUndo && <Button variant="text" size="sm" onClick={onUndo}>{undoLabel}</Button>}
        {onUndo && (
          <svg className={styles.pillRing} width="22" height="22" viewBox="0 0 22 22" aria-hidden="true">
            <circle className={styles.pillTrack} cx="11" cy="11" r="8.5" />
            <circle className={styles.pillDrain} cx="11" cy="11" r="8.5" transform="rotate(-90 11 11)" />
          </svg>
        )}
      </div>
    );
  }
  return (
    <div
      className={cx(styles.toast, kind !== 'info' && styles[kind], className)}
      data-state={leaving ? 'leave' : 'open'}
      data-paused={paused}
      style={{ ...dur, ...style } as CSSProperties}
      {...rest}
    >
      <span className={styles.tile}>
        <Icon name={icon ?? KIND_ICON[kind]} size={18} />
        {onUndo && (
          <svg className={styles.ring} viewBox="0 0 42 42" aria-hidden>
            <circle className={styles.track} cx="21" cy="21" r="19" />
            <circle className={styles.drain} cx="21" cy="21" r="19" />
          </svg>
        )}
      </span>
      <span className={styles.text}>
        <span className={styles.title}>{children}</span>
        {detail && <span className={styles.detail}>{detail}</span>}
      </span>
      {onUndo && <Button size="sm" onClick={onUndo}>{undoLabel}</Button>}
    </div>
  );
}
