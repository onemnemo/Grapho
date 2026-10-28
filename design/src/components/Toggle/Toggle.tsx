import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { Icon } from '@/assets/icons';
import { cx } from '@/lib/cx';
import { useControllable } from '@/lib/useControllable';
import styles from './Toggle.module.css';

export interface ToggleProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'onChange' | 'children'> {
  checked?: boolean;
  defaultChecked?: boolean;
  onChange?: (checked: boolean) => void;
  /** plain is the 46 x 28 switch; marked adds a check on the knob (Settings). */
  variant?: 'plain' | 'marked';
  /** sm is the 40 x 24 switch of dense rows such as the studio inspector. */
  size?: 'md' | 'sm';
  /** Visible text beside the switch. Without it, pass aria-label. */
  label?: ReactNode;
  hint?: ReactNode;
}

export function Toggle({
  checked,
  defaultChecked = false,
  onChange,
  variant = 'plain',
  size = 'md',
  label,
  hint,
  className,
  disabled,
  onClick,
  type = 'button',
  ...rest
}: ToggleProps) {
  const [on, setOn] = useControllable(checked, defaultChecked, onChange);
  const sw = (
    <button
      type={type}
      role="switch"
      aria-checked={on}
      disabled={disabled}
      className={cx(styles.toggle, variant === 'marked' && styles.marked, size === 'sm' && styles.sm, !label && className)}
      onClick={(e) => {
        onClick?.(e);
        if (!e.defaultPrevented) setOn(!on);
      }}
      {...rest}
    >
      {variant === 'marked' && <Icon name="check" size={14} strokeWidth={3} />}
    </button>
  );
  if (!label) return sw;
  return (
    <label className={cx(styles.opt, disabled && styles.disabled, className)}>
      <span className={styles.text}>
        {label}
        {hint && <small>{hint}</small>}
      </span>
      {sw}
    </label>
  );
}
