import { useState } from 'react';
import { contentTypes, type ContentItem, type ContentType } from '@/assets/content';
import { Button } from '@/components/Button';
import { SearchField } from '@/components/SearchField';
import { Tabs } from '@/components/Tabs';
import { ContentTile } from '../ContentPage';
import styles from './PackPicker.module.css';

export interface PackPickerProps {
  items: ContentItem[];
  picked: Set<string>;
  onChange: (picked: Set<string>) => void;
}

/** Step one: type tabs across the top, checkable tiles below. Picks are kept when you change tab. */
export function PackPicker({ items, picked, onChange }: PackPickerProps) {
  const [type, setType] = useState<ContentType>('stickers');
  const [query, setQuery] = useState('');
  const q = query.trim().toLowerCase();
  const list = items.filter((i) => i.type === type && (!q || i.name.toLowerCase().includes(q)));
  const inTab = list.filter((i) => picked.has(i.id)).length;
  const toggle = (id: string) => { const n = new Set(picked); if (n.has(id)) n.delete(id); else n.add(id); onChange(n); };
  const all = list.length > 0 && inTab === list.length;
  const flip = () => { const n = new Set(picked); list.forEach((i) => (all ? n.delete(i.id) : n.add(i.id))); onChange(n); };
  const shape = contentTypes.find((t) => t.id === type)?.shape;
  return (
    <div className={styles.picker}>
      <Tabs
        aria-label="Type" layout="hug" value={type} onChange={(v) => setType(v as ContentType)}
        tabs={contentTypes.map((t) => ({ value: t.id, label: t.label, count: items.filter((i) => i.type === t.id && picked.has(i.id)).length || undefined }))}
      />
      <div className={styles.bar}>
        <SearchField size="sm" width={220} placeholder="Search" value={query} onChange={setQuery} />
        <Button variant="text" size="sm" onClick={flip} disabled={!list.length}>{all ? 'Clear' : 'Select all'}</Button>
      </div>
      <div className={styles.grid} data-shape={shape}>
        {list.map((it) => <ContentTile key={it.id} item={it} selectable selected={picked.has(it.id)} onClick={() => toggle(it.id)} />)}
        {!list.length && <p className={styles.none}>Nothing matches.</p>}
      </div>
    </div>
  );
}
