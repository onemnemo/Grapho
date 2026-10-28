import type { HTMLAttributes, ReactNode } from 'react';
import { MascotSprite, type AnimName } from '@/assets/mascot';
import type { IconName } from '@/assets/icons';
import { cx } from '@/lib/cx';
import { Button } from '../Button';
import styles from './EmptyState.module.css';

export interface EmptyAction {
  label: string;
  onClick?: () => void;
  icon?: IconName;
}

export interface EmptyStateProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  title: ReactNode;
  /** One calm sentence. */
  children?: ReactNode;
  /** The mascot's tiny lowercase speech bubble. Leave out for none. */
  bubble?: string;
  anim?: AnimName;
  seed?: number;
  /** md fills a page; sm sits in a card or a panel. */
  size?: 'md' | 'sm';
  /** The main action, shown as the solid button. */
  primary?: EmptyAction;
  /** A quieter second action. */
  secondary?: EmptyAction;
}

/** The mascot with its bubble, a title, a sentence and up to two buttons. */
export function EmptyState({ title, children, bubble, anim = 'idle', seed = 3, size = 'md', primary, secondary, className, ...rest }: EmptyStateProps) {
  return (
    <div className={cx(styles.empty, size === 'sm' && styles.sm, className)} {...rest}>
      {bubble && <div className={styles.bubble}>{bubble.toLowerCase()}</div>}
      <MascotSprite anim={anim} scale={size === 'sm' ? 5 : 6} seed={seed} />
      <h3 className={styles.title}>{title}</h3>
      {children && <p className={styles.text}>{children}</p>}
      {(primary || secondary) && (
        <div className={styles.actions}>
          {primary && <Button variant="primary" icon={primary.icon} onClick={primary.onClick}>{primary.label}</Button>}
          {secondary && <Button onClick={secondary.onClick}>{secondary.label}</Button>}
        </div>
      )}
    </div>
  );
}
