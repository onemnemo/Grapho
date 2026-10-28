import type { IconName } from '@/assets/icons';

export interface ImportGroup {
  key: string;
  label: string;
  icon: IconName;
  items: string[];
}

export interface ImportSummary {
  count: number;
  /** The kinds that will be installed, in order. */
  kinds: string[];
}

/** The contents of a sample .graphopack, for stories and the Settings screen. */
export const sampleImportGroups: ImportGroup[] = [
  {
    key: 'stickers', label: 'Stickers', icon: 'sticker',
    items: ['Fern', 'Monstera', 'Ivy', 'Cactus', 'Aloe', 'Lily', 'Tulip', 'Daisy', 'Poppy', 'Lavender', 'Mint', 'Basil', 'Oak leaf', 'Maple leaf', 'Ginkgo', 'Bamboo', 'Orchid', 'Rose', 'Sunflower', 'Clover', 'Moss', 'Acorn', 'Pinecone', 'Seedling'],
  },
  { key: 'covers', label: 'Covers', icon: 'notebook', items: ['Meadow', 'Herbarium', 'Greenhouse', 'Field guide'] },
  { key: 'folders', label: 'Folder styles', icon: 'folder', items: ['Fern folder', 'Dried petals'] },
  { key: 'templates', label: 'Templates', icon: 'page', items: ['Plant log', 'Seasons planner', 'Sketch grid', 'Botanical lined'] },
  { key: 'pens', label: 'Pens', icon: 'pen', items: ['Fine nib', 'Wash brush', 'Seed pencil'] },
  { key: 'colours', label: 'Colour sets', icon: 'palette', items: ['Leaf greens', 'Petal pastels'] },
  { key: 'themes', label: 'Themes', icon: 'sun', items: ['Greenhouse'] },
];
