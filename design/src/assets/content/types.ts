import type { PackArt } from '../packs';
import type { IconName } from '../icons';
import type { Design, TileArt } from '../studio';

/** The seven kinds of content. A pack can hold any mix of them. */
export type ContentType = 'pens' | 'colours' | 'covers' | 'folders' | 'stickers' | 'templates' | 'themes';

export interface ContentTypeInfo {
  id: ContentType;
  label: string;
  /** Used in "New pack" and in a grouped list: "Colour sets". */
  icon: IconName;
  /** How its tile is shaped in a grid. */
  shape: 'square' | 'wide' | 'tall' | 'stroke' | 'page';
}

export const contentTypes: ContentTypeInfo[] = [
  { id: 'pens', label: 'Pens', icon: 'pen', shape: 'stroke' },
  { id: 'colours', label: 'Colour sets', icon: 'palette', shape: 'wide' },
  { id: 'covers', label: 'Covers', icon: 'notebook', shape: 'tall' },
  { id: 'folders', label: 'Folder styles', icon: 'folder', shape: 'wide' },
  { id: 'stickers', label: 'Stickers', icon: 'sticker', shape: 'square' },
  { id: 'templates', label: 'Templates', icon: 'page', shape: 'page' },
  { id: 'themes', label: 'Themes', icon: 'sun', shape: 'wide' },
];

export const contentType = (id: ContentType) => contentTypes.find((t) => t.id === id) as ContentTypeInfo;

export type PageLines = 'blank' | 'ruled' | 'grid' | 'dots' | 'cornell' | 'plan' | 'music' | 'checklist';

/** What a tile draws. Each kind has its own small drawing. */
export type ItemArt =
  | { kind: 'tile'; art: TileArt; wide?: boolean }
  | { kind: 'design'; design: Design }
  | { kind: 'pen'; width: number; dash?: boolean; soft?: boolean }
  | { kind: 'colours'; colours: string[] }
  | { kind: 'page'; lines: PageLines }
  | { kind: 'theme'; bg: string; panel: string; ink: string; accent: string };

export interface ContentItem {
  id: string;
  type: ContentType;
  name: string;
  /** 'built', 'own' or a pack id. */
  source: string;
  /** Higher is more recent. */
  usedAt: number;
  /** Higher is newer. */
  added: number;
  art: ItemArt;
}

export interface ContentSource {
  id: string;
  name: string;
  kind: 'built' | 'own' | 'pack';
  art?: PackArt;
  enabled: boolean;
}
