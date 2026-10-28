import type { CSSProperties, HTMLAttributes } from 'react';
import type { PackArt } from '@/assets/packs';
import { PixelIcon } from '@/assets/pixel';
import { cx } from '@/lib/cx';
import styles from './PackCover.module.css';

export interface PackCoverProps extends HTMLAttributes<HTMLSpanElement> {
  art: PackArt;
  /** square is a fixed edge; wide fills its parent at 4 to 3, as in the catalogue. */
  fit?: 'square' | 'wide';
  /** Edge length in px for a square cover. */
  size?: number;
  /** Pixel size of one cell in the glyph. */
  cell?: number;
}

/** A pack's cover: a diagonal gradient with one pixel glyph on it. */
export function PackCover({ art, fit = 'square', size = 64, cell = 4, className, style, ...rest }: PackCoverProps) {
  return (
    <span className={cx(styles.cover, fit === 'wide' && styles.wide, className)} style={{ ...(fit === 'square' && { width: size, height: size }), backgroundImage: art.gradient, ...style } as CSSProperties} {...rest}>
      <PixelIcon name={art.glyph} cell={cell} />
    </span>
  );
}
