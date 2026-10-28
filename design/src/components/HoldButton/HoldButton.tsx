import { useCallback, useEffect, useRef, useState, type ButtonHTMLAttributes, type CSSProperties } from 'react';
import { Icon, type IconName } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './HoldButton.module.css';

export type HoldButtonSize = 'sm' | 'md' | 'lg';

export interface HoldButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children' | 'onPointerDown'> {
  /** Permanent deletes only. Recoverable deletes use an Undo toast instead. */
  label?: string;
  icon?: IconName;
  size?: HoldButtonSize;
  /** How long the press must last, in ms. */
  duration?: number;
  /** Fires shortly after the fill completes and the tick has drawn. */
  onConfirm?: () => void;
}

type Phase = 'idle' | 'holding' | 'drain' | 'done';
const clamp = (v: number, lo: number, hi: number) => Math.min(hi, Math.max(lo, v));

/** Hold to confirm. The red fill sweeps across while pressed and drains back if let go early. */
export function HoldButton({
  label = 'Hold to delete',
  icon = 'trash',
  size = 'md',
  duration = 900,
  onConfirm,
  className,
  disabled,
  style,
  ...rest
}: HoldButtonProps) {
  const el = useRef<HTMLButtonElement>(null);
  const phase = useRef<Phase>('idle');
  const p = useRef(0);
  const raf = useRef(0);
  const t0 = useRef(0);
  const [state, setState] = useState<Phase>('idle');
  const [pulse, setPulse] = useState(0);

  const set = useCallback((v: number) => {
    p.current = v;
    el.current?.style.setProperty('--p', v.toFixed(4));
  }, []);
  const go = (next: Phase) => {
    phase.current = next;
    setState(next);
  };

  const done = useCallback(() => {
    set(1);
    go('done');
    window.setTimeout(() => onConfirm?.(), 650);
  }, [onConfirm, set]);

  const start = () => {
    if (phase.current === 'done' || phase.current === 'holding' || disabled) return;
    cancelAnimationFrame(raf.current);
    if (p.current === 0) setPulse((n) => n + 1);
    go('holding');
    t0.current = performance.now() - p.current * duration;
    const tick = (t: number) => {
      if (phase.current !== 'holding') return;
      const v = clamp((t - t0.current) / duration, 0, 1);
      set(v);
      if (v >= 1) done();
      else raf.current = requestAnimationFrame(tick);
    };
    raf.current = requestAnimationFrame(tick);
  };

  const release = () => {
    if (phase.current !== 'holding') return;
    go('drain');
    cancelAnimationFrame(raf.current);
    const from = p.current;
    const dur = 160 + from * 260;
    const s0 = performance.now();
    const step = (t: number) => {
      if (phase.current !== 'drain') return;
      const k = clamp((t - s0) / dur, 0, 1);
      set(from * (1 - (1 - Math.pow(1 - k, 3))));
      if (k < 1) raf.current = requestAnimationFrame(step);
      else {
        set(0);
        go('idle');
      }
    };
    raf.current = requestAnimationFrame(step);
  };

  useEffect(() => () => cancelAnimationFrame(raf.current), []);

  const content = (
    <>
      <Icon name={icon} size={18} />
      {label}
    </>
  );

  return (
    <button
      ref={el}
      type="button"
      disabled={disabled}
      data-state={state}
      data-pulse={pulse % 2}
      className={cx(styles.hold, styles[size], state === 'holding' && styles.holding, pulse > 0 && styles.tick, className)}
      style={{ '--p': 0, ...style } as CSSProperties}
      onPointerDown={(e) => {
        if (e.button) return;
        e.preventDefault();
        try {
          e.currentTarget.setPointerCapture(e.pointerId);
        } catch {
          /* pointer already gone */
        }
        e.currentTarget.focus({ preventScroll: true });
        start();
      }}
      onPointerUp={release}
      onPointerCancel={release}
      onLostPointerCapture={release}
      onContextMenu={(e) => e.preventDefault()}
      onKeyDown={(e) => {
        if ((e.key === ' ' || e.key === 'Enter') && !e.repeat) {
          e.preventDefault();
          start();
        }
      }}
      onKeyUp={(e) => (e.key === ' ' || e.key === 'Enter') && release()}
      onBlur={release}
      {...rest}
    >
      <span className={styles.fill} />
      <span className={styles.lab}>{content}</span>
      {/* the same label in white, revealed from the left as the fill passes under it */}
      <span className={cx(styles.lab, styles.on)} aria-hidden="true">{content}</span>
      <span className={styles.ok} aria-hidden="true">
        <Icon name="check" size={20} strokeWidth={2.2} />
      </span>
    </button>
  );
}
