import { Banner } from '@/components/Banner';
import { EmptyState } from '@/components/EmptyState';
import { Button } from '@/components/Button';
import { RemovableChip } from '@/components/Chip';
import { SectionHeader } from '@/blocks/SectionHeader';
import { ScrollRow } from '@/components/ScrollRow';
import { FolderItem, ItemsView } from './tiles';
import type { Library } from './useLibrary';
import type { FilterState } from '@/blocks/FilterSheet';
import styles from './layout.module.css';

const TYPE_LABEL = { notebook: 'Notebooks', pdf: 'PDFs', canvas: 'Canvases' } as const;
const EDITED_LABEL = { week: 'Past week', month: 'Past month', year: 'Past year' } as const;

function FilterChips({ lib }: { lib: Library }) {
  const f = lib.filters;
  const set = (next: Partial<FilterState>) => lib.setFilters({ ...f, ...next });
  return (
    <>
      {f.types.map((t) => <RemovableChip key={t} removeLabel={`Remove ${TYPE_LABEL[t]}`} onRemove={() => set({ types: f.types.filter((x) => x !== t) })}>{TYPE_LABEL[t]}</RemovableChip>)}
      {f.tags.map((t) => <RemovableChip key={t} removeLabel={`Remove ${t}`} onRemove={() => set({ tags: f.tags.filter((x) => x !== t) })}>{t}</RemovableChip>)}
      {f.edited !== 'any' && <RemovableChip removeLabel="Remove date" onRemove={() => set({ edited: 'any' })}>{EDITED_LABEL[f.edited]}</RemovableChip>}
      {f.starred && <RemovableChip removeLabel="Remove starred" onRemove={() => set({ starred: false })}>Starred</RemovableChip>}
      {lib.filterActive && <Button variant="text" size="sm" onClick={() => lib.setFilters({ types: [], edited: 'any', starred: false, tags: [] })}>Clear all</Button>}
    </>
  );
}

/** Folders in a row, then the notes. The folder view uses the same body one level down. */
export function HomeBody({ lib, onSeeAll, pinnedRow = false }: { lib: Library; onSeeAll?: () => void; pinnedRow?: boolean }) {
  const sub = lib.path.length > 0;
  const folderWidth = pinnedRow ? 150 : sub ? 190 : 250;
  const hasNotes = lib.inScope.length > 0;
  if (!hasNotes && lib.here.length === 0) {
    return (
      <div className={sub ? styles.centerLow : styles.center}>
        {sub ? (
          <EmptyState anim="sit" bubble="nothing yet" title="This folder is empty" primary={{ label: 'New notebook', icon: 'plus', onClick: () => lib.create('notebook') }}>
            Drag notebooks here from the library or make a new one.
          </EmptyState>
        ) : (
          <EmptyState anim="wave" bubble="start here" title="Your library is empty" primary={{ label: 'New notebook', icon: 'plus', onClick: () => lib.create('notebook') }} secondary={{ label: 'Import PDF', onClick: () => lib.create('pdf') }}>
            Make a notebook or bring in a PDF. Everything you write stays on your device as plain files.
          </EmptyState>
        )}
      </div>
    );
  }
  const title = sub ? 'Notes' : lib.scopeAll ? 'All notes' : 'Recent';
  return (
    <>
      {lib.banner && (
        <Banner
          className={styles.banner}
          title={lib.banner.title}
          actionLabel={lib.banner.action}
          onAction={lib.dismissBanner}
          onClose={lib.dismissBanner}
        >
          {lib.banner.text}
        </Banner>
      )}
      {lib.here.length > 0 && (
        <section className={styles.section}>
          <SectionHeader title={sub ? 'Folders' : pinnedRow ? 'Pinned folders' : 'Folders'} onSeeAll={sub ? undefined : onSeeAll} />
          <ScrollRow gap={pinnedRow || sub ? 16 : 22}>
            {lib.here.map((f) => <FolderItem key={f.id} lib={lib} folder={f} width={folderWidth} compactMeta={pinnedRow} />)}
          </ScrollRow>
        </section>
      )}
      <section className={styles.section}>
        <SectionHeader
          title={title}
          view={lib.view}
          onView={lib.setView}
          sort={{
            scope: 'items', sortKey: lib.sortKey, sortDir: lib.sortDir, group: lib.group, size: lib.size,
            onSortKey: lib.setSortKey, onSortDir: lib.setSortDir, onGroup: lib.setGroup, onSize: lib.setSize,
          }}
        >
          <FilterChips lib={lib} />
        </SectionHeader>
        {lib.visible.length > 0 ? (
          <ItemsView lib={lib} items={lib.sortedVisible} />
        ) : hasNotes ? (
          <div className={styles.center}>
            <EmptyState anim="lookat" bubble="no match" title="Nothing matches these filters" primary={undefined} secondary={{ label: 'Reset filters', onClick: () => lib.setFilters({ types: [], edited: 'any', starred: false, tags: [] }) }}>
              Remove one or reset them all.
            </EmptyState>
          </div>
        ) : null}
      </section>
    </>
  );
}
