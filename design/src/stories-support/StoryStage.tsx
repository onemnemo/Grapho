import type { CSSProperties, ReactNode } from 'react';
import styles from './StoryStage.module.css';

/** Storybook helper: a positioned frame with a quiet library behind it, so overlays have something to sit on. */
export function StoryStage({ height = 640, backdrop = true, children }: { height?: number; backdrop?: boolean; children?: ReactNode }) {
  return (
    <div className={styles.stage} style={{ '--stage-h': `${height}px` } as CSSProperties}>
      {backdrop && (
        <div className={styles.back} aria-hidden>
          <h4>Library</h4>
          <div className={styles.grid}>
            {Array.from({ length: 8 }, (_, i) => <i key={i} />)}
          </div>
        </div>
      )}
      {children}
    </div>
  );
}
