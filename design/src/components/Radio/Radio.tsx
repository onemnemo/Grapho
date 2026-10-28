import type { ButtonHTMLAttributes, KeyboardEvent, ReactNode } from 'react';
import { useRef } from 'react';
import { cx } from '@/lib/cx';
import { useControllable } from '@/lib/useControllable';
import styles from './Radio.module.css';

export interface RadioProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  checked?: boolean;
  label?: ReactNode;
  hint?: ReactNode;
}

/** One radio. Use RadioGroup for a set: it owns selection and arrow keys. */
export function Radio({ checked = false, label, hint, className, disabled, type = 'button', ...rest }: RadioProps) {
  const dot = (
    <button
      type={type}
      role="radio"
      aria-checked={checked}
      tabIndex={checked ? 0 : -1}
      disabled={disabled}
      className={cx(styles.radio, !label && className)}
      {...rest}
    />
  );
  if (!label) return dot;
  return (
    <label className={cx(styles.opt, disabled && styles.disabled, className)}>
      {dot}
      <span className={styles.text}>
        {label}
        {hint && <small>{hint}</small>}
      </span>
    </label>
  );
}

export interface RadioOption {
  value: string;
  label: ReactNode;
  hint?: ReactNode;
  disabled?: boolean;
}

export interface RadioGroupProps {
  options: RadioOption[];
  value?: string;
  defaultValue?: string;
  onChange?: (value: string) => void;
  className?: string;
  'aria-label': string;
}

export function RadioGroup({ options, value, defaultValue, onChange, className, ...rest }: RadioGroupProps) {
  const [current, setCurrent] = useControllable(value, defaultValue ?? '', onChange);
  const root = useRef<HTMLDivElement>(null);
  const hasCurrent = options.some((o) => o.value === current);
  const firstEnabled = options.find((o) => !o.disabled);
  const move = (e: KeyboardEvent<HTMLDivElement>) => {
    const d = ({ ArrowDown: 1, ArrowRight: 1, ArrowUp: -1, ArrowLeft: -1 } as Record<string, number>)[e.key];
    if (!d) return;
    const enabled = options.filter((o) => !o.disabled);
    const i = enabled.findIndex((o) => o.value === current);
    const next = enabled[(i + d + enabled.length) % enabled.length];
    if (!next) return;
    e.preventDefault();
    setCurrent(next.value);
    root.current?.querySelector<HTMLElement>(`[data-value="${CSS.escape(next.value)}"]`)?.focus();
  };
  return (
    <div ref={root} role="radiogroup" className={cx(styles.group, className)} onKeyDown={move} {...rest}>
      {options.map((o) => {
        const tabbable = o.value === current || (!hasCurrent && o === firstEnabled);
        return (
          <Radio
            key={o.value}
            data-value={o.value}
            checked={o.value === current}
            tabIndex={tabbable ? 0 : -1}
            label={o.label}
            hint={o.hint}
            disabled={o.disabled}
            onClick={() => setCurrent(o.value)}
          />
        );
      })}
    </div>
  );
}
