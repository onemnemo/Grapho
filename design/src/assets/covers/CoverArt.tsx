import { useId, type HTMLAttributes } from 'react';
import { COVER_ART, COVER_COLOURS, type CoverPreset } from './coverPresets';
import { TextureLayer, PatternLayer, type CoverPattern, type CoverTexture } from './coverFinish';
import styles from './CoverArt.module.css';

export interface CoverArtProps extends Omit<HTMLAttributes<HTMLDivElement>, 'color'> {
  /** Ready-made artwork. 'plain' is a bare colour. */
  preset?: CoverPreset;
  /** Base colour. Defaults to the preset's own colour. */
  color?: string;
  /** Tiled pattern over the colour, under the artwork. */
  pattern?: CoverPattern;
  /** Colour of the pattern marks. */
  patternColor?: string;
  /** Material finish over everything but the label. */
  texture?: CoverTexture;
  /** Draw the preset's drawing. Turn off to show only colour, pattern and texture. */
  artwork?: boolean;
  /** Handwritten label tab at the top, set in Caveat. */
  label?: string;
  /** Lighter edge down the left, where the pages are bound. */
  spine?: boolean;
  /** Dark elastic strap near the right edge. */
  strap?: boolean;
}

/** Notebook cover. It fills its parent (give the parent a 3:4 box) and scales its label with the width. */
export function CoverArt({
  preset = 'plain',
  color,
  pattern = 'none',
  patternColor,
  texture = 'none',
  artwork = true,
  label,
  spine = true,
  strap = true,
  className,
  style,
  ...rest
}: CoverArtProps) {
  const id = useId().replace(/:/g, '');
  const base = color ?? COVER_COLOURS[preset];
  return (
    <div
      className={[styles.cover, className].filter(Boolean).join(' ')}
      style={{ ['--cover-base' as string]: base, ...style }}
      {...rest}
    >
      {pattern !== 'none' && <PatternLayer id={id} pattern={pattern} colour={patternColor} className={styles.layer} />}
      {artwork && (
        <svg className={styles.layer} viewBox="0 0 120 160" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
          {COVER_ART[preset](id)}
        </svg>
      )}
      {texture !== 'none' && <TextureLayer id={id} texture={texture} className={styles.layer} />}
      {spine && <span className={styles.spine} />}
      {strap && <span className={styles.strap} />}
      {label ? <span className={styles.label}>{label}</span> : null}
    </div>
  );
}
