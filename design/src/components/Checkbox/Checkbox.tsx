import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { cx } from '@/lib/cx';
import { useControllable } from '@/lib/useControllable';
import styles from './Checkbox.module.css';

export type CheckState = boolean | 'mixed';

export interface CheckboxProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'onChange' | 'children' | 'defaultChecked'> {
  /** true, false, or 'mixed' for a parent whose children are partly selected. */
  checked?: CheckState;
  defaultChecked?: CheckState;
  onChange?: (checked: boolean) => void;
  label?: ReactNode;
  hint?: ReactNode;
}

export function Checkbox({
  checked,
  defaultChecked = false,
  onChange,
  label,
  hint,
  className,
  disabled,
  onClick,
  type = 'button',
  ...rest
}: CheckboxProps) {
  const [state, setState] = useControllable<CheckState>(checked, defaultChecked, (v) => onChange?.(v === true));
  const box = (
    <button
      type={type}
      role="checkbox"
      aria-checked={state}
      disabled={disabled}
      className={cx(styles.check, !label && className)}
      onClick={(e) => {
        onClick?.(e);
        if (!e.defaultPrevented) setState(state !== true);
      }}
      {...rest}
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={3} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path className={styles.ck} d="M5 12.6l4.6 4.6L19 7.6" />
        <path className={styles.mx} d="M6 12h12" />
      </svg>
    </button>
  );
  if (!label) return box;
  return (
    <label className={cx(styles.opt, disabled && styles.disabled, className)}>
      {box}
      <span className={styles.text}>
        {label}
        {hint && <small>{hint}</small>}
      </span>
    </label>
  );
}
