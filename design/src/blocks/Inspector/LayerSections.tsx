import { CLIPS, FONTS, PAINT, PAINTKEYS, PAT, PATKEYS, SHAPE_STROKEONLY, TEX, TEX_TINTS, WEIGHT_NAMES, paintCSS, type Design, type FontKey, type Layer, type PatternKey, type TextureKey } from '@/assets/studio';
import { ActionRow } from '@/components/ActionRow';
import { Select } from '@/components/Select';
import { TextArea } from '@/components/TextArea';
import { ColourRow, Row, Section, SegRow, SliderRow, ToggleRow, type Edit } from './rows';
import styles from './Inspector.module.css';

export type InspectorAction = 'dup' | 'lock' | 'del' | 'bind' | 'fill' | 'crop' | 'fwd' | 'bwd' | 'flipx' | 'flipy' | 'ctrx' | 'ctry';

const fmtN = (v: number) => String(Math.round(v * 100) / 100);
const times = (v: number) => `${v.toFixed(1)}×`;
const fontOptions = Object.entries(FONTS).map(([k, v]) => ({ value: k, label: v[1] }));

interface Props { layer: Layer; design: Design; edit: Edit; onAction: (a: InspectorAction) => void }

function TextBox({ layer: l, design, edit }: Pick<Props, 'layer' | 'design' | 'edit'>) {
  return <TextArea aria-label="Text" value={l.bind ? design.name : l.text || ''} onChange={(v) => edit('text', v, false)} onBlur={() => edit('text', l.bind ? design.name : l.text || '', true)} />;
}

/** The sections that depend on what kind of layer is selected. */
export function TypeSections({ layer: l, design, edit, onAction }: Props) {
  switch (l.type) {
    case 'text': {
      const weights = FONTS[(l.font || 'sans') as FontKey][2];
      return (
        <>
          <Section title="Text">
            <TextBox layer={l} design={design} edit={edit} />
            {!l.bind && <ActionRow icon="link" className={styles.bleed} onClick={() => onAction('bind')}>Use the notebook name</ActionRow>}
            <Row label="Font"><Select aria-label="Font" options={fontOptions} value={l.font || 'sans'} onChange={(v) => edit('font', v, true)} /></Row>
            <SegRow label="Weight" path="weight" value={weights.includes(l.weight || 0) ? l.weight : weights[0]} choices={weights.map((w) => ({ value: w, label: WEIGHT_NAMES[w] }))} edit={edit} />
            <SliderRow label="Size" path="size" value={l.size || 16} min={8} max={120} edit={edit} />
            <SliderRow label="Spacing" path="ls" value={l.ls || 0} min={-0.05} max={0.4} step={0.01} fmt={(v) => v.toFixed(2)} edit={edit} />
            <SegRow label="Align" path="align" value={l.align} choices={[{ value: 'left', icon: 'align_left', label: undefined }, { value: 'center', icon: 'align_center' }, { value: 'right', icon: 'align_right' }]} edit={edit} />
            <ToggleRow label="Uppercase" path="caps" value={!!l.caps} edit={edit} />
          </Section>
          <Section title="Colour"><ColourRow path="colour" value={l.colour} edit={edit} /></Section>
        </>
      );
    }
    case 'card':
      return (
        <>
          <Section title="Card">
            <TextBox layer={l} design={design} edit={edit} />
            <Row label="Font"><Select aria-label="Font" options={fontOptions} value={l.font || 'sans'} onChange={(v) => edit('font', v, true)} /></Row>
            <SliderRow label="Size" path="size" value={l.size || 16} min={8} max={80} edit={edit} />
            <SliderRow label="Corners" path="rad" value={l.rad || 0} min={0} max={24} edit={edit} />
          </Section>
          <Section title="Colour">
            <ColourRow label="Card" path="colour" value={l.colour} edit={edit} none="transparent" />
            <ColourRow label="Text" path="ink" value={l.ink} edit={edit} />
          </Section>
        </>
      );
    case 'shape': {
      const k = l.shape || 'rect';
      const so = !!SHAPE_STROKEONLY[k];
      const line = so || k === 'ring';
      return (
        <>
          <Section title={line ? 'Colour' : 'Fill'}><ColourRow path="fill" value={l.fill} edit={edit} none={line ? undefined : 'none'} /></Section>
          {!line ? (
            <Section title="Outline">
              <ColourRow path="stroke" value={l.stroke} edit={edit} none="none" />
              <SliderRow label="Width" path="sw" value={l.sw || 0} min={0} max={12} step={0.5} fmt={fmtN} edit={edit} />
              <ToggleRow label="Dashed" path="dash" value={!!l.dash} edit={edit} />
            </Section>
          ) : (
            <Section title="Line">
              <SliderRow label="Width" path="sw" value={l.sw || 6} min={1} max={16} step={0.5} fmt={fmtN} edit={edit} />
              <ToggleRow label="Dotted" path="dash" value={!!l.dash} edit={edit} />
            </Section>
          )}
          {(k === 'rect' || k === 'rounded') && <Section><SliderRow label="Corners" path="radius" value={l.radius || 0} min={0} max={60} edit={edit} /></Section>}
        </>
      );
    }
    case 'icon':
      return (
        <>
          <Section title="Colour"><ColourRow path="colour" value={l.colour} edit={edit} /></Section>
          <Section title="Style">
            <SliderRow label="Weight" path="sw" value={l.sw || 1.75} min={1} max={3} step={0.25} fmt={fmtN} edit={edit} />
            {!l.mask && <SegRow label="Style" path="soft" value={!!l.soft} choices={[{ value: false, label: 'Line' }, { value: true, label: 'Soft' }]} edit={edit} />}
          </Section>
        </>
      );
    case 'sticker':
      return <Section title="Sticker"><SegRow label="Border" path="cut" value={l.cut || 0} choices={[{ value: 0, label: 'None' }, { value: 2, label: 'Thin' }, { value: 4, label: 'Thick' }]} edit={edit} /></Section>;
    case 'image':
      return (
        <Section title="Image">
          <ActionRow icon="crop" className={styles.bleed} onClick={() => onAction('crop')}>Crop</ActionRow>
          <SliderRow label="Corners" path="rad" value={l.rad || 0} min={0} max={60} edit={edit} />
        </Section>
      );
    case 'tex': {
      const t = TEX[l.key as TextureKey];
      return (
        <Section>
          {t.mode === 'material' && l.key !== 'holo' && <ColourRow label="Tint" path="colour" value={l.colour} edit={edit} palette={TEX_TINTS} />}
          <SliderRow label="Scale" path="scale" value={l.scale || 1} min={0.5} max={2.5} step={0.1} fmt={times} edit={edit} />
          <ActionRow icon="fullscreen" className={styles.bleed} onClick={() => onAction('fill')}>Fill the {design.kind === 'cover' ? 'cover' : 'front'}</ActionRow>
        </Section>
      );
    }
    case 'pat':
      return (
        <>
          <Section title="Pattern">
            <Row label="Style"><Select aria-label="Pattern" options={PATKEYS.map((k) => ({ value: k, label: PAT[k].name }))} value={l.key as PatternKey} onChange={(v) => edit('key', v, true)} /></Row>
            <SliderRow label="Scale" path="scale" value={l.scale || 1} min={0.5} max={3} step={0.1} fmt={times} edit={edit} />
          </Section>
          <Section title="Ink"><ColourRow path="ink" value={l.ink} edit={edit} /></Section>
          <Section title="Background"><ColourRow path="colour" value={l.colour} edit={edit} none="transparent" /></Section>
        </>
      );
    case 'paint':
      return (
        <Section title="Painted fill">
          <div className={styles.paints} role="radiogroup" aria-label="Painted fill">
            {PAINTKEYS.map((k) => (
              <button key={k} type="button" role="radio" aria-checked={l.key === k} className={styles.paint} style={{ background: paintCSS(k) }} title={PAINT[k].name} aria-label={PAINT[k].name} onClick={() => edit('key', k, true)} />
            ))}
          </div>
        </Section>
      );
    default: {
      const len = l.clip ? CLIPS[l.clip].len : 8;
      return (
        <>
          <Section title="Video">
            {l.clip && <SliderRow label="Poster" path="poster" value={l.poster || 0} min={0} max={len} step={0.1} fmt={(v) => `${v.toFixed(1)}s`} edit={edit} />}
            <Row label="Trim"><span className={`${styles.value} ${styles.wideValue} ${styles.dim}`}>0:00 to 0:{String(len).padStart(2, '0')}</span></Row>
          </Section>
        </>
      );
    }
  }
}
