import { useRef, useState, type RefObject } from 'react';
import { Icon } from '@/assets/icons';
import { GlyphPicker } from '@/components/GlyphPicker';
import { IconButton } from '@/components/IconButton';
import { Menu, MenuItem } from '@/components/Menu';
import { Popover, type PopoverProps } from '@/components/Popover';
import { OptionGroup, OutlineChoice, WidthChoice } from './OptionParts';
import { EndingGlyph, endings, type LineEnding, type LinePath, type LineValues } from './lineTypes';
import styles from './LineOptions.module.css';

export interface LineOptionsProps extends Omit<PopoverProps, 'children' | 'kind' | 'onChange' | 'anchorRef'> {
  values: LineValues;
  onChange: (next: LineValues) => void;
  /** The toolbar button that opens it. The popover hangs from it. */
  anchorRef: RefObject<HTMLElement | null>;
}

const PATHS = [
  { value: 'straight', label: 'Straight', glyph: <Icon name="path_straight" size={20} /> },
  { value: 'curved', label: 'Curved', glyph: <Icon name="path_curved" size={20} /> },
  { value: 'elbow', label: 'Elbow', glyph: <Icon name="path_elbow" size={20} /> },
];

/** A compact dropdown that shows the current ending as a drawing. The start one is mirrored so it points the way it will on the page. */
function CapMenu({ label, value, flip, onChange }: { label: string; value: LineEnding; flip: boolean; onChange: (e: LineEnding) => void }) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLButtonElement>(null);
  const name = endings.find((e) => e.value === value)?.label ?? '';
  return (
    <>
      <button ref={ref} type="button" className={styles.cap} aria-haspopup="menu" aria-expanded={open} aria-label={`${label}: ${name}`} onClick={() => setOpen(!open)}>
        <EndingGlyph ending={value} flip={flip} />
        <Icon name="chevron_down" size={14} />
      </button>
      <Popover open={open} onClose={() => setOpen(false)} anchorRef={ref} placement="bottom-start" offset={4}>
        <Menu label={label} onClose={() => setOpen(false)}>
          {endings.map((e) => (
            <MenuItem key={e.value} checked={e.value === value} onSelect={() => onChange(e.value)}>
              <span className={styles.glyph}><EndingGlyph ending={e.value} flip={flip} />{e.label}</span>
            </MenuItem>
          ))}
        </Menu>
      </Popover>
    </>
  );
}

/**
 * Options for the selected line or arrow, in a quiet popover that hangs from its toolbar button.
 * No title and no close: pressing away closes it. Everything is a drawing, so there are almost no words.
 */
export function LineOptions({ values: v, onChange, className, ...pop }: LineOptionsProps) {
  const set = (p: Partial<LineValues>) => onChange({ ...v, ...p });
  return (
    <Popover kind="compact" aria-label="Line options" className={[styles.pop, className].filter(Boolean).join(' ')} placement="bottom-start" {...pop}>
      <OptionGroup label="Path">
        <GlyphPicker aria-label="Path" value={v.path} onChange={(path) => set({ path: path as LinePath })} options={PATHS} />
      </OptionGroup>
      <OptionGroup label="Ends">
        <div className={styles.ends}>
          <CapMenu label="Start" value={v.start} flip onChange={(start) => set({ start })} />
          <IconButton icon="swap" label="Swap ends" size="sm" variant="ghost" onClick={() => set({ start: v.end, end: v.start })} />
          <CapMenu label="End" value={v.end} flip={false} onChange={(end) => set({ end })} />
        </div>
      </OptionGroup>
      <OptionGroup label="Outline"><OutlineChoice value={v.outline} onChange={(outline) => set({ outline })} /></OptionGroup>
      <OptionGroup label="Width"><WidthChoice value={v.width} onChange={(width) => set({ width })} /></OptionGroup>
    </Popover>
  );
}
