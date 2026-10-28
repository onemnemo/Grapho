/* Pointer handling for the wheel: taps, turning the rings, dragging a value, the hold to carry,
   the grip, the closed tab, the mouse wheel and the hover tooltips. */
import { DEG, clamp, normDeg, polar, tick, wrapDeg } from './math';
import {
  DOT_NUMBERS, ERASER_FROM, ERASER_STEPS, ERASER_TO, GRIP_SLOP, HOLD_MS, OP_FROM, OP_TO, PART_AT, SIZE_FROM, SIZE_TO, TAB_H, TOOL_REACH, TOUCH_SLOP, WHOLE_AT,
  alphaAt, eraserAngle, eraserPtAt, fromScreen, sizeAngle, sizeNumberAt, stepOf, swatchFade, toScreen, toolAngle, toolFade, widthOfNumber, EDGES, type Edge, type Geo,
} from './geometry';
import type { Face } from './face';
import type { WheelController } from './controller';
import type { Spin } from './springs';
import type { ToolId } from './model';

type Kind = 'stem' | 'btn' | 'core' | 'dead' | 'tools' | 'esize' | 'spin' | 'opacity' | 'size';
interface Hit {
  kind: Kind;
  which?: 'whole' | 'part' | 'size' | 'rainbow';
  dx: number; dy: number; r: number; deg: number; e: Edge; g: Geo;
}
interface Gesture {
  id: number;
  mode: string;
  x0: number; y0: number;
  face?: Face;
  h?: Hit;
  spin?: Spin | null;
  wasMoving?: boolean;
  target?: HTMLElement | null;
  timer?: number;
  last?: { x: number; y: number };
  lastAng?: number;
  samples?: Array<{ t: number; o: number }>;
  move?: (ev: PointerEvent) => void;
  up?: (ev: PointerEvent) => void;
}

const press = (el: HTMLElement | null | undefined, on: boolean) => {
  if (!el) return;
  if (on) el.dataset.press = '';
  else delete el.dataset.press;
};

export class WheelInput {
  private g: Gesture | null = null;
  private tipT = 0;
  private off: Array<() => void> = [];

  constructor(private c: WheelController) {}

  bind() {
    const c = this.c;
    const on = <K extends keyof HTMLElementEventMap>(el: HTMLElement | null, type: K, fn: (e: HTMLElementEventMap[K]) => void, opts?: AddEventListenerOptions) => {
      if (!el) return;
      el.addEventListener(type, fn as EventListener, opts);
      this.off.push(() => el.removeEventListener(type, fn as EventListener, opts));
    };
    for (const face of [c.disc, c.circ]) {
      const root = face.refs.root;
      on(root, 'pointerdown', (e) => this.faceDown(face, e));
      on(root, 'pointerover', (e) => this.tipOver(e));
      on(root, 'wheel', (e) => this.faceWheel(face, e), { passive: false });
      on(root, 'pointerout', () => this.tipOut());
    }
    on(c.dom.grip, 'pointerdown', (e) => this.gripDown(e));
    on(c.dom.pin, 'click', () => c.setPinned(!c.model.pinned));
    on(c.dom.tab, 'pointerdown', (e) => this.tabDown(e));
  }
  unbind() {
    this.off.forEach((f) => f());
    this.off = [];
    clearTimeout(this.tipT);
    if (this.g) this.endGesture(this.g);
  }

  private local(e: { clientX: number; clientY: number }) {
    const c = this.c;
    const r = c.dom.layer!.getBoundingClientRect();
    const s = r.width / c.W || 1;
    return { x: (e.clientX - r.left) / s, y: (e.clientY - r.top) / s };
  }
  private atRest(face: Face) {
    const c = this.c;
    return c.isOpen && !c.motion.active && !c.motion.pose && face.full !== c.p.docked && c.openE.x > 0.9;
  }

  private faceWheel(face: Face, e: WheelEvent) {
    if (!this.atRest(face)) return;
    const m = this.c.model;
    const h = this.hitTest(face, this.local(e));
    if (!h) return;
    const editing = m.sizeMode || m.mode === 'eraser';
    const sp = editing ? null : h.r < face.g.inner ? face.toolSpin : face.rimSpin;
    if (!sp) return;
    e.preventDefault();
    const d = (Math.abs(e.deltaY) > Math.abs(e.deltaX) ? e.deltaY : e.deltaX) * (e.deltaMode === 1 ? 14 : 1) * 0.22;
    sp.grab();
    sp.dragBy(-d);
    clearTimeout(sp._wt);
    sp._wt = window.setTimeout(() => sp.release(0), 110);
    this.c.wake();
  }

  private tipOver(e: PointerEvent) {
    const t = (e.target as Element | null)?.closest?.<HTMLElement>('[data-tip]');
    const tip = this.c.dom.tip;
    if (!t || e.pointerType === 'touch' || !tip) return;
    clearTimeout(this.tipT);
    this.tipT = window.setTimeout(() => {
      const r = t.getBoundingClientRect();
      const h = this.c.dom.layer!.getBoundingClientRect();
      const s = h.width / this.c.W || 1;
      tip.textContent = t.dataset.tip ?? '';
      tip.style.transform = `translate3d(${((r.left + r.width / 2 - h.left) / s).toFixed(1)}px,${((r.top - h.top) / s - 8).toFixed(1)}px,0) translate(-50%,-100%)`;
      tip.dataset.on = '';
    }, 420);
  }
  private tipOut() {
    clearTimeout(this.tipT);
    if (this.c.dom.tip) delete this.c.dom.tip.dataset.on;
  }

  private hitTest(face: Face, p: { x: number; y: number }): Hit | null {
    const c = this.c;
    const ctr = c.motion.centre;
    const g = face.g;
    const e: Edge = face === c.circ ? 'left' : c.p.edge;
    const dx = p.x - ctr.x;
    const dy = p.y - ctr.y;
    const r = Math.hypot(dx, dy);
    const m = c.model;
    if (r > g.r + 2) return null;
    const deg = fromScreen(e, Math.atan2(dy, dx) / DEG);
    const base = { dx, dy, r, deg, e, g };
    const editing = m.sizeMode || m.mode === 'eraser';
    /* the grown plate: the selected tool out to the rim while its controls are open */
    if (editing) {
      const ai = face.tb.findIndex((b) => b.on);
      const ta = ai >= 0 ? face.tb[ai].a ?? 0 : 0;
      const a0 = polar(g.toolR, toScreen(e, ta));
      const a1 = polar(g.rimR, toScreen(e, ta));
      const vx = a1.x - a0.x;
      const vy = a1.y - a0.y;
      const l2 = vx * vx + vy * vy || 1;
      const tt = clamp(((dx - a0.x) * vx + (dy - a0.y) * vy) / l2, 0, 1);
      if (Math.hypot(dx - (a0.x + vx * tt), dy - (a0.y + vy * tt)) <= 24) return { ...base, kind: 'stem' };
    }
    const near = (rad: number, ang: number, lim: number) => {
      const q = polar(rad, toScreen(e, ang));
      return Math.hypot(dx - q.x, dy - q.y) <= lim;
    };
    if (m.mode === 'eraser') {
      if (near(g.rimR, WHOLE_AT, 22)) return { ...base, kind: 'btn', which: 'whole' };
      if (near(g.rimR, PART_AT, 22)) return { ...base, kind: 'btn', which: 'part' };
    } else if (!m.sizeMode) {
      if (near(g.rimR, g.sizeBtn, 22)) return { ...base, kind: 'btn', which: 'size' };
      if (near(g.rimR, g.rainbow, 22)) return { ...base, kind: 'btn', which: 'rainbow' };
    }
    if (r <= g.core) return { ...base, kind: 'core' };
    if (r < g.inner) {
      if (editing) return { ...base, kind: 'dead' };
      if (g.full || Math.abs(deg) <= TOOL_REACH) return { ...base, kind: 'tools' };
      return { ...base, kind: 'dead' };
    }
    if (m.mode === 'eraser') return deg >= ERASER_FROM - 10 && deg <= ERASER_TO + 10 ? { ...base, kind: 'esize' } : { ...base, kind: 'dead' };
    if (!m.sizeMode) return { ...base, kind: 'spin' };
    if (deg >= OP_FROM - 6 && deg <= OP_TO + 10) return { ...base, kind: 'opacity' };
    if (deg >= SIZE_FROM - 10 && deg <= SIZE_TO + 10) return { ...base, kind: 'size' };
    return { ...base, kind: 'dead' };
  }

  private faceDown(face: Face, e: PointerEvent) {
    const c = this.c;
    if (!this.atRest(face) || e.button > 0 || this.g) return;
    e.preventDefault();
    this.tipOut();
    const p = this.local(e);
    const h = this.hitTest(face, p);
    if (!h) return;
    if (h.kind !== 'core') c.hooks.closePens?.();
    const g: Gesture = { id: e.pointerId, face, h, x0: p.x, y0: p.y, mode: 'pending' };
    this.g = c.gesture = g;
    const spin = h.kind === 'spin' ? face.rimSpin : h.kind === 'tools' ? face.toolSpin : null;
    g.spin = spin;
    g.wasMoving = !!spin?.moving;
    spin?.grab();
    g.target = (e.target as Element).closest<HTMLElement>('[data-hit]');
    press(g.target, true);
    g.timer = window.setTimeout(() => {
      if (this.g === g && g.mode === 'pending') this.startCarry(g, g.last ?? p);
    }, HOLD_MS);
    g.move = (ev) => this.gMove(g, ev);
    g.up = (ev) => this.gUp(g, ev);
    window.addEventListener('pointermove', g.move);
    window.addEventListener('pointerup', g.up);
    window.addEventListener('pointercancel', g.up);
    c.wake();
  }
  private startCarry(g: Gesture, p: { x: number; y: number }) {
    clearTimeout(g.timer);
    press(g.target, false);
    g.spin?.release(0);
    g.mode = 'carry';
    this.c.hooks.closePens?.();
    this.c.motion.begin(p);
  }
  private gMove(g: Gesture, ev: PointerEvent) {
    if (ev.pointerId !== g.id) return;
    const c = this.c;
    const p = this.local(ev);
    g.last = p;
    if (g.mode === 'carry') { c.motion.move(p); return; }
    const h = g.h!;
    const dist = Math.hypot(p.x - g.x0, p.y - g.y0);
    if (g.mode === 'pending' && dist > TOUCH_SLOP) {
      clearTimeout(g.timer);
      press(g.target, false);
      if (g.spin) {
        g.mode = 'spin';
        g.lastAng = h.deg;
        g.samples = [{ t: performance.now(), o: g.spin.offset }];
      } else if (h.kind === 'size' || h.kind === 'opacity' || h.kind === 'esize') g.mode = 'value';
      else { g.mode = 'none'; this.endGesture(g); return; }
    }
    if (g.mode === 'spin' || g.mode === 'value') {
      const ctr = c.motion.centre;
      const ang = fromScreen(h.e, Math.atan2(p.y - ctr.y, p.x - ctr.x) / DEG);
      if (g.mode === 'spin') {
        g.spin!.dragBy(normDeg(ang - g.lastAng!));
        g.lastAng = ang;
        const now = performance.now();
        g.samples!.push({ t: now, o: g.spin!.offset });
        while (g.samples!.length > 2 && now - g.samples![0].t > 100) g.samples!.shift();
      } else this.applyValue(h.kind, ang);
    }
  }
  private gUp(g: Gesture, ev: PointerEvent) {
    if (ev.pointerId !== g.id) return;
    clearTimeout(g.timer);
    press(g.target, false);
    if (g.mode === 'carry') this.c.motion.end();
    else if (g.mode === 'spin') {
      const s = g.samples!;
      const a = s[0];
      const b = s[s.length - 1];
      const dt = (b.t - a.t) / 1000;
      const v = dt > 0.01 && performance.now() - b.t < 90 ? (b.o - a.o) / dt : 0;
      g.spin!.release(v);
    } else if (g.mode === 'pending') this.tap(g);
    this.endGesture(g);
  }
  private endGesture(g: Gesture) {
    if (g.move) window.removeEventListener('pointermove', g.move);
    if (g.up) {
      window.removeEventListener('pointerup', g.up);
      window.removeEventListener('pointercancel', g.up);
    }
    if (g.spin && g.mode === 'pending') g.spin.release(0);
    this.g = null;
    this.c.gesture = null;
    this.c.wake();
  }
  private applyValue(kind: Kind, deg: number) {
    const m = this.c.model;
    if (kind === 'size') m.applySize(widthOfNumber(m.hlMode, sizeNumberAt(deg)));
    else if (kind === 'opacity') m.applyAlpha(alphaAt(deg));
    else if (kind === 'esize') m.setEraser(eraserPtAt(deg));
  }

  private tap(g: Gesture) {
    const c = this.c;
    const h = g.h!;
    const m = c.model;
    const face = g.face!;
    if (g.wasMoving) return;
    switch (h.kind) {
      case 'core':
        tick(8);
        if (m.mode === 'eraser' || m.mode === 'fill' || m.mode === 'lasso') m.setMode(m.drawMode);
        else c.hooks.togglePens?.();
        break;
      case 'stem':
        tick(8);
        if (m.mode === 'eraser') m.toggleTool('eraser');
        else m.setSizeMode(false);
        break;
      case 'btn':
        tick(8);
        if (h.which === 'size') { m.setSizeMode(true); c.hooks.closePens?.(); }
        else if (h.which === 'rainbow') c.hooks.openPicker?.();
        else m.setEraser(null, h.which === 'part');
        break;
      case 'tools': {
        const i = this.toolAt(face, h);
        if (i != null) { tick(8); this.doTool(face.tb[i].id); }
        break;
      }
      case 'spin': {
        const i = this.swatchAt(face, h);
        const col = i != null ? m.rim[i] : null;
        if (col) { tick(8); m.applyColor(col); }
        break;
      }
      case 'size': {
        let best: number | null = null;
        let bd = 7;
        DOT_NUMBERS.forEach((n) => {
          const d = Math.abs(sizeAngle(n) - h.deg);
          if (d <= bd) { bd = d; best = n; }
        });
        if (best != null) { tick(8); m.applySize(widthOfNumber(m.hlMode, best)); }
        break;
      }
      case 'opacity': m.applyAlpha(alphaAt(h.deg)); break;
      case 'esize': {
        let best: number | null = null;
        let bd = 7;
        ERASER_STEPS.forEach((pt, i) => {
          const d = Math.abs(eraserAngle(i) - h.deg);
          if (d <= bd) { bd = d; best = pt; }
        });
        if (best != null) { tick(8); m.setEraser(best); }
        break;
      }
    }
  }
  private toolAt(face: Face, h: Hit) {
    let best: number | null = null;
    let bd = 26;
    const n = face.tb.length;
    const off = face.toolSpin.offset;
    for (let i = 0; i < n; i++) {
      const a = toolAngle(face.full, i, off, n);
      if (toolFade(face.full, a) < 0.4) continue;
      const q = polar(face.g.toolR, toScreen(h.e, a));
      const d = Math.hypot(h.dx - q.x, h.dy - q.y);
      if (d <= bd) { bd = d; best = i; }
    }
    return best;
  }
  private swatchAt(face: Face, h: Hit) {
    const m = this.c.model;
    const n = m.rim.length;
    const step = stepOf(face.full, n);
    let best: number | null = null;
    let bd = 22;
    for (let i = 0; i < n; i++) {
      if (m.rim[i] === null) continue;
      const a = wrapDeg(i * step + face.rimSpin.offset, n * step);
      if (swatchFade(face.g, a, step) < 0.4) continue;
      const q = polar(face.g.rimR, toScreen(h.e, a));
      const d = Math.hypot(h.dx - q.x, h.dy - q.y);
      if (d <= bd) { bd = d; best = i; }
    }
    return best;
  }
  /** A tool tapped again opens its own controls (pen, highlighter); the others go back to the pen. */
  private doTool(id: ToolId) {
    const m = this.c.model;
    if (m.mode === id) {
      if (id === 'pen' || id === 'highlighter') m.setSizeMode(!m.sizeMode);
      else m.toggleTool(id);
    } else {
      m.sizeMode = false;
      m.setMode(id);
    }
    this.c.hooks.closePens?.();
  }

  private gripDown(e: PointerEvent) {
    const c = this.c;
    if (!c.isOpen || e.button > 0 || this.g) return;
    e.preventDefault();
    e.stopPropagation();
    const p0 = this.local(e);
    const g: Gesture = { id: e.pointerId, mode: 'grip', x0: p0.x, y0: p0.y };
    this.g = c.gesture = g;
    const grip = c.dom.grip;
    press(grip, true);
    g.move = (ev) => {
      if (ev.pointerId !== g.id) return;
      const p = this.local(ev);
      if (g.mode === 'grip' && Math.hypot(p.x - g.x0, p.y - g.y0) > GRIP_SLOP) {
        g.mode = 'carry';
        c.hooks.closePens?.();
        c.motion.begin(p);
      } else if (g.mode === 'carry') c.motion.move(p);
    };
    g.up = (ev) => {
      if (ev.pointerId !== g.id) return;
      if (g.mode === 'carry') c.motion.end();
      press(grip, false);
      this.endGesture(g);
    };
    window.addEventListener('pointermove', g.move);
    window.addEventListener('pointerup', g.up);
    window.addEventListener('pointercancel', g.up);
    c.wake();
  }

  private tabDown(e: PointerEvent) {
    const c = this.c;
    if (c.isOpen || e.button > 0 || this.g) return;
    e.preventDefault();
    const p0 = this.local(e);
    const vert = EDGES[c.p.edge].vertical;
    const a0 = clamp(c.tabAlong(), TAB_H / 2, (vert ? c.H : c.W) - TAB_H / 2);
    const g: Gesture = { id: e.pointerId, mode: 'pending', x0: p0.x, y0: p0.y };
    this.g = c.gesture = g;
    const tab = c.dom.tab;
    press(tab, true);
    g.move = (ev) => {
      if (ev.pointerId !== g.id) return;
      const p = this.local(ev);
      const d = vert ? p.y - g.y0 : p.x - g.x0;
      if (g.mode === 'pending' && Math.hypot(p.x - g.x0, p.y - g.y0) > TOUCH_SLOP) {
        g.mode = 'drag';
        if (tab) tab.dataset.drag = '';
      }
      if (g.mode === 'drag') {
        const len = vert ? c.H : c.W;
        c.p = { ...c.p, along: clamp(a0 + d, TAB_H / 2, len - TAB_H / 2) };
        c.dirty = true;
        c.wake();
      }
    };
    g.up = (ev) => {
      if (ev.pointerId !== g.id) return;
      press(tab, false);
      if (tab) delete tab.dataset.drag;
      this.endGesture(g);
      if (g.mode === 'pending') {
        c.p = { ...c.p, docked: true };
        c.motion.reset();
        c.open();
      } else c.hooks.onPlace?.(c.p);
      c.wake();
    };
    window.addEventListener('pointermove', g.move);
    window.addEventListener('pointerup', g.up);
    window.addEventListener('pointercancel', g.up);
  }
}
