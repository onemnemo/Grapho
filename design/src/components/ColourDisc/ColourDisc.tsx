import { useEffect, useRef, useState, type PointerEvent as RPointerEvent } from 'react';
import { Icon } from '@/assets/icons';
import { SAVED_COLOURS, clamp, hex2hsv, hsv2hex } from '@/assets/studio';
import { cx } from '@/lib/cx';
import { useT } from '@/i18n';
import styles from './ColourDisc.module.css';

export interface ColourDiscProps {
  value: string;
  /** Called while dragging with final false, and when the pointer is released, a colour is picked or a hex typed, with final true. */
  onChange: (hex: string, final: boolean) => void;
  /** Diameter of the disc in px. */
  size?: number;
  /** Quick colours under the hex field. */
  saved?: string[];
  className?: string;
}

/** One colour picker: a disc for hue and saturation, a brightness bar, a hex field, the dropper and saved colours. */
export function ColourDisc({ value, onChange, size = 196, saved = SAVED_COLOURS, className }: ColourDiscProps) {
  const t = useT();
  const [hsv, setHsv] = useState<[number, number, number]>(() => hex2hsv(value));
  const [typing, setTyping] = useState<string | null>(null);
  const [drag, setDrag] = useState<'disc' | 'bar' | null>(null);
  const disc = useRef<HTMLDivElement>(null);
  const bar = useRef<HTMLDivElement>(null);
  const last = useRef(value);
  const [h, s, v] = hsv;
  const hex = hsv2hex(h, s, v);
  const R = size / 2;

  /* follow the value when it changes from outside, but not while this picker is the one changing it */
  useEffect(() => {
    if (value.toLowerCase() !== last.current.toLowerCase()) { setHsv(hex2hsv(value)); last.current = value; }
  }, [value]);

  const emit = (n: [number, number, number], final: boolean) => {
    setHsv(n);
    const x = hsv2hex(...n);
    last.current = x;
    onChange(x, final);
  };
  const atDisc = (e: RPointerEvent) => {
    const b = disc.current!.getBoundingClientRect(), sc = b.width / size;
    const dx = (e.clientX - b.left) / sc - R, dy = (e.clientY - b.top) / sc - R;
    return [((Math.atan2(dy, dx) * 180) / Math.PI + 360) % 360, Math.min(1, Math.hypot(dx, dy) / (R - 2)), v] as [number, number, number];
  };
  const atBar = (e: RPointerEvent) => {
    const b = bar.current!.getBoundingClientRect();
    return [h, s, clamp((e.clientX - b.left) / b.width, 0, 1)] as [number, number, number];
  };
  const start = (kind: 'disc' | 'bar') => (e: RPointerEvent<HTMLDivElement>) => {
    e.currentTarget.setPointerCapture(e.pointerId);
    setDrag(kind);
    emit(kind === 'disc' ? atDisc(e) : atBar(e), false);
  };
  const move = (kind: 'disc' | 'bar') => (e: RPointerEvent) => { if (drag === kind) emit(kind === 'disc' ? atDisc(e) : atBar(e), false); };
  const end = () => { if (drag) { setDrag(null); onChange(hex, true); } };

  const a = (h * Math.PI) / 180, r = s * (R - 2);
  const dropper = async () => {
    try {
      const EyeDropper = (window as unknown as { EyeDropper?: new () => { open: () => Promise<{ sRGBHex: string }> } }).EyeDropper;
      if (!EyeDropper) return;
      const res = await new EyeDropper().open();
      emit(hex2hsv(res.sRGBHex), true);
    } catch { /* cancelled */ }
  };

  return (
    <div className={cx(styles.cp, className)}>
      <div ref={disc} className={cx(styles.disc, drag === 'disc' && styles.drag)} style={{ width: size, height: size }}
        onPointerDown={start('disc')} onPointerMove={move('disc')} onPointerUp={end} onPointerCancel={end} role="group" aria-label={t('colour.hueSat')}>
        <i className={styles.dim} style={{ opacity: 1 - v }} />
        <b className={styles.knob} style={{ transform: `translate(${R + Math.cos(a) * r}px,${R + Math.sin(a) * r}px)`, background: hex }} />
      </div>
      <div ref={bar} className={styles.bar} style={{ background: `linear-gradient(90deg,var(--pick-floor),${hsv2hex(h, s, 1)})` }}
        onPointerDown={start('bar')} onPointerMove={move('bar')} onPointerUp={end} onPointerCancel={end} role="group" aria-label={t('colour.brightness')}>
        <b className={styles.barKnob} style={{ left: `${v * 100}%`, background: hex }} />
      </div>
      <div className={styles.foot}>
        <label className={styles.hex}>
          <span>#</span>
          <input maxLength={6} spellCheck={false} aria-label={t('colour.hex')} value={typing ?? hex.slice(1).toUpperCase()}
            onFocus={() => setTyping(hex.slice(1).toUpperCase())}
            onBlur={() => setTyping(null)}
            onChange={(e) => {
              const t = e.target.value.replace(/[^0-9a-f]/gi, '').slice(0, 6);
              setTyping(t.toUpperCase());
              if (t.length === 6) emit(hex2hsv('#' + t), true);
            }} />
        </label>
        <button type="button" className={styles.drop} aria-label={t('colour.pick')} title={t('colour.pick')} onClick={dropper}><Icon name="dropper" size={18} /></button>
      </div>
      <div className={styles.saved}>
        {saved.map((c) => <button key={c} type="button" style={{ ['--c' as string]: c }} aria-label={c} onClick={() => emit(hex2hsv(c), true)} />)}
      </div>
    </div>
  );
}
