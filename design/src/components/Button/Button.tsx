import type { ComponentPropsWithRef, ReactNode } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './Button.module.css';

export type ButtonVariant = 'primary' | 'secondary' | 'text' | 'danger';
export type ButtonSize = 'xs' | 'sm' | 'md' | 'lg' | 'bar';

export interface ButtonProps extends ComponentPropsWithRef<'button'> {
  /** primary is the one solid white action on a surface; secondary is the quiet default. */
  variant?: ButtonVariant;
  /** bar is the 48px button of the floating selection and note bars. */
  size?: ButtonSize;
  /** Leading icon. Use it when the action has an obvious glyph: create, import, customise. */
  icon?: IconName;
  block?: boolean;
  children: ReactNode;
}

export function Button({
  variant = 'secondary',
  size = 'md',
  icon,
  block = false,
  className,
  type = 'button',
  children,
  ...rest
}: ButtonProps) {
  const showIcon = !!icon;
  return (
    <button
      type={type}
      className={cx(styles.button, styles[variant], styles[size], block && styles.block, className)}
      {...rest}
    >
      {showIcon && <Icon name={icon} size={size === 'sm' || size === 'xs' ? 16 : size === 'bar' ? 20 : 18} />}
      {children}
    </button>
  );
}
