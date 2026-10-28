import type { HTMLAttributes } from 'react';
import { Icon } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './SelectionBadge.module.css';

export interface SelectionBadgeProps extends HTMLAttributes<HTMLSpanElement> {
  /** Selection mode is on, so the empty ring shows. Off, the badge is hidden. */
  visible?: boolean;
  /** This item is picked: the ring fills white with a tick. */
  selected?: boolean;
  size?: 'md' | 'sm';
}

/** The round tick on a cover, folder or row while you are choosing. The parent places it. */
export function SelectionBadge({ visible = true, selected = false, size = 'md', className, ...rest }: SelectionBadgeProps) {
  return (
    <span
      className={cx(styles.badge, size === 'sm' && styles.sm, className)}
      data-visible={visible || selected}
      data-selected={selected}
      aria-hidden="true"
      {...rest}
    >
      <Icon name="check" size={16} strokeWidth={2.6} />
    </span>
  );
}
