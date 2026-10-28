import { useEffect, useState } from 'react';
import { ActionRow } from '@/components/ActionRow';
import { ExtensionDetail } from './ExtensionDetail';
import { ExtensionRow } from './ExtensionRow';
import { groupLabels, groupOrder, type ExtensionItem } from './extensionTypes';
import styles from './ExtensionsPage.module.css';

export interface ExtensionsPageProps {
  extensions: ExtensionItem[];
  /** Open one extension's page at first, for stories. */
  initialOpen?: string | null;
  /** Tells the page around it when a detail is open, so it can drop its own title. */
  onDetail?: (id: string | null) => void;
  onToggle?: (id: string, enabled: boolean) => void;
  onRemove?: (id: string) => void;
  onInstallFile?: () => void;
  onBrowse?: () => void;
}

/** Three groups with plain headings: Built in, From Grapho, Installed by you. The group is the source, so rows carry no labels. */
export function ExtensionsPage({ extensions, initialOpen = null, onDetail, onToggle, onRemove, onInstallFile, onBrowse }: ExtensionsPageProps) {
  const [open, setOpen] = useState<string | null>(initialOpen);
  const current = extensions.find((e) => e.id === open);
  const currentId = current?.id ?? null;
  useEffect(() => { onDetail?.(currentId); }, [currentId, onDetail]);
  if (current) {
    return (
      <ExtensionDetail
        extension={current} onBack={() => setOpen(null)} onToggle={(v) => onToggle?.(current.id, v)}
        onRemove={() => { onRemove?.(current.id); setOpen(null); }}
      />
    );
  }
  return (
    <div className={styles.page}>
      {groupOrder.map((g) => (
        <section key={g}>
          <h3 className={styles.head}>{groupLabels[g]}</h3>
          <div className={styles.list}>
            {extensions.filter((e) => e.group === g).map((e) => (
              <ExtensionRow key={e.id} extension={e} onOpen={() => setOpen(e.id)} onToggle={(v) => onToggle?.(e.id, v)} />
            ))}
            {g === 'grapho' && <ActionRow icon="grid" onClick={onBrowse}>Discover extensions</ActionRow>}
            {g === 'user' && <ActionRow icon="import" onClick={onInstallFile}>Install a .graphoext file</ActionRow>}
          </div>
        </section>
      ))}
    </div>
  );
}
