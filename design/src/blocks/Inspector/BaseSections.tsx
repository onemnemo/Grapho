import { COVERCOL, FOLDER_BACKS, FOLDER_FRONTS, FOLDER_TABS, STRAP_COLOURS, STRAP_DARK, TEX, type Design, type TextureKey } from '@/assets/studio';
import { ActionRow } from '@/components/ActionRow';
import { ColourRow, Row, Section, SegRow, SliderRow, ToggleRow, type Edit } from './rows';
import styles from './Inspector.module.css';

const COVER_MATERIALS: TextureKey[] = ['paper', 'linen', 'leather', 'felt', 'kraft', 'cork', 'grain'];
const FOLDER_MATERIALS: TextureKey[] = ['linen', 'leather', 'felt', 'paper', 'marble', 'grain'];
const deg = (v: number) => `${Math.round(v)}°`;

function Materials({ keys, onMaterial }: { keys: TextureKey[]; onMaterial: (k: TextureKey) => void }) {
  return (
    <Section title="Add material">
      <div className={styles.acts}>{keys.map((k) => <ActionRow key={k} icon="plus" onClick={() => onMaterial(k)}>{TEX[k].name}</ActionRow>)}</div>
    </Section>
  );
}

/** Settings for the cover or folder itself, shown when no layer is selected. */
export function BaseSections({ design: d, edit, safe, onSafe, onMaterial }: { design: Design; edit: Edit; safe: boolean; onSafe: (on: boolean) => void; onMaterial: (k: TextureKey) => void }) {
  const b = d.base;
  if (d.kind === 'cover') {
    const c = d.cover ?? {};
    return (
      <>
        <Section title="Colour">
          <ColourRow path="base.c1" value={b.c1} edit={edit} palette={COVERCOL} />
          <ToggleRow label="Gradient" path="base.grad" value={!!b.c2} edit={edit} />
          {b.c2 && (
            <>
              <Row label="To" />
              <ColourRow path="base.c2" value={b.c2} edit={edit} palette={COVERCOL} />
              <SliderRow label="Angle" path="base.ang" value={b.ang ?? 160} min={0} max={360} step={5} fmt={deg} edit={edit} />
            </>
          )}
        </Section>
        <Section title="Shape"><SliderRow label="Corners" path="cover.rad" value={c.rad ?? 14} min={0} max={40} edit={edit} /></Section>
        <Section title="Binding">
          <ToggleRow label="Spine highlight" path="cover.spine" value={!!c.spine} edit={edit} />
          <ColourRow
            label="Strap" path="cover.strap" none="none" palette={STRAP_COLOURS} pickerTitle="Strap colour"
            value={c.strap === true ? STRAP_DARK : c.strap || 'none'}
            edit={(path, v, final) => edit(path, v === STRAP_DARK ? true : v === 'none' || v === 'transparent' ? false : v, final)}
          />
        </Section>
        <Materials keys={COVER_MATERIALS} onMaterial={onMaterial} />
      </>
    );
  }
  const f = d.folder!;
  return (
    <>
      <Section title="Back and tab">
        <ColourRow path="folder.back" value={f.back} edit={edit} palette={FOLDER_BACKS} />
        <ToggleRow label="Different tab colour" path="folder.tabc" value={!!f.tab} edit={edit} />
        {f.tab && <ColourRow path="folder.tab" value={f.tab} edit={edit} palette={FOLDER_TABS} />}
      </Section>
      <Section title="Front">
        <ColourRow path="base.c1" value={b.c1} edit={edit} palette={FOLDER_FRONTS} />
        <ToggleRow label="Soft gradient" path="base.grad" value={!!b.c2} edit={edit} />
      </Section>
      <Section title="Name">
        <SegRow label="Place" path="folder.name" value={f.name} choices={[{ value: 'left', label: 'Left' }, { value: 'centre', label: 'Centre' }, { value: 'below', label: 'Below' }, { value: 'hidden', label: 'Hidden' }]} edit={edit} />
        <SegRow label="Colour" path="folder.ink" value={f.ink} choices={[{ value: 'light', label: 'Light' }, { value: 'dark', label: 'Dark' }]} edit={edit} />
        <ToggleRow label="Show name area" path="safe" value={safe} edit={(_p, v) => onSafe(!!v)} />
      </Section>
      <Section title="Papers"><SegRow label="Peeking" path="folder.papersN" value={(f.papers || []).length} choices={[0, 1, 2, 3].map((n) => ({ value: n, label: String(n) }))} edit={edit} /></Section>
      <Materials keys={FOLDER_MATERIALS} onMaterial={onMaterial} />
    </>
  );
}
