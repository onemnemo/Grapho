import type { HTMLAttributes, ReactNode } from 'react';
import { cx } from '@/lib/cx';
import styles from './Surface.module.css';

/** Storybook helper: the app surface (s1) with padding, so tiles are seen on what they sit on. */
export function Surface({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cx(styles.surface, className)} {...rest} />;
}

/** Storybook helper: a wrapping row of captioned examples. */
export function Cells({ items, width }: { items: Array<{ caption: string; node: ReactNode }>; width?: number }) {
  return (
    <Surface>
      <div className={styles.row}>
        {items.map((i) => (
          <div key={i.caption} className={styles.cell} style={width ? { width } : undefined}>
            {i.node}
            <span>{i.caption}</span>
          </div>
        ))}
      </div>
    </Surface>
  );
}
