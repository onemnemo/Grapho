import type { ButtonHTMLAttributes, HTMLAttributes, ReactNode } from 'react';
import { Icon } from '@/assets/icons';
import { cx } from '@/lib/cx';
import { useControllable } from '@/lib/useControllable';
import { useT } from '@/i18n';
import styles from './Chip.module.css';

export type ChipSize = 'sm' | 'md';

export interface ChipProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'onChange'> {
  /** Filter state. The chip is a toggle button: on holds a soft white fill and a tick. */
  selected?: boolean;
  defaultSelected?: boolean;
  onChange?: (selected: boolean) => void;
  /** Result count after the label, e.g. type filters with how many items match. */
  count?: number;
  size?: ChipSize;
  children: ReactNode;
}

/** Filter chip: Notebooks 12, Canvases 4. */
export function Chip({ selected, defaultSelected = false, onChange, count, size = 'md', className, children, onClick, type = 'button', ...rest }: ChipProps) {
  const [on, setOn] = useControllable(selected, defaultSelected, onChange);
  return (
    <button
      type={type}
      aria-pressed={on}
      className={cx(styles.chip, size === 'sm' && styles.sm, className)}
      onClick={(e) => {
        onClick?.(e);
        if (!e.defaultPrevented) setOn(!on);
      }}
      {...rest}
    >
      <span className={styles.tick} aria-hidden="true">
        <Icon name="check" size={16} strokeWidth={2.4} />
      </span>
      {children}
      {count !== undefined && <em className={styles.count}>{count}</em>}
    </button>
  );
}

export interface RemovableChipProps extends Omit<HTMLAttributes<HTMLSpanElement>, 'children'> {
  onRemove: () => void;
  /** Small colour dot before the label, e.g. a tag colour. */
  dot?: string;
  /** Accessible name of the remove button, e.g. "Remove maths". */
  removeLabel?: string;
  children: ReactNode;
}

/** A chosen value you can take away again, such as a selected tag. */
export function RemovableChip({ onRemove, removeLabel, dot, className, children, ...rest }: RemovableChipProps) {
  const t = useT();
  return (
    <span className={cx(styles.token, className)} {...rest}>
      {dot && <i className={styles.dot} style={{ background: dot }} aria-hidden="true" />}
      {children}
      <button type="button" className={styles.x} aria-label={removeLabel ?? t('common.remove')} onClick={onRemove}>
        <Icon name="close" size={14} strokeWidth={2.2} />
      </button>
    </span>
  );
}
