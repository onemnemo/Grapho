import { Fragment, useRef, useState, type HTMLAttributes, type ReactNode } from 'react';
import { Icon } from '@/assets/icons';
import { Button } from '@/components/Button';
import { IconButton } from '@/components/IconButton';
import { SearchField } from '@/components/SearchField';
import { RollingNumber } from '@/components/RollingNumber';
import { cx } from '@/lib/cx';
import { NewMenu, type NewKind } from '../NewMenu';
import styles from './LibraryHeader.module.css';

export interface Crumb {
  label: string;
  onClick: () => void;
}

export interface LibraryHeaderProps extends Omit<HTMLAttributes<HTMLElement>, 'title'> {
  /** The screen title at the top level: Library. Inside a folder the last crumb is the title. */
  title?: string;
  /** Small caption after the title, such as "148 items". */
  caption?: string;
  /** The path above the current folder, outermost first. Empty at the top level. */
  path?: Crumb[];
  /** Name of the folder or view you are in when path is not empty. */
  current?: string;
  onBack?: () => void;
  /** Selection mode: the count rolls in and Select all and Done replace the tools. */
  selecting?: boolean;
  selectedCount?: number;
  allSelected?: boolean;
  onSelectAll?: () => void;
  onDone?: () => void;
  /** Search. The icon grows into a field that fills the header. */
  searching?: boolean;
  query?: string;
  searchPlaceholder?: string;
  onSearchOpen?: () => void;
  onSearchClose?: () => void;
  onQueryChange?: (query: string) => void;
  /** Tools. Leave a handler out to hide that tool. */
  onFilter?: () => void;
  filterActive?: boolean;
  onSettings?: () => void;
  onNew?: (kind: NewKind) => void;
  /** Soft line under the header once the page has scrolled. */
  scrolled?: boolean;
  /** Open the New menu at first, for stories. */
  defaultNewOpen?: boolean;
}

/** Title, search, filter, settings and New. In a folder the title becomes a path; in selection mode it becomes the count. */
export function LibraryHeader({
  title = 'Library', caption, path = [], current, onBack,
  selecting = false, selectedCount = 0, allSelected = false, onSelectAll, onDone,
  searching = false, query = '', searchPlaceholder = 'Search notebooks, folders and text', onSearchOpen, onSearchClose, onQueryChange,
  onFilter, filterActive = false, onSettings, onNew, scrolled = false, defaultNewOpen = false,
  className, ...rest
}: LibraryHeaderProps) {
  const [newOpen, setNewOpen] = useState(defaultNewOpen);
  const newRef = useRef<HTMLSpanElement>(null);
  const sub = path.length > 0 || current != null;
  const tools = !selecting && !searching;
  return (
    <header
      className={cx(styles.head, className)}
      data-scrolled={scrolled}
      data-sub={sub}
      data-selecting={selecting}
      data-searching={searching}
      {...rest}
    >
      <div className={styles.left}>
        <h1 className={styles.title}>
          <span className={styles.back}>
            <IconButton icon="back" label="Back" variant="ghost" onClick={onBack} tabIndex={sub ? 0 : -1} />
          </span>
          {sub ? (
            <span className={styles.crumbs}>
              {path.map((c) => (
                <Fragment key={c.label}>
                  <button type="button" className={styles.crumb} onClick={c.onClick}>{c.label}</button>
                  <Icon name="chevron_right" size={16} />
                </Fragment>
              ))}
              <b className={styles.current}>{current}</b>
            </span>
          ) : (
            <>
              {title}
              {caption && <span className={styles.sub}>{caption}</span>}
            </>
          )}
        </h1>
        <div className={styles.picked} aria-live="polite">
          <IconButton icon="close" label="Close selection" variant="solid" onClick={onDone} />
          <RollingNumber value={selectedCount} />
          <span>selected</span>
        </div>
      </div>
      <div className={styles.right}>
        <SearchField
          className={cx(styles.search, !searching && styles.collapsed, selecting && styles.hide)}
          value={query}
          onChange={onQueryChange}
          placeholder={searchPlaceholder}
          width={searching ? 2000 : 44}
          focusWidth={searching ? 2000 : 44}
          onFocus={() => !searching && onSearchOpen?.()}
          onKeyDown={(e) => { if (e.key === 'Escape' && !query) onSearchClose?.(); }}
        />
        {onFilter && (
          <Slot on={tools}><IconButton icon="filter" label="Filter" variant="solid" dot={filterActive} onClick={onFilter} /></Slot>
        )}
        {onSettings && (
          <Slot on={tools}><IconButton icon="settings" label="Settings" variant="solid" onClick={onSettings} /></Slot>
        )}
        {onNew && (
          <Slot on={tools}>
            <span ref={newRef}>
              <Button className={styles.new} variant="primary" icon="plus" data-open={newOpen} onClick={() => setNewOpen((v) => !v)}>New</Button>
            </span>
          </Slot>
        )}
        <Slot on={searching}><Button onClick={onSearchClose}>Cancel</Button></Slot>
        <Slot on={selecting}><Button onClick={onSelectAll}>{allSelected ? 'Select none' : 'Select all'}</Button></Slot>
        <Slot on={selecting}><Button variant="primary" onClick={onDone}>Done</Button></Slot>
      </div>
      {onNew && <NewMenu open={newOpen} onClose={() => setNewOpen(false)} anchorRef={newRef} onChoose={(k) => onNew(k)} />}
    </header>
  );
}

/** A tool that slides open and closed beside the others. */
function Slot({ on, children }: { on: boolean; children: ReactNode }) {
  return (
    <div className={styles.slot} data-on={on} aria-hidden={!on || undefined} inert={!on || undefined}>
      <div>{children}</div>
    </div>
  );
}
