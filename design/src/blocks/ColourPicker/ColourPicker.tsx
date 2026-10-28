import { useState } from 'react';
import { hexToHsv, hsvToHex } from '@/assets/wheel/colour';
import { Button } from '@/components/Button';
import { Swatch } from '@/components/Swatch';
import { TextField } from '@/components/TextField';
import { ALPHA_MAX, ALPHA_MIN } from '@/blocks/PenWheel/engine/geometry';
import { hexToRgba } from '@/blocks/PenWheel/engine/math';
import { rimIndexOf, type WheelModel } from '@/blocks/PenWheel/engine/model';
import { useWheelModel } from '@/blocks/PenWheel/useWheelModel';
import { WheelPanel } from '@/blocks/WheelPanel';
import { AddToSet } from './AddToSet';
import { ColourBar, SVSquare } from './parts';
import styles from './ColourPicker.module.css';

export interface ColourPickerProps {
  model: WheelModel;
  /** Edit one colour of a set instead of choosing the live ink. */
  edit?: { s: number; i: number };
  /** Done, or Save: the panel should close. */
  onClose?: () => void;
  /** Editing: Cancel was pressed. */
  onCancel?: () => void;
  /** Editing: Save was pressed. */
  onSaved?: () => void;
  onManage?: () => void;
  /** A colour was added to a set; turn the wheel's ring to it. */
  onFocusRim?: (index: number) => void;
  state?: 'open' | 'leave';
}

const WIDTH = 352;

/** Pick any colour. The wheel's ink follows as you drag; Add to palette keeps it in a set. */
export function ColourPicker({ model, edit, onClose, onCancel, onSaved, onManage, onFocusRim, state = 'open' }: ColourPickerProps) {
  useWheelModel(model);
  const editing = !!edit;
  const startHex = edit ? model.sets[edit.s]?.colours[edit.i] ?? model.setting.color : model.setting.color;
  const [[hue, sat, val], setHsv] = useState(() => hexToHsv(startHex));
  const [hexText, setHexText] = useState(startHex.slice(1).toUpperCase());
  const [target, setTarget] = useState(() => {
    const i = model.sets.findIndex((s) => s.colours.includes(model.setting.color.toUpperCase()));
    return i < 0 ? model.sets.length - 1 : i;
  });
  const [added, setAdded] = useState(false);
  const current = hsvToHex(hue, sat, val);

  const push = (h: number, s: number, v: number, fromHex = false) => {
    setHsv([h, s, v]);
    const c = hsvToHex(h, s, v);
    if (!fromHex) setHexText(c.slice(1));
    if (!editing) model.applyColor(c);
  };
  const alpha = model.alpha;
  const opacityFrac = (alpha - ALPHA_MIN) / (ALPHA_MAX - ALPHA_MIN);

  const add = () => {
    const sets = model.sets.map((s, i) => (i === target ? { ...s, colours: [...s.colours, current] } : s));
    model.setSets(sets);
    model.addRecent(current);
    onFocusRim?.(rimIndexOf(sets, target, sets[target].colours.length - 1));
    setAdded(true);
    window.setTimeout(() => setAdded(false), 900);
  };
  const save = () => {
    if (!edit) return;
    model.setSets(model.sets.map((s, i) => (i === edit.s ? { ...s, colours: s.colours.map((c, k) => (k === edit.i ? current : c)) } : s)));
    onSaved?.();
  };

  return (
    <WheelPanel width={WIDTH} variant="form" state={state} role="dialog" aria-label={editing ? 'Edit colour' : 'Custom colour'}>
      <SVSquare width={WIDTH - 44} height={180} hue={hue} sat={sat} val={val} onChange={(s, v) => push(hue, s, v)} />
      <ColourBar kind="hue" label="Hue" value={hue / 360} onChange={(x) => push(x * 360, sat, val)} />
      {!editing && (
        <ColourBar
          kind="opacity"
          label="Opacity"
          value={opacityFrac}
          gradient={`linear-gradient(90deg,${hexToRgba(current, 0.1)},${current})`}
          onChange={(x) => model.applyAlpha(Math.round(ALPHA_MIN + x * (ALPHA_MAX - ALPHA_MIN)))}
        />
      )}
      <div className={styles.hexRow}>
        <i className={styles.preview} style={{ background: current }} />
        <span className={styles.hash}>#</span>
        <TextField
          variant="inline"
          className={styles.field}
          label="Hex colour"
          maxLength={6}
          value={hexText}
          onChange={(e) => {
            const v = e.target.value.toUpperCase().replace(/[^0-9A-F]/g, '');
            setHexText(v);
            if (v.length === 6) {
              const [h, s, b] = hexToHsv('#' + v);
              push(h, s, b, true);
            }
          }}
        />
      </div>
      <div className={styles.recentGroup}>
        <div className={styles.cap}>Recent</div>
        <div className={styles.recent}>
          {model.recent.map((c) => (
            <span key={c} className={styles.rc}>
              <Swatch color={c} label={`Recent colour ${c}`} size="md" onClick={() => { const [h, s, v] = hexToHsv(c); push(h, s, v); }} />
            </span>
          ))}
        </div>
      </div>
      {!editing && <AddToSet sets={model.sets} target={target} onTarget={setTarget} added={added} onAdd={add} />}
      <div className={styles.footer}>
        {editing ? (
          <>
            <Button variant="text" onClick={onCancel}>Cancel</Button>
            <Button variant="primary" className={styles.done} onClick={save}>Save</Button>
          </>
        ) : (
          <>
            <Button variant="text" icon="palette" onClick={onManage}>Manage colours</Button>
            <Button variant="primary" className={styles.done} onClick={() => { model.addRecent(current); onClose?.(); }}>Done</Button>
          </>
        )}
      </div>
    </WheelPanel>
  );
}
