import { useState } from 'react';
import { Icon } from '@/assets/icons';
import { Button } from '@/components/Button';
import { Dialog } from '@/components/Dialog';
import { EmptyState } from '@/components/EmptyState';
import { IconButton } from '@/components/IconButton';
import { LibraryRow } from '@/blocks/LibraryRow';
import { binName, plural, type BinEntry } from './libraryModel';
import type { Library } from './useLibrary';
import styles from './layout.module.css';

const daysLeft = (e: BinEntry) => (e.left === 1 ? '1 day left' : `${e.left} days left`);

/** Recently deleted: everything deleted in the last 30 days, each with Restore. Deleting for good is asked first. */
export function DeletedBody({ lib }: { lib: Library }) {
  const [asking, setAsking] = useState<BinEntry | 'all' | null>(null);
  if (!lib.bin.length) {
    return (
      <div className={styles.center}>
        <EmptyState anim="sleep" bubble="all clear" title="Nothing deleted">Deleted notebooks and files stay here for 30 days.</EmptyState>
      </div>
    );
  }
  const what = asking === 'all' ? plural(lib.bin.length, 'item') : asking ? binName(asking) : '';
  return (
    <section className={styles.section}>
      <div className={styles.binHead}>
        <Button variant="text" size="sm" onClick={() => lib.restore(lib.bin.map((e) => e.id))}>Restore all</Button>
        <Button variant="text" size="sm" onClick={() => setAsking('all')}>Delete all</Button>
      </div>
      <div className={styles.rows}>
        {lib.bin.map((e) => {
          const folder = e.folders[0] && e.id === `f:${e.folders[0].id}` ? e.folders[0] : null;
          const item = folder ? null : e.items[0];
          const inside = folder ? e.items.length + e.folders.length - 1 : 0;
          return (
            <LibraryRow
              key={e.id} compact className={styles.binRow}
              kind={folder ? 'folder' : item!.kind} colour={folder?.colour} cover={item?.cover} doc={item?.doc}
              title={binName(e)} path={inside ? `${plural(inside, 'item')} inside · ${daysLeft(e)}` : daysLeft(e)} typeLabel=""
              aside={
                <span className={styles.binActions}>
                  <Button size="sm" icon="restore" onClick={(ev) => { ev.stopPropagation(); lib.restore([e.id]); }}>Restore</Button>
                  <IconButton icon="trash" size="sm" variant="ghost" label={`Delete ${binName(e)} now`} onClick={(ev) => { ev.stopPropagation(); setAsking(e); }} />
                </span>
              }
            />
          );
        })}
      </div>
      <Dialog
        open={asking !== null} onClose={() => setAsking(null)} role="alertdialog" placement="container"
        title={`Delete ${what} now?`}
        actions={
          <>
            <Button variant="text" onClick={() => setAsking(null)}>Cancel</Button>
            <Button variant="danger" onClick={() => { lib.purge(asking === 'all' ? undefined : asking ? [asking.id] : []); setAsking(null); }}>Delete</Button>
          </>
        }
      >
        This can't be undone.
      </Dialog>
    </section>
  );
}

/** The way in from All folders, at the end of the list. */
export function DeletedLink({ count, onOpen }: { count: number; onOpen: () => void }) {
  return (
    <button type="button" className={styles.binLink} onClick={onOpen}>
      <span className={styles.binIcon}><Icon name="trash" size={20} /></span>
      <span className={styles.binLabel}>Recently deleted</span>
      {count > 0 && <span className={styles.binCount}>{count}</span>}
    </button>
  );
}
