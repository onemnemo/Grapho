import type { ReactNode } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import type { TileArt } from '@/assets/studio';
import { cx } from '@/lib/cx';
import { TileArtView } from '../StickerTile';
import styles from './PackCategory.module.css';

export interface PackCategoryProps {
  title: string;
  /** How many items it holds. */
  count: number;
  open: boolean;
  onToggle: () => void;
  /** Up to three small pictures, shown in the header while it is closed. */
  previews?: TileArt[];
  /** The grid of tiles, shown while open. */
  children?: ReactNode;
  /** The glyph before the title. */
  icon?: IconName;
  /** card is the studio's boxed header; plain has no boxes, for Settings. */
  variant?: 'card' | 'plain';
  className?: string;
}

/** A collapsible category inside a pack. Closed, it previews what is in it; open, it shows the grid. */
export function PackCategory({ title, count, open, onToggle, previews = [], children, variant = 'card', icon, className }: PackCategoryProps) {
  return (
    <section className={cx(styles.cat, variant === 'plain' && styles.plain, className)} data-open={open || undefined}>
      <button type="button" className={styles.head} aria-expanded={open} onClick={onToggle}>
        {icon && <Icon name={icon} size={16} className={styles.glyph} />}
        <b>{title}</b>
        <span className={styles.count}>{count}</span>
        {!open && previews.length > 0 && (
          <span className={styles.strip}>
            {previews.slice(0, 3).map((a, i) => <i key={i}><TileArtView art={a} /></i>)}
          </span>
        )}
        <Icon name="chevron_down" size={16} className={styles.chev} />
      </button>
      {open && <div className={styles.body}>{children}</div>}
    </section>
  );
}
