import { forwardRef, useCallback, useLayoutEffect, useMemo, useRef, useState, type CSSProperties, type HTMLAttributes, type Ref } from 'react';
import { DIM, type Design, type Layer } from '../types';
import { LayerView } from './LayerView';
import styles from './DesignArt.module.css';

export interface DesignArtProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  design: Design;
  /** The tile shadow of the library. The studio canvas draws its own, so it turns this off. */
  shadow?: boolean;
  /** Clips and videos loop while this is true, and show their poster frame otherwise. */
  playing?: boolean;
  /** Called after layout with the real heights of text layers, so selection boxes fit the text. */
  onMeasure?: (heights: Record<string, number>) => void;
  /** A layer that has just been added plays its pop-in. */
  appearId?: string | null;
  /** The text layer being edited in place. */
  editingId?: string | null;
  editingStart?: string;
  onEditInput?: (text: string) => void;
  onEditEnd?: () => void;
  hostRef?: Ref<HTMLDivElement>;
}

const placeGroup = (d: Design, l: Layer) => (d.kind === 'folder' ? (l.pop === 'behind' ? 0 : l.pop ? 2 : 1) : 1);

/**
 * A cover or a folder front drawn from its layers. It fills its parent's width and keeps the shape of the design
 * (3:4 for a cover, 8:5 for a folder), so give it a width and nothing else.
 */
export const DesignArt = forwardRef<HTMLDivElement, DesignArtProps>(function DesignArt(
  { design: d, shadow = true, playing = false, onMeasure, appearId = null, editingId = null, editingStart = '', onEditInput, onEditEnd, hostRef, className, style, ...rest },
  ref,
) {
  const dim = DIM[d.kind];
  const host = useRef<HTMLDivElement | null>(null);
  const setHost = useCallback((el: HTMLDivElement | null) => {
    host.current = el;
    for (const r of [ref, hostRef]) {
      if (typeof r === 'function') r(el);
      else if (r) (r as { current: HTMLDivElement | null }).current = el;
    }
  }, [ref, hostRef]);
  const [k, setK] = useState(0);
  const [heights, setHeights] = useState<Record<string, number>>({});

  useLayoutEffect(() => {
    const el = host.current;
    if (!el) return;
    const fit = () => { if (el.clientWidth) setK(el.clientWidth / dim.W); };
    fit();
    const ro = new ResizeObserver(fit);
    ro.observe(el);
    return () => ro.disconnect();
  }, [dim.W]);

  /* text layers have no fixed height: measure them after layout */
  useLayoutEffect(() => {
    const el = host.current;
    if (!el) return;
    const next: Record<string, number> = {};
    el.querySelectorAll<HTMLElement>('[data-layer-id]').forEach((n) => {
      const id = n.dataset.layerId!;
      const l = d.layers.find((x) => x.id === id);
      if (l?.type === 'text') next[id] = Math.max(8, n.offsetHeight);
    });
    setHeights((prev) => {
      const same = Object.keys(next).length === Object.keys(prev).length && Object.keys(next).every((id) => prev[id] === next[id]);
      return same ? prev : next;
    });
  });
  useLayoutEffect(() => { onMeasure?.(heights); }, [heights, onMeasure]);

  const groups = useMemo(() => {
    const g: Layer[][] = [[], [], []];
    d.layers.forEach((l) => g[placeGroup(d, l)].push(l));
    return g;
  }, [d]);

  const view = (l: Layer) => (
    <LayerView key={l.id} layer={l} designName={d.name} measuredH={heights[l.id]} playing={playing} appear={appearId === l.id}
      editing={editingId === l.id ? { start: editingStart, onInput: onEditInput ?? (() => undefined), onEnd: onEditEnd ?? (() => undefined) } : null} />
  );

  const b = d.base;
  let body;
  if (d.kind === 'cover') {
    const c = d.cover || {};
    const r = c.rad ?? 14;
    body = (
      <div className={styles.cv} style={{ background: b.c2 ? `linear-gradient(${b.ang ?? 160}deg, ${b.c1}, ${b.c2})` : b.c1, borderRadius: `${(r * 0.55).toFixed(1)}px ${r}px ${r}px ${(r * 0.55).toFixed(1)}px` }}>
        <div className={styles.cvClip}><div className={styles.lay}>{groups[1].map(view)}</div></div>
        {c.spine && <i className={styles.spine} />}
        {c.strap && <i className={styles.strap} style={typeof c.strap === 'string' ? { background: c.strap } : undefined} />}
      </div>
    );
  } else {
    const f = d.folder!;
    const rot = [-3, 1, 4], dy = [6, 0, 10];
    const pos = f.name === 'centre' ? styles.posCentre : f.name === 'hidden' ? styles.posHidden : f.name === 'below' ? styles.posBelow : '';
    body = (
      <div className={styles.fo}>
        <i className={styles.foTab} style={{ background: f.tab || f.back }} />
        <i className={styles.foBack} style={{ background: f.back }} />
        <div className={styles.foPapers}>
          {(f.papers || []).map((c, i) => <i key={i} style={{ background: c, ['--rot' as string]: `${rot[i % 3]}deg`, ['--dy' as string]: `${dy[i % 3]}px` } as CSSProperties} />)}
        </div>
        <div className={`${styles.foBehind} ${styles.lay}`}>{groups[0].map(view)}</div>
        <div className={styles.foFront} />
        <div className={styles.foClip} style={{ background: b.c2 ? `linear-gradient(180deg, ${b.c1}, ${b.c2}) 0 92px / 100% 208px no-repeat` : b.c1 }}>
          <div className={styles.lay}>{groups[1].map(view)}</div>
        </div>
        <i className={styles.foHl} />
        <div className={`${styles.foName} ${pos} ${f.ink === 'dark' ? styles.isDark : ''}`}><b>{d.name}</b><span>{d.meta}</span></div>
        <div className={`${styles.foPop} ${styles.lay}`}>{groups[2].map(view)}</div>
      </div>
    );
  }

  return (
    <div
      ref={setHost}
      data-design-host=""
      data-kind={d.kind}
      className={[styles.host, shadow && (d.kind === 'cover' ? styles.shadowCover : styles.shadowFolder), className].filter(Boolean).join(' ')}
      style={{ aspectRatio: `${dim.W} / ${dim.H}`, ...style }}
      {...rest}
    >
      <div className={styles.dz} style={{ width: dim.W, height: dim.H, transform: `scale(${k || 0.0001})`, visibility: k ? undefined : 'hidden' }}>{body}</div>
    </div>
  );
});
