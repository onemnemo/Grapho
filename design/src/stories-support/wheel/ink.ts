/* A small ink engine for the playground: real strokes in the wheel's live pen, an eraser that cuts whole
   strokes or parts of them, a lasso, a fill ripple. It exists so the wheel can be tried on a note. */
import { clamp } from '@/blocks/PenWheel/engine/math';
import type { Line, WheelModel } from '@/blocks/PenWheel/engine/model';

interface Pt { x: number; y: number }
interface Stroke { color: string; w: number; alpha: number; line: Line; pts: Pt[] }
interface Live extends Partial<Stroke> { erase?: boolean; lasso?: boolean; last?: Pt; pts: Pt[] }

export interface InkSurface {
  base: HTMLCanvasElement;
  live: HTMLCanvasElement;
  hit: HTMLElement;
  cursor: HTMLElement;
  stage: HTMLElement;
}

export class Ink {
  private strokes: Stroke[] = [];
  private undone: Stroke[] = [];
  private cur: Live | null = null;
  private ctx: CanvasRenderingContext2D;
  private lctx: CanvasRenderingContext2D;
  /** Return true to cancel the stroke (the wheel is being carried). */
  onDown: (() => boolean | void) | null = null;
  onStrokes: (() => void) | null = null;
  private off: Array<() => void> = [];

  constructor(private s: InkSurface, private model: WheelModel, private W: number, private H: number) {
    this.ctx = s.base.getContext('2d')!;
    this.lctx = s.live.getContext('2d')!;
    this.ctx.scale(2, 2);
    this.lctx.scale(2, 2);
    const on = (type: string, fn: (e: PointerEvent) => void) => {
      s.hit.addEventListener(type, fn as EventListener);
      this.off.push(() => s.hit.removeEventListener(type, fn as EventListener));
    };
    on('pointerdown', (e) => this.down(e));
    on('pointermove', (e) => this.hover(e));
    on('pointerleave', () => { s.cursor.style.opacity = '0'; });
  }
  dispose() {
    this.off.forEach((f) => f());
  }
  get canUndo() { return this.strokes.length > 0; }
  get canRedo() { return this.undone.length > 0; }
  private local(e: PointerEvent) {
    const r = this.s.stage.getBoundingClientRect();
    const k = r.width / this.W;
    return { x: (e.clientX - r.left) / k, y: (e.clientY - r.top) / k };
  }
  private styleOf() {
    const m = this.model;
    const hl = m.mode === 'highlighter';
    const s = m.setting;
    return { color: s.color, w: hl ? s.w * 1.5 : Math.max(1.3, s.w * 1.5), alpha: m.alpha / 255, line: s.line };
  }
  private hover(e: PointerEvent) {
    const c = this.s.cursor;
    if (this.model.mode !== 'eraser') { c.style.opacity = '0'; return; }
    const p = this.local(e);
    const d = this.model.eraserPt * 1.1;
    c.style.width = c.style.height = d + 'px';
    c.style.transform = `translate3d(${p.x - d / 2}px,${p.y - d / 2}px,0)`;
    c.style.opacity = '1';
  }
  private down(e: PointerEvent) {
    if (e.button > 0) return;
    if (this.onDown?.()) return;
    e.preventDefault();
    const hit = this.s.hit;
    hit.setPointerCapture(e.pointerId);
    const p = this.local(e);
    const mode = this.model.mode;
    const mv = (ev: PointerEvent) => {
      (ev.getCoalescedEvents ? ev.getCoalescedEvents() : [ev]).forEach((c) => this.move(this.local(c)));
      this.hover(ev);
    };
    const up = () => {
      hit.removeEventListener('pointermove', mv);
      hit.removeEventListener('pointerup', up);
      hit.removeEventListener('pointercancel', up);
      this.end();
    };
    hit.addEventListener('pointermove', mv);
    hit.addEventListener('pointerup', up);
    hit.addEventListener('pointercancel', up);
    if (mode === 'pen' || mode === 'highlighter') {
      this.cur = { ...this.styleOf(), pts: [p] };
      this.s.live.style.opacity = String(this.cur.alpha);
      this.lctx.clearRect(0, 0, this.W, this.H);
      this.lctx.save();
      this.lctx.fillStyle = this.cur.color!;
      this.lctx.beginPath();
      this.lctx.arc(p.x, p.y, this.cur.w! / 2, 0, 7);
      this.lctx.fill();
      this.lctx.restore();
    } else if (mode === 'eraser') { this.cur = { erase: true, last: p, pts: [] }; this.erase(p, p); }
    else if (mode === 'fill') this.ripple(p);
    else if (mode === 'lasso') { this.cur = { lasso: true, pts: [p] }; this.s.live.style.opacity = '1'; }
  }
  private move(p: Pt) {
    const c = this.cur;
    if (!c) return;
    if (c.erase) { this.erase(c.last!, p); c.last = p; return; }
    const a = c.pts[c.pts.length - 1];
    const d = Math.hypot(p.x - a.x, p.y - a.y);
    if (d < 0.8) return;
    const n = Math.floor(d / 3);
    for (let i = 1; i <= n; i++) c.pts.push({ x: a.x + ((p.x - a.x) * i) / (n + 1), y: a.y + ((p.y - a.y) * i) / (n + 1) });
    c.pts.push(p);
    if (c.lasso) {
      const l = this.lctx;
      l.clearRect(0, 0, this.W, this.H);
      l.save();
      l.strokeStyle = getComputedStyle(this.s.stage).getPropertyValue('--fg');
      l.lineWidth = 1.6;
      l.setLineDash([6, 5]);
      l.beginPath();
      c.pts.forEach((q, i) => (i ? l.lineTo(q.x, q.y) : l.moveTo(q.x, q.y)));
      l.stroke();
      l.restore();
      return;
    }
    if (c.line === 'solid') this.seg(this.lctx, c as Stroke);
    else { this.lctx.clearRect(0, 0, this.W, this.H); this.path(this.lctx, c as Stroke); }
  }
  private end() {
    const c = this.cur;
    this.cur = null;
    if (!c) return;
    if (c.lasso) {
      let a = 1;
      const fade = () => {
        a -= 0.08;
        this.s.live.style.opacity = String(Math.max(a, 0));
        if (a > 0) requestAnimationFrame(fade);
        else this.lctx.clearRect(0, 0, this.W, this.H);
      };
      fade();
      return;
    }
    if (c.erase) return;
    const st = c as Stroke;
    this.strokes.push({ color: st.color, w: st.w, alpha: st.alpha, line: st.line, pts: st.pts });
    this.undone.length = 0;
    this.ctx.save();
    this.ctx.globalAlpha = st.alpha;
    this.path(this.ctx, st);
    this.ctx.restore();
    this.lctx.clearRect(0, 0, this.W, this.H);
    this.s.live.style.opacity = '1';
    this.onStrokes?.();
  }
  private setup(ctx: CanvasRenderingContext2D, c: Stroke) {
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.strokeStyle = c.color;
    ctx.fillStyle = c.color;
    ctx.lineWidth = c.w;
    ctx.setLineDash(c.line === 'dashed' ? [c.w * 3, c.w * 2.6] : c.line === 'dotted' ? [0.01, c.w * 2.4] : []);
  }
  private seg(ctx: CanvasRenderingContext2D, c: Stroke) {
    const n = c.pts.length;
    ctx.save();
    this.setup(ctx, c);
    if (n < 3) {
      const a = c.pts[n - 2] || c.pts[0];
      const b = c.pts[n - 1];
      ctx.beginPath(); ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y); ctx.stroke(); ctx.restore();
      return;
    }
    const a = c.pts[n - 3], b = c.pts[n - 2], d = c.pts[n - 1];
    const m1 = { x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 };
    const m2 = { x: (b.x + d.x) / 2, y: (b.y + d.y) / 2 };
    ctx.beginPath(); ctx.moveTo(m1.x, m1.y); ctx.quadraticCurveTo(b.x, b.y, m2.x, m2.y); ctx.stroke(); ctx.restore();
  }
  private path(ctx: CanvasRenderingContext2D, c: Stroke) {
    const pts = c.pts;
    ctx.save();
    this.setup(ctx, c);
    ctx.beginPath();
    if (pts.length === 1) { ctx.arc(pts[0].x, pts[0].y, c.w / 2, 0, 7); ctx.fill(); ctx.restore(); return; }
    ctx.moveTo(pts[0].x, pts[0].y);
    for (let i = 1; i < pts.length - 1; i++) {
      const m = { x: (pts[i].x + pts[i + 1].x) / 2, y: (pts[i].y + pts[i + 1].y) / 2 };
      ctx.quadraticCurveTo(pts[i].x, pts[i].y, m.x, m.y);
    }
    ctx.lineTo(pts[pts.length - 1].x, pts[pts.length - 1].y);
    ctx.stroke();
    ctx.restore();
  }
  private redraw() {
    this.ctx.clearRect(0, 0, this.W, this.H);
    this.strokes.forEach((s) => { this.ctx.save(); this.ctx.globalAlpha = s.alpha; this.path(this.ctx, s); this.ctx.restore(); });
  }
  private erase(a: Pt, b: Pt) {
    const m = this.model;
    const r = m.eraserPt * 0.55;
    const area = m.eraserArea;
    let changed = false;
    const hit = (q: Pt) => {
      const dx = b.x - a.x, dy = b.y - a.y, l2 = dx * dx + dy * dy || 1;
      const t = clamp(((q.x - a.x) * dx + (q.y - a.y) * dy) / l2, 0, 1);
      return Math.hypot(q.x - (a.x + dx * t), q.y - (a.y + dy * t));
    };
    const out: Stroke[] = [];
    for (const s of this.strokes) {
      const rr = r + s.w / 2;
      if (!area) {
        if (s.pts.some((q) => hit(q) <= rr)) { changed = true; continue; }
        out.push(s);
      } else {
        let run: Pt[] = [];
        const pieces: Pt[][] = [];
        let cut = false;
        for (const q of s.pts) {
          if (hit(q) <= rr) { cut = true; if (run.length) pieces.push(run); run = []; } else run.push(q);
        }
        if (run.length) pieces.push(run);
        if (!cut) out.push(s);
        else { changed = true; pieces.filter((p) => p.length > 1).forEach((p) => out.push({ ...s, pts: p })); }
      }
    }
    if (changed) { this.strokes = out; this.redraw(); }
  }
  private ripple(p: Pt) {
    const d = document.createElement('div');
    d.style.cssText = `position:absolute;left:${p.x - 6}px;top:${p.y - 6}px;width:12px;height:12px;border-radius:50%;background:${this.model.setting.color};opacity:.5;pointer-events:none;transition:transform .5s var(--ease),opacity .5s`;
    this.s.stage.appendChild(d);
    requestAnimationFrame(() => { d.style.transform = 'scale(14)'; d.style.opacity = '0'; });
    setTimeout(() => d.remove(), 560);
  }
  undo() { const s = this.strokes.pop(); if (s) { this.undone.push(s); this.redraw(); } this.onStrokes?.(); }
  redo() { const s = this.undone.pop(); if (s) { this.strokes.push(s); this.redraw(); } this.onStrokes?.(); }
}
