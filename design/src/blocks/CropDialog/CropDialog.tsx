import { useLayoutEffect, useRef, useState, type PointerEvent, type ReactNode } from 'react';
import { clamp, PHOTOS } from '@/assets/studio';
import { Button } from '@/components/Button';
import { Chip } from '@/components/Chip';
import { Dialog } from '@/components/Dialog';
import { Slider } from '@/components/Slider';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import styles from './CropDialog.module.css';

export interface Crop { z: number; px: number; py: number }

export interface CropDialogProps {
  open: boolean;
  onClose: () => void;
  src?: string;
  /** Aspect of the frame to start with. Defaults to the picture's own. */
  aspect?: number;
  crop?: Crop;
  /** Name and shape of the cover or folder front, offered as the last aspect. */
  frameLabel?: 'Cover' | 'Front';
  frameAspect?: number;
  /** adding: the dialog title is "Crop image" and the main button says Add. */
  mode?: 'edit' | 'adding';
  onBack?: () => void;
  onApply?: (crop: Crop, aspect: number) => void;
  placement?: OverlayPlacement;
}

const FRAME_ASPECT = { Cover: 0.75, Front: 480 / 208 };

export interface CropParts { title: string; body: ReactNode; actions: ReactNode }

/** The pieces of the crop dialog, so a flow can show them in a dialog it already has open. */
export function useCropper({ onClose, src = PHOTOS.hills.src, aspect, crop, frameLabel = 'Cover', frameAspect, mode = 'edit', onBack, onApply }: Omit<CropDialogProps, 'open' | 'placement'>): CropParts {
  const [nat, setNat] = useState(1.5);
  const [asp, setAsp] = useState(aspect ?? 1.5);
  const [cr, setCr] = useState<Crop>({ z: 1, px: 0, py: 0, ...crop });
  const [drag, setDrag] = useState(false);
  const [stageW, setStageW] = useState(450);
  const stage = useRef<HTMLDivElement>(null);
  const frame = useRef<HTMLDivElement>(null);
  const pd = useRef<{ x: number; y: number; px: number; py: number; w: number; h: number } | null>(null);

  useLayoutEffect(() => {
    const im = new Image();
    im.onload = () => { const r = im.naturalWidth / im.naturalHeight; setNat(r); if (aspect == null) setAsp(r); };
    im.src = src;
  }, [src, aspect]);
  useLayoutEffect(() => { if (stage.current) setStageW(stage.current.clientWidth); }, []);

  const fa = frameAspect ?? FRAME_ASPECT[frameLabel];
  const aspects: Array<[string, number]> = [['Original', nat], ['1:1', 1], ['4:3', 4 / 3], ['3:4', 3 / 4], ['16:9', 16 / 9], [frameLabel, fa]];
  const mw = Math.min(450, stageW - 8);
  let w = mw, h = w / asp;
  if (h > 280) { h = 280; w = h * asp; }

  const limit = (c: Crop): Crop => { const m = (c.z - 1) * 50; return { ...c, px: clamp(c.px, -m, m), py: clamp(c.py, -m, m) }; };
  const down = (e: PointerEvent<HTMLDivElement>) => {
    const r = e.currentTarget.getBoundingClientRect();
    pd.current = { x: e.clientX, y: e.clientY, px: cr.px, py: cr.py, w: r.width, h: r.height };
    e.currentTarget.setPointerCapture(e.pointerId);
    setDrag(true);
  };
  const move = (e: PointerEvent<HTMLDivElement>) => {
    const p = pd.current;
    if (!p) return;
    setCr((c) => limit({ ...c, px: p.px + ((e.clientX - p.x) / p.w) * 100, py: p.py + ((e.clientY - p.y) / p.h) * 100 }));
  };
  const up = () => { pd.current = null; setDrag(false); };

  const actions = (
    <>
      <Button variant="text" onClick={onBack ?? onClose}>{onBack ? 'Back' : 'Cancel'}</Button>
      <Button variant="primary" onClick={() => onApply?.(cr, asp)}>{mode === 'adding' ? 'Add' : 'Apply'}</Button>
    </>
  );
  const body = (
    <>
      <div ref={stage} className={styles.stage}>
        <div ref={frame} className={styles.frame} data-drag={drag || undefined} style={{ width: w, height: h }} onPointerDown={down} onPointerMove={move} onPointerUp={up} onPointerCancel={up}>
          <img src={src} alt="" draggable={false} style={{ transform: `translate(${cr.px}%,${cr.py}%) scale(${cr.z})` }} />
        </div>
      </div>
      <div className={styles.chips} role="group" aria-label="Shape">
        {aspects.map(([n, a]) => (
          <Chip key={n} size="sm" selected={Math.abs(a - asp) < 0.01} onChange={() => setAsp(a)}>{n}</Chip>
        ))}
      </div>
      <div className={styles.row}>
        <span className={styles.label}>Zoom</span>
        <Slider size="sm" aria-label="Zoom" className={styles.zoom} min={1} max={4} step={0.01} value={cr.z} onChange={(z) => setCr((c) => limit({ ...c, z }))} />
        <span className={styles.value}>{cr.z.toFixed(1)}{'×'}</span>
      </div>
    </>
  );
  return { title: mode === 'adding' ? 'Crop image' : 'Crop', body, actions };
}

/** Pan and zoom a picture inside a frame, and pick the shape of the frame. The thirds grid shows where the picture will be cut. */
export function CropDialog({ open, onClose, placement = 'viewport', ...rest }: CropDialogProps) {
  const { title, body, actions } = useCropper({ onClose, ...rest });
  return <Dialog open={open} onClose={onClose} placement={placement} width={520} density="compact" title={title} actions={actions}>{body}</Dialog>;
}
