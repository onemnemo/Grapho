import type { CSSProperties, RefObject } from 'react';
import { OptionTile, OptionTileGroup } from '@/components/OptionTile';
import { Popover, type Placement } from '@/components/Popover';
import { SwatchRow, type SwatchOption } from '@/components/Swatch';
import { cx } from '@/lib/cx';
import styles from './PageBackground.module.css';

export type PagePattern = 'blank' | 'lined' | 'grid' | 'dotted';
export type PageColour = 'white' | 'cream' | 'slate' | 'dark' | 'black' | 'custom';

export interface PageBackgroundValue { pattern: PagePattern; colour: PageColour }

const PATTERNS: Array<{ value: PagePattern; label: string }> = [
  { value: 'blank', label: 'Blank' },
  { value: 'lined', label: 'Lined' },
  { value: 'grid', label: 'Grid' },
  { value: 'dotted', label: 'Dotted' },
];

const COLOURS: SwatchOption[] = [
  { value: 'white', color: 'var(--page-white)', label: 'White' },
  { value: 'cream', color: 'var(--page-cream)', label: 'Cream' },
  { value: 'slate', color: 'var(--page-slate)', label: 'Slate' },
  { value: 'dark', color: 'var(--page-dark)', label: 'Dark' },
  { value: 'black', color: 'var(--page-black)', label: 'Black' },
  { value: 'custom', kind: 'custom', label: 'Custom colour' },
];

const PAPER: Record<Exclude<PageColour, 'custom'>, string> = { white: 'var(--page-white)', cream: 'var(--page-cream)', slate: 'var(--page-slate)', dark: 'var(--page-dark)', black: 'var(--page-black)' };
const isDark = (c: PageColour) => c === 'slate' || c === 'dark' || c === 'black';

/** The little page behind each pattern option, drawn in the chosen paper colour. */
export function PagePreview({ pattern, colour = 'white', className }: { pattern: PagePattern; colour?: PageColour; className?: string }) {
  const paper = colour === 'custom' ? 'var(--page-cream)' : PAPER[colour];
  return <span className={cx(styles.mini, className)} data-pattern={pattern} data-tone={isDark(colour) ? 'dark' : 'light'} style={{ '--pp': paper } as CSSProperties} aria-hidden />;
}

export interface PageBackgroundPickerProps {
  value: PageBackgroundValue;
  onChange: (value: PageBackgroundValue) => void;
  /** The custom colour swatch was pressed: open the colour picker. */
  onCustomColour?: () => void;
}

/** Pattern as four tiles that show the pattern, colour as a row of five papers and a wheel for any other. */
export function PageBackgroundPicker({ value, onChange, onCustomColour }: PageBackgroundPickerProps) {
  return (
    <div className={styles.picker}>
      <section className={styles.sec}>
        <h4 className={styles.label}>Pattern</h4>
        <OptionTileGroup columns={4} aria-label="Pattern">
          {PATTERNS.map((p) => (
            <OptionTile key={p.value} layout="tile" label={p.label} selected={value.pattern === p.value} onClick={() => onChange({ ...value, pattern: p.value })} visual={<PagePreview pattern={p.value} colour={value.colour} />} />
          ))}
        </OptionTileGroup>
      </section>
      <section className={styles.sec}>
        <h4 className={styles.label}>Colour</h4>
        <SwatchRow
          aria-label="Page colour"
          swatches={COLOURS}
          value={value.colour}
          size="md"
          gap={12}
          onChange={(c) => (c === 'custom' ? onCustomColour?.() : onChange({ ...value, colour: c as PageColour }))}
        />
      </section>
    </div>
  );
}

export interface PageBackgroundProps extends PageBackgroundPickerProps {
  open: boolean;
  onClose: () => void;
  anchorRef: RefObject<HTMLElement | null>;
  placement?: Placement;
  boundary?: 'viewport' | 'container';
}

/** Page background as a popover from the menu row that opens it. Choices apply as you touch them. */
export function PageBackground({ open, onClose, anchorRef, placement = 'bottom-end', boundary = 'container', ...picker }: PageBackgroundProps) {
  return (
    <Popover open={open} onClose={onClose} anchorRef={anchorRef} placement={placement} boundary={boundary} kind="content" aria-label="Page background">
      <PageBackgroundPicker {...picker} />
    </Popover>
  );
}
