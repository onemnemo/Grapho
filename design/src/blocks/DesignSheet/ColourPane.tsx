import { useState } from 'react';
import { Icon } from '@/assets/icons';
import { ColourDisc } from '@/components/ColourDisc';
import { Swatch } from '@/components/Swatch';
import styles from './panes.module.css';

export interface ColourPaneProps {
  /** Colours in reading order, filled row by row. */
  colours: Array<{ hex: string; label: string }>;
  columns: number;
  /** How many rows the tall custom button spans. */
  rows: number;
  value: string;
  /** The colour chosen in the picker, so the custom button shows it. */
  custom?: string | null;
  onPick: (hex: string) => void;
  onCustom: (hex: string) => void;
  className?: string;
}

/** A grid of colours with a tall rainbow button at the end. The button swaps the grid for a colour picker, with a way back. */
export function ColourPane({ colours, columns, rows, value, custom = null, onPick, onCustom }: ColourPaneProps) {
  const [picking, setPicking] = useState(false);
  if (picking) {
    return (
      <div className={styles.picker}>
        <button type="button" className={styles.back} onClick={() => setPicking(false)}><Icon name="chevron_left" size={16} />Swatches</button>
        <ColourDisc size={150} value={custom ?? value} onChange={(hex) => onCustom(hex)} />
      </div>
    );
  }
  return (
    <div className={styles.dots} role="radiogroup" aria-label="Colour" style={{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr)) 44px` }}>
      {colours.map((c) => (
        <Swatch key={c.hex} color={c.hex} label={c.label} check selected={!custom && c.hex.toLowerCase() === value.toLowerCase()} onClick={() => onPick(c.hex)} />
      ))}
      <button
        type="button" className={styles.custom} data-has={custom ? '' : undefined} data-selected={custom ? '' : undefined}
        aria-label="Custom colour" style={{ gridRow: `1 / ${rows + 1}`, gridColumn: columns + 1, ...(custom ? { '--c': custom } : null) } as React.CSSProperties}
        onClick={() => setPicking(true)}
      >
        <span><Icon name="plus" size={16} strokeWidth={2.2} /></span>
      </button>
    </div>
  );
}
