import type { SelectHTMLAttributes } from 'react';
import { Icon } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './Select.module.css';

export interface SelectOption { value: string; label: string }

export interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'onChange' | 'size'> {
  options: SelectOption[];
  value: string;
  onChange: (value: string) => void;
  'aria-label': string;
}

/** A quiet native select: s2 field, one chevron. Used for short lists such as fonts and blend modes. */
export function Select({ options, value, onChange, className, ...rest }: SelectProps) {
  return (
    <label className={cx(styles.select, className)}>
      <select value={value} onChange={(e) => onChange(e.target.value)} {...rest}>
        {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
      </select>
      <Icon name="chevron_down" size={16} className={styles.chev} />
    </label>
  );
}
