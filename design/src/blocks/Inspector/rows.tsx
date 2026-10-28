import { useRef, useState, type ReactNode } from 'react';
import { NEUTRAL_GREY, PALETTE } from '@/assets/studio';
import { ColourDisc } from '@/components/ColourDisc';
import { Popover } from '@/components/Popover';
import { Slider } from '@/components/Slider';
import { SegmentedControl, type SegmentOption } from '@/components/SegmentedControl';
import { SwatchRow, type SwatchOption } from '@/components/Swatch';
import { Toggle } from '@/components/Toggle';
import { cx } from '@/lib/cx';
import styles from './Inspector.module.css';

/** Where an edit goes. `final` is false while a slider or colour is still moving, true when it is finished. */
export type Edit = (path: string, value: unknown, final: boolean) => void;

export function Section({ title, children, className }: { title?: string; children: ReactNode; className?: string }) {
  return (
    <section className={cx(styles.section, className)}>
      {title && <h4>{title}</h4>}
      {children}
    </section>
  );
}

/** One row rhythm: a label of fixed width, then the control. A long segmented control drops under its label and takes the full width. */
export function Row({ label, children, stacked = false }: { label: string; children?: ReactNode; stacked?: boolean }) {
  return (
    <div className={cx(styles.row, stacked && styles.stacked)}>
      <span className={styles.label}>{label}</span>
      {children}
    </div>
  );
}

export function SliderRow({ label, path, value, min, max, step = 1, fmt = (v: number) => String(Math.round(v)), edit }: {
  label: string; path: string; value: number; min: number; max: number; step?: number; fmt?: (v: number) => string; edit: Edit;
}) {
  return (
    <Row label={label}>
      <Slider size="sm" aria-label={label} className={styles.slider} min={min} max={max} step={step} value={value} onChange={(v) => edit(path, v, false)} onChangeEnd={(v) => edit(path, v, true)} />
      <span className={styles.value}>{fmt(value)}</span>
    </Row>
  );
}

export function ToggleRow({ label, path, value, edit }: { label: string; path: string; value: boolean; edit: Edit }) {
  return (
    <div className={styles.row}>
      <span className={cx(styles.label, styles.wide)}>{label}</span>
      <Toggle size="sm" aria-label={label} checked={value} onChange={(v) => edit(path, v, true)} />
    </div>
  );
}

export interface SegChoice { value: string | number | boolean; label?: string; icon?: SegmentOption['icon']; title?: string }

/** A segmented control over values that are not strings: the value is matched through JSON. */
export function SegRow({ label, path, value, choices, edit, bare = false }: { label: string; path: string; value: unknown; choices: SegChoice[]; edit: Edit; bare?: boolean }) {
  const key = (v: unknown) => JSON.stringify(v);
  const long = choices.map((c) => c.label ?? '').join('').length > 14;
  const control = (
    <SegmentedControl
      size="xs" fit aria-label={label} iconsOnly={choices.every((c) => c.icon && !c.label)}
      value={key(value)} onChange={(k) => edit(path, JSON.parse(k), true)}
      options={choices.map((c) => ({ value: key(c.value), label: c.label, icon: c.icon }))}
    />
  );
  return bare ? control : <Row label={label} stacked={long}>{control}</Row>;
}

const PALETTE_NAMES = (c: string) => c;

/** Swatches for one colour, ending in the rainbow ring that opens the colour picker. */
export function ColourRow({ label, path, value, edit, palette = PALETTE, none, pickerTitle = 'Colour' }: {
  label?: string; path: string; value: string | null | undefined; edit: Edit; palette?: string[]; none?: 'none' | 'transparent'; pickerTitle?: string;
}) {
  const anchor = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const v = (value ?? '').toString();
  const lc = v.toLowerCase();
  const isNone = !v || v === 'none' || v === 'transparent';
  const matched = isNone && none ? 'none' : palette.find((c) => c.toLowerCase() === lc);
  const custom = /^#/.test(v) && !matched;
  const swatches: SwatchOption[] = [
    ...(none ? [{ value: 'none', kind: 'none' as const, label: 'None' }] : []),
    ...palette.map((c) => ({ value: c, color: c, label: PALETTE_NAMES(c) })),
    { value: '__custom', kind: 'custom' as const, color: /^#/.test(v) ? v : undefined, label: 'Custom colour' },
  ];
  return (
    <>
      {label && <Row label={label} />}
      <div ref={anchor} className={styles.swatches}>
        <SwatchRow
          aria-label={label ?? 'Colour'} size="sm" gap={10} swatches={swatches}
          value={custom ? '__custom' : (matched as string | undefined) ?? ''}
          onChange={(c) => (c === '__custom' ? setOpen(true) : edit(path, c === 'none' ? none : c, true))}
        />
      </div>
      <Popover open={open} onClose={() => { setOpen(false); edit(path, value ?? NEUTRAL_GREY, true); }} anchorRef={anchor} placement="left-start" kind="content" offset={12} className={styles.pop} aria-label={pickerTitle}>
        <div className={styles.popTitle}>{pickerTitle}</div>
        <ColourDisc size={200} value={/^#/.test(v) ? v : NEUTRAL_GREY} onChange={(c, final) => edit(path, c, final)} />
      </Popover>
    </>
  );
}

export function Note({ children }: { children: ReactNode }) { return <p className={styles.note}>{children}</p>; }
