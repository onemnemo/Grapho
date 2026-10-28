import { useEffect, useLayoutEffect, useMemo, useRef, useState, useSyncExternalStore, type CSSProperties } from 'react';
import { Icon } from '@/assets/icons';
import { usePresence } from '@/lib/usePresence';
import { ColourPicker } from '@/blocks/ColourPicker';
import { CustomiseWheel } from '@/blocks/CustomiseWheel';
import { ManageColours } from '@/blocks/ManageColours';
import { PenList } from '@/blocks/PenList';
import { ToastProvider, useToast } from '@/components/Toast';
import { PanelModal } from '@/blocks/WheelPanel';
import { DISC_CY, type Placement } from './engine/geometry';
import { WheelController, emptyDom, type WheelDom } from './engine/controller';
import { WheelInput } from './engine/input';
import { WheelModel, type ModelOptions } from './engine/model';
import type { Pose } from './engine/motion';
import { WheelFace } from './WheelFace';
import styles from './PenWheel.module.css';

export interface PenWheelProps {
  /** The wheel's state: tool, ink, pens, colour sets. Share one with your canvas. Without it the wheel keeps its own. */
  model?: WheelModel;
  /** Starting state when the wheel makes its own model. */
  modelOptions?: ModelOptions;
  /** Docked half disc on an edge, or the floating full circle. */
  placement?: Partial<Placement>;
  /** The wheel was put down somewhere new. */
  onPlacementChange?: (placement: Placement) => void;
  /** Open as the wheel, or closed as the tab on the edge. */
  open?: boolean;
  onOpenChange?: (open: boolean) => void;
  /** Hold a pose without a finger, for specimens: mid-carry, over a dock edge. */
  pose?: Pose;
  /** Start with the rings turned: a colour slot and a tool slot, counted from the first. */
  turn?: { colour?: number; tools?: number };
  /** Open a panel at the start, for specimens. */
  initialPanel?: 'pens' | 'customise-tools' | 'customise-pens' | 'picker' | 'colours';
  /** Panels (pens, colours, customise) can be turned off for a bare wheel. */
  panels?: boolean;
  className?: string;
}

const DEFAULT_PLACEMENT: Placement = { docked: true, edge: 'left', along: DISC_CY + 40, floatX: 420, floatY: 360 };

type Modal = { kind: 'customise'; tab: 'tools' | 'pens'; open?: number } | { kind: 'picker'; edit?: { s: number; i: number } } | { kind: 'colours'; pick?: { s: number; i: number } | null };

/**
 * The pen wheel. Docked it is a half disc on any edge; pulled into the page it is a full circle. The colour
 * ring and the tool ring turn, tapping the chosen pen opens its size and opacity inside the wheel, and the
 * middle opens your pens. Place it inside a positioned parent: it fills it.
 */
export function PenWheel(props: PenWheelProps) {
  return (
    <ToastProvider placement="container" variant="pill">
      <PenWheelBody {...props} />
    </ToastProvider>
  );
}

function PenWheelBody({ model: given, modelOptions, placement, onPlacementChange, open = true, onOpenChange, pose, turn, initialPanel, panels = true, className }: PenWheelProps) {
  const own = useMemo(() => new WheelModel(modelOptions), []); // eslint-disable-line react-hooks/exhaustive-deps
  const model = given ?? own;
  const dom = useRef<WheelDom>(emptyDom());
  const ctrl = useRef<WheelController | null>(null);
  const merged: Placement = { ...DEFAULT_PLACEMENT, ...placement };
  const hooksRef = useRef({ onPlacementChange, onOpenChange });
  hooksRef.current = { onPlacementChange, onOpenChange };
  const structure = useSyncExternalStore(model.subscribe, () => model.structVersion);

  const [pensOpen, setPensOpen] = useState(false);
  const [anchor, setAnchor] = useState({ x: 8, y: 8 });
  const [modal, setModal] = useState<Modal | null>(null);
  const toasts = useToast();
  const [carrying, setCarrying] = useState(false);
  const [edge, setEdge] = useState<'left' | 'right' | 'top' | 'bottom' | 'free'>('left');
  const pens = usePresence(pensOpen && panels);
  const lastModal = useRef<Modal | null>(null);
  if (modal) lastModal.current = modal;
  const penHeight = (n: number) => Math.min((ctrl.current?.H ?? 800) - 16, 16 + 34 + 40 + n * 62 + 56);
  const place = () => {
    const c = ctrl.current;
    if (!c) return;
    setAnchor(c.sideAnchor(260, penHeight(model.pens.length)));
    setEdge(c.p.docked ? c.p.edge : 'free');
  };
  const placeRef = useRef(place);
  placeRef.current = place;

  useLayoutEffect(() => {
    const c = new WheelController({
      model,
      dom: dom.current,
      placement: merged,
      open,
      pose,
      hooks: {
        onPlace: (p) => { hooksRef.current.onPlacementChange?.(p); },
        onOpenChange: (o) => hooksRef.current.onOpenChange?.(o),
        togglePens: () => { setModal(null); setPensOpen((v) => { if (!v) placeRef.current(); return !v; }); },
        closePens: () => setPensOpen(false),
        openPicker: () => { setPensOpen(false); setModal({ kind: 'picker' }); },
        carry: (on) => { setCarrying(on); if (!on) setTimeout(() => placeRef.current(), 0); },
        closeAll: () => { setPensOpen(false); setModal(null); },
      },
    });
    ctrl.current = c;
    const layer = dom.current.layer!;
    const measure = () => c.resize(layer.clientWidth, layer.clientHeight);
    measure();
    const ro = new ResizeObserver(measure);
    ro.observe(layer);
    const input = new WheelInput(c);
    input.bind();
    c.start();
    if (turn) c.turnTo(turn.colour, turn.tools);
    if (initialPanel === 'pens') { setPensOpen(true); placeRef.current(); }
    else if (initialPanel === 'customise-tools') setModal({ kind: 'customise', tab: 'tools' });
    else if (initialPanel === 'customise-pens') setModal({ kind: 'customise', tab: 'pens', open: 3 });
    else if (initialPanel === 'picker') setModal({ kind: 'picker' });
    else if (initialPanel === 'colours') setModal({ kind: 'colours', pick: { s: 0, i: 2 } });
    const tab = dom.current.tab;
    const ink = () => tab?.style.setProperty('--tabc', model.setting.color);
    ink();
    const off = model.subscribe((what) => {
      if (what === 'mode' || what === 'colour' || what === 'size' || what === 'alpha') ink();
      if (what === 'mode') setPensOpen(false);
      c.dirty = true;
      c.wake();
    });
    setEdge(c.p.docked ? c.p.edge : 'free');
    return () => {
      off();
      ro.disconnect();
      input.unbind();
      c.dispose();
      ctrl.current = null;
    };
  }, [model]); // eslint-disable-line react-hooks/exhaustive-deps

  /* markup changed (colour ring or tool list): hand the new elements to the engine */
  useLayoutEffect(() => {
    ctrl.current?.syncStructure();
  }, [structure]);

  const key = `${merged.docked}|${merged.edge}|${Math.round(merged.along)}|${Math.round(merged.floatX)}|${Math.round(merged.floatY)}`;
  useEffect(() => {
    const c = ctrl.current;
    if (!c) return;
    const p = c.p;
    if (p.docked !== merged.docked || p.edge !== merged.edge || Math.round(p.along) !== Math.round(merged.along) || Math.round(p.floatX) !== Math.round(merged.floatX) || Math.round(p.floatY) !== Math.round(merged.floatY)) {
      c.setPlacement(merged);
      setEdge(merged.docked ? merged.edge : 'free');
    }
  }, [key]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => {
    const c = ctrl.current;
    if (c && c.isOpen !== open) c.setOpen(open);
  }, [open]);
  useEffect(() => {
    if (pose) ctrl.current?.setPose(pose);
  }, [pose]);

  const undoable = (text: string, undo: () => void) => toasts.show(text, { undo });
  const mc = lastModal.current;
  const toBody = (m: Modal | null) => {
    if (!m) return null;
    const state = modal ? 'open' : 'leave';
    const close = () => setModal(null);
    if (m.kind === 'customise') return <CustomiseWheel model={model} tab={m.tab} openPen={m.open} state={state} onClose={close} onUndoable={undoable}
      onSharePens={(pens) => toasts.show(pens.length === 1 ? 'Saved 1 pen as My pens.graphopack' : `Saved ${pens.length} pens as My pens.graphopack`)}
      onImportPens={() => toasts.show('Opens Files')} />;
    if (m.kind === 'picker') {
      return (
        <ColourPicker
          model={model}
          edit={m.edit}
          state={state}
          onClose={close}
          onManage={() => setModal({ kind: 'colours' })}
          onCancel={() => setModal({ kind: 'colours', pick: m.edit })}
          onSaved={() => setModal({ kind: 'colours', pick: m.edit })}
          onFocusRim={(i) => ctrl.current?.focusRim(i)}
        />
      );
    }
    return <ManageColours model={model} pick={m.pick} state={state} onClose={close} onEditColour={(at) => setModal({ kind: 'picker', edit: at })} onUndoable={undoable} />;
  };

  return (
    <div className={`${styles.root}${className ? ` ${className}` : ''}`}>
      <div ref={(el) => { dom.current.layer = el; }} className={styles.layer}>
        <div ref={(el) => { dom.current.hint = el; }} className={styles.hint} />
        <WheelFace model={model} refs={dom.current.disc} full={false} />
        <WheelFace model={model} refs={dom.current.circ} full />
        <div ref={(el) => { dom.current.handle = el; }} className={styles.handle}>
          <div ref={(el) => { dom.current.grip = el; }} className={styles.hg} role="button" aria-label="Move wheel" data-tip="Move wheel"><Icon name="grip" size={16} /></div>
          <i className={styles.hd} />
          <div ref={(el) => { dom.current.pin = el; }} className={styles.hp} role="button" aria-label="Pin wheel open" data-tip="Pin open">
            <span ref={(el) => { dom.current.pinIcon = el; }} className={styles.hpI}><Icon name="pin" size={18} /></span>
          </div>
        </div>
        <div ref={(el) => { dom.current.tab = el; }} className={styles.tab} role="button" aria-label="Open pen wheel"><i className={styles.tabDot} /></div>
        <div ref={(el) => { dom.current.tip = el; }} className={styles.tip} />
      </div>
      {panels && (
        <div className={styles.pop} data-carrying={carrying || undefined}>
          {pens.mounted && (
            <div className={styles.penWrap} style={{ transform: `translate3d(${anchor.x}px,${anchor.y}px,0)`, opacity: carrying ? 0 : 1 } as CSSProperties}>
              <PenList
                model={model}
                edge={edge}
                state={pens.leaving ? 'leave' : 'open'}
                maxHeight={penHeight(model.pens.length)}
                onEdit={() => { setPensOpen(false); setModal({ kind: 'customise', tab: 'pens' }); }}
                onPicked={() => setPensOpen(false)}
                onDeleted={(pen, i) => undoable('Pen deleted', () => model.setPens(model.pens.slice(0, i).concat(pen, model.pens.slice(i))))}
              />
            </div>
          )}
          <PanelModal open={!!modal && panels} onDismiss={() => setModal(null)}>
            {toBody(modal ?? mc)}
          </PanelModal>
        </div>
      )}
    </div>
  );
}
