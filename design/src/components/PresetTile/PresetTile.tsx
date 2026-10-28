import type { ButtonHTMLAttributes, CSSProperties, ReactNode } from 'react';
import { Icon } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './PresetTile.module.css';

export type PresetShape = 'folder' | 'cover' | 'canvas';

export interface PresetTileProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  /** The look itself, drawn small: a folder, a cover or a canvas. It lifts a little on hover. */
  preview: ReactNode;
  label: string;
  /** A second quiet line, such as "Saved just now". */
  caption?: string;
  selected?: boolean;
  /** sheet is the tile in the Presets tab; picker is the bigger tile in Presets and library; swatch is a texture or pattern tile whose picture fills a rounded well. */
  variant?: 'sheet' | 'picker' | 'swatch';
  /** What kind of thing it shows, which sets the height of the preview slot. */
  shape?: PresetShape;
  /** In the picker while choosing several: the check is always drawn, as an empty ring until ticked. */
  selecting?: boolean;
  /** swatch only: height of the well in px. 62 by default, 84 in a three column grid. */
  slotHeight?: number;
  /** swatch only: no well behind the picture, so it can overflow, as the canvas patterns do. */
  bare?: boolean;
}

/** A named look to pick, shown as the thing itself. Selected is a white ring and a check that pops in. */
export function PresetTile({ preview, label, caption, selected = false, variant = 'sheet', shape = 'folder', selecting = false, slotHeight, bare = false, className, style, type = 'button', ...rest }: PresetTileProps) {
  return (
    <button type={type} aria-pressed={selected} aria-label={label} data-selecting={selecting || undefined}
      className={cx(styles.tile, styles[variant], styles[shape], bare && styles.bare, className)}
      style={slotHeight ? ({ '--slot-h': `${slotHeight}px`, ...style } as CSSProperties) : style} {...rest}>
      <span className={styles.slot}>{preview}</span>
      <span className={styles.name}>{label}</span>
      {caption && <span className={styles.caption}>{caption}</span>}
      <i className={styles.check} aria-hidden="true"><Icon name="check" size={14} strokeWidth={variant === 'picker' ? 2.6 : 2.4} /></i>
    </button>
  );
}
