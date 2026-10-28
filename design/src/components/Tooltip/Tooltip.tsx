import { cloneElement, useEffect, useId, useRef, useState, type ReactElement, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { useAnchoredPosition } from '@/lib/anchor';
import styles from './Tooltip.module.css';

export interface TooltipProps {
  /** The short name of what the control does. */
  content: ReactNode;
  /** A shortcut shown after the name, such as ⌘Z. */
  shortcut?: string;
  side?: 'top' | 'bottom' | 'left' | 'right';
  /** ms before it appears on hover. Focus shows it at once. */
  delay?: number;
  /** Force it open, for stories. */
  open?: boolean;
  children: ReactElement<{ 'aria-describedby'?: string }>;
}

/** A small white label for icon-only controls. Shows on hover after a beat and on keyboard focus; never on touch. */
export function Tooltip({ content, shortcut, side = 'top', delay = 380, open: forced, children }: TooltipProps) {
  const id = useId();
  const [shown, setShown] = useState(false);
  const wrapRef = useRef<HTMLSpanElement>(null);
  const tipRef = useRef<HTMLDivElement>(null);
  const timer = useRef<number>(undefined);
  const visible = forced ?? shown;
  const pos = useAnchoredPosition(wrapRef, tipRef, visible, side, 8);

  useEffect(() => () => window.clearTimeout(timer.current), []);
  useEffect(() => {
    if (!visible || forced) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setShown(false);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [visible, forced]);

  const hide = () => { window.clearTimeout(timer.current); setShown(false); };
  return (
    <span
      ref={wrapRef}
      className={styles.wrap}
      onPointerEnter={(e) => {
        if (e.pointerType === 'touch') return;
        timer.current = window.setTimeout(() => setShown(true), delay);
      }}
      onPointerLeave={hide}
      onPointerDown={hide}
      onFocus={(e) => { if (e.target.matches(':focus-visible')) setShown(true); }}
      onBlur={hide}
    >
      {cloneElement(children, { 'aria-describedby': visible ? id : undefined })}
      {visible && createPortal(
        <div
          ref={tipRef}
          id={id}
          role="tooltip"
          className={styles.tip}
          data-ready={pos ? 'true' : 'false'}
          style={{ left: pos?.left ?? 0, top: pos?.top ?? 0 }}
        >
          {content}
          {shortcut && <kbd className={styles.keys}>{shortcut}</kbd>}
        </div>,
        document.body,
      )}
    </span>
  );
}
