/* Pieces shared by the line and shape options popovers: a quiet label over a control, and the outline and width choices. */
import type { ReactNode } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { GlyphPicker } from '@/components/GlyphPicker';
import styles from './LineOptions.module.css';

export type OutlineKind = 'solid' | 'dashed' | 'dotted';

/** A control under a quiet 11px label. Spacing, not lines, separates one group from the next. */
export function OptionGroup({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className={styles.group}>
      <div className={styles.label}>{label}</div>
      {children}
    </div>
  );
}

/** A label with one small control at its end: a stepper or a switch. */
export function OptionRow({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className={styles.row}>
      <span className={styles.rowLabel}>{label}</span>
      {children}
    </div>
  );
}

const icons = (names: Array<[string, string, IconName]>) => names.map(([value, label, icon]) => ({ value, label, glyph: <Icon name={icon} size={20} /> }));

const OUTLINES = icons([['solid', 'Solid', 'outline_solid'], ['dashed', 'Dashed', 'outline_dashed'], ['dotted', 'Dotted', 'outline_dotted']]);

export function OutlineChoice({ value, onChange }: { value: OutlineKind; onChange: (v: OutlineKind) => void }) {
  return <GlyphPicker aria-label="Outline" value={value} onChange={(v) => onChange(v as OutlineKind)} options={OUTLINES} />;
}

/** Four widths, drawn as lines of increasing thickness. */
export const WIDTHS = [2, 4, 7, 12] as const;
export const nearestWidth = (w: number) => WIDTHS.reduce((a, b) => (Math.abs(b - w) < Math.abs(a - w) ? b : a));

const WIDTH_OPTIONS = WIDTHS.map((w) => ({
  value: String(w),
  label: `${w} dp`,
  glyph: (
    <svg width="28" height="14" viewBox="0 0 28 14" aria-hidden="true">
      <line x1="3" y1="7" x2="25" y2="7" stroke="currentColor" strokeWidth={Math.max(1.5, w * 0.8)} strokeLinecap="round" />
    </svg>
  ),
}));

export function WidthChoice({ value, onChange }: { value: number; onChange: (w: number) => void }) {
  return <GlyphPicker aria-label="Width" value={String(nearestWidth(value))} onChange={(v) => onChange(Number(v))} options={WIDTH_OPTIONS} />;
}
