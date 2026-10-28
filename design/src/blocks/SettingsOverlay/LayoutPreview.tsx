import type { BarPosition, BarVariant } from '../NoteBar';
import { cx } from '@/lib/cx';
import styles from './LayoutPreview.module.css';

export interface LayoutPreviewProps {
  /** Draws the toolbar in this style and place. Leave out for a page with no bar. */
  bar?: { variant: BarVariant; position: BarPosition };
  /** Draws the pen wheel peeking in from this edge. */
  wheel?: 'left' | 'right' | 'bottom';
  /** lg is wide (21 by 9) for the style cards; md is 16 by 9; the default 10 by 7 is for the small tiles. */
  ratio?: 'lg' | 'md' | 'sm';
}

/** A small picture of a ruled page with the toolbar and the wheel placed on it. Used as the visual of a choice. */
export function LayoutPreview({ bar, wheel, ratio = 'sm' }: LayoutPreviewProps) {
  return (
    <span className={cx(styles.mini, styles[ratio])} data-w={wheel} aria-hidden="true">
      {bar && <i className={cx(styles.bar, bar.variant === 'pill' && styles.pill)} data-pos={bar.position} />}
      {wheel && <b />}
    </span>
  );
}
