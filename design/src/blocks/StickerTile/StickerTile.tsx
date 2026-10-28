import { useState, type ButtonHTMLAttributes, type ReactNode } from 'react';
import { CLIPS, type TileArt, type TileSize } from '@/assets/studio';
import { cx } from '@/lib/cx';
import { TileArtView } from '@/assets/studio';
import styles from './StickerTile.module.css';

export interface StickerTileProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  art: TileArt;
  /** Read by screen readers, and shown as the caption when `caption` is on. */
  name: string;
  /** normal is square, wide suits labels and tape, sm is a shape or icon, swatch is a 16:10 material. */
  size?: TileSize;
  /** A line of text under the tile, as under textures and clips. */
  caption?: boolean;
  /** Shows the tile as the dashed Import tile instead of a picture. */
  add?: ReactNode;
  /** The tile is the one being carried: the copy that stays behind dims. */
  carried?: boolean;
  /** Pins the hover pose for stories. */
  state?: 'rest' | 'hover';
}

const framed = (a: TileArt) => a.t === 'photo' || a.t === 'card';

/** One thing to add to the cover or folder: a sticker, shape, image, card, icon, texture or clip. Tap to add, or drag onto the canvas. */
export function StickerTile({ art, name, size = 'normal', caption = false, add, carried = false, state = 'rest', className, ...rest }: StickerTileProps) {
  const [hover, setHover] = useState(false);
  const isVideo = art.t === 'video';
  const tile = (
    <button
      type="button"
      title={name}
      aria-label={name}
      data-state={state === 'hover' ? 'hover' : undefined}
      data-carried={carried || undefined}
      className={cx(styles.tile, styles[size], framed(art) && styles.framed, add != null && styles.add, isVideo && styles.video, className)}
      onPointerEnter={() => setHover(true)}
      onPointerLeave={() => setHover(false)}
      {...rest}
    >
      {add != null ? add : <TileArtView art={art} fit={art.t === 'img' ? 62 : 100} playing={isVideo && (hover || state === 'hover')} />}
      {isVideo && <em className={styles.dur}>0:{String(CLIPS[art.clip].len).padStart(2, '0')}</em>}
    </button>
  );
  if (!caption) return tile;
  return <div className={styles.cell}>{tile}<span>{name}</span></div>;
}
