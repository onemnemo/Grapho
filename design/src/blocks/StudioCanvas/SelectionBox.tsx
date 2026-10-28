import { Icon } from '@/assets/icons';
import type { Design, Layer } from '@/assets/studio';
import { edgesOf, type GestureView } from '@/lib/studio';
import styles from './SelectionBox.module.css';

export interface SelectionBoxProps {
  design: Design;
  selected: Layer | null;
  K: number;
  heightOf: (l: Layer) => number;
  view: GestureView;
  playing: boolean;
  onTogglePlay: () => void;
}

const box = (l: Layer, K: number, h: number) => {
  const w = l.w * K, hh = h * K;
  return { width: w, height: hh, transform: `translate(${l.x * K - w / 2}px,${l.y * K - hh / 2}px) rotate(${l.r}deg)` };
};

/** Everything drawn over the art: the box and handles of the selection, the outline under the pointer, alignment guides and video play buttons. */
export function SelectionBox({ design, selected: l, K, heightOf, view, playing, onTogglePlay }: SelectionBoxProps) {
  const hov = view.hoverId && !view.moving ? design.layers.find((x) => x.id === view.hoverId) : null;
  return (
    <div className={styles.ov}>
      {design.layers.filter((v) => v.type === 'video' && !v.hide).map((v) => (
        <button key={v.id} type="button" className={styles.glyph} data-vplay="" aria-label={playing ? 'Pause' : 'Play'} style={{ left: v.x * K, top: v.y * K }} onClick={onTogglePlay}>
          <Icon name={playing ? 'pause' : 'play'} size={20} />
        </button>
      ))}
      {hov && !hov.hide && <i className={styles.hover} style={box(hov, K, heightOf(hov))} />}
      {l && !l.hide && (
        <div className={styles.sel} data-lock={l.lock || undefined} data-snap={view.snapRot || undefined} style={box(l, K, heightOf(l))}>
          <i className={styles.edge} />
          {l.lock ? (
            <span className={styles.lock}><Icon name="lock" size={14} /></span>
          ) : (
            <>
              {['nw', 'ne', 'se', 'sw', ...edgesOf(l)].map((c) => <b key={c} className={`${styles.hd} ${styles[c]}`} data-hd={c} />)}
              <i className={styles.line} />
              <b className={`${styles.hd} ${styles.rot}`} data-hd="rot" title="Rotate"><Icon name="rotate" size={14} /></b>
            </>
          )}
          {view.chip && <em className={styles.chip} style={{ transform: `rotate(${-l.r}deg)` }}>{view.chip}</em>}
        </div>
      )}
      {view.guides.map((g, i) => <i key={i} className={`${styles.guide} ${g.v ? styles.gv : styles.gh}`} style={g.v ? { left: g.p * K } : { top: g.p * K }} />)}
    </div>
  );
}
