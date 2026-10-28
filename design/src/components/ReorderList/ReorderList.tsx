import { useCallback, useEffect, useLayoutEffect, useRef, useState, type CSSProperties, type HTMLAttributes, type KeyboardEvent, type PointerEvent, type ReactNode } from 'react';
import { Icon } from '@/assets/icons';
import { cx } from '@/lib/cx';
import { moveItem, shiftFor, targetAt, type Box, type Slot, type Target } from './reorderMath';
import { readSpots, useFlip, type Spots } from './useFlip';
import styles from './ReorderList.module.css';

export interface ReorderItemState {
  /** This item is the one being held. */
  dragging: boolean;
  /** The grip button. Place it at the start of the row; it shows on hover and focus. */
  handle: ReactNode;
  /** Position inside its section. */
  index: number;
  section: string;
}

export interface ReorderSection<T> {
  id: string;
  items: T[];
  /** A title above the section. */
  heading?: ReactNode;
  /** Shown while the section has no items. */
  empty?: ReactNode;
  /** The section cannot give up items below this count. */
  min?: number;
}

export interface ReorderGroupsProps<T> extends Omit<HTMLAttributes<HTMLDivElement>, 'onChange' | 'children'> {
  sections: Array<ReorderSection<T>>;
  getId: (item: T) => string;
  /** The item's name, for the handle's label and the spoken result. */
  getLabel: (item: T) => string;
  renderItem: (item: T, state: ReorderItemState) => ReactNode;
  /** The new arrangement. `from` and `to` differ when an item moved to another section. */
  onChange: (sections: Array<Slot<T>>, moved: { id: string; from: string; to: string }) => void;
  itemClassName?: string;
}

const HOLD_MS = 380;
const NO_DRAG = 'button, input, select, textarea, a, [role="switch"], [role="slider"], [data-no-drag]';
const EDGE = 40;

interface Held {
  id: string;
  from: number;
  removedAt: number;
  room: number;
  target: Target;
}

interface Gesture {
  pid: number;
  id: string;
  phase: 'pending' | 'drag';
  x: number;
  y: number;
  timer: number;
  y0: number;
  scroll0: number;
  scroller: HTMLElement | null;
  top: number;
  height: number;
  boxes: Box[];
  mids: number[][];
  last: number;
  raf: number;
}

const scrollParent = (el: HTMLElement | null): HTMLElement | null => {
  for (let n = el?.parentElement ?? null; n; n = n.parentElement) {
    const o = getComputedStyle(n).overflowY;
    if ((o === 'auto' || o === 'scroll') && n.scrollHeight > n.clientHeight) return n;
  }
  return null;
};

/**
 * Lists in one or more sections whose items can be dragged. A grip appears on hover; touch uses a long press;
 * with the grip focused the arrow keys move the item. Others slide aside while one is held, the dropped item
 * glides into place, and an item that moves to another section (or is moved by the parent) travels there.
 */
export function ReorderGroups<T>({ sections, getId, getLabel, renderItem, onChange, itemClassName, className, ...rest }: ReorderGroupsProps<T>) {
  const root = useRef<HTMLDivElement>(null);
  const [held, setHeld] = useState<Held | null>(null);
  const [armed, setArmed] = useState<string | null>(null);
  const [said, setSaid] = useState('');
  const gesture = useRef<Gesture | null>(null);
  const spots = useRef<Spots | null>(null);
  const focusId = useRef<string | null>(null);
  const live = useRef({ sections, getId, onChange, getLabel });
  live.current = { sections, getId, onChange, getLabel };
  const stop = useRef<() => void>(() => {});
  const heldRef = useRef<Held | null>(null);
  const put = useCallback((h: Held | null) => { heldRef.current = h; setHeld(h); }, []);

  useFlip(root, held !== null, spots);
  useLayoutEffect(() => {
    if (!focusId.current) return;
    root.current?.querySelector<HTMLElement>(`[data-reorder-handle="${CSS.escape(focusId.current)}"]`)?.focus({ preventScroll: true });
    focusId.current = null;
  });
  useEffect(() => () => stop.current(), []);

  const lifted = (id: string) => root.current?.querySelector<HTMLElement>(`[data-reorder-id="${CSS.escape(id)}"]`) ?? null;

  const begin = useCallback((g: Gesture) => {
    const el = root.current;
    const item = lifted(g.id);
    if (!el || !item) return false;
    const { sections: list } = live.current;
    const base = el.getBoundingClientRect();
    const rel = (n: Element) => { const r = n.getBoundingClientRect(); return { top: r.top - base.top, bottom: r.bottom - base.top, h: r.height }; };
    const secs = [...el.querySelectorAll<HTMLElement>(':scope > [data-reorder-section]')];
    let from = 0;
    let own = 0;
    let gap = 0;
    const mids: number[][] = [];
    const counts: number[] = [];
    secs.forEach((s, i) => {
      const nodes = [...s.querySelectorAll<HTMLElement>('[data-reorder-id]')];
      const rects = nodes.map(rel);
      if (rects.length > 1 && !gap) gap = rects[1].top - rects[0].bottom;
      const at = nodes.indexOf(item);
      if (at >= 0) { from = i; own = at; }
      const keep = rects.filter((_, k) => k !== at);
      mids.push(keep.map((r) => (r.top + r.bottom) / 2));
      counts.push(keep.length);
    });
    const removedAt = counts.slice(0, from).reduce((a, b) => a + b, 0) + own;
    const mine = rel(item);
    const lock = (list[from].min ?? 0) >= list[from].items.length;
    g.boxes = secs.map((s, i) => (lock && i !== from ? { top: Infinity, bottom: Infinity } : { top: rel(s).top, bottom: rel(s).bottom }));
    g.mids = mids;
    g.top = mine.top;
    g.height = mine.h;
    g.y0 = g.y;
    g.scroller = scrollParent(item);
    g.scroll0 = g.scroller?.scrollTop ?? 0;
    g.phase = 'drag';
    g.last = g.y;
    const target = targetAt(mine.top + mine.h / 2, g.boxes, g.mids);
    setArmed(null);
    put({ id: g.id, from, removedAt, room: mine.h + gap, target });
    return true;
  }, [put]);

  const move = useCallback((g: Gesture, clientY: number) => {
    const item = lifted(g.id);
    if (!item) return;
    g.last = clientY;
    const dy = clientY - g.y0 + ((g.scroller?.scrollTop ?? 0) - g.scroll0);
    item.style.transform = `translateY(${dy}px)`;
    const t = targetAt(g.top + g.height / 2 + dy, g.boxes, g.mids);
    const h = heldRef.current;
    if (h && (h.target.flat !== t.flat || h.target.section !== t.section)) put({ ...h, target: t });
  }, [put]);

  const finish = useCallback((commit: boolean) => {
    const g = gesture.current;
    stop.current();
    if (!g || g.phase !== 'drag') return;
    const el = root.current;
    const item = lifted(g.id);
    if (el) spots.current = readSpots(el);
    if (item) item.style.transform = '';
    const { sections: list, getId: idOf, onChange: change } = live.current;
    const h = heldRef.current;
    put(null);
    if (h && commit) {
      const t = h.target;
      const slots = list.map((s) => ({ id: s.id, items: s.items }));
      const next = moveItem(slots, h.id, idOf, t);
      const key = (x: Array<Slot<T>>) => x.map((s) => s.items.map(idOf).join()).join('|');
      if (key(slots) !== key(next)) change(next, { id: h.id, from: list[h.from].id, to: list[t.section].id });
    }
    // a long press ends in a click on whatever was under the finger; swallow it
    const swallow = (e: Event) => { e.stopPropagation(); e.preventDefault(); };
    window.addEventListener('click', swallow, { capture: true, once: true });
    window.setTimeout(() => window.removeEventListener('click', swallow, true), 60);
  }, [put]);

  const track = useCallback((g: Gesture) => {
    const onMove = (e: globalThis.PointerEvent) => {
      if (e.pointerId !== g.pid) return;
      if (g.phase === 'pending') {
        g.y = e.clientY;
        if (Math.hypot(e.clientX - g.x, e.clientY - g.y) > 8) stop.current();
        return;
      }
      move(g, e.clientY);
    };
    const onUp = (e: globalThis.PointerEvent) => { if (e.pointerId === g.pid) finish(e.type === 'pointerup'); };
    const onKey = (e: globalThis.KeyboardEvent) => { if (e.key === 'Escape' && g.phase === 'drag') { e.stopPropagation(); finish(false); } };
    const onTouch = (e: TouchEvent) => { if (g.phase === 'drag' && e.cancelable) e.preventDefault(); };
    const onMenu = (e: Event) => e.preventDefault();
    const tick = () => {
      if (g.phase === 'drag' && g.scroller) {
        const r = g.scroller.getBoundingClientRect();
        const d = g.last < r.top + EDGE ? -1 : g.last > r.bottom - EDGE ? 1 : 0;
        if (d) { g.scroller.scrollTop += d * 8; move(g, g.last); }
      }
      g.raf = requestAnimationFrame(tick);
    };
    g.raf = requestAnimationFrame(tick);
    window.addEventListener('pointermove', onMove);
    window.addEventListener('pointerup', onUp);
    window.addEventListener('pointercancel', onUp);
    window.addEventListener('keydown', onKey, true);
    window.addEventListener('touchmove', onTouch, { passive: false });
    window.addEventListener('contextmenu', onMenu);
    stop.current = () => {
      window.clearTimeout(g.timer);
      cancelAnimationFrame(g.raf);
      window.removeEventListener('pointermove', onMove);
      window.removeEventListener('pointerup', onUp);
      window.removeEventListener('pointercancel', onUp);
      window.removeEventListener('keydown', onKey, true);
      window.removeEventListener('touchmove', onTouch);
      window.setTimeout(() => window.removeEventListener('contextmenu', onMenu), 400);
      setArmed(null);
      gesture.current = null;
      stop.current = () => {};
    };
  }, [finish, move]);

  const start = (e: PointerEvent<HTMLElement>, id: string, immediate: boolean) => {
    if (gesture.current && gesture.current.phase === 'drag') return;
    stop.current();
    const g: Gesture = { pid: e.pointerId, id, phase: 'pending', x: e.clientX, y: e.clientY, timer: 0, y0: 0, scroll0: 0, scroller: null, top: 0, height: 0, boxes: [], mids: [], last: e.clientY, raf: 0 };
    gesture.current = g;
    track(g);
    if (immediate) { e.preventDefault(); begin(g); return; }
    setArmed(id);
    g.timer = window.setTimeout(() => { if (begin(g)) navigator.vibrate?.(8); }, HOLD_MS);
  };

  const keys = (e: KeyboardEvent<HTMLElement>, id: string) => {
    const step = e.key === 'ArrowUp' || e.key === 'ArrowLeft' ? -1 : e.key === 'ArrowDown' || e.key === 'ArrowRight' ? 1 : e.key === 'Home' ? -Infinity : e.key === 'End' ? Infinity : 0;
    if (!step) return;
    e.preventDefault();
    const si = sections.findIndex((s) => s.items.some((t) => getId(t) === id));
    if (si < 0) return;
    const items = sections[si].items;
    const at = items.findIndex((t) => getId(t) === id);
    const to = Math.max(0, Math.min(items.length - 1, at + step));
    if (to === at) return;
    focusId.current = id;
    setSaid(`${getLabel(items[at])} moved to position ${to + 1} of ${items.length}`);
    onChange(moveItem(sections.map((s) => ({ id: s.id, items: s.items })), id, getId, { section: si, index: to }), { id, from: sections[si].id, to: sections[si].id });
  };

  let flat = 0;
  return (
    <div ref={root} className={cx(styles.root, className)} data-dragging={held ? '' : undefined} {...rest}>
      {sections.map((s) => (
        <div key={s.id} className={styles.section} data-reorder-section={s.id}>
          {s.heading != null && <div className={styles.heading}>{s.heading}</div>}
          <div className={styles.list} role="list" aria-label={typeof s.heading === 'string' ? s.heading : undefined}>
            {s.items.map((item, index) => {
              const id = getId(item);
              const isHeld = held?.id === id;
              let shift = 0;
              if (held && !isHeld) shift = shiftFor(flat, held.removedAt, held.target.flat, held.room);
              if (!isHeld) flat += 1;
              const handle = (
                <button
                  type="button"
                  className={styles.handle}
                  data-reorder-handle={id}
                  aria-label={`Move ${getLabel(item)}`}
                  aria-describedby={undefined}
                  onPointerDown={(e) => { if (e.button === 0) start(e, id, true); }}
                  onKeyDown={(e) => keys(e, id)}
                >
                  <Icon name="grip" size={16} />
                </button>
              );
              return (
                <div
                  key={id}
                  role="listitem"
                  data-reorder-id={id}
                  data-lifted={isHeld ? '' : undefined}
                  data-armed={armed === id ? '' : undefined}
                  className={cx(styles.item, itemClassName)}
                  style={shift ? ({ transform: `translateY(${shift}px)` } as CSSProperties) : undefined}
                  onPointerDown={(e) => {
                    if (e.pointerType === 'mouse' || (e.target as HTMLElement).closest(NO_DRAG)) return;
                    start(e, id, false);
                  }}
                >
                  {renderItem(item, { dragging: isHeld, handle, index, section: s.id })}
                </div>
              );
            })}
            {s.items.length === 0 && s.empty != null && <div className={styles.empty}>{s.empty}</div>}
          </div>
        </div>
      ))}
      <span className={styles.live} role="status" aria-live="polite">{said}</span>
    </div>
  );
}

export interface ReorderListProps<T> extends Omit<ReorderGroupsProps<T>, 'sections' | 'onChange'> {
  items: T[];
  onChange: (items: T[]) => void;
}

/** One list whose items can be dragged. See ReorderGroups for items that move between sections. */
export function ReorderList<T>({ items, onChange, ...rest }: ReorderListProps<T>) {
  return <ReorderGroups {...rest} sections={[{ id: 'list', items }]} onChange={(next) => onChange(next[0].items)} />;
}
