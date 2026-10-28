import { Icon, type IconName } from '@/assets/icons';
import { LAYER_ICON, layerLabel, SHADOW_NAMES, BLENDS, type Design, type Layer, type TextureKey } from '@/assets/studio';
import { IconButton } from '@/components/IconButton';
import { Select } from '@/components/Select';
import { cx } from '@/lib/cx';
import { BaseSections } from './BaseSections';
import { InspectorAction, TypeSections } from './LayerSections';
import { Row, Section, SegRow, SliderRow, type Edit } from './rows';
import styles from './Inspector.module.css';

export type { InspectorAction } from './LayerSections';

export interface InspectorProps {
  design: Design;
  /** The selected layer. Without one, the cover or folder settings show. */
  layer: Layer | null;
  /** A value changed. `final` is false while a slider or colour is still moving. */
  onEdit: Edit;
  onAction?: (a: InspectorAction) => void;
  onMaterial?: (key: TextureKey) => void;
  /** Whether the "name area" guide is drawn on a folder. */
  safe?: boolean;
  onSafe?: (on: boolean) => void;
  className?: string;
}

const ARRANGE: Array<[InspectorAction, IconName, string]> = [
  ['fwd', 'bring_front', 'Bring forward'], ['bwd', 'send_back', 'Send backward'], ['flipx', 'flip_h', 'Flip horizontally'],
  ['flipy', 'flip_v', 'Flip vertically'], ['ctrx', 'align_center', 'Centre horizontally'], ['ctry', 'align_center', 'Centre vertically'],
];
const SHADOWED = ['sticker', 'image', 'shape', 'card', 'icon', 'text'];

/** The right panel. A layer header stays pinned at the top while the rows scroll under it, and every row is a label and one control. */
export function Inspector({ design, layer: l, onEdit, onAction = () => undefined, onMaterial = () => undefined, safe = true, onSafe = () => undefined, className }: InspectorProps) {
  if (!l) {
    return (
      <div className={cx(styles.inspector, className)}>
        <BaseSections design={design} edit={onEdit} safe={safe} onSafe={onSafe} onMaterial={onMaterial} />
      </div>
    );
  }
  return (
    <div className={cx(styles.inspector, className)}>
      <header className={styles.head}>
        <span className={styles.headIcon}><Icon name={LAYER_ICON[l.type]} size={18} /></span>
        <b>{layerLabel(l, design)}</b>
        <span className={styles.actions}>
          <IconButton size="sm" icon="copy" label="Duplicate" onClick={() => onAction('dup')} />
          <IconButton size="sm" icon={l.lock ? 'lock' : 'unlock'} label={l.lock ? 'Unlock' : 'Lock'} onClick={() => onAction('lock')} />
          <IconButton size="sm" icon="trash" label="Delete" onClick={() => onAction('del')} />
        </span>
      </header>
      <TypeSections layer={l} design={design} edit={onEdit} onAction={onAction} />
      <Section><SliderRow label="Opacity" path="op" value={l.op} min={0} max={1} step={0.01} fmt={(v) => `${Math.round(v * 100)}%`} edit={onEdit} /></Section>
      {SHADOWED.includes(l.type) && <Section><SegRow label="Shadow" path="sh" value={l.sh} choices={SHADOW_NAMES.map((n, i) => ({ value: i, label: n }))} edit={onEdit} /></Section>}
      <Section>
        <Row label="Blend"><Select aria-label="Blend" options={BLENDS.map(([value, label]) => ({ value, label }))} value={l.blend} onChange={(v) => onEdit('blend', v, true)} /></Row>
      </Section>
      <Section title="Arrange">
        <SliderRow label="Rotate" path="r" value={l.r} min={-180} max={180} fmt={(v) => `${Math.round(v)}°`} edit={onEdit} />
        <div className={styles.arrange}>
          {ARRANGE.map(([a, icon, label]) => <IconButton key={a} size="sm" icon={icon} label={label} className={a === 'ctry' ? styles.turned : undefined} onClick={() => onAction(a)} />)}
        </div>
        {design.kind === 'folder' && (
          <SegRow label="Place" path="pop" value={l.pop || false} choices={[{ value: 'behind', label: 'Behind' }, { value: false, label: 'On front' }, { value: 'above', label: 'Above' }]} edit={onEdit} />
        )}
      </Section>
    </div>
  );
}
