import type { ButtonHTMLAttributes, ReactNode } from 'react';
import type { TileArt } from '@/assets/studio';
import { cx } from '@/lib/cx';
import { TileArtView } from '../StickerTile';
import styles from './PackTile.module.css';

export interface PackTileProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  name: string;
  /** A quiet second line, such as "41 stickers". */
  detail: string;
  /** Three small pictures from inside the pack. */
  art?: TileArt[];
  /** Replaces the pictures, for the dashed Import tile. */
  icon?: ReactNode;
  variant?: 'pack' | 'import';
  state?: 'rest' | 'hover';
}

/** A pack in the studio panel: three pictures from inside it, its name and how much it holds. Tap to open. */
export function PackTile({ name, detail, art = [], icon, variant = 'pack', state = 'rest', className, ...rest }: PackTileProps) {
  return (
    <button type="button" aria-label={name} data-state={state === 'hover' ? 'hover' : undefined} className={cx(styles.tile, variant === 'import' && styles.import, className)} {...rest}>
      <span className={styles.art}>
        {icon ?? art.slice(0, 3).map((a, i) => <span key={i} className={cx(styles.pic, styles[`p${i}`])}><TileArtView art={a} /></span>)}
      </span>
      <b>{name}</b>
      <span>{detail}</span>
    </button>
  );
}
