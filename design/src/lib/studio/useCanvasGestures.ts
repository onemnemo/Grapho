import { useRef, useState, type PointerEvent, type RefObject } from 'react';
import { clamp, DIM, type Design, type Layer } from '@/assets/studio';

export interface Guide { v: boolean; p: number }
export interface GestureView {
  guides: Guide[];
  /** A small tag by the box while it changes: "45°" or "120 × 80". */
  chip: string | null;
  snapRot: boolean;
  moving: boolean;
  hoverId: string | null;
}

export interface CanvasGestureOptions {
  design: Design;
  selected: Layer | null;
  /** Screen pixels per design unit. */
  K: number;
  /** The element the design is drawn in, for turning pointer positions into design units. */
  hostRef: RefObject<HTMLElement | null>;
  /** Real height of a layer: text layers have no fixed height. */
  heightOf: (l: Layer) => number;
  onSelect: (id: string | null) => void;
  onLive: (fn: (d: Design) => void) => void;
  onCommit: () => void;
  /** True while text is being edited in place: the canvas leaves the pointer alone. */
  editing: boolean;
  onEndEdit: () => void;
}

const canResizeEdges = (l: Layer) =>
  !(l.type === 'sticker' || l.type === 'icon' || (l.type === 'shape' && !['rect', 'rounded', 'pill', 'circle', 'arch', 'ring', 'line'].includes(l.shape ?? '')));
/** Which side handles a layer shows: none when locked, east and west only for text. */
export const edgesOf = (l: Layer): string[] => (l.lock || !canResizeEdges(l) ? [] : l.type === 'text' ? ['e', 'w'] : ['n', 'e', 's', 'w']);

type Drag =
  | { kind: 'move'; id: string; sx: number; sy: number; ox: number; oy: number; moved: boolean }
  | { kind: 'rot'; id: string; a0: number; o: { x: number; y: number; r: number }; moved: boolean }
  | { kind: 'scale'; id: string; h: string; o: { x: number; y: number; w: number; h: number; r: number; size?: number }; moved: boolean };

/**
 * Pointer gestures on the studio canvas: select, move with guides, scale from a handle (corners keep the shape), and rotate with a snap every 15 degrees.
 * Hold Alt to switch snapping off. Spread `bind` on the element that holds both the art and its selection box.
 */
export function useCanvasGestures(o: CanvasGestureOptions) {
  const opts = useRef(o);
  opts.current = o;
  const drag = useRef<Drag | null>(null);
  const [view, setView] = useState<GestureView>({ guides: [], chip: null, snapRot: false, moving: false, hoverId: null });
  const patchView = (p: Partial<GestureView>) => setView((v) => ({ ...v, ...p }));

  const pt = (e: { clientX: number; clientY: number }) => {
    const { design, hostRef } = opts.current;
    const b = hostRef.current?.getBoundingClientRect();
    if (!b) return { x: 0, y: 0 };
    const s = b.width / DIM[design.kind].W;
    return { x: (e.clientX - b.left) / s, y: (e.clientY - b.top) / s };
  };

  const snapMove = (l: Layer, nx: number, ny: number, off: boolean): { x: number; y: number; guides: Guide[] } => {
    const { design: d, K, heightOf } = opts.current;
    if (off) return { x: nx, y: ny, guides: [] };
    const { W, H } = DIM[d.kind];
    const isF = d.kind === 'folder';
    const th = 6 / K;
    const ext = (q: Layer) => {
      const a = (q.r * Math.PI) / 180, w = q.w, h = heightOf(q);
      return { hw: Math.abs((w / 2) * Math.cos(a)) + Math.abs((h / 2) * Math.sin(a)), hh: Math.abs((w / 2) * Math.sin(a)) + Math.abs((h / 2) * Math.cos(a)) };
    };
    const { hw, hh } = ext(l);
    const tx = [0, W / 2, W, 16, W - 16];
    const fy = DIM.folder.fy;
    const ty = isF ? [fy, (fy + H) / 2, H, fy + 16, H - 16] : [0, H / 2, H, 16, H - 16];
    d.layers.forEach((q) => {
      if (q.id === l.id || q.hide || (q.w > W * 0.9 && heightOf(q) > (isF ? 150 : H * 0.9))) return;
      const e = ext(q);
      tx.push(q.x - e.hw, q.x, q.x + e.hw);
      ty.push(q.y - e.hh, q.y, q.y + e.hh);
    });
    let bx: { d: number; v: number; line: number } | null = null;
    let by: { d: number; v: number; line: number } | null = null;
    [-hw, 0, hw].forEach((off2) => tx.forEach((t) => { const dd = Math.abs(nx + off2 - t); if (dd < th && (!bx || dd < bx.d)) bx = { d: dd, v: t - off2, line: t }; }));
    [-hh, 0, hh].forEach((off2) => ty.forEach((t) => { const dd = Math.abs(ny + off2 - t); if (dd < th && (!by || dd < by.d)) by = { d: dd, v: t - off2, line: t }; }));
    const guides: Guide[] = [];
    const X = bx as { v: number; line: number } | null, Y = by as { v: number; line: number } | null;
    if (X) { nx = X.v; guides.push({ v: true, p: X.line }); }
    if (Y) { ny = Y.v; guides.push({ v: false, p: Y.line }); }
    return { x: nx, y: ny, guides };
  };

  const layerEl = (t: EventTarget | null) => (t as HTMLElement | null)?.closest?.('[data-layer-id]') as HTMLElement | null;

  const onPointerDown = (e: PointerEvent<HTMLElement>) => {
    const c = opts.current;
    if (c.editing) { if (!layerEl(e.target)) c.onEndEdit(); else return; }
    const t = e.target as HTMLElement;
    if (t.closest('[data-vplay]')) return;
    const hd = t.closest('[data-hd]') as HTMLElement | null;
    const sel = c.selected;
    if (hd && sel) {
      e.preventDefault();
      const p = pt(e), h = hd.dataset.hd!;
      const o2 = { x: sel.x, y: sel.y, w: sel.w, h: c.heightOf(sel), r: sel.r, size: sel.size };
      drag.current = h === 'rot'
        ? { kind: 'rot', id: sel.id, a0: (Math.atan2(p.y - sel.y, p.x - sel.x) * 180) / Math.PI, o: o2, moved: false }
        : { kind: 'scale', id: sel.id, h, o: o2, moved: false };
      e.currentTarget.setPointerCapture(e.pointerId);
      return;
    }
    const el = layerEl(e.target);
    if (!el) { if (sel) c.onSelect(null); return; }
    const l = c.design.layers.find((x) => x.id === el.dataset.layerId);
    if (!l) return;
    if (c.selected?.id !== l.id) c.onSelect(l.id);
    if (l.lock) return;
    e.preventDefault();
    const p = pt(e);
    drag.current = { kind: 'move', id: l.id, sx: p.x, sy: p.y, ox: l.x, oy: l.y, moved: false };
    e.currentTarget.setPointerCapture(e.pointerId);
  };

  const onPointerMove = (e: PointerEvent<HTMLElement>) => {
    const c = opts.current;
    const dr = drag.current;
    if (!dr) {
      const el = layerEl(e.target);
      const id = el && el.dataset.layerId !== c.selected?.id ? el.dataset.layerId! : null;
      if (id !== view.hoverId) patchView({ hoverId: id });
      return;
    }
    const p = pt(e);
    const l = c.design.layers.find((x) => x.id === dr.id);
    if (!l) return;
    if (dr.kind === 'move') {
      const dx = p.x - dr.sx, dy = p.y - dr.sy;
      if (!dr.moved && Math.hypot(dx, dy) * c.K < 3) return;
      dr.moved = true;
      const s = snapMove(l, dr.ox + dx, dr.oy + dy, e.altKey);
      c.onLive((d) => { const t = d.layers.find((x) => x.id === dr.id); if (t) { t.x = s.x; t.y = s.y; } });
      setView((v) => ({ ...v, guides: s.guides, chip: null, snapRot: false, moving: true, hoverId: null }));
    } else if (dr.kind === 'rot') {
      const a = (Math.atan2(p.y - dr.o.y, p.x - dr.o.x) * 180) / Math.PI;
      let r = dr.o.r + (a - dr.a0);
      r = ((r + 540) % 360) - 180;
      let snapped = false;
      if (!e.altKey) { const n = Math.round(r / 15) * 15; if (Math.abs(r - n) < 4) { r = n; snapped = true; } }
      dr.moved = true;
      const rr = Math.round(r * 10) / 10;
      c.onLive((d) => { const t = d.layers.find((x) => x.id === dr.id); if (t) t.r = rr; });
      setView((v) => ({ ...v, guides: [], chip: `${Math.round(rr)}°`, snapRot: snapped, moving: true }));
    } else {
      const { o: o2, h } = dr;
      const a = (o2.r * Math.PI) / 180, ca = Math.cos(a), sa = Math.sin(a);
      const sx = h.includes('e') ? 1 : h.includes('w') ? -1 : 0, sy = h.includes('s') ? 1 : h.includes('n') ? -1 : 0;
      const loc = (px: number, py: number): [number, number] => { const dx = px - o2.x, dy = py - o2.y; return [dx * ca + dy * sa, -dx * sa + dy * ca]; };
      const glob = (lx: number, ly: number): [number, number] => [o2.x + lx * ca - ly * sa, o2.y + lx * sa + ly * ca];
      const ax = -sx * (o2.w / 2) * (sx ? 1 : 0), ay = -sy * (o2.h / 2) * (sy ? 1 : 0);
      const [mx, my] = loc(p.x, p.y);
      dr.moved = true;
      let nw = o2.w, nh = o2.h, nx = o2.x, ny = o2.y, size: number | undefined;
      if (sx && sy) {
        const vx = mx - ax, vy = my - ay, dxn = sx * o2.w, dyn = sy * o2.h;
        const k = Math.max(12 / Math.min(o2.w, o2.h), (vx * dxn + vy * dyn) / (dxn * dxn + dyn * dyn));
        nw = o2.w * k; nh = o2.h * k;
        [nx, ny] = glob(ax + (sx * nw) / 2, ay + (sy * nh) / 2);
        if ((l.type === 'text' || l.type === 'card') && o2.size) size = clamp(Math.round(o2.size * k), 6, 240);
      } else if (sx) {
        nw = Math.max(16, (mx - ax) * sx);
        [nx, ny] = glob(ax + (sx * nw) / 2, 0);
      } else {
        nh = Math.max(12, (my - ay) * sy);
        [nx, ny] = glob(0, ay + (sy * nh) / 2);
      }
      c.onLive((d) => {
        const t = d.layers.find((x) => x.id === dr.id);
        if (!t) return;
        t.w = nw; t.h = nh; t.x = nx; t.y = ny;
        if (size !== undefined) t.size = size;
      });
      setView((v) => ({ ...v, guides: [], chip: `${Math.round(nw)} × ${Math.round(nh)}`, snapRot: false, moving: true }));
    }
  };

  const end = (e: PointerEvent<HTMLElement>) => {
    const dr = drag.current;
    if (!dr) return;
    drag.current = null;
    setView((v) => ({ ...v, guides: [], chip: null, snapRot: false, moving: false }));
    if (dr.moved) opts.current.onCommit();
    try { e.currentTarget.releasePointerCapture(e.pointerId); } catch { /* the pointer was already released */ }
  };

  return {
    view,
    bind: {
      onPointerDown, onPointerMove, onPointerUp: end, onPointerCancel: end,
      onPointerLeave: () => patchView({ hoverId: null }),
    },
  };
}
