import { FieldRow } from '@/components/FieldRow';
import { SegmentedControl } from '@/components/SegmentedControl';
import { Toggle } from '@/components/Toggle';
import type { BatteryAuto } from '../settingsTypes';
import { SectionHead } from './SectionHead';
import type { SectionProps } from './ToolbarSection';
import styles from './sections.module.css';

export function BatterySection({ values: v, onChange }: SectionProps) {
  return (
    <>
      <SectionHead>Battery saver</SectionHead>
      <div className={styles.rows}>
        <FieldRow plain label="Battery saver">
          <Toggle variant="marked" aria-label="Battery saver" checked={v.batterySaver} onChange={(batterySaver) => onChange({ batterySaver })} />
        </FieldRow>
        <FieldRow plain label="Switch on at" control="below">
          <SegmentedControl
            size="sm" fit aria-label="Switch on automatically" value={v.batteryAuto} onChange={(batteryAuto) => onChange({ batteryAuto: batteryAuto as BatteryAuto })}
            options={[{ value: 'off', label: 'Never' }, { value: '20', label: '20%' }, { value: '10', label: '10%' }]}
          />
        </FieldRow>
      </div>
    </>
  );
}
