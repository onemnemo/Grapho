/* The wheel controller: placement, open and close, the one frame loop step, and the handle and tab.
   React renders the markup once and hands over the elements; every frame is written from here as
   transforms and opacities. Pointer handling lives in input.ts. */
import { CARRY_SCALE, DISC_R, DOCKED_SCALE, EDGES, POPUP_R, TAB_H, TAB_W, handleTopLeft, type Edge, type Placement } from './geometry';
import { Face, emptyRefs, type FaceRefs } from './face';
import { Motion, type MotionHost, type Pose } from './motion';
import { clamp, mix, smooth } from './math';
import type { WheelModel } from './model';
import { Ease, Loop, Spring, type Tickable } from './springs';

export interface WheelHooks {
  /** The disc or circle was put down somewhere new. */
  onPlace?(p: Placement): void;
  onOpenChange?(open: boolean): void;
  onPinChange?(pinned: boolean): void;
  /** The core was tapped and the pen list should toggle. */
  togglePens?(): void;
  closePens?(): void;
  openPicker?(): void;
  /** Carry started or ended; floating panels hide while it lasts. */
  carry?(on: boolean): void;
  closeAll?(): void;
}

export interface WheelDom {
  layer: HTMLElement | null;
  hint: HTMLElement | null;
  tip: HTMLElement | null;
  handle: HTMLElement | null;
  grip: HTMLElement | null;
  pin: HTMLElement | null;
  pinIcon: HTMLElement | null;
  tab: HTMLElement | null;
  disc: FaceRefs;
  circ: FaceRefs;
}
export const emptyDom = (): WheelDom => ({ layer: null, hint: null, tip: null, handle: null, grip: null, pin: null, pinIcon: null, tab: null, disc: emptyRefs(), circ: emptyRefs() });

export interface ControllerOptions {
  model: WheelModel;
  dom: WheelDom;
  placement: Placement;
  open: boolean;
  pose?: Pose;
  hooks: WheelHooks;
}

export class WheelController implements MotionHost, Tickable {
  model: WheelModel;
  dom: WheelDom;
  hooks: WheelHooks;
  p: Placement;
  W = 0;
  H = 0;
  isOpen: boolean;
  dim: number | null = null;
  dirty = true;
  disc: Face;
  circ: Face;
  motion: Motion;
  openE = new Ease(0);
  pinAnim: Spring;
  private eases: Ease[] = [];
  private registered = false;
  private alive = true;
  private handleState = { tx: '', op: '', inter: null as boolean | null, hid: null as boolean | null };
  private tabState = { tx: '', op: '', hid: null as boolean | null };
  tabAlongV: number | null = null;
  gesture: { mode: string } | null = null;

  constructor(o: ControllerOptions) {
    this.model = o.model;
    this.dom = o.dom;
    this.hooks = o.hooks;
    this.p = { ...o.placement };
    this.isOpen = o.open;
    this.disc = new Face(this, false);
    this.circ = new Face(this, true);
    this.disc.refs = o.dom.disc;
    this.circ.refs = o.dom.circ;
    this.motion = new Motion(this);
    this.pinAnim = new Spring(o.model.pinned ? 1 : 0, 500, 0.7);
    this.eases.push(this.openE);
    if (o.open) this.openE.set(1);
    if (o.pose) this.setPose(o.pose);
  }

  setFlag(name: 'sliding' | 'carrying', on: boolean) {
    const l = this.dom.layer;
    if (!l) return;
    if (on) l.dataset[name] = '';
    else delete l.dataset[name];
  }
  commit(p: Placement) {
    this.p = { ...p };
    this.hooks.onPlace?.(this.p);
  }
  carry(on: boolean) {
    this.hooks.carry?.(on);
  }
  markDirty() {
    this.dirty = true;
  }
  track(...e: Ease[]) {
    this.eases.push(...e);
  }

  /** Start up after React has rendered: sync the markup with the model and draw the first frame. */
  start() {
    this.syncStructure();
    this.updateTabDom();
    this.syncPin();
    this.centerSelected();
    this.render(true);
    this.wake();
  }
  syncStructure() {
    this.disc.syncStructure();
    this.circ.syncStructure();
    if (this.pendingRim != null) {
      this.applyRim(this.pendingRim);
      this.pendingRim = null;
    }
    this.dirty = true;
  }
  private pendingRim: number | null = null;
  /** Turn the rings to a slot, for specimens: colour slot counted along the ring, tool slot along the tools. */
  turnTo(colour?: number, tools?: number) {
    if (colour != null) {
      this.disc.rimSpin.snapTo(-colour * 17);
      this.circ.rimSpin.snapTo(-colour * (360 / this.model.rim.length));
    }
    if (tools != null) {
      this.disc.toolSpin.snapTo(-tools * 38);
      this.circ.toolSpin.snapTo(-tools * (360 / this.model.tools.length));
    }
    this.dirty = true;
    this.wake();
  }
  /** Turn both colour rings so the colour at this ring slot sits in the middle. Applied after the next structure sync. */
  focusRim(idx: number) {
    this.pendingRim = idx;
    this.wake();
  }
  private applyRim(idx: number) {
    this.disc.rimSpin.snapTo(-idx * 17);
    this.circ.rimSpin.snapTo(-idx * (360 / this.model.rim.length));
  }
  dispose() {
    this.alive = false;
    Loop.remove(this);
    this.registered = false;
  }
  resize(W: number, H: number) {
    this.W = W;
    this.H = H;
    this.dirty = true;
    this.wake();
  }
  setPlacement(p: Placement) {
    this.p = { ...p };
    this.motion.reset();
    this.updateTabDom();
    this.dirty = true;
    this.wake();
  }

  wake() {
    if (!this.alive || this.registered) return;
    this.registered = true;
    Loop.add(this);
  }

  tick(dt: number) {
    let busy = false;
    if (this.pendingRim != null && this.disc.rimInSync && this.circ.rimInSync) {
      this.applyRim(this.pendingRim);
      this.pendingRim = null;
    }
    for (const e of this.eases) if (!e.done) { e.step(dt); busy = true; }
    if (this.disc.step(dt)) busy = true;
    if (this.circ.step(dt)) busy = true;
    if (this.motion.tick(dt)) busy = true;
    if (!this.pinAnim.done) { this.pinAnim.step(dt); busy = true; this.applyPin(); }
    this.render();
    if (this.gesture && this.gesture.mode !== 'none') busy = true;
    if (!busy && !this.dirty) { this.registered = false; return false; }
    this.dirty = false;
    return true;
  }

  /** One frame of drawing: transforms and opacities only. */
  render(force = false) {
    if (!this.W) return;
    const m = this.motion;
    const c = m.centre;
    const s = m.shape;
    const lift = m.lift;
    const open = this.openE.x;
    const sm = smooth(clamp(s, 0, 1));
    const fromScale = this.p.docked ? 0.6 : 0.3;
    const openScale = mix(fromScale, 1, open);
    const openAlpha = clamp(open * 1.7, 0, 1);
    const showWheel = open > 0.002;
    const rest = !m.active && this.isOpen;
    const edge = this.p.edge;
    const dim = this.dim ?? 1;
    const discAlpha = showWheel ? (1 - sm) * openAlpha : 0;
    const grown = mix(DOCKED_SCALE, 1, clamp(s, 0, 1)) * (1 + CARRY_SCALE * lift);
    const circAlpha = showWheel ? sm * openAlpha * dim : 0;
    this.disc.place(c.x, c.y, openScale, discAlpha * dim, rest && this.p.docked, 0);
    this.circ.place(c.x, c.y, grown * openScale, circAlpha, rest && !this.p.docked, lift);
    if (discAlpha > 0.003 || force) this.disc.draw(edge);
    if (circAlpha > 0.003 || force) this.circ.draw('left');
    const onDisc = handleTopLeft(DISC_R, edge);
    const onCirc = handleTopLeft(POPUP_R * grown, null);
    const hx = c.x + mix(onDisc.x, onCirc.x, s);
    const hy = c.y + mix(onDisc.y, onCirc.y, s);
    this.placeHandle(hx, hy, mix(0.9, 1, open), showWheel ? openAlpha * dim : 0, rest);
    const hint = this.dom.hint;
    if (m.hintEdge && hint) {
      const hc = m.hintCentre(m.hintEdge);
      const f = m.hintFade;
      hint.style.opacity = f.toFixed(3);
      hint.style.transform = `translate3d(${hc.x.toFixed(2)}px,${hc.y.toFixed(2)}px,0) scale(${(0.96 + 0.04 * f).toFixed(3)})`;
      hint.style.visibility = f > 0.003 ? 'visible' : 'hidden';
    }
    this.placeTab(1 - open);
  }

  private placeHandle(x: number, y: number, sc: number, a: number, inter: boolean) {
    const r = this.dom.handle;
    if (!r) return;
    const H = this.handleState;
    const hide = a <= 0.003;
    if (hide !== H.hid) { H.hid = hide; r.style.visibility = hide ? 'hidden' : 'visible'; }
    if (hide) return;
    const tx = `translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) scale(${sc.toFixed(3)})`;
    if (tx !== H.tx) { H.tx = tx; r.style.transform = tx; }
    const o = a.toFixed(3);
    if (o !== H.op) { H.op = o; r.style.opacity = o; }
    const pe = inter || this.motion.held;
    if (pe !== H.inter) { H.inter = pe; r.style.pointerEvents = pe ? 'auto' : 'none'; }
  }
  setPinned(v: boolean) {
    this.model.setPinned(v);
    this.pinAnim.to(v ? 1 : 0, 520, 0.7);
    this.syncPin();
    this.wake();
    this.hooks.onPinChange?.(v);
  }
  syncPin() {
    const pin = this.dom.pin;
    if (pin) {
      pin.setAttribute('aria-label', this.model.pinned ? 'Unpin wheel' : 'Pin wheel open');
      pin.setAttribute('aria-pressed', String(this.model.pinned));
      if (this.model.pinned) pin.dataset.pinned = '';
      else delete pin.dataset.pinned;
    }
    this.applyPin();
  }
  private applyPin() {
    const t = clamp(this.pinAnim.x, 0, 1.2);
    if (this.dom.pinIcon) this.dom.pinIcon.style.transform = `rotate(${(45 * (1 - t)).toFixed(2)}deg)`;
  }

  tabAlong() {
    return this.tabAlongV ?? this.p.along;
  }
  updateTabDom() {
    const r = this.dom.tab;
    if (!r) return;
    const e = this.p.edge;
    r.dataset.edge = e;
    r.style.width = (EDGES[e].vertical ? TAB_W : TAB_H) + 'px';
    r.style.height = (EDGES[e].vertical ? TAB_H : TAB_W) + 'px';
    this.tabState.tx = '';
  }
  private placeTab(closed: number) {
    const r = this.dom.tab;
    if (!r) return;
    const T = this.tabState;
    const hide = closed <= 0.003;
    if (hide !== T.hid) { T.hid = hide; r.style.visibility = hide ? 'hidden' : 'visible'; r.style.pointerEvents = hide ? 'none' : 'auto'; }
    if (hide) return;
    const e = this.p.edge;
    const { W, H } = this;
    const vert = EDGES[e].vertical;
    const len = vert ? H : W;
    const c = clamp(this.tabAlong(), TAB_H / 2, Math.max(TAB_H / 2, len - TAB_H / 2));
    const x = e === 'left' ? 0 : e === 'right' ? W - TAB_W : c - TAB_H / 2;
    const y = e === 'top' ? 0 : e === 'bottom' ? H - TAB_W : c - TAB_H / 2;
    const k = mix(0.6, 1, clamp(closed, 0, 1));
    const tx = `translate3d(${x.toFixed(2)}px,${y.toFixed(2)}px,0) scale(${k.toFixed(3)})`;
    if (tx !== T.tx) { T.tx = tx; r.style.transform = tx; }
    const o = clamp(closed, 0, 1).toFixed(3);
    if (o !== T.op) { T.op = o; r.style.opacity = o; }
  }

  open(at?: { x: number; y: number }) {
    if (this.isOpen) return;
    this.isOpen = true;
    if (at) {
      this.p = { ...this.p, docked: false, floatX: at.x, floatY: at.y };
      this.motion.reset();
    }
    this.openE.set(0);
    this.openE.spring(1, 420, 0.78);
    this.hooks.onOpenChange?.(true);
    this.wake();
  }
  close() {
    if (!this.isOpen) return;
    this.isOpen = false;
    this.hooks.closeAll?.();
    this.motion.reset();
    this.openE.spring(0, 520, 1);
    this.hooks.onOpenChange?.(false);
    this.wake();
  }
  /** Set open without the sound of it, for controlled use. */
  setOpen(v: boolean) {
    if (v) this.open();
    else this.close();
  }
  setPose(pose: Pose) {
    this.motion.pose = pose;
    if (pose.hintEdge) this.motion.hintEdge = pose.hintEdge;
    if (pose.slide) this.setFlag('sliding', true);
    this.setFlag('carrying', true);
    this.dirty = true;
  }
  /** Turn both rings so the ink colour sits in the middle. */
  centerSelected() {
    const m = this.model;
    const i = m.rim.indexOf(m.setting.color.toUpperCase());
    if (i < 0) return;
    this.disc.rimSpin.snapTo(-i * 17);
    this.circ.rimSpin.snapTo(-i * (360 / m.rim.length));
    this.dirty = true;
    this.wake();
  }

  /** Where a panel goes: on the page side of the wheel. */
  sideAnchor(w: number, h: number) {
    const m = this.motion;
    const c = m.centre;
    const e: Edge = this.p.edge;
    const { W, H } = this;
    let x: number;
    let y: number;
    if (!this.p.docked) {
      const right = W - (c.x + POPUP_R) >= c.x - POPUP_R;
      x = right ? c.x + POPUP_R + 8 : c.x - POPUP_R - 8 - w;
      y = c.y - h / 2;
    } else if (e === 'left') { x = DISC_R + 8; y = c.y - h / 2; }
    else if (e === 'right') { x = W - DISC_R - 8 - w; y = c.y - h / 2; }
    else if (e === 'top') { x = c.x - w / 2; y = DISC_R + 8; }
    else { x = c.x - w / 2; y = H - DISC_R - 8 - h; }
    return { x: clamp(x, 8, W - w - 8), y: clamp(y, 8, H - h - 8) };
  }
}
