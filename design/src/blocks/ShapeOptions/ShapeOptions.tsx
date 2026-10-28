import type { RefObject } from 'react';
import { Icon } from '@/assets/icons';
import { GlyphPicker } from '@/components/GlyphPicker';
import { Popover, type PopoverProps } from '@/components/Popover';
import { Slider } from '@/components/Slider';
import { Stepper } from '@/components/Stepper';
import { Toggle } from '@/components/Toggle';
import { OptionGroup, OptionRow, OutlineChoice, WidthChoice } from '../LineOptions/OptionParts';
import styles from '../LineOptions/LineOptions.module.css';
import type { ShapeKind, ShapeValues } from './shapeTypes';

export interface ShapeOptionsProps extends Omit<PopoverProps, 'children' | 'kind' | 'onChange' | 'anchorRef'> {
  values: ShapeValues;
  onChange: (next: ShapeValues) => void;
  /** The toolbar button that opens it. */
  anchorRef: RefObject<HTMLElement | null>;
}

const KINDS = [
  { value: 'star', label: 'Star', glyph: <Icon name="star" size={20} /> },
  { value: 'polygon', label: 'Polygon', glyph: <Icon name="polygon" size={20} /> },
  { value: 'box', label: 'Box', glyph: <Icon name="rect" size={20} /> },
  { value: 'oval', label: 'Oval', glyph: <Icon name="ellipse" size={20} /> },
];

/** Options for the selected shape, in the same quiet popover as the line options. Only the rows that fit the shape show; every change applies live. */
export function ShapeOptions({ values: v, onChange, className, ...pop }: ShapeOptionsProps) {
  const set = (p: Partial<ShapeValues>) => onChange({ ...v, ...p });
  return (
    <Popover kind="compact" aria-label="Shape options" className={[styles.pop, className].filter(Boolean).join(' ')} placement="bottom-start" {...pop}>
      <OptionGroup label="Shape">
        <GlyphPicker aria-label="Shape" value={v.kind} onChange={(k) => set({ kind: k as ShapeKind })} options={KINDS} />
      </OptionGroup>
      {v.kind === 'star' && (
        <>
          <OptionRow label="Points"><Stepper aria-label="Points" min={3} max={12} value={v.points} onChange={(points) => set({ points })} /></OptionRow>
          <OptionGroup label="Sharpness"><Slider aria-label="Sharpness" min={10} max={90} unit="%" bubble value={v.sharpness} onChange={(sharpness) => set({ sharpness })} /></OptionGroup>
        </>
      )}
      {v.kind === 'polygon' && <OptionRow label="Sides"><Stepper aria-label="Sides" min={3} max={12} value={v.sides} onChange={(sides) => set({ sides })} /></OptionRow>}
      {v.kind === 'box' && <OptionGroup label="Corners"><Slider aria-label="Corner radius" min={0} max={50} unit="dp" bubble value={v.radius} onChange={(radius) => set({ radius })} /></OptionGroup>}
      <OptionGroup label="Outline"><OutlineChoice value={v.outline} onChange={(outline) => set({ outline })} /></OptionGroup>
      <OptionGroup label="Width"><WidthChoice value={v.width} onChange={(width) => set({ width })} /></OptionGroup>
      <OptionRow label="Fill"><Toggle aria-label="Fill" checked={v.fill} onChange={(fill) => set({ fill })} /></OptionRow>
    </Popover>
  );
}
