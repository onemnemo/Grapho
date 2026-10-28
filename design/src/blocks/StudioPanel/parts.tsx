import type { ReactNode } from 'react';
import { ActionRow } from '@/components/ActionRow';
import { cx } from '@/lib/cx';
import styles from './StudioPanel.module.css';

/** The actions at the foot of a pack list: get more, or bring one in from a file. */
export function PackActions({ onGet, onImport, getLabel = 'Discover more' }: { onGet?: () => void; onImport?: () => void; getLabel?: string }) {
  return (
    <div className={styles.actions}>
      <ActionRow icon="pack" onClick={onGet}>{getLabel}</ActionRow>
      {onImport && <ActionRow icon="import" onClick={onImport}>Import pack</ActionRow>}
    </div>
  );
}

export function Sub({ children }: { children: ReactNode }) { return <div className={styles.sub}>{children}</div>; }
export function Note({ children, className }: { children: ReactNode; className?: string }) { return <p className={cx(styles.note, className)}>{children}</p>; }
