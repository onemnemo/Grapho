import { memo, useEffect, useRef, type CSSProperties } from 'react';
import { iconPaths, type IconName } from '../../icons';
import { CARDS, FONTS, type CardDef } from '../cards';
import { ClipArt } from '../ClipArt';
import { PAINT, paintCSS, type PaintKey } from '../paints';
import { PAT, patURL, type PatternKey } from '../patterns';
import { PHOTOS } from '../photos';
import { SHAPE_CSS, SHAPE_STROKEONLY, SHAPE_SVG } from '../shapes';
import { HOLO, TEX, texMapURL, type TextureKey } from '../textures';
import { SHADOWS, type Layer } from '../types';
import type { ClipKey } from '../clips';
import styles from './DesignArt.module.css';

const CUT_STEPS: Array<[number, number]> = [[1, 0], [-1, 0], [0, 1], [0, -1], [0.7, 0.7], [-0.7, 0.7], [0.7, -0.7], [-0.7, -0.7]];

/** The white die-cut outline of a sticker, then its shadow. */
/** The CSS module turns .card-label into cardLabel. */
const cardClass = (k?: string) => `card${(k ?? '').charAt(0).toUpperCase()}${(k ?? '').slice(1)}`;

export function layerFilter(l: Layer): string {
  const f: string[] = [];
  if (l.type === 'sticker' && l.cut) {
    const c = l.cut;
    CUT_STEPS.forEach(([a, b]) => f.push(`drop-shadow(${(a * c).toFixed(2)}px ${(b * c).toFixed(2)}px 0 #fff)`));
  }
  if (l.sh) f.push(SHADOWS[l.sh]);
  return f.join(' ') || 'none';
}

export interface LayerViewProps {
  layer: Layer;
  /** The design's name, shown by text and cards that are bound to it. */
  designName: string;
  /** Height of an auto-height text layer, measured after layout. */
  measuredH?: number;
  playing?: boolean;
  appear?: boolean;
  /** In-place editing: the text is editable and reports what is typed. */
  editing?: { start: string; onInput: (text: string) => void; onEnd: () => void } | null;
}

function Content({ l, text, playing, editing }: { l: Layer; text: string; playing: boolean; editing: LayerViewProps['editing'] }) {
  const ff = FONTS[l.font || 'sans']?.[0] ?? FONTS.sans[0];
  const txt = useRef<HTMLSpanElement>(null);
  useEffect(() => {
    if (!editing || !txt.current) return;
    const t = txt.current;
    t.focus();
    const r = document.createRange();
    r.selectNodeContents(t);
    const s = window.getSelection();
    s?.removeAllRanges();
    s?.addRange(r);
  }, [editing]);
  const lines = (s: string) => s.split('\n').flatMap((p, i, a) => (i < a.length - 1 ? [p, <br key={i} />] : [p]));
  const body = (s: string) =>
    editing ? (
      <span ref={txt} className={styles.txt} contentEditable="plaintext-only" suppressContentEditableWarning spellCheck={false}
        onInput={(e) => editing.onInput(e.currentTarget.innerText.replace(/\n$/, ''))} onBlur={editing.onEnd}>{editing.start}</span>
    ) : <span className={styles.txt}>{lines(s)}</span>;

  switch (l.type) {
    case 'sticker':
      if (l.src) return <div className={`${styles.dlIn} ${styles.sticker}`}><img src={l.src} alt="" draggable={false} style={l.pix ? { imageRendering: 'pixelated' } : undefined} /></div>;
      return <div className={`${styles.dlIn} ${styles.sticker}`} dangerouslySetInnerHTML={{ __html: l.svg || '' }} />;
    case 'image': {
      const c = l.crop || { z: 1, px: 0, py: 0 };
      return (
        <div className={`${styles.dlIn} ${styles.image}`} style={{ borderRadius: (l.rad || 0) + 'px' }}>
          <img src={l.src} alt="" draggable={false} style={{ transform: `translate(${c.px}%,${c.py}%) scale(${c.z})` }} />
        </div>
      );
    }
    case 'text': {
      const st: CSSProperties = {
        fontFamily: ff, fontSize: l.size, fontWeight: l.weight || 500, color: l.colour, textAlign: l.align || 'left',
        letterSpacing: `${l.ls || 0}em`, lineHeight: l.lh || 1.1, textTransform: l.caps ? 'uppercase' : 'none',
        ...(l.italic ? { fontStyle: 'italic' } : null),
        ...(l.outline ? { WebkitTextStroke: `${l.outline}px ${l.outlineC || '#17181C'}`, paintOrder: 'stroke fill' } : null),
      };
      return <div className={`${styles.dlIn} ${styles.text}`} style={st}>{body(text)}</div>;
    }
    case 'shape': {
      const k = l.shape || 'rect', fill = l.fill || 'none', st = l.stroke || 'none', sw = l.sw || 0;
      if (SHAPE_CSS.includes(k)) {
        const rad = ({ rect: `${l.radius || 0}px`, rounded: `${l.radius ?? 16}px`, pill: '999px', circle: '50%', arch: '999px 999px 0 0', ring: '50%', line: '999px' } as Record<string, string>)[k];
        const css: CSSProperties = { borderRadius: rad, background: k === 'ring' ? 'transparent' : fill };
        if (sw && st !== 'none') Object.assign(css, { border: `${sw}px ${l.dash ? 'dashed' : 'solid'} ${st}`, boxSizing: 'border-box' });
        if (k === 'ring' && !sw) Object.assign(css, { border: `6px solid ${fill}`, boxSizing: 'border-box' });
        return <div className={styles.dlIn} style={css} />;
      }
      const so = SHAPE_STROKEONLY[k];
      return (
        <div className={styles.dlIn}>
          <svg className={styles.shapeSvg} viewBox="0 0 100 100" preserveAspectRatio="xMidYMid meet">
            <path d={SHAPE_SVG[k]} fill={so ? 'none' : fill} stroke={so ? (l.stroke && l.stroke !== 'none' ? l.stroke : fill) : st}
              strokeWidth={so ? ((sw || 6) * 100) / Math.max(10, l.w) : (sw * 100) / Math.max(10, l.w)} strokeLinejoin="round" strokeLinecap="round"
              strokeDasharray={l.dash ? '1 9' : undefined} />
          </svg>
        </div>
      );
    }
    case 'card': {
      const kd = CARDS.find((c) => c[0] === l.kind) || CARDS[0];
      const st = { '--card-bg': l.colour, '--card-ink': l.ink, fontFamily: FONTS[l.font || kd[5]][0], fontSize: l.size, '--cr': `${l.rad || 0}px` } as CSSProperties;
      return (
        <div className={`${styles.dlIn} ${styles.card} ${styles[cardClass(l.kind)]}`} style={st}>
          {l.kind === 'polaroid' && <i className={styles.plph} style={{ backgroundImage: `url('${PHOTOS[l.photo || 'hills'].src}')` }} />}
          {l.kind === 'index' && <i className={styles.ixl} />}
          {body(text)}
        </div>
      );
    }
    case 'icon':
      if (l.mask) return <div className={styles.dlIn} style={{ background: l.colour, WebkitMask: `url("${l.mask}") center/contain no-repeat`, mask: `url("${l.mask}") center/contain no-repeat` }} />;
      return (
        <div className={styles.dlIn}>
          <svg className={styles.iconSvg} viewBox="0 0 24 24" style={{ color: l.colour }} fill={l.soft ? 'currentColor' : 'none'} fillOpacity={l.soft ? 0.2 : undefined}
            stroke="currentColor" strokeWidth={l.sw || 1.75} strokeLinecap="round" strokeLinejoin="round">
            <path d={iconPaths[(l.ico || '') as IconName] || ''} />
          </svg>
        </div>
      );
    case 'tex': {
      const t = TEX[l.key as TextureKey];
      const size = `${256 * (l.scale || 1)}px`;
      if (l.key === 'holo') return <div className={`${styles.dlIn} ${styles.tex} ${styles.holo}`} style={{ background: HOLO, backgroundSize: '100% 100%,100% 100%' }} />;
      if (t.mode === 'relief') return <div className={`${styles.dlIn} ${styles.tex}`} style={{ backgroundImage: texMapURL(l.key as TextureKey), backgroundSize: size }} />;
      return (
        <div className={`${styles.dlIn} ${styles.tex}`} style={{ background: l.colour }}>
          <i className={styles.texGrain} style={{ backgroundImage: texMapURL(l.key as TextureKey), backgroundSize: size, mixBlendMode: t.inner as CSSProperties['mixBlendMode'] }} />
        </div>
      );
    }
    case 'pat': {
      const k = l.key as PatternKey, s = l.scale || 1;
      return <div className={`${styles.dlIn} ${styles.tex}`} style={{ backgroundColor: l.colour || 'transparent', backgroundImage: patURL(k, l.ink || '#fff', s), backgroundSize: `${PAT[k].w * s}px ${PAT[k].h * s}px` }} />;
    }
    case 'paint':
      return <div className={styles.dlIn} style={{ background: paintCSS((l.key as PaintKey) in PAINT ? (l.key as PaintKey) : 'ember') }} />;
    case 'video':
      if (l.src) return <VideoFile src={l.src} playing={playing} />;
      return <div className={`${styles.dlIn} ${styles.video}`}><ClipArt clip={(l.clip || 'aurora') as ClipKey} poster={l.poster || 0} playing={playing} /></div>;
  }
}

function VideoFile({ src, playing }: { src: string; playing: boolean }) {
  const v = useRef<HTMLVideoElement>(null);
  useEffect(() => {
    if (playing) v.current?.play().catch(() => undefined);
    else v.current?.pause();
  }, [playing]);
  return <div className={`${styles.dlIn} ${styles.video}`}><video ref={v} muted loop playsInline preload="auto" src={src} /></div>;
}

/** One layer, placed by its centre, rotated and flipped. */
export const LayerView = memo(function LayerView({ layer: l, designName, measuredH, playing = false, appear = false, editing = null }: LayerViewProps) {
  const text = l.bind ? designName : l.text || '';
  const isText = l.type === 'text';
  const h = isText ? measuredH ?? l.h : l.h;
  const style: CSSProperties = {
    width: l.w,
    height: isText ? 'auto' : l.h,
    transform: `translate(${l.x - l.w / 2}px,${l.y - h / 2}px) rotate(${l.r}deg)`,
    opacity: l.hide ? 0 : l.op,
    visibility: l.hide ? 'hidden' : undefined,
    mixBlendMode: l.blend && l.blend !== 'normal' ? (l.blend as CSSProperties['mixBlendMode']) : undefined,
    filter: layerFilter(l),
  };
  const inner: CSSProperties | undefined = l.fx || l.fy ? { transform: `scale(${l.fx ? -1 : 1},${l.fy ? -1 : 1})` } : undefined;
  return (
    <div className={`${styles.dl} ${appear ? styles.popIn : ''} ${editing ? styles.editing : ''}`} data-layer-id={l.id} style={style}>
      <div style={inner} className={isText ? styles.flipRel : styles.flipAbs}>
        <Content l={l} text={text} playing={playing} editing={editing} />
      </div>
    </div>
  );
});

/** A card at its own size, scaled down with zoom, for the Cards panel. */
export function CardFace({ def }: { def: CardDef }) {
  const [kind, , text, bg, ink, font, w, h] = def;
  const zoom = kind === 'ribbon' || kind === 'tag' ? 0.58 : kind === 'polaroid' ? 0.42 : 0.5;
  const st = {
    '--card-bg': bg, '--card-ink': ink, '--cr': '0px', fontFamily: FONTS[font][0], fontSize: kind === 'tag' ? 11 : kind === 'polaroid' ? 16 : 17,
    width: w, height: h, position: 'relative', zoom, pointerEvents: 'none',
  } as CSSProperties;
  return (
    <div className={`${styles.card} ${styles[cardClass(kind)]}`} style={st}>
      {kind === 'polaroid' && <i className={styles.plph} style={{ backgroundImage: `url('${PHOTOS.hills.src}')` }} />}
      {kind === 'index' && <i className={styles.ixl} />}
      <span className={styles.txt}>{text}</span>
    </div>
  );
}
