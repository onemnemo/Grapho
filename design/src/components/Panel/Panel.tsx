import type { HTMLAttributes, ReactNode } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { IconButton } from '../IconButton';
import { cx } from '@/lib/cx';
import styles from './Panel.module.css';

export interface PanelProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  title: ReactNode;
  icon?: IconName;
  /** Shows a close button in the header. */
  onClose?: () => void;
  closeLabel?: string;
  /** floating has the 24 radius; docked has square corners and sits against the edge of a window. */
  shape?: 'floating' | 'docked';
  children: ReactNode;
}

/** A floating panel with a header and a scrolling body, for options that sit beside the page. */
export function Panel({ title, icon, onClose, closeLabel = 'Close', shape = 'floating', className, children, ...rest }: PanelProps) {
  return (
    <section className={cx(styles.panel, shape === 'docked' && styles.docked, className)} {...rest}>
      <header className={styles.head}>
        {icon && <Icon name={icon} size={20} />}
        <h3>{title}</h3>
        {onClose && <IconButton icon="close" label={closeLabel} size="sm" variant="ghost" onClick={onClose} />}
      </header>
      <div className={styles.body}>{children}</div>
    </section>
  );
}
