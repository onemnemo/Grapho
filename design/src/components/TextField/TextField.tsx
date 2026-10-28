import { useId, type InputHTMLAttributes, type ReactNode } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './TextField.module.css';

export type TextFieldVariant = 'floating' | 'stacked' | 'inline';
export type TextFieldSize = 'sm' | 'md';

export interface TextFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  label: string;
  /** floating: the label sits inside the box and rises when there is text (sheets).
   *  stacked: a caption above a 44px field, with room for an icon and a trailing control.
   *  inline: a bare field with no visible label, for renaming inside a panel; the label becomes its aria-label. */
  variant?: TextFieldVariant;
  hint?: string;
  /** Error text. Turns the outline red and replaces the hint. */
  error?: string;
  /** Shows a green tick on the right (floating only). */
  ok?: boolean;
  /** Inline only. md is a 40px field on a panel; sm is a 36px field recessed into a dark well. */
  size?: TextFieldSize;
  /** Stacked only. */
  icon?: IconName;
  trailing?: ReactNode;
}

export function TextField({
  label,
  variant = 'floating',
  size = 'md',
  hint,
  error,
  ok = false,
  icon,
  trailing,
  className,
  id,
  disabled,
  placeholder,
  ...rest
}: TextFieldProps) {
  const auto = useId();
  const inputId = id ?? auto;
  const helpId = `${inputId}-help`;
  const help = error ?? hint;
  const common = { id: inputId, disabled, 'aria-invalid': error ? true : undefined, 'aria-describedby': help ? helpId : undefined } as const;

  if (variant === 'inline') {
    return <input {...common} aria-label={label} data-size={size} className={cx(styles.inline, className)} placeholder={placeholder} {...rest} />;
  }

  if (variant === 'stacked') {
    return (
      <div className={cx(styles.stack, error && styles.err, disabled && styles.dis, className)}>
        <label className={styles.cap} htmlFor={inputId}>{label}</label>
        <div className={styles.line}>
          {icon && <Icon name={icon} size={18} />}
          <input {...common} placeholder={placeholder} {...rest} />
          {trailing && <span className={styles.trail}>{trailing}</span>}
        </div>
        <p className={styles.help} id={helpId}>{help}</p>
      </div>
    );
  }

  return (
    <div className={cx(styles.field, error && styles.err, ok && !error && styles.ok, disabled && styles.dis, className)}>
      <div className={styles.box}>
        {/* the single space placeholder lets CSS tell an empty field from a filled one */}
        <input {...common} placeholder=" " {...rest} />
        <label htmlFor={inputId}>{label}</label>
        <span className={styles.st} aria-hidden="true">
          <Icon name={error ? 'close' : 'check'} size={14} strokeWidth={2.6} />
        </span>
      </div>
      <p className={styles.help} id={helpId}>{help}</p>
    </div>
  );
}
