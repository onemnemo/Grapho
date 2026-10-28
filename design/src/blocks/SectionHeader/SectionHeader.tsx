import { useRef, useState, type HTMLAttributes, type ReactNode } from 'react';
import { Icon } from '@/assets/icons';
import { SegmentedControl } from '@/components/SegmentedControl';
import { cx } from '@/lib/cx';
import { SortMenu, SORT_LABELS, type SortMenuProps, type ViewMode } from '../SortMenu';
import styles from './SectionHeader.module.css';

export interface SectionSort extends Omit<SortMenuProps, 'open' | 'onClose' | 'anchorRef' | 'view' | 'placement'> {}

export interface SectionHeaderProps extends Omit<HTMLAttributes<HTMLDivElement>, 'title'> {
  title: string;
  /** Small number after the title: 12, or "3 of 40" while filtered. */
  count?: ReactNode;
  /** title puts the count after the title; end puts it beside the right hand tools. */
  countAt?: 'title' | 'end';
  /** Shows the See all button. */
  onSeeAll?: () => void;
  seeAllLabel?: string;
  /** Shows the Sort button and its menu. */
  sort?: SectionSort;
  /** Shows the grid and list switch. */
  view?: ViewMode;
  onView?: (view: ViewMode) => void;
  /** Open the sort menu at first, for stories. */
  defaultSortOpen?: boolean;
  /** Extra content after the title, such as the chips for active filters. */
  children?: ReactNode;
}

/** The heading of a library section: Folders 12, Recent 40. */
export function SectionHeader({ title, count, countAt = 'title', onSeeAll, seeAllLabel = 'See all', sort, view, onView, defaultSortOpen = false, className, children, ...rest }: SectionHeaderProps) {
  const [sortOpen, setSortOpen] = useState(defaultSortOpen);
  const sortRef = useRef<HTMLButtonElement>(null);
  return (
    <div className={cx(styles.head, className)} {...rest}>
      <div className={styles.left}>
        <h2 className={styles.title}>
          {title}
          {count != null && countAt === 'title' && <small className={styles.count}>{count}</small>}
        </h2>
        {children && <div className={styles.extra}>{children}</div>}
      </div>
      <div className={styles.right}>
        {count != null && countAt === 'end' && <span className={styles.count}>{count}</span>}
        {onSeeAll && (
          <button type="button" className={styles.seeAll} onClick={onSeeAll}>
            {seeAllLabel}
            <Icon name="chevron_right" size={14} />
          </button>
        )}
        {sort && (
          <>
            <button ref={sortRef} type="button" className={styles.sort} aria-haspopup="menu" aria-expanded={sortOpen} onClick={() => setSortOpen((v) => !v)}>
              <span>Sort</span>{SORT_LABELS[sort.sortKey]}
            </button>
            <SortMenu {...sort} view={view} open={sortOpen} onClose={() => setSortOpen(false)} anchorRef={sortRef} />
          </>
        )}
        {view && (
          <SegmentedControl
            aria-label="View"
            iconsOnly
            value={view}
            onChange={(v) => onView?.(v as ViewMode)}
            options={[{ value: 'grid', label: 'Grid view', icon: 'grid' }, { value: 'list', label: 'List view', icon: 'list' }]}
          />
        )}
      </div>
    </div>
  );
}
