import { useLayoutEffect, useState, type RefObject } from 'react';

export type Side = 'top' | 'bottom' | 'left' | 'right';
export type Placement =
  | 'bottom-start' | 'bottom' | 'bottom-end'
  | 'top-start' | 'top' | 'top-end'
  | 'right-start' | 'right' | 'left-start' | 'left';

export interface Position {
  left: number;
  top: number;
  /** The placement actually used after flipping. */
  placement: Placement;
  /** Where the panel grows from, as a CSS transform-origin. */
  origin: string;
}

const OPPOSITE: Record<Side, Side> = { top: 'bottom', bottom: 'top', left: 'right', right: 'left' };

/** Places a panel of `size` next to `anchor`, flipping to the other side when it would leave the viewport. */
export function computePosition(
  anchor: DOMRect,
  size: { w: number; h: number },
  placement: Placement,
  offset: number,
  margin = 8,
  /** The room to fit in. The window by default; a framed preview passes its own frame. */
  bounds?: { w: number; h: number },
): Position {
  const [rawSide, align = 'center'] = placement.split('-') as [Side, 'start' | 'end' | undefined];
  const vw = bounds?.w ?? window.innerWidth;
  const vh = bounds?.h ?? window.innerHeight;
  const room = (s: Side) =>
    s === 'bottom' ? vh - anchor.bottom - offset - margin
    : s === 'top' ? anchor.top - offset - margin
    : s === 'right' ? vw - anchor.right - offset - margin
    : anchor.left - offset - margin;
  const need = rawSide === 'top' || rawSide === 'bottom' ? size.h : size.w;
  const side = room(rawSide) < need && room(OPPOSITE[rawSide]) > room(rawSide) ? OPPOSITE[rawSide] : rawSide;

  let left: number;
  let top: number;
  if (side === 'top' || side === 'bottom') {
    top = side === 'bottom' ? anchor.bottom + offset : anchor.top - offset - size.h;
    left = align === 'start' ? anchor.left : align === 'end' ? anchor.right - size.w : anchor.left + anchor.width / 2 - size.w / 2;
  } else {
    left = side === 'right' ? anchor.right + offset : anchor.left - offset - size.w;
    top = align === 'start' ? anchor.top - 6 : align === 'end' ? anchor.bottom - size.h + 6 : anchor.top + anchor.height / 2 - size.h / 2;
  }
  left = Math.max(margin, Math.min(left, vw - size.w - margin));
  top = Math.max(margin, Math.min(top, vh - size.h - margin));

  const ax = Math.max(0, Math.min(size.w, anchor.left + anchor.width / 2 - left));
  const ay = Math.max(0, Math.min(size.h, anchor.top + anchor.height / 2 - top));
  const origin =
    side === 'bottom' ? `${ax}px 0%` : side === 'top' ? `${ax}px 100%` : side === 'right' ? `0% ${ay}px` : `100% ${ay}px`;
  return { left, top, placement: (align === 'center' ? side : `${side}-${align}`) as Placement, origin };
}

/** Measures the anchor and the panel and keeps the panel next to the anchor while the page moves. */
export function useAnchoredPosition(
  anchorRef: RefObject<HTMLElement | null>,
  panelRef: RefObject<HTMLElement | null>,
  active: boolean,
  placement: Placement,
  offset: number,
  /** Place inside the panel's positioned parent instead of the window. */
  contained = false,
): Position | null {
  const [pos, setPos] = useState<Position | null>(null);

  useLayoutEffect(() => {
    if (!active) {
      setPos(null);
      return;
    }
    const update = () => {
      const a = anchorRef.current;
      const p = panelRef.current;
      if (!a || !p) return;
      const size = { w: p.offsetWidth, h: p.offsetHeight };
      const parent = contained ? (p.offsetParent as HTMLElement | null) : null;
      if (!parent) {
        setPos(computePosition(a.getBoundingClientRect(), size, placement, offset));
        return;
      }
      const ar = a.getBoundingClientRect();
      const pr = parent.getBoundingClientRect();
      const rel = new DOMRect(ar.left - pr.left - parent.clientLeft, ar.top - pr.top - parent.clientTop, ar.width, ar.height);
      setPos(computePosition(rel, size, placement, offset, 8, { w: parent.clientWidth, h: parent.clientHeight }));
    };
    update();
    window.addEventListener('resize', update);
    window.addEventListener('scroll', update, true);
    const ro = new ResizeObserver(update);
    if (panelRef.current) ro.observe(panelRef.current);
    return () => {
      window.removeEventListener('resize', update);
      window.removeEventListener('scroll', update, true);
      ro.disconnect();
    };
  }, [active, anchorRef, panelRef, placement, offset, contained]);

  return pos;
}
