import type { HTMLAttributes, ReactNode } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { CloseButton } from '@/components/CloseButton';
import { cx } from '@/lib/cx';
import { Button } from '../Button';
import styles from './Banner.module.css';

export type BannerTone = 'neutral' | 'ok' | 'warn' | 'danger';

export interface BannerProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  title: ReactNode;
  /** One short sentence that says what to do next. */
  children?: ReactNode;
  icon?: IconName;
  tone?: BannerTone;
  /** Label of the one action. */
  actionLabel?: string;
  onAction?: () => void;
  /** Shows the close button. */
  onClose?: () => void;
  closeLabel?: string;
}

/** An inline notice that sits in the page flow, above the thing it is about. Never a corner card. */
export function Banner({
  title,
  children,
  icon = 'info',
  tone = 'neutral',
  actionLabel,
  onAction,
  onClose,
  closeLabel = 'Dismiss',
  className,
  ...rest
}: BannerProps) {
  return (
    <div
      role={tone === 'danger' ? 'alert' : 'status'}
      className={cx(styles.banner, tone !== 'neutral' && styles[tone], className)}
      {...rest}
    >
      <span className={styles.icon}><Icon name={icon} size={18} /></span>
      <div className={styles.text}>
        <b className={styles.title}>{title}</b>
        {children && <span className={styles.body}>{children}</span>}
      </div>
      {actionLabel && <Button onClick={onAction}>{actionLabel}</Button>}
      {onClose && <CloseButton onClick={onClose} aria-label={closeLabel} />}
    </div>
  );
}
