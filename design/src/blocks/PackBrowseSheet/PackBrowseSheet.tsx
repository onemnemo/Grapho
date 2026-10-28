import { useState } from 'react';
import { packArt, type PackArt } from '@/assets/packs';
import type { ContentType } from '@/assets/content';
import { Button } from '@/components/Button';
import { SearchField } from '@/components/SearchField';
import { Sheet } from '@/components/Sheet';
import { Tabs } from '@/components/Tabs';
import { PackCover } from '../PackRow';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import styles from './PackBrowseSheet.module.css';

/** What a Discover entry holds: one content type, or an extension. */
export type DiscoverKind = ContentType | 'extensions';

export interface CataloguePack {
  id: string;
  name: string;
  meta: string;
  art: PackArt;
  kind: DiscoverKind;
  /** Who made it. Grapho's own are marked as such. */
  by: string;
}

export const sampleCatalogue: CataloguePack[] = [
  { id: 'c1', name: 'Moon phases', meta: '28 stickers', art: packArt.moon, kind: 'stickers', by: 'Grapho' },
  { id: 'c2', name: 'Lab notebook', meta: '9 templates', art: packArt.lab, kind: 'templates', by: 'Grapho' },
  { id: 'c3', name: 'Paper textures', meta: '18 covers', art: packArt.paper, kind: 'covers', by: 'Mira Holt' },
  { id: 'c4', name: 'Fountain pens', meta: '5 pens', art: packArt.nib, kind: 'pens', by: 'Grapho' },
  { id: 'c5', name: 'Sticky tabs', meta: '40 stickers', art: packArt.tabs, kind: 'stickers', by: 'Jonas Berg' },
  { id: 'c6', name: 'Dusk', meta: '2 themes', art: packArt.dusk, kind: 'themes', by: 'Grapho' },
  { id: 'c7', name: 'Pastel highlighters', meta: '6 pens', art: packArt.blush, kind: 'pens', by: 'Ana Ruiz' },
  { id: 'c8', name: 'Earth tones', meta: '3 colour sets', art: packArt.forest, kind: 'colours', by: 'Grapho' },
  { id: 'c9', name: 'Semester folders', meta: '8 folder styles', art: packArt.azure, kind: 'folders', by: 'Jonas Berg' },
  { id: 'c10', name: 'Lecture capture', meta: 'Adds a capture panel', art: packArt.graphite, kind: 'extensions', by: 'Grapho' },
];

const KINDS: Array<[DiscoverKind | 'all', string]> = [
  ['all', 'All'], ['pens', 'Pens'], ['colours', 'Colours'], ['stickers', 'Stickers'], ['covers', 'Covers'],
  ['folders', 'Folders'], ['templates', 'Templates'], ['themes', 'Themes'], ['extensions', 'Extensions'],
];

export interface PackBrowseSheetProps {
  open: boolean;
  onClose: () => void;
  packs?: CataloguePack[];
  /** Ids already installed from this sheet. */
  installed?: string[];
  onInstall: (pack: CataloguePack) => void;
  /** The tab it opens on, as when a Content page asks for more of its own type. */
  initialKind?: DiscoverKind | 'all';
  placement?: OverlayPlacement;
}

/** Discover: free packs, themes and extensions from grapho.one, by type, with who made each one. */
export function PackBrowseSheet({ open, onClose, packs = sampleCatalogue, installed = [], onInstall, initialKind = 'all', placement = 'viewport' }: PackBrowseSheetProps) {
  const [query, setQuery] = useState('');
  const [kind, setKind] = useState<string>(initialKind);
  const q = query.trim().toLowerCase();
  const shown = packs.filter((p) => (kind === 'all' || p.kind === kind) && (!q || p.name.toLowerCase().includes(q) || p.by.toLowerCase().includes(q)));
  return (
    <Sheet open={open} onClose={onClose} title="Discover" width={860} placement={placement}>
      <div className={styles.browse}>
        <SearchField size="sm" width={280} placeholder="Search Discover" value={query} onChange={setQuery} />
        <Tabs aria-label="Type" layout="hug" tabs={KINDS.map(([value, label]) => ({ value, label }))} value={kind} onChange={setKind} />
        {shown.length ? (
          <div className={styles.grid}>
            {shown.map((p) => {
              const done = installed.includes(p.id);
              return (
                <div key={p.id} className={styles.item}>
                  <PackCover className={styles.art} art={p.art} fit="wide" cell={5} />
                  <span className={styles.name}>{p.name}</span>
                  <span className={styles.meta}>{p.meta}</span>
                  <span className={styles.by}>{p.by === 'Grapho' ? 'By Grapho' : `By ${p.by}`}</span>
                  <Button size="sm" disabled={done} onClick={() => onInstall(p)}>{done ? 'Installed' : p.kind === 'extensions' ? 'Review and install' : 'Install'}</Button>
                </div>
              );
            })}
          </div>
        ) : (
          <p className={styles.none}>Nothing matches.</p>
        )}
        <p className={styles.submit}>Submit your own packs and extensions at grapho.one.</p>
      </div>
    </Sheet>
  );
}
