import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './ActionRow.module.css';

export interface ActionRowProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children' | 'value'> {
  icon: IconName;
  children: ReactNode;
  /** A quiet value at the end, such as the time of the last backup. */
  value?: ReactNode;
  tone?: 'default' | 'danger';
}

/** Something you do: an icon and a label on a plain row. Options are tiles or segments, so the two never look alike. */
export function ActionRow({ icon, children, value, tone = 'default', className, type = 'button', ...rest }: ActionRowProps) {
  return (
    <button type={type} className={cx(styles.row, tone === 'danger' && styles.danger, className)} {...rest}>
      <Icon name={icon} size={20} />
      <span className={styles.label}>{children}</span>
      {value != null && <span className={styles.value}>{value}</span>}
    </button>
  );
}
