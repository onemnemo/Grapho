import { FieldRow } from '@/components/FieldRow';
import { OptionTile, OptionTileGroup } from '@/components/OptionTile';
import { SegmentedControl } from '@/components/SegmentedControl';
import { LayoutPreview } from '../LayoutPreview';
import type { ShapeDetection, WheelEdge } from '../settingsTypes';
import { SectionHead } from './SectionHead';
import type { SectionProps } from './ToolbarSection';
import styles from './sections.module.css';

const EDGES: WheelEdge[] = ['left', 'right', 'bottom'];
const MODES: Array<{ value: ShapeDetection; label: string }> = [
  { value: 'suggest', label: 'Suggest' },
  { value: 'snap', label: 'Snap on hold' },
  { value: 'off', label: 'Off' },
];
/** The one thing the mode names do not say: how a shape is accepted. */
const EXPLAIN: Partial<Record<ShapeDetection, string>> = {
  suggest: 'Tap the chip after a stroke to make it a clean shape',
  snap: 'Hold the pen still at the end of a stroke',
};

export function PenSection({ values: v, onChange }: SectionProps) {
  return (
    <>
      <SectionHead>Pen wheel</SectionHead>
      <OptionTileGroup columns={3} aria-label="Pen wheel edge">
        {EDGES.map((e) => (
          <OptionTile key={e} layout="tile" selected={v.wheelEdge === e} onClick={() => onChange({ wheelEdge: e })} label={e[0].toUpperCase() + e.slice(1)} visual={<LayoutPreview ratio="md" wheel={e} />} />
        ))}
      </OptionTileGroup>

      <SectionHead>Shapes</SectionHead>
      <div className={styles.rows}>
        <FieldRow plain label="Shape detection" hint={EXPLAIN[v.detection]} control="below">
          <SegmentedControl size="sm" fit aria-label="Shape detection" value={v.detection} onChange={(d) => onChange({ detection: d as ShapeDetection })} options={MODES} />
        </FieldRow>
      </div>
    </>
  );
}
