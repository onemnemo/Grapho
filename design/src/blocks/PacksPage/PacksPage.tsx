import { useEffect, useState } from 'react';
import type { ContentItem } from '@/assets/content';
import { ActionRow } from '@/components/ActionRow';
import { SearchField } from '@/components/SearchField';
import { PackDetail } from './PackDetail';
import { PackRow, type PackItem, type PackRowAction } from '../PackRow';
import styles from './PacksPage.module.css';

export interface PacksPageProps {
  packs: PackItem[];
  /** All content, to count what each pack holds. */
  items: ContentItem[];
  phone?: boolean;
  boundary?: 'viewport' | 'container';
  /** Open a pack's contents at first, for stories. */
  initialOpen?: string | null;
  /** Tells the page around it when a detail is open, so it can drop its own title. */
  onDetail?: (id: string | null) => void;
  onToggle?: (id: string, enabled: boolean) => void;
  /** Export or remove one pack. */
  onPackAction?: (id: string, action: Exclude<PackRowAction, 'open'>) => void;
  onNewPack?: () => void;
  onImport?: () => void;
  onBrowse?: () => void;
}

/** Packs as sources of content: turn each on or off, export it, remove it. Opening one shows what is inside, by type. */
export function PacksPage({ packs, items, phone = false, boundary = 'viewport', initialOpen = null, onDetail, onToggle, onPackAction, onNewPack, onImport, onBrowse }: PacksPageProps) {
  const [open, setOpen] = useState<string | null>(initialOpen);
  const [query, setQuery] = useState('');
  const current = packs.find((p) => p.id === open);
  const currentId = current?.id ?? null;
  useEffect(() => { onDetail?.(currentId); }, [currentId, onDetail]);

  if (current) {
    return (
      <PackDetail
        pack={current} items={items} phone={phone} boundary={boundary} onBack={() => setOpen(null)}
        onToggle={(v) => onToggle?.(current.id, v)}
        onExport={() => onPackAction?.(current.id, 'export')}
        onRemove={() => { onPackAction?.(current.id, 'remove'); }}
      />
    );
  }
  const q = query.trim().toLowerCase();
  const shown = packs.filter((p) => !q || p.name.toLowerCase().includes(q));
  return (
    <div className={styles.page}>
      <div className={styles.actions}>
        <ActionRow icon="plus" onClick={onNewPack}>Make a pack</ActionRow>
        <ActionRow icon="import" onClick={onImport}>Import a pack</ActionRow>
        <ActionRow icon="grid" onClick={onBrowse}>Discover packs, themes and extensions</ActionRow>
      </div>
      <section>
        <div className={styles.head}>
          <h3>Your packs</h3>
          {packs.length > 6 && <SearchField size="sm" width={phone ? 140 : 200} placeholder="Search" value={query} onChange={setQuery} />}
        </div>
        <div className={styles.list}>
          {shown.map((p) => (
            <PackRow
              key={p.id} pack={p} compact={phone} boundary={boundary} detail={`${items.filter((i) => i.source === p.id).length} items`}
              onToggle={(v) => onToggle?.(p.id, v)}
              onAction={(a) => (a === 'open' ? setOpen(p.id) : onPackAction?.(p.id, a))}
            />
          ))}
          {!shown.length && <p className={styles.none}>{packs.length ? 'Nothing matches.' : 'No packs yet.'}</p>}
        </div>
      </section>
    </div>
  );
}
