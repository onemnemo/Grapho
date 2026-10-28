import type { ButtonHTMLAttributes } from 'react';
import { Icon, type IconName, type IconSize } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './IconButton.module.css';

export type IconButtonVariant = 'quiet' | 'solid' | 'ghost';
export type IconButtonSize = 'sm' | 'md' | 'lg' | 'bar';

export interface IconButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  icon: IconName;
  /** Icon-only controls always need a name for assistive tech. */
  label: string;
  /** quiet: the bar button, a soft disc appears on hover. solid: a resting s2 disc, used in headers.
   *  ghost: no disc until hover, the full hit area. */
  variant?: IconButtonVariant;
  size?: IconButtonSize;
  /** Selected tool or toggled state: white disc, dark icon, and the icon fills softly. */
  active?: boolean;
  /** Small orange dot for "something changed here". */
  dot?: boolean;
  iconSize?: IconSize;
}

const iconFor: Record<IconButtonSize, IconSize> = { sm: 18, md: 20, lg: 20, bar: 20 };

export function IconButton({
  icon,
  label,
  variant = 'quiet',
  size = 'md',
  active = false,
  dot = false,
  iconSize,
  className,
  type = 'button',
  ...rest
}: IconButtonProps) {
  return (
    <button
      type={type}
      aria-label={label}
      aria-pressed={active || undefined}
      data-active={active || undefined}
      className={cx(styles.btn, styles[variant], styles[size], className)}
      {...rest}
    >
      <Icon name={icon} size={iconSize ?? iconFor[size]} active={active} />
      {dot && <span className={styles.dot} aria-hidden="true" />}
    </button>
  );
}
