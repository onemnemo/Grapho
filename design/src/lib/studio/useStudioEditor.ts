import { useCallback, useMemo, useRef, useState } from 'react';
import { clamp, deep, designArea, DIM, isBackground, uid, type Design, type Layer } from '@/assets/studio';

const snap = (d: Design) => JSON.stringify(d);

export interface StudioEditor {
  design: Design;
  selectedId: string | null;
  selected: Layer | null;
  canUndo: boolean;
  canRedo: boolean;
  /** True when the design differs from the last saved state. */
  dirty: boolean;
  select: (id: string | null) => void;
  /** Change the design without adding an undo step (while dragging, typing, sliding). */
  live: (fn: (d: Design) => void) => void;
  /** Close the current gesture: one undo step for everything changed since the last commit. */
  commit: () => void;
  /** Change the design and commit in one step. */
  apply: (fn: (d: Design) => void) => void;
  patchLayer: (id: string, patch: Partial<Layer>, commit?: boolean) => void;
  addLayer: (l: Layer, at?: { x: number; y: number }) => void;
  /** Removes a layer and returns a function that puts it back. */
  deleteLayer: (id: string) => () => void;
  duplicate: (id: string) => void;
  /** Swap the layer with its neighbour in the stack: +1 forward, -1 backward. */
  move: (id: string, dir: 1 | -1) => void;
  /** Reorder from the layer list: the new order of the visible ids, top first. */
  reorder: (idsTopFirst: string[]) => void;
  undo: () => void;
  redo: () => void;
  markSaved: () => void;
  /** Go back to the last saved state. */
  discard: () => void;
  /** Replace everything with another design, keeping the name and meta. */
  replaceWith: (d: Design, keepName?: boolean) => void;
}

/** The editing model of the studio: one design, a selection and an undo history of 80 steps. */
export function useStudioEditor(initial: Design): StudioEditor {
  const [design, setDesign] = useState<Design>(() => deep(initial));
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [tick, setTick] = useState(0);
  const ref = useRef(design);
  const hist = useRef<{ s: string[]; i: number }>({ s: [snap(design)], i: 0 });
  const saved = useRef(snap(design));
  const bump = () => setTick((t) => t + 1);

  const commit = useCallback(() => {
    const h = hist.current, j = snap(ref.current);
    if (h.s[h.i] === j) return;
    h.s = h.s.slice(0, h.i + 1);
    h.s.push(j);
    h.i++;
    if (h.s.length > 80) { h.s.shift(); h.i--; }
    bump();
  }, []);

  const live = useCallback((fn: (d: Design) => void) => {
    const n = deep(ref.current);
    fn(n);
    ref.current = n;
    setDesign(n);
  }, []);
  const apply = useCallback((fn: (d: Design) => void) => { live(fn); commit(); }, [live, commit]);

  const travel = useCallback((dir: number) => {
    const h = hist.current, n = h.i + dir;
    if (n < 0 || n >= h.s.length) return;
    h.i = n;
    const d = JSON.parse(h.s[n]) as Design;
    ref.current = d;
    setDesign(d);
    setSelectedId((id) => (id && d.layers.some((l) => l.id === id) ? id : null));
    bump();
  }, []);

  const patchLayer = useCallback((id: string, patch: Partial<Layer>, c = false) => {
    live((d) => { const l = d.layers.find((x) => x.id === id); if (l) Object.assign(l, patch); });
    if (c) commit();
  }, [live, commit]);

  const addLayer = useCallback((l: Layer, at?: { x: number; y: number }) => {
    apply((d) => {
      if (at && !isBackground(l)) {
        const { W, H } = DIM[d.kind];
        l.x = clamp(at.x, 8, W - 8);
        l.y = clamp(at.y, 8, H - 8);
        if (d.kind === 'folder' && l.type === 'sticker' && at.y < DIM.folder.fy) l.pop = 'above';
      }
      if (isBackground(l)) {
        let i = 0;
        while (i < d.layers.length && isBackground(d.layers[i]) && !d.layers[i].pop) i++;
        d.layers.splice(i, 0, l);
      } else d.layers.push(l);
    });
    setSelectedId(l.id);
  }, [apply]);

  const deleteLayer = useCallback((id: string) => {
    const d0 = ref.current, i = d0.layers.findIndex((l) => l.id === id);
    if (i < 0) return () => undefined;
    const copy = deep(d0.layers[i]);
    apply((d) => { d.layers.splice(i, 1); });
    setSelectedId(null);
    return () => {
      apply((d) => { d.layers.splice(Math.min(i, d.layers.length), 0, copy); });
      setSelectedId(copy.id);
    };
  }, [apply]);

  const duplicate = useCallback((id: string) => {
    let nid = '';
    apply((d) => {
      const i = d.layers.findIndex((l) => l.id === id);
      if (i < 0) return;
      const n = deep(d.layers[i]);
      n.id = nid = uid();
      n.x += 14; n.y += 14;
      d.layers.splice(i + 1, 0, n);
    });
    if (nid) setSelectedId(nid);
  }, [apply]);

  const move = useCallback((id: string, dir: 1 | -1) => {
    apply((d) => {
      const i = d.layers.findIndex((l) => l.id === id), j = i + dir;
      if (i < 0 || j < 0 || j >= d.layers.length) return;
      [d.layers[i], d.layers[j]] = [d.layers[j], d.layers[i]];
    });
  }, [apply]);

  const reorder = useCallback((idsTopFirst: string[]) => {
    apply((d) => {
      const order = idsTopFirst.slice().reverse().map((id) => d.layers.find((l) => l.id === id)!).filter(Boolean);
      const slots = d.layers.map((l, i) => (idsTopFirst.includes(l.id) ? i : -1)).filter((i) => i >= 0);
      slots.forEach((slot, k) => { d.layers[slot] = order[k]; });
    });
  }, [apply]);

  const replaceWith = useCallback((src: Design, keepName = true) => {
    apply((d) => {
      const n = deep(src);
      if (keepName) { n.name = d.name; n.meta = d.meta; }
      n.layers.forEach((l) => { l.id = uid(); });
      Object.keys(d).forEach((k) => delete (d as unknown as Record<string, unknown>)[k]);
      Object.assign(d, n);
    });
    setSelectedId(null);
  }, [apply]);

  const markSaved = useCallback(() => { saved.current = snap(ref.current); bump(); }, []);
  const discard = useCallback(() => {
    const d = JSON.parse(saved.current) as Design;
    ref.current = d;
    setDesign(d);
    setSelectedId(null);
    commit();
  }, [commit]);

  const selected = useMemo(() => design.layers.find((l) => l.id === selectedId) ?? null, [design, selectedId]);
  void tick;
  const h = hist.current;
  return {
    design, selectedId, selected,
    canUndo: h.i > 0, canRedo: h.i < h.s.length - 1,
    dirty: snap(design) !== saved.current,
    select: setSelectedId, live, commit, apply, patchLayer, addLayer, deleteLayer, duplicate, move, reorder,
    undo: () => travel(-1), redo: () => travel(1), markSaved, discard, replaceWith,
  };
}

export { designArea };
