/* One face of the wheel: the docked half disc or the floating circle. It owns the turning rings,
   the selection rings and the editing state, and writes transforms and opacities onto the elements
   React rendered (see WheelFace). Nothing here reads layout. */
import { ALPHA_MAX, DOT_NUMBERS, EDGES, ERASER_STEPS, PART_AT, TOOL_REACH, TOOL_STEP, WHOLE_AT, angleOfAlpha, dotRadius, makeGeo, nearestDot, nearestEraserStep, sizeAngle, stepOf, swatchFade, toScreen, toolAngle, toolFade, eraserAngle, type Edge, type Geo } from './geometry';
import { clamp, hexOf, hexToRgba, normDeg, polar, tick, wrapDeg } from './math';
import { Ease, Spin } from './springs';
import { drawBand, bandColours, type BandColours } from './band';
import { nibTipPath } from '@/assets/wheel/PenGlyph';
import type { WheelModel, ToolId } from './model';

export interface FaceRefs {
  root: HTMLElement | null;
  lift: HTMLElement | null;
  stem: SVGSVGElement | null;
  stemPath: SVGPathElement | null;
  read: HTMLElement | null;
  readText: HTMLElement | null;
  scrollSvg: SVGSVGElement | null;
  scrollTrack: SVGPathElement | null;
  scrollThumb: SVGPathElement | null;
  band: HTMLCanvasElement | null;
  marker: HTMLElement | null;
  plate: HTMLElement | null;
  nibBox: HTMLElement | null;
  tipPen: SVGPathElement | null;
  tipHl: SVGPathElement | null;
  btnSize: HTMLElement | null;
  btnRain: HTMLElement | null;
  btnWhole: HTMLElement | null;
  btnPart: HTMLElement | null;
  ringSw: HTMLElement | null;
  ringSd: HTMLElement | null;
  ringEd: HTMLElement | null;
  sw: Array<HTMLElement | null>;
  dots: Array<HTMLElement | null>;
  edots: Array<HTMLElement | null>;
  tb: Array<HTMLElement | null>;
}
export const emptyRefs = (): FaceRefs => ({
  root: null, lift: null, stem: null, stemPath: null, read: null, readText: null, scrollSvg: null, scrollTrack: null, scrollThumb: null,
  band: null, marker: null, plate: null, nibBox: null, tipPen: null, tipHl: null, btnSize: null, btnRain: null, btnWhole: null, btnPart: null,
  ringSw: null, ringSd: null, ringEd: null, sw: [], dots: [], edots: [], tb: [],
});

export interface FaceHost {
  model: WheelModel;
  /** Ask for another frame. */
  wake(): void;
}

interface RingState {
  el: HTMLElement | null;
  id: number | null;
  k: Ease;
  vis: Ease;
  ox: number; oy: number; px: number; py: number; os: number; sc: number;
  has: boolean;
}
const newRing = (): RingState => ({ el: null, id: null, k: new Ease(0), vis: new Ease(0), ox: 0, oy: 0, px: 0, py: 0, os: 0, sc: 1, has: false });

interface SwItem { strip: boolean; c: string | null; vis: number }
interface ToolItem { id: ToolId; vis: number; on: boolean; a: number | null }
interface DotItem { ang: number; r: number; n: number; placed: boolean; edge: Edge | null; px: number; py: number; on: boolean }

const setFlag = (el: HTMLElement | null, key: string, on: boolean) => {
  if (!el) return;
  if (on) el.dataset[key] = '';
  else delete el.dataset[key];
};



export class Face {
  g: Geo;
  refs: FaceRefs = emptyRefs();
  rimSpin: Spin;
  toolSpin: Spin;
  editE = new Ease(0);
  plateE = new Ease(1);
  rings = { sw: newRing(), sd: newRing(), ed: newRing() };
  sw: SwItem[] = [];
  tb: ToolItem[] = [];
  dots: DotItem[];
  edots: DotItem[];
  hidden = true;
  rimView: string | null = null;
  plateFrom = 0;
  plateIdx = -2;
  plateNew = false;
  private tx = '';
  private op = '';
  private inter: boolean | null = null;
  private liftV = '';
  private last: Record<string, unknown> = {};
  private bandInk: string | null = null;
  private bandCols: BandColours | null = null;
  private markerTxt: string | null = null;
  private btnKey = new WeakMap<HTMLElement, string>();
  private rimN = -1;
  private toolN = -1;

  constructor(private host: FaceHost, public full: boolean) {
    this.g = makeGeo(full);
    this.dots = DOT_NUMBERS.map((n) => ({ n, ang: sizeAngle(n), r: dotRadius(n), placed: false, edge: null, px: 0, py: 0, on: false }));
    this.edots = ERASER_STEPS.map((_, i) => ({ n: i, ang: eraserAngle(i), r: dotRadius(1 + (99 * i) / (ERASER_STEPS.length - 1)), placed: false, edge: null, px: 0, py: 0, on: false }));
    this.rimSpin = this.makeRimSpin(host.model.rim.length);
    this.toolSpin = this.makeToolSpin(host.model.tools.length);
  }

  syncStructure() {
    const m = this.host.model;
    if (this.rimN !== m.rim.length || this.last.rimVersion !== m.rimVersion) {
      const first = this.rimN < 0;
      const off = this.rimSpin.offset;
      this.rimN = m.rim.length;
      this.sw = m.rim.map((c) => ({ strip: c === null, c, vis: -1 }));
      this.rimSpin = this.makeRimSpin(m.rim.length);
      if (!first) this.rimSpin.snapTo(this.full ? 0 : off);
    }
    if (this.toolN !== m.tools.length || this.last.toolVersion !== m.toolsVersion) {
      this.toolN = m.tools.length;
      this.tb = m.tools.map((id) => ({ id, vis: -1, on: false, a: null }));
      this.toolSpin = this.makeToolSpin(m.tools.length);
      this.plateIdx = -2;
    }
    this.last.rimVersion = m.rimVersion;
    this.last.toolVersion = m.toolsVersion;
    this.rings.sw.el = this.refs.ringSw;
    this.rings.sd.el = this.refs.ringSd;
    this.rings.ed.el = this.refs.ringEd;
    this.host.wake();
  }
  private makeRimSpin(n: number) {
    const s = new Spin(stepOf(this.full, n));
    s.onDetent = () => tick(8);
    s.onChange = () => this.host.wake();
    return s;
  }
  private makeToolSpin(n: number) {
    const last = Math.max(n - 1, 0);
    const s = this.full
      ? new Spin(n > 0 ? 360 / n : TOOL_STEP)
      : new Spin(TOOL_STEP, { min: -last * TOOL_STEP, max: 0, start: -Math.min(Math.floor((n - 1) / 2), 2) * TOOL_STEP });
    s.onDetent = () => tick(6);
    s.onChange = () => this.host.wake();
    return s;
  }

  /** True when the ring markup matches the model's colour ring. */
  get rimInSync() {
    return this.rimN === this.host.model.rim.length && this.last.rimVersion === this.host.model.rimVersion;
  }

  /** Step this face's own animations. Returns true while something still moves. */
  step(dt: number) {
    let busy = false;
    if (!this.plateE.done) { this.plateE.step(dt); busy = true; }
    if (!this.editE.done) { this.editE.step(dt); busy = true; }
    for (const R of Object.values(this.rings)) {
      if (!R.k.done) { R.k.step(dt); busy = true; }
      if (!R.vis.done) { R.vis.step(dt); busy = true; }
    }
    if (this.rimSpin.tick(dt)) busy = true;
    if (this.toolSpin.tick(dt)) busy = true;
    return busy;
  }

  place(cx: number, cy: number, scale: number, alpha: number, interactive: boolean, lift: number) {
    const root = this.refs.root;
    if (!root) return;
    const hide = alpha <= 0.003;
    if (hide !== this.hidden) {
      this.hidden = hide;
      root.style.display = hide ? 'none' : '';
    }
    if (hide) return;
    const tx = `translate3d(${cx.toFixed(2)}px,${cy.toFixed(2)}px,0) scale(${scale.toFixed(4)})`;
    if (tx !== this.tx) { this.tx = tx; root.style.transform = tx; }
    const o = alpha.toFixed(3);
    if (o !== this.op) { this.op = o; root.style.opacity = o; }
    if (interactive !== this.inter) {
      this.inter = interactive;
      root.style.pointerEvents = interactive ? 'auto' : 'none';
      setFlag(root, 'live', interactive);
    }
    const l = lift.toFixed(3);
    const liftEl = this.refs.lift;
    if (l !== this.liftV && liftEl) {
      this.liftV = l;
      liftEl.style.opacity = l;
      liftEl.style.transform = `translate3d(0,${(8 * lift).toFixed(2)}px,0)`;
      setFlag(root, 'lifted', lift > 0.05);
    }
  }

  private ringTo(R: RingState, id: number | null, x: number, y: number, scale: number, show: boolean) {
    const el = R.el;
    if (!el) return;
    if (!show || id == null) {
      if (R.has) { R.has = false; R.vis.spring(0, 520, 1); }
      R.id = null;
      if (R.vis.x < 0.01) { el.style.opacity = '0'; return; }
    } else if (id !== R.id) {
      if (R.has && R.vis.x > 0.3) {
        R.ox = R.px - x; R.oy = R.py - y; R.os = R.sc - scale;
        R.k.set(1); R.k.spring(0, 360, 0.78);
      } else {
        R.ox = R.oy = R.os = 0;
        R.k.set(0); R.vis.set(0);
      }
      R.id = id; R.has = true; R.vis.spring(1, 520, 0.9);
    }
    const k = R.k.x;
    R.px = x + R.ox * k; R.py = y + R.oy * k; R.sc = scale + R.os * k;
    const v = clamp(R.vis.x, 0, 1);
    el.style.opacity = v.toFixed(3);
    el.style.transform = `translate3d(${R.px.toFixed(2)}px,${R.py.toFixed(2)}px,0) scale(${(R.sc * (0.7 + 0.3 * v)).toFixed(3)})`;
  }

  private placeBtn(b: HTMLElement | null, r: number, deg: number) {
    if (!b) return;
    const k = r.toFixed(1) + deg.toFixed(1);
    if (this.btnKey.get(b) === k) return;
    this.btnKey.set(b, k);
    const p = polar(r, deg);
    b.style.transform = `translate3d(${p.x.toFixed(2)}px,${p.y.toFixed(2)}px,0)`;
  }

  /** Turn the whole layout onto the wheel's edge: the nib and the opacity band. */
  private orient(e: Edge) {
    const E = EDGES[e];
    const nib = this.refs.nibBox;
    if (nib) {
      if (this.full) nib.style.transform = 'translate3d(0,0,0) rotate(-40deg)';
      else {
        const p = polar(26, E.face);
        nib.style.transform = `translate3d(${p.x}px,${p.y}px,0) rotate(${E.face}deg)`;
      }
    }
    if (this.refs.band) this.refs.band.style.transform = `rotate(${E.face}deg) scale(1,${E.mirror})`;
    this.dots.forEach((d) => { d.placed = false; });
    this.edots.forEach((d) => { d.placed = false; });
    for (const b of [this.refs.btnSize, this.refs.btnRain, this.refs.btnWhole, this.refs.btnPart]) if (b) this.btnKey.delete(b);
  }

  draw(e: Edge) {
    const m = this.host.model;
    const R = this.refs;
    const root = R.root;
    if (!root) return;
    const g = this.g;
    const erasing = m.mode === 'eraser';
    const view = erasing ? 'erase' : m.sizeMode ? 'size' : 'colour';
    const editing = erasing || m.sizeMode;
    if (view !== this.rimView) { this.rimView = view; root.dataset.view = view; }
    if (e !== this.last.edge) { this.last.edge = e; this.orient(e); }
    const setting = m.setting;
    const alpha = m.alpha;
    const ink = setting.color;
    const sizeNumber = m.sizeNumber;
    if (this.last.ink !== ink || this.last.alpha !== alpha || this.last.size !== sizeNumber || this.last.mode !== m.mode) {
      const t = clamp((sizeNumber - 1) / 99, 0, 1);
      root.style.setProperty('--ink', hexToRgba(ink, alpha / ALPHA_MAX));
      root.style.setProperty('--ink-solid', ink);
      root.style.setProperty('--dotr', dotRadius(sizeNumber).toFixed(2) + 'px');
      if (R.nibBox) R.nibBox.dataset.kind = m.nibKind;
      R.tipPen?.setAttribute('d', nibTipPath('pen', t));
      R.tipHl?.setAttribute('d', nibTipPath('highlighter', t));
      setFlag(R.nibBox, 'dim', !(m.mode === 'pen' || m.mode === 'highlighter'));
      this.last.ink = ink; this.last.alpha = alpha; this.last.size = sizeNumber; this.last.mode = m.mode;
    }
    const inPalette = m.rim.includes(hexOf(ink));
    if (this.last.inPal !== inPalette) { this.last.inPal = inPalette; setFlag(R.btnRain, 'off', !inPalette); }
    setFlag(R.btnWhole, 'on', !m.eraserArea);
    setFlag(R.btnPart, 'on', m.eraserArea);

    if (view === 'colour') this.drawColours(e, ink);
    if (view === 'size') {
      const sel = nearestDot(sizeNumber);
      this.dots.forEach((d, i) => {
        const el = R.dots[i];
        if (!el) return;
        if (!d.placed || d.edge !== e) {
          const p = polar(g.rimR, toScreen(e, d.ang));
          el.style.transform = `translate3d(${p.x.toFixed(2)}px,${p.y.toFixed(2)}px,0)`;
          d.placed = true; d.edge = e; d.px = p.x; d.py = p.y;
        }
        const on = d.n === sel;
        if (on !== d.on) { d.on = on; setFlag(el, 'sel', on); }
        if (on) this.ringTo(this.rings.sd, d.n, d.px, d.py, (d.r + 5) / 14, true);
      });
      const cv = R.band;
      if (cv && (this.bandInk !== ink || !this.bandCols)) {
        this.bandCols = this.bandCols ?? bandColours(root);
        this.bandInk = ink;
        drawBand(cv, (g.rimR + 15) * 2, g.rimR, ink, this.bandCols);
      }
      if (R.marker) {
        const ma = polar(g.rimR, toScreen(e, angleOfAlpha(alpha)));
        R.marker.style.transform = `translate3d(${ma.x.toFixed(2)}px,${ma.y.toFixed(2)}px,0)`;
        const pc = String(Math.round(alpha / 2.55));
        if (this.markerTxt !== pc) { this.markerTxt = pc; R.marker.textContent = pc; }
      }
    }
    if (view === 'erase') {
      const sel = nearestEraserStep(m.eraserPt);
      this.edots.forEach((d, i) => {
        const el = R.edots[i];
        if (!el) return;
        if (!d.placed || d.edge !== e) {
          const p = polar(g.rimR, toScreen(e, d.ang));
          el.style.transform = `translate3d(${p.x.toFixed(2)}px,${p.y.toFixed(2)}px,0)`;
          d.placed = true; d.edge = e; d.px = p.x; d.py = p.y;
        }
        const on = i === sel;
        if (on !== d.on) { d.on = on; setFlag(el, 'sel', on); }
        if (on) this.ringTo(this.rings.ed, i, d.px, d.py, (d.r + 5) / 14, true);
      });
      this.placeBtn(R.btnWhole, g.rimR, toScreen(e, WHOLE_AT));
      this.placeBtn(R.btnPart, g.rimR, toScreen(e, PART_AT));
    }
    this.drawTools(e);
    this.drawEdit(e, editing);
    this.drawScroll(e);
  }

  private drawColours(e: Edge, ink: string) {
    const m = this.host.model;
    const g = this.g;
    const n = m.rim.length;
    const step = stepOf(this.full, n);
    const off = this.rimSpin.offset;
    let selI = -1, selX = 0, selY = 0, selF = 0, selA = 0;
    for (let i = 0; i < n; i++) {
      const s = this.sw[i];
      const el = this.refs.sw[i];
      if (!s || !el) continue;
      const a = wrapDeg(i * step + off, n * step);
      const fade = swatchFade(g, a, step);
      if (fade <= 0) {
        if (s.vis !== 0) { s.vis = 0; el.style.opacity = '0'; el.style.visibility = 'hidden'; }
        continue;
      }
      if (s.vis !== 1) { s.vis = 1; el.style.visibility = 'visible'; }
      const sa = toScreen(e, a);
      const p = polar(g.rimR, sa);
      if (s.strip) {
        el.style.opacity = (0.22 * fade).toFixed(3);
        el.style.transform = `translate3d(${p.x.toFixed(2)}px,${p.y.toFixed(2)}px,0) rotate(${sa.toFixed(2)}deg)`;
      } else {
        const lens = 1 + 0.16 * clamp(1 - Math.abs(a) / (step * 1.25), 0, 1) * (this.full ? 0 : 1);
        el.style.opacity = fade.toFixed(3);
        el.style.transform = `translate3d(${p.x.toFixed(2)}px,${p.y.toFixed(2)}px,0) scale(${(lens * (0.86 + 0.14 * fade)).toFixed(3)})`;
        const sel = (s.c ?? '').toUpperCase() === hexOf(ink);
        if (sel && (selI < 0 || Math.abs(a) < Math.abs(selA))) { selI = i; selX = p.x; selY = p.y; selF = fade; selA = a; }
      }
    }
    this.ringTo(this.rings.sw, selI < 0 ? null : selI, selX, selY, 1, selI >= 0 && selF > 0.25);
    this.placeBtn(this.refs.btnSize, g.rimR, toScreen(e, g.sizeBtn));
    this.placeBtn(this.refs.btnRain, g.rimR, toScreen(e, g.rainbow));
  }

  /** The open controls of the selected tool: the grown plate, the readout, the other tools stepping in. */
  private drawEdit(e: Edge, editing: boolean) {
    const g = this.g;
    const k = this.editE.x;
    const ai = this.tb.findIndex((b) => b.on);
    if (editing !== this.last.editing) {
      this.last.editing = editing;
      this.editE.spring(editing ? 1 : 0, 380, 0.82);
      this.host.wake();
      setFlag(this.refs.root, 'editing', editing);
      if (editing && ai >= 0) {
        const n = this.tb.length;
        const off = this.toolSpin.offset;
        this.toolSpin.glideTo(this.full ? -ai * (360 / n) + 360 * Math.round((off + ai * (360 / n)) / 360) : -ai * TOOL_STEP);
      }
    }
    const R = this.refs;
    const show = k > 0.002 && ai >= 0;
    if (!show) {
      if (this.last.stemOn) {
        this.last.stemOn = false;
        if (R.stem) R.stem.style.display = 'none';
        if (R.read) R.read.style.opacity = '0';
      }
      return;
    }
    if (!this.last.stemOn) { this.last.stemOn = true; if (R.stem) R.stem.style.display = ''; }
    const ta = this.tb[ai].a ?? 0;
    const sa = toScreen(e, ta);
    const rr = 21 * (this.full ? 0.88 : 1);
    const c0 = g.toolR;
    const c1 = g.toolR + (g.rimR - g.toolR) * clamp(k, 0, 1.06);
    if (R.stem) R.stem.style.transform = `rotate(${sa.toFixed(2)}deg)`;
    R.stemPath?.setAttribute('d', `M${c0.toFixed(1)} ${-rr}H${c1.toFixed(1)}A${rr} ${rr} 0 0 1 ${c1.toFixed(1)} ${rr}H${c0.toFixed(1)}A${rr} ${rr} 0 0 1 ${c0.toFixed(1)} ${-rr}Z`);
    const rp = polar(g.rimR, sa);
    if (R.read) {
      R.read.style.transform = `translate3d(${rp.x.toFixed(2)}px,${rp.y.toFixed(2)}px,0)`;
      R.read.style.opacity = clamp((k - 0.5) * 2, 0, 1).toFixed(3);
    }
    const m = this.host.model;
    const txt = m.mode === 'eraser' ? String(Math.round(m.eraserPt)) : String(m.sizeNumber);
    if (this.last.readTxt !== txt) { this.last.readTxt = txt; if (R.readText) R.readText.textContent = txt; }
  }

  /** The grey marker on the tool band edge: where the visible tools sit in the list. */
  private drawScroll(e: Edge) {
    const R = this.refs;
    if (!R.scrollSvg) return;
    if (this.full) { R.scrollSvg.style.display = 'none'; return; }
    const m = this.host.model;
    const n = this.tb.length;
    const off = this.toolSpin.offset;
    const over = n * TOOL_STEP > 2 * TOOL_REACH - 20;
    const key = `${over}:${off.toFixed(1)}${e}${m.sizeMode}${m.mode}`;
    if (key === this.last.scrollKey) return;
    this.last.scrollKey = key;
    const hide = !over || m.sizeMode || m.mode === 'eraser';
    R.scrollSvg.style.opacity = hide ? '0' : '1';
    if (!over) return;
    const r = this.g.inner - 5;
    const lo = clamp((-TOOL_REACH - off) / TOOL_STEP, 0, n - 1);
    const hi = clamp((TOOL_REACH - off) / TOOL_STEP, 0, n - 1);
    const A = 78;
    const a0 = -A + (2 * A * lo) / (n - 1);
    const a1 = -A + (2 * A * hi) / (n - 1);
    const arc = (x: number, y: number) => {
      const p = polar(r, toScreen(e, x));
      const q = polar(r, toScreen(e, y));
      const sw = EDGES[e].mirror === 1 ? 1 : 0;
      return `M${p.x.toFixed(1)} ${p.y.toFixed(1)}A${r} ${r} 0 0 ${sw} ${q.x.toFixed(1)} ${q.y.toFixed(1)}`;
    };
    R.scrollTrack?.setAttribute('d', arc(-A, A));
    R.scrollThumb?.setAttribute('d', arc(a0, Math.max(a1, a0 + 1)));
  }

  isActive(id: ToolId) {
    return this.host.model.mode === id;
  }

  private drawTools(e: Edge) {
    const g = this.g;
    const n = this.tb.length;
    const off = this.toolSpin.offset;
    let activeIdx = -1;
    const ek = this.editE.x;
    this.tb.forEach((b, i) => {
      const el = this.refs.tb[i];
      if (!el) return;
      const a = toolAngle(this.full, i, off, n);
      const fade = toolFade(this.full, a);
      const on = this.isActive(b.id);
      if (on) activeIdx = i;
      if (on !== b.on) {
        b.on = on;
        setFlag(el, 'on', on);
        if (on) { delete el.dataset.pop; void el.offsetWidth; el.dataset.pop = ''; }
      }
      b.a = a;
      const collapse = !on ? clamp(ek * 1.5, 0, 1) : 0;
      const vis = fade * (1 - collapse) > 0.002 ? 1 : 0;
      if (vis === 0) {
        if (b.vis !== 0) { b.vis = 0; el.style.opacity = '0'; el.style.visibility = 'hidden'; }
        return;
      }
      if (b.vis !== 1) { b.vis = 1; el.style.visibility = 'visible'; }
      const selA = this.tb.findIndex((q) => q.on);
      const ta2 = selA >= 0 && this.tb[selA].a != null ? (this.tb[selA].a as number) : a;
      const aa = on ? a : a + normDeg(ta2 - a) * ek * 0.8;
      const p = polar(g.toolR, toScreen(e, aa));
      el.style.opacity = (fade * (1 - collapse)).toFixed(3);
      el.style.transform = `translate3d(${p.x.toFixed(2)}px,${p.y.toFixed(2)}px,0) scale(${(1 - 0.3 * collapse).toFixed(3)})`;
      setFlag(el, 'nohit', fade < 0.4 || (!on && ek > 0.05));
    });
    /* the plate slides to the active tool along the ring */
    if (activeIdx !== this.plateIdx) {
      const prev = this.plateIdx;
      if (prev >= 0 && this.tb[prev]) this.plateFrom = this.tb[prev].a ?? 0;
      else if (activeIdx >= 0) this.plateFrom = this.tb[activeIdx].a ?? 0;
      this.plateNew = prev < 0;
      this.plateIdx = activeIdx;
      this.plateE.set(0);
      this.plateE.spring(1, 520, 0.74);
      this.host.wake();
    }
    const plate = this.refs.plate;
    if (!plate) return;
    if (activeIdx >= 0) {
      const b = this.tb[activeIdx];
      const ta = b.a ?? 0;
      const k = this.plateE.x;
      const ang = this.plateFrom + normDeg(ta - this.plateFrom) * k;
      const p = polar(g.toolR, toScreen(e, ang));
      const fadeNow = toolFade(this.full, ang);
      const s = this.plateNew ? 0.5 + 0.5 * clamp(k, 0, 1.2) : 1;
      plate.style.opacity = (this.editE.x > 0.002 ? 0 : fadeNow * (this.plateNew ? clamp(k, 0, 1) : 1)).toFixed(3);
      plate.style.transform = `translate3d(${p.x.toFixed(2)}px,${p.y.toFixed(2)}px,0) scale(${s.toFixed(3)})`;
    } else plate.style.opacity = '0';
  }
}

