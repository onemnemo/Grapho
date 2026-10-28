import { useEffect, useRef, useState, type ReactNode } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { ILLUS, outlineFilter } from '@/assets/studio';
import { Button } from '@/components/Button';
import { Dialog } from '@/components/Dialog';
import { Slider } from '@/components/Slider';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import { cutoutURL } from '@/lib/studio';
import styles from './BackgroundRemover.module.css';

export interface StickerResult { src: string; /** Outline in px: 0, 2 or 3. */ cut: number; removed: boolean; ratio: number }

export interface BackgroundRemoverProps {
  open: boolean;
  onClose: () => void;
  onBack?: () => void;
  onAdd?: (r: StickerResult) => void;
  /** The picture. A white page around an illustration is the case it is made for. */
  src?: string;
  /** Start from a later step, for stories. */
  initial?: 'original' | 'scanning' | 'removed';
  /** The tool that starts selected, for stories. */
  initialTool?: 'auto' | 'erase' | 'restore';
  placement?: OverlayPlacement;
}

const AMOUNT = { min: 12, max: 90 };
const BRUSH = { min: 8, max: 56 };
const OUTLINES = [{ value: '0', label: 'None' }, { value: '2', label: 'Thin' }, { value: '3', label: 'Thick' }];
type Tool = 'auto' | 'erase' | 'restore';
const TOOLS: Array<{ value: Tool; label: string; icon: IconName }> = [
  { value: 'auto', label: 'Auto', icon: 'sparkle' }, { value: 'erase', label: 'Erase', icon: 'eraser' }, { value: 'restore', label: 'Restore', icon: 'restore' },
];

export interface RemoverParts { title: string; body: ReactNode; actions: ReactNode }

/** The pieces of the remover, so a flow can show them in a dialog it already has open. */
export function useBackgroundRemover({ onClose, onBack, onAdd, src = ILLUS, initial = 'original', initialTool = 'auto' }: Pick<BackgroundRemoverProps, 'onClose' | 'onBack' | 'onAdd' | 'src' | 'initial' | 'initialTool'>): RemoverParts {
  const [phase, setPhase] = useState<'original' | 'scanning' | 'removed'>(initial === 'scanning' ? 'scanning' : initial);
  const [view, setView] = useState<'o' | 'c'>(initial === 'removed' ? 'c' : 'o');
  const [amount, setAmount] = useState(40);
  const [outline, setOutline] = useState('3');
  const [tool, setTool] = useState<Tool>(initialTool);
  const [brush, setBrush] = useState(26);
  const [at, setAt] = useState<{ x: number; y: number } | null>(null);
  const [cut, setCut] = useState<{ url: string; ratio: number } | null>(null);
  const timer = useRef<number>(undefined);
  const frame = useRef<number>(0);

  useEffect(() => { let live = true; cutoutURL(src, 420, amount).then((c) => { if (live) setCut(c); }); return () => { live = false; }; }, [src, amount]);
  useEffect(() => () => window.clearTimeout(timer.current), []);
  useEffect(() => {
    if (phase !== 'scanning') return;
    timer.current = window.setTimeout(() => { setPhase('removed'); setView('c'); }, 1100);
  }, [phase]);

  const removed = phase === 'removed';
  const showCut = removed && view === 'c';
  const cutStyle = showCut ? { filter: `${outlineFilter(Number(outline) * 1.1)} drop-shadow(0 6px 7px var(--st-sticker-shadow))` } : undefined;

  const actions = (
    <>
      <Button variant="text" onClick={onBack ?? onClose}>Back</Button>
      <Button variant="primary" onClick={() => onAdd?.({ src: removed && cut ? cut.url : src, cut: Number(outline), removed, ratio: removed && cut ? cut.ratio : 1 })}>Add sticker</Button>
    </>
  );
  const painting = removed && tool !== 'auto';
  const body = (
    <div className={styles.rm} data-cut={removed || undefined} data-original={view === 'o' || undefined} data-scan={phase === 'scanning' || undefined}>
      <div className={styles.stage} data-paint={painting || undefined}
        onPointerMove={(e) => { if (!painting) return; const r = e.currentTarget.getBoundingClientRect(); setAt({ x: e.clientX - r.left, y: e.clientY - r.top }); }}
        onPointerLeave={() => setAt(null)}>
        <div className={styles.pic}>
          <img className={styles.orig} src={src} alt="" />
          {cut && <img className={styles.cut} src={cut.url} alt="" style={cutStyle} />}
          <i className={styles.scan} />
        </div>
        {painting && at && <i className={styles.brush} data-tool={tool} style={{ left: at.x, top: at.y, width: brush, height: brush }} />}
        <button type="button" className={styles.compare} disabled={!removed} aria-pressed={view === 'o'} aria-label="Compare with the original"
          onPointerDown={() => setView('o')} onPointerUp={() => setView('c')} onPointerLeave={() => removed && setView('c')}
          onKeyDown={(e) => { if (e.key === ' ' || e.key === 'Enter') setView('o'); }} onKeyUp={() => removed && setView('c')}>
          <Icon name="compare" size={18} /><span>Compare</span>
        </button>
      </div>
      <div className={styles.tools} role="radiogroup" aria-label="Tool">
        {TOOLS.map((t) => (
          <button key={t.value} type="button" role="radio" aria-checked={tool === t.value} className={styles.tool} disabled={t.value !== 'auto' && !removed} onClick={() => setTool(t.value)}>
            <Icon name={t.icon} size={20} /><span>{t.label}</span>
          </button>
        ))}
      </div>
      <div className={styles.adjust}>
        {tool === 'auto' ? (
          removed ? (
            <>
              <span className={styles.label}>Strength</span>
              <Slider size="sm" aria-label="How much background to remove" className={styles.slide} min={AMOUNT.min} max={AMOUNT.max} value={amount}
                onChange={(v) => { cancelAnimationFrame(frame.current); frame.current = requestAnimationFrame(() => setAmount(v)); }} />
            </>
          ) : (
            <Button className={styles.go} disabled={phase === 'scanning'} onClick={() => { if (phase === 'original') setPhase('scanning'); }}>
              <Icon name="sparkle" size={18} />{phase === 'scanning' ? 'Removing' : 'Remove background'}
            </Button>
          )
        ) : (
          <>
            <span className={styles.label}>Brush</span>
            <span className={styles.dotWell}><i className={styles.dot} style={{ width: brush * 0.6, height: brush * 0.6 }} /></span>
            <Slider size="sm" aria-label="Brush size" className={styles.slide} min={BRUSH.min} max={BRUSH.max} value={brush} onChange={setBrush} />
          </>
        )}
      </div>
      <div className={styles.outline} role="radiogroup" aria-label="Outline">
        <span className={styles.label}>Outline</span>
        {OUTLINES.map((o) => (
          <button key={o.value} type="button" role="radio" aria-checked={outline === o.value} className={styles.chipBtn} onClick={() => setOutline(o.value)}>
            <i className={styles.glyph} data-w={o.value} />{o.label}
          </button>
        ))}
      </div>
    </div>
  );
  return { title: 'Remove background', body, actions };
}

/** Takes the white page away from a picture so it can sit on the cover as a sticker. Amount sets how much counts as background. */
export function BackgroundRemover({ open, onClose, placement = 'viewport', ...rest }: BackgroundRemoverProps) {
  const { title, body, actions } = useBackgroundRemover({ onClose, ...rest });
  return <Dialog open={open} onClose={onClose} placement={placement} width={560} density="compact" title={title} actions={actions}>{body}</Dialog>;
}

