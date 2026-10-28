import { useRef, type PointerEvent } from 'react';

export interface DragInOptions {
  /** The area a tile can be dropped on. */
  target: () => DOMRect | null;
  /** The pointer entered or left the drop area while carrying a tile. */
  onOver?: (over: boolean) => void;
  /** Carrying starts or ends, so the rest of the screen can step back. */
  onCarry?: (carrying: boolean) => void;
  /** Dropped on the target, with the pointer position in the window. */
  onDrop: (spec: string, clientX: number, clientY: number) => void;
  /** A plain tap, no drag. */
  onTap: (spec: string) => void;
}

const inside = (r: DOMRect | null, x: number, y: number) => !!r && x > r.left && x < r.right && y > r.top && y < r.bottom;

/**
 * Drag a tile out of the panel onto the canvas, or tap it to add. While carrying, a copy of the tile follows the pointer;
 * released outside the target it flies back. Spread `bind(spec)` onto a tile.
 */
export function useDragIn({ target, onOver, onCarry, onDrop, onTap }: DragInOptions) {
  const st = useRef<{ el: HTMLElement; sx: number; sy: number; ghost: HTMLElement | null; s: number; id: number; over: boolean } | null>(null);

  const place = (g: HTMLElement, el: HTMLElement, s: number, x: number, y: number) => {
    g.style.transform = `translate(${x - (el.offsetWidth * s) / 2}px,${y - (el.offsetHeight * s) / 2}px) scale(${s * 1.15}) rotate(var(--st-ghost-tilt))`;
  };

  const end = (e: PointerEvent<HTMLElement>, cancelled: boolean) => {
    const d = st.current;
    if (!d) return;
    st.current = null;
    onOver?.(false);
    if (d.ghost) {
      onCarry?.(false);
      if (!cancelled && inside(target(), e.clientX, e.clientY)) {
        d.ghost.remove();
        onDrop(d.el.dataset.spec || '', e.clientX, e.clientY);
      } else {
        const g = d.ghost;
        const r = d.el.getBoundingClientRect();
        g.style.transition = 'transform .22s var(--ease), opacity .22s';
        g.style.transform = `translate(${r.left}px,${r.top}px) scale(${d.s})`;
        g.style.opacity = '0';
        window.setTimeout(() => g.remove(), 230);
      }
    } else if (!cancelled) onTap(d.el.dataset.spec || '');
  };

  const bind = (spec: string) => ({
    'data-spec': spec,
    onPointerDown: (e: PointerEvent<HTMLElement>) => {
      if (e.button > 0) return;
      const el = e.currentTarget;
      st.current = { el, sx: e.clientX, sy: e.clientY, ghost: null, s: 1, id: e.pointerId, over: false };
      el.setPointerCapture(e.pointerId);
    },
    onPointerMove: (e: PointerEvent<HTMLElement>) => {
      const d = st.current;
      if (!d) return;
      if (!d.ghost && Math.hypot(e.clientX - d.sx, e.clientY - d.sy) > 7) {
        const g = d.el.cloneNode(true) as HTMLElement;
        g.removeAttribute('data-spec');
        d.s = d.el.getBoundingClientRect().width / d.el.offsetWidth || 1;
        Object.assign(g.style, {
          position: 'fixed', left: '0', top: '0', width: `${d.el.offsetWidth}px`, height: `${d.el.offsetHeight}px`, pointerEvents: 'none', zIndex: '99',
          transformOrigin: '50% 50%', boxShadow: 'var(--e2)', opacity: '.96', background: 'var(--s3)', transform: 'none', animation: 'none',
        });
        document.body.appendChild(g);
        d.ghost = g;
        onCarry?.(true);
      }
      if (d.ghost) {
        place(d.ghost, d.el, d.s, e.clientX, e.clientY);
        const over = inside(target(), e.clientX, e.clientY);
        if (over !== d.over) { d.over = over; onOver?.(over); }
      }
    },
    onPointerUp: (e: PointerEvent<HTMLElement>) => end(e, false),
    onPointerCancel: (e: PointerEvent<HTMLElement>) => end(e, true),
  });

  return bind;
}
