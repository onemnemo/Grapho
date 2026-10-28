import type { PackArt } from '@/assets/packs';

/** A pack as a source of content. What it holds is counted from the content itself. */
export interface PackItem {
  id: string;
  name: string;
  art: PackArt;
  enabled: boolean;
}
