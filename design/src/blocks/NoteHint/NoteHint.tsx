import type { HTMLAttributes } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './NoteHint.module.css';

export interface NoteHintProps extends HTMLAttributes<HTMLSpanElement> {
  /** A glyph that says what the hint is about. */
  icon?: IconName;
}

/** A quiet pill with a few words, such as what the bar is doing. Grey text on the overlay, no outline. Never takes pointer events. */
export function NoteHint({ icon, className, children, ...rest }: NoteHintProps) {
  return (
    <span role="status" className={cx(styles.hint, className)} {...rest}>
      {icon && <Icon name={icon} size={16} />}
      {children}
    </span>
  );
}
