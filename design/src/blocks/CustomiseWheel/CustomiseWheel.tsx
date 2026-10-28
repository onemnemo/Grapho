import { useState } from 'react';
import { Button } from '@/components/Button';
import { SegmentedControl } from '@/components/SegmentedControl';
import type { PenPreset, WheelModel } from '@/blocks/PenWheel/engine/model';
import { useWheelModel } from '@/blocks/PenWheel/useWheelModel';
import { WheelPanel } from '@/blocks/WheelPanel';
import { PenRows } from './PenRows';
import { ToolRows } from './ToolRows';
import styles from './CustomiseWheel.module.css';

export interface CustomiseWheelProps {
  model: WheelModel;
  /** Which tab opens first. */
  tab?: 'tools' | 'pens';
  /** Pen whose editor starts open. */
  openPen?: number | null;
  /** Tallest the list may grow before it scrolls, in dp. */
  maxBody?: number;
  onClose?: () => void;
  onUndoable?: (text: string, undo: () => void) => void;
  /** Share the chosen pens as a pack. */
  onSharePens?: (pens: PenPreset[]) => void;
  /** Import a pack of pens. */
  onImportPens?: () => void;
  state?: 'open' | 'leave';
}

/** Choose and order the tools on the wheel, and edit your pens. */
export function CustomiseWheel({ model, tab = 'tools', openPen = null, maxBody = 540, onClose, onUndoable, onSharePens, onImportPens, state = 'open' }: CustomiseWheelProps) {
  useWheelModel(model);
  const [current, setCurrent] = useState(tab);
  const [open, setOpen] = useState<string | null>(openPen == null ? null : `pen${openPen}`);
  return (
    <WheelPanel width={360} variant="dialog" state={state} role="dialog" aria-label="Customise wheel">
      <SegmentedControl
        fit
        aria-label="Customise"
        options={[{ value: 'tools', label: 'Tools' }, { value: 'pens', label: 'Pens' }]}
        value={current}
        onChange={(v) => { setCurrent(v as 'tools' | 'pens'); setOpen(null); }}
      />
      <div className={styles.body} style={{ maxHeight: maxBody }}>
        {current === 'pens' ? <PenRows model={model} open={open} onOpen={setOpen} onUndoable={onUndoable} onShare={onSharePens} onImport={onImportPens} /> : <ToolRows model={model} />}
      </div>
      <Button block variant="primary" onClick={onClose}>Done</Button>
    </WheelPanel>
  );
}
