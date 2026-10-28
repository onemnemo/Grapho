import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { usePresence } from '@/lib/usePresence';
import type { IconName } from '@/assets/icons';
import { tokenMs } from '@/lib/tokenMs';
import { Toast, type ToastKind, type ToastVariant } from './Toast';
import styles from './Toast.module.css';

export interface ToastOptions {
  detail?: string;
  kind?: ToastKind;
  icon?: IconName;
  /** Shows Undo with the draining ring. */
  undo?: () => void;
  /** ms before it goes. Defaults to --d-toast with Undo, --d-toast-plain without. */
  duration?: number;
}

export interface ToastApi {
  /** Show a message. One toast at a time: a new one replaces the old one. */
  show: (message: string, options?: ToastOptions) => void;
  /**
   * Report a delete. Deletes that land while the toast is still up merge into "Deleted N items",
   * and one Undo brings all of them back.
   */
  deleted: (name: string, undo: () => void) => void;
  dismiss: () => void;
}

interface Active {
  key: number;
  message: string;
  detail?: string;
  kind?: ToastKind;
  icon?: IconName;
  undo?: () => void;
  duration: number;
  /** Merge state for deletes. */
  del?: { names: string[]; undos: Array<() => void> };
}

const ToastContext = createContext<ToastApi | null>(null);

export function useToast(): ToastApi {
  const api = useContext(ToastContext);
  if (!api) throw new Error('useToast needs a ToastProvider above it.');
  return api;
}

export interface ToastProviderProps {
  children: ReactNode;
  /** viewport pins to the window; container pins to the nearest positioned parent. */
  placement?: 'viewport' | 'container';
  /** above-bar lifts the toast clear of a selection bar. */
  offset?: 'default' | 'above-bar';
  /** card is the full toast; pill is the one-line toast for canvases such as the pen wheel. */
  variant?: ToastVariant;
}

const undoMs = () => tokenMs('--d-toast', 6000);
const plainMs = () => tokenMs('--d-toast-plain', 2200);

const plural = (n: number) => `${n} items`;
const deletedText = (names: string[]) => (names.length === 1 ? `Deleted ${names[0]}` : `Deleted ${plural(names.length)}`);

/** Holds the one toast on screen and the timer that retires it. Hover or focus pauses the clock. */
export function ToastProvider({ children, placement = 'viewport', offset = 'default', variant = 'card' }: ToastProviderProps) {
  const [active, setActive] = useState<Active | null>(null);
  const [paused, setPaused] = useState(false);
  const keyRef = useRef(0);
  const activeRef = useRef<Active | null>(null);
  activeRef.current = active;
  const clock = useRef<{ id?: number; started: number; left: number }>({ started: 0, left: 0 });

  const stop = () => window.clearTimeout(clock.current.id);
  const start = useCallback((ms: number) => {
    stop();
    clock.current.left = ms;
    clock.current.started = Date.now();
    clock.current.id = window.setTimeout(() => setActive(null), ms);
  }, []);

  const show = useCallback<ToastApi['show']>((message, options = {}) => {
    const duration = options.duration ?? (options.undo ? undoMs() : plainMs());
    setActive({ key: ++keyRef.current, message, detail: options.detail, kind: options.kind, icon: options.icon, undo: options.undo, duration });
    setPaused(false);
    start(duration);
  }, [start]);

  const deleted = useCallback<ToastApi['deleted']>((name, undo) => {
    const prev = activeRef.current?.del;
    const del = { names: [...(prev?.names ?? []), name], undos: [...(prev?.undos ?? []), undo] };
    const undos = del.undos;
    const ms = undoMs();
    setActive({
      key: ++keyRef.current,
      message: deletedText(del.names),
      icon: 'trash',
      undo: () => undos.forEach((u) => u()),
      duration: ms,
      del,
    });
    setPaused(false);
    start(ms);
  }, [start]);

  const dismiss = useCallback(() => {
    stop();
    setActive(null);
  }, []);

  const pause = () => {
    if (!active || paused) return;
    stop();
    clock.current.left = Math.max(400, clock.current.left - (Date.now() - clock.current.started));
    setPaused(true);
  };
  const resume = () => {
    if (!active || !paused) return;
    setPaused(false);
    start(clock.current.left);
  };

  useEffect(() => stop, []);

  const api = useMemo<ToastApi>(() => ({ show, deleted, dismiss }), [show, deleted, dismiss]);

  // Keep the last toast mounted while it leaves.
  const shown = useRef<Active | null>(null);
  if (active) shown.current = active;
  const { mounted, leaving } = usePresence(active !== null);
  const item = active ?? shown.current;

  const region = mounted && item && (
    <div className={styles.region} data-placement={placement} data-offset={offset} data-variant={variant} role="status" aria-live="polite">
      <Toast
        key={item.key}
        leaving={leaving}
        variant={variant}
        kind={item.kind}
        icon={item.icon}
        detail={item.detail}
        duration={item.duration}
        paused={paused}
        onUndo={item.undo ? () => { item.undo?.(); dismiss(); } : undefined}
        onPointerEnter={pause}
        onPointerLeave={resume}
        onFocus={pause}
        onBlur={resume}
      >
        {item.message}
      </Toast>
    </div>
  );

  return (
    <ToastContext.Provider value={api}>
      {children}
      {placement === 'viewport' && region ? createPortal(region, document.body) : region}
    </ToastContext.Provider>
  );
}
