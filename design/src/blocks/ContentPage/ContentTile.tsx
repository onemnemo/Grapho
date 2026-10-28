import type { ButtonHTMLAttributes } from 'react';
import { Icon } from '@/assets/icons';
import { contentType, type ContentItem } from '@/assets/content';
import { cx } from '@/lib/cx';
import { ContentArt } from './ContentArt';
import styles from './ContentTile.module.css';

export interface ContentTileProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  item: ContentItem;
  view?: 'grid' | 'list';
  /** Select mode: the tile shows an empty ring, and a check when picked. */
  selectable?: boolean;
  selected?: boolean;
  /** The quiet source name at the end of a list row. */
  sourceName?: string;
}

/** One item of content. A picture and a name in a grid, or a thumbnail row in a list. No border at rest, a fill on hover. */
export function ContentTile({ item, view = 'grid', selectable = false, selected = false, sourceName, className, ...rest }: ContentTileProps) {
  const shape = contentType(item.type).shape;
  const live = selectable || !!rest.onClick;
  const Tag = live ? 'button' : 'div';
  return (
    <Tag
      {...(live ? { type: 'button' as const, 'aria-pressed': selectable ? selected : undefined } : {})}
      className={cx(styles.tile, view === 'list' ? styles.row : styles.cell, className)}
      data-shape={shape}
      data-selected={selected || undefined}
      title={item.name}
      {...(rest as object)}
    >
      <span className={styles.art}><ContentArt item={item} /></span>
      <span className={styles.name}>{item.name}</span>
      {view === 'list' && sourceName && <span className={styles.src}>{sourceName}</span>}
      {selectable && (
        <span className={styles.check} aria-hidden>
          <Icon name="check" size={14} />
        </span>
      )}
    </Tag>
  );
}
