import { useLayoutEffect, useRef, type MutableRefObject, type RefObject } from 'react';
import { tokenMs } from '@/lib/tokenMs';

export type Spots = Map<string, { x: number; y: number }>;

const ANIM = 'reorder';

/** Where every `[data-reorder-id]` inside `root` sits now, measured from the root's own corner so scrolling does not count. */
export function readSpots(root: HTMLElement): Spots {
  const base = root.getBoundingClientRect();
  const out: Spots = new Map();
  root.querySelectorAll<HTMLElement>('[data-reorder-id]').forEach((el) => {
    const r = el.getBoundingClientRect();
    out.set(el.dataset.reorderId as string, { x: r.left - base.left, y: r.top - base.top });
  });
  return out;
}

/**
 * Glides items from where they were to where they are after every render (FLIP).
 * An item that changes list, or is dropped, therefore travels instead of jumping.
 * `from` can hold the spots to start from, set just before a change that is about to render.
 */
export function useFlip(root: RefObject<HTMLElement | null>, paused: boolean, from: MutableRefObject<Spots | null>) {
  const last = useRef<Spots>(new Map());
  useLayoutEffect(() => {
    const el = root.current;
    if (!el || paused) return;
    el.querySelectorAll<HTMLElement>('[data-reorder-id]').forEach((n) => n.getAnimations().forEach((a) => a.id === ANIM && a.cancel()));
    const now = readSpots(el);
    const before = from.current ?? last.current;
    from.current = null;
    const calm = typeof matchMedia !== 'undefined' && matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (!calm) {
      const ms = tokenMs('--d-slow', 340);
      el.querySelectorAll<HTMLElement>('[data-reorder-id]').forEach((n) => {
        const id = n.dataset.reorderId as string;
        const a = before.get(id);
        const b = now.get(id);
        if (!a || !b) return;
        const dx = a.x - b.x;
        const dy = a.y - b.y;
        if (Math.abs(dx) < 0.5 && Math.abs(dy) < 0.5) return;
        n.animate([{ transform: `translate(${dx}px, ${dy}px)` }, { transform: 'translate(0, 0)' }], { duration: ms, easing: 'cubic-bezier(.2,.8,.2,1)', id: ANIM });
      });
    }
    last.current = now;
  });
}
