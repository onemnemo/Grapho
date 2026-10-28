import { Icon } from '@/assets/icons';
import { FieldRow } from '@/components/FieldRow';
import { OptionTile, OptionTileGroup } from '@/components/OptionTile';
import { SegmentedControl } from '@/components/SegmentedControl';
import { Toggle } from '@/components/Toggle';
import type { BarPosition, BarVariant } from '../../NoteBar';
import { LayoutPreview } from '../LayoutPreview';
import type { AutoHide, SettingsValues } from '../settingsTypes';
import { SectionHead } from './SectionHead';
import styles from './sections.module.css';

export interface SectionProps {
  values: SettingsValues;
  onChange: (patch: Partial<SettingsValues>) => void;
  phone: boolean;
}

const POSITIONS: BarPosition[] = ['top', 'bottom', 'left', 'right'];
const cap = (s: string) => s[0].toUpperCase() + s.slice(1);

export function ToolbarSection({ values: v, onChange, phone, onEditTools }: SectionProps & { onEditTools?: () => void }) {
  return (
    <>
      <SectionHead>Toolbar</SectionHead>
      <OptionTileGroup columns={2} aria-label="Toolbar style">
        {([['pill', 'Floating bar'], ['line', 'Full line']] as Array<[BarVariant, string]>).map(([id, label]) => (
          <OptionTile key={id} selected={v.barStyle === id} onClick={() => onChange({ barStyle: id })} label={label} visual={<LayoutPreview ratio="lg" bar={{ variant: id, position: 'top' }} />} />
        ))}
      </OptionTileGroup>

      <SectionHead>Position</SectionHead>
      <OptionTileGroup columns={phone ? 2 : 4} aria-label="Toolbar position">
        {POSITIONS.map((p) => (
          <OptionTile key={p} layout="tile" selected={v.barPosition === p} onClick={() => onChange({ barPosition: p })} label={cap(p)} visual={<LayoutPreview ratio="md" bar={{ variant: v.barStyle, position: p }} />} />
        ))}
      </OptionTileGroup>

      <SectionHead>Behaviour</SectionHead>
      <div className={styles.rows}>
        <FieldRow plain label="Auto-hide" control="below">
          <SegmentedControl
            size="sm" fit aria-label="Auto-hide" value={v.autoHide} onChange={(autoHide) => onChange({ autoHide: autoHide as AutoHide })}
            options={[{ value: 'off', label: 'Off' }, { value: 'while', label: 'While writing' }, { value: 'always', label: 'Always' }]}
          />
        </FieldRow>
        <FieldRow plain label="Show note title">
          <Toggle variant="marked" aria-label="Show note title" checked={v.showTitle} onChange={(showTitle) => onChange({ showTitle })} />
        </FieldRow>
        <FieldRow plain label="Tools in the bar" onClick={onEditTools ?? (() => {})}>
          <Icon name="chevron_right" size={18} />
        </FieldRow>
      </div>
    </>
  );
}
