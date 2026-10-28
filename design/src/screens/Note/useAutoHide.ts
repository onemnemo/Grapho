import { useCallback, useEffect, useRef, useState, type HTMLAttributes, type PointerEvent } from 'react';
import type { AutoHide } from '@/blocks/SettingsOverlay';
import type { BarPosition, BarVariant } from '@/blocks/NoteBar';

const initialHint = (auto: AutoHide, _variant: BarVariant) =>
  auto === 'while' ? 'Write to hide the bar' : auto === 'always' ? 'Move to the edge to show the bar' : '';

/**
 * Auto-hide for the note bar. While writing: the bar leaves 600ms after the pen lifts and a faint handle shows for a moment.
 * The pointer near the edge, or a tap on it, brings it back; it leaves again after about 4s unused. Always: starts hidden.
 */
export function useAutoHide(auto: AutoHide, position: BarPosition, variant: BarVariant) {
  const host = useRef<HTMLDivElement>(null);
  const [hidden, setHiddenState] = useState(auto === 'always');
  const [handle, setHandle] = useState(auto === 'always');
  const [hint, setHint] = useState(initialHint(auto, variant));
  const state = useRef({ hidden: auto === 'always', writing: false, auto, hide: 0, idle: 0, handle: 0 });
  state.current.auto = auto;

  const clear = () => {
    window.clearTimeout(state.current.hide);
    window.clearTimeout(state.current.idle);
    window.clearTimeout(state.current.handle);
  };
  const apply = useCallback((h: boolean) => {
    const s = state.current;
    s.hidden = h;
    setHiddenState(h);
    window.clearTimeout(s.idle);
    if (h) {
      setHandle(true);
      window.clearTimeout(s.handle);
      s.handle = window.setTimeout(() => setHandle(false), 2600);
      setHint('');
    } else {
      setHandle(false);
      if (s.auto !== 'off') s.idle = window.setTimeout(() => { if (!s.writing) apply(true); }, 4000);
      setHint('');
    }
  }, []);

  useEffect(() => {
    clear();
    const h = auto === 'always';
    state.current.hidden = h;
    state.current.writing = false;
    setHiddenState(h);
    setHandle(h);
    setHint(initialHint(auto, variant));
    return clear;
  }, [auto, variant]);

  const near = (e: PointerEvent, px: number) => {
    const r = host.current?.getBoundingClientRect();
    if (!r) return false;
    const x = e.clientX - r.left, y = e.clientY - r.top;
    return { top: y < px, bottom: r.height - y < px, left: x < px, right: r.width - x < px }[position];
  };

  const bind: HTMLAttributes<HTMLDivElement> = {
    onPointerMove: (e) => {
      const s = state.current;
      if (!s.hidden || s.writing || s.auto === 'off') return;
      if (near(e, 56)) apply(false);
    },
    onPointerDownCapture: (e) => {
      if (state.current.hidden && near(e, 30)) {
        apply(false);
        e.stopPropagation();
      }
    },
    onPointerLeave: () => {
      const s = state.current;
      if (!s.hidden && s.auto !== 'off' && !s.writing) {
        window.clearTimeout(s.idle);
        s.idle = window.setTimeout(() => apply(true), 1800);
      }
    },
    onPointerEnter: () => window.clearTimeout(state.current.idle),
  };

  return {
    host, hidden, handle, hint, bind,
    strokeStart: () => {
      state.current.writing = true;
      window.clearTimeout(state.current.hide);
      window.clearTimeout(state.current.idle);
    },
    strokeEnd: () => {
      const s = state.current;
      s.writing = false;
      if (s.auto === 'while' && !s.hidden) s.hide = window.setTimeout(() => apply(true), 600);
    },
  };
}
