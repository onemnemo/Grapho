import { contentTypes, type ContentItem, type ContentType } from '@/assets/content';
import { ActionRow } from '@/components/ActionRow';
import styles from './sections.module.css';

export interface ContentIndexProps {
  items: ContentItem[];
  onOpen: (type: ContentType) => void;
}

/** The seven kinds of content, each with how many there are. Opens that type's page. */
export function ContentIndexSection({ items, onOpen }: ContentIndexProps) {
  return (
    <div className={styles.rows}>
      {contentTypes.map((t) => (
        <ActionRow key={t.id} icon={t.icon} value={items.filter((i) => i.type === t.id).length} onClick={() => onOpen(t.id)}>{t.label}</ActionRow>
      ))}
    </div>
  );
}
