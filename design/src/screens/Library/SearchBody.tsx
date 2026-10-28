import { useState, type CSSProperties } from 'react';
import { EmptyState } from '@/components/EmptyState';
import { ListRow } from '@/components/ListRow';
import { SegmentedControl } from '@/components/SegmentedControl';
import { Icon } from '@/assets/icons';
import { SectionHeader } from '@/blocks/SectionHeader';
import { LibraryRow } from '@/blocks/LibraryRow';
import { Highlight } from '@/components/Highlight';
import { ScrollRow } from '@/components/ScrollRow';
import { KIND_LABEL, plural } from './libraryModel';
import { FolderItem, ItemTile } from './tiles';
import type { Library } from './useLibrary';
import styles from './layout.module.css';

type Scope = 'all' | 'titles' | 'notes';

/** What shows while searching: recent searches before you type, grouped results after. */
export function SearchBody({ lib, recent }: { lib: Library; recent: string[] }) {
  const [scope, setScope] = useState<Scope>('all');
  const q = lib.query.trim().toLowerCase();
  if (!q) {
    return (
      <section className={styles.section}>
        <SectionHeader title="Recent searches" />
        <div className={styles.recents}>
          {recent.map((r) => (
            <ListRow key={r} leadingIcon="clock" title={r} onClick={() => lib.setQuery(r)} />
          ))}
        </div>
      </section>
    );
  }
  const folders = scope === 'notes' ? [] : lib.folders.filter((f) => f.name.toLowerCase().includes(q));
  const titles = scope === 'notes' ? [] : lib.items.filter((i) => i.title.toLowerCase().includes(q));
  const notes = scope === 'titles' ? [] : lib.items.flatMap((i) => i.notes.filter((n) => n.t.toLowerCase().includes(q)).map((n) => ({ item: i, note: n })));
  const total = folders.length + titles.length + notes.length;
  return (
    <>
      <div className={styles.results}>
        <SegmentedControl
          aria-label="Search in"
          value={scope}
          onChange={(v) => setScope(v as Scope)}
          options={[{ value: 'all', label: 'Everything' }, { value: 'titles', label: 'Titles' }, { value: 'notes', label: 'Inside notes' }]}
        />
        <span aria-live="polite">{plural(total, 'result')}</span>
      </div>
      {total === 0 && (
        <div className={styles.center}>
          <EmptyState anim="worry" bubble="no match" title="Nothing found" secondary={{ label: 'Clear search', onClick: () => lib.setQuery('') }}>
            Check the spelling or try fewer letters.
          </EmptyState>
        </div>
      )}
      {folders.length > 0 && (
        <section className={styles.section}>
          <SectionHeader title="Folders" />
          <ScrollRow gap={16}>{folders.map((f) => <FolderItem key={f.id} lib={lib} folder={f} width={190} query={q} />)}</ScrollRow>
        </section>
      )}
      {titles.length > 0 && (
        <section className={styles.section}>
          <SectionHeader title="Notebooks and PDFs" />
          <div className={styles.grid} style={{ '--cols': 8 } as CSSProperties}>
            {titles.map((i) => <ItemTile key={i.id} lib={lib} item={i} short query={q} />)}
          </div>
        </section>
      )}
      {notes.length > 0 && (
        <section className={styles.section}>
          <SectionHeader title="Inside notes" />
          <div className={styles.searchRows}>
            {notes.map(({ item, note }) => (
              <LibraryRow
                key={`${item.id}-${note.p}`} compact kind={item.kind} cover={item.cover} color={item.color} doc={item.doc} title={item.title} typeLabel={KIND_LABEL[item.kind]}
                path={<Highlight text={note.t} query={q} />} aside={<>Page {note.p} <Icon name="chevron_right" size={16} /></>}
              />
            ))}
          </div>
        </section>
      )}
    </>
  );
}
