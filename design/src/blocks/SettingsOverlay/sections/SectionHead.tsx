import type { ReactNode } from 'react';
import styles from './sections.module.css';

/** A small heading above a group of settings, with an optional action at the far end. */
export function SectionHead({ children, action }: { children: ReactNode; action?: ReactNode }) {
  return (
    <div className={styles.sh}>
      <h3>{children}</h3>
      {action}
    </div>
  );
}
