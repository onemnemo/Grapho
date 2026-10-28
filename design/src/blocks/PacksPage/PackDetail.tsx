import { useRef, useState } from 'react';
import { Icon } from '@/assets/icons';
import { contentType, contentTypes, type ContentItem, type ContentType } from '@/assets/content';
import type { TileArt } from '@/assets/studio';
import { Button } from '@/components/Button';
import { IconButton } from '@/components/IconButton';
import { Menu, MenuItem } from '@/components/Menu';
import { Popover } from '@/components/Popover';
import { Toggle } from '@/components/Toggle';
import { ContentTile } from '../ContentPage';
import { PackCategory } from '../PackCategory';
import { PackCover, type PackItem } from '../PackRow';
import styles from './PackDetail.module.css';

export interface PackDetailProps {
  pack: PackItem;
  items: ContentItem[];
  phone?: boolean;
  boundary?: 'viewport' | 'container';
  onBack: () => void;
  onToggle: (enabled: boolean) => void;
  onExport: () => void;
  onRemove: () => void;
}

const previewsOf = (list: ContentItem[]): TileArt[] =>
  list.flatMap((i) => (i.art.kind === 'tile' ? [i.art.art] : [])).slice(0, 3);

/** What a pack holds, grouped by type. Each group opens to its tiles. */
export function PackDetail({ pack, items, phone = false, boundary = 'viewport', onBack, onToggle, onExport, onRemove }: PackDetailProps) {
  const mine = items.filter((i) => i.source === pack.id);
  const groups = contentTypes.map((t) => [t.id, mine.filter((i) => i.type === t.id)] as [ContentType, ContentItem[]]).filter(([, l]) => l.length);
  const [open, setOpen] = useState<ContentType[]>(groups[0] ? [groups[0][0]] : []);
  const moreRef = useRef<HTMLButtonElement>(null);
  const [menu, setMenu] = useState(false);
  return (
    <div className={styles.detail}>
      <button type="button" className={styles.back} onClick={onBack}>
        <Icon name="back" size={18} />
        Packs
      </button>
      <header className={styles.head}>
        <PackCover art={pack.art} size={phone ? 56 : 72} cell={phone ? 4 : 5} />
        <div className={styles.title}>
          <h3>{pack.name}</h3>
          <small>{mine.length} items</small>
        </div>
        <Toggle variant="marked" checked={pack.enabled} onChange={onToggle} aria-label={`${pack.name} on`} />
        {!phone && <Button variant="secondary" size="sm" onClick={onExport}>Export</Button>}
        <span ref={moreRef}>
          <IconButton icon="more" label={`More for ${pack.name}`} size="sm" variant="ghost" aria-haspopup="menu" aria-expanded={menu} onClick={() => setMenu(!menu)} />
        </span>
        <Popover open={menu} onClose={() => setMenu(false)} anchorRef={moreRef} placement="bottom-end" kind="menu" boundary={boundary}>
          <Menu onClose={() => setMenu(false)} label={`${pack.name} menu`}>
            {phone && <MenuItem icon="export" onSelect={onExport}>Export .graphopack</MenuItem>}
            <MenuItem icon="trash" tone="danger" onSelect={onRemove}>Remove</MenuItem>
          </Menu>
        </Popover>
      </header>
      <div className={styles.groups} data-off={!pack.enabled || undefined}>
        {groups.map(([type, list]) => {
          const isOpen = open.includes(type);
          return (
            <PackCategory
              key={type} variant="plain" icon={contentType(type).icon} title={contentType(type).label} count={list.length} open={isOpen} previews={previewsOf(list)}
              onToggle={() => setOpen(isOpen ? open.filter((t) => t !== type) : [...open, type])}
            >
              <div className={styles.grid} data-shape={contentType(type).shape}>
                {list.map((it) => <ContentTile key={it.id} item={it} />)}
              </div>
            </PackCategory>
          );
        })}
      </div>
    </div>
  );
}
