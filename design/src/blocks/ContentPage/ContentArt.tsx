import { DesignArt, TileArtView } from '@/assets/studio';
import type { ContentItem } from '@/assets/content';
import styles from './ContentArt.module.css';

/** The small drawing for one item of content, by type. Sized by its parent. */
export function ContentArt({ item }: { item: ContentItem }) {
  const a = item.art;
  switch (a.kind) {
    case 'tile':
      return <span className={styles.tile} data-wide={a.wide || undefined}><TileArtView art={a.art} fit={88} /></span>;
    case 'design':
      return <span className={styles.design} data-kind={a.design.kind}><DesignArt design={a.design} shadow={false} /></span>;
    case 'pen':
      return (
        <svg className={styles.pen} viewBox="0 0 96 32" aria-hidden>
          <path d="M8 22C22 6 34 6 46 16s26 10 42-8" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth={a.width} strokeDasharray={a.dash ? '0.1 3.4' : undefined} opacity={a.soft ? 0.55 : 1} />
        </svg>
      );
    case 'colours':
      return (
        <span className={styles.colours}>
          {a.colours.map((c, i) => <i key={i} style={{ background: c }} />)}
        </span>
      );
    case 'page':
      return <span className={styles.page} data-lines={a.lines} />;
    case 'theme':
      return (
        <span className={styles.theme} style={{ background: a.bg }}>
          <i className={styles.themeBar} style={{ background: a.panel }} />
          <i className={styles.themeSide} style={{ background: a.panel }} />
          <i className={styles.themeLine} style={{ background: a.ink }} />
          <i className={styles.themeDot} style={{ background: a.accent }} />
        </span>
      );
  }
}
