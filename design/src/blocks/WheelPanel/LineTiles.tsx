import type { Line } from '@/blocks/PenWheel/engine/model';
import { GlyphPicker } from '@/components/GlyphPicker';
import { cx } from '@/lib/cx';
import styles from './WheelPanel.module.css';

const DEFS: Array<{ value: Line; label: string; dash?: string }> = [
  { value: 'solid', label: 'Solid line' },
  { value: 'dashed', label: 'Dashed line', dash: '5 5' },
  { value: 'dotted', label: 'Dotted line', dash: '0.1 5.5' },
];

const OPTIONS = DEFS.map((d) => ({
  value: d.value,
  label: d.label,
  glyph: (
    <svg width="36" height="12" viewBox="0 0 36 12" aria-hidden="true">
      <line x1="2" y1="6" x2="34" y2="6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeDasharray={d.dash} />
    </svg>
  ),
}));

export interface LineTilesProps {
  value: Line;
  onChange: (line: Line) => void;
  /** The pen list shows them in a 40 high strip. */
  inPanel?: boolean;
  className?: string;
}

/** Solid, dashed and dotted, as three drawings: a soft fill under the pointer and on the chosen one. */
export function LineTiles({ value, onChange, inPanel = false, className }: LineTilesProps) {
  return <GlyphPicker aria-label="Line style" className={cx(inPanel && styles.linesIn, className)} value={value} onChange={(v) => onChange(v as Line)} options={OPTIONS} />;
}
