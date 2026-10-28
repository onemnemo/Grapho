import type { HTMLAttributes } from 'react';
import { cx } from '@/lib/cx';
import type { BarPosition } from '../NoteBar';
import styles from './NoteBarHandle.module.css';

export interface NoteBarHandleProps extends HTMLAttributes<HTMLElement> {
  position?: BarPosition;
  /** Visible for a moment after the bar leaves so the way back can be learned; invisible otherwise. */
  visible?: boolean;
}

/** The faint way back: a small bar at the edge where the note bar went. */
export function NoteBarHandle({ position = 'top', visible = false, className, ...rest }: NoteBarHandleProps) {
  return <i aria-hidden="true" data-pos={position} data-on={visible || undefined} className={cx(styles.handle, className)} {...rest} />;
}
