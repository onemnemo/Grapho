import type { CSSProperties } from 'react';
import { iconPaths, type IconName } from '../icons';
import { CardFace } from './DesignArt';
import { ClipArt } from './ClipArt';
import { CLIPS } from './clips';
import { SHAPE_STROKEONLY, SHAPE_SVG, SHAPE_THUMB_CSS } from './shapes';
import type { TileArt } from './panelItems';
import styles from './TileArtView.module.css';

export interface TileArtViewProps {
  art: TileArt;
  /** How much of the slot the picture fills, in percent. The layer list uses a smaller fill than the panel. */
  fit?: number;
  playing?: boolean;
  /** Draws an icon at this size in px instead of filling the slot. */
  iconPx?: number;
}

/** One small picture of a sticker, shape, texture, card or clip, drawn the same way in the panel, pack tiles and layer list. */
export function TileArtView({ art, fit = 100, playing = false, iconPx }: TileArtViewProps) {
  switch (art.t) {
    case 'html': return <span className={styles.svg} dangerouslySetInnerHTML={{ __html: art.html }} />;
    case 'pixel': return <span className={styles.px} dangerouslySetInnerHTML={{ __html: art.html }} />;
    case 'img': return <img className={styles.img} src={art.src} alt="" draggable={false} style={{ width: art.pixel ? `${Math.min(fit, 62)}%` : '100%', imageRendering: art.pixel ? 'pixelated' : undefined }} />;
    case 'shape': {
      const c = art.colour || 'var(--shape-ink, var(--fg))';
      const css = SHAPE_THUMB_CSS[art.key];
      if (css) {
        const st: CSSProperties = { background: c, borderRadius: css.radius };
        if (css.ring) Object.assign(st, { background: 'none', border: `3px solid ${c}` });
        return <i className={`${styles.shape} ${css.pill ? styles.pill : ''} ${css.line ? styles.line : ''}`} style={st} />;
      }
      const so = SHAPE_STROKEONLY[art.key];
      return (
        <svg className={styles.shapeSvg} viewBox="0 0 100 100">
          <path d={SHAPE_SVG[art.key]} fill={so ? 'none' : c} stroke={c} strokeWidth={so ? 7 : 4} strokeLinejoin="round" strokeLinecap="round" />
        </svg>
      );
    }
    case 'swatch':
      return (
        <i className={styles.tsw} style={{ background: art.base, ...(art.image ? { backgroundImage: art.image, backgroundSize: art.size } : null) }}>
          {art.map && <i style={{ backgroundImage: art.map.image, backgroundSize: '128px', mixBlendMode: art.map.blend as CSSProperties['mixBlendMode'] }} />}
        </i>
      );
    case 'photo': return <i className={styles.photo} style={{ backgroundImage: `url('${art.src}')`, ...(fit < 100 ? { inset: `${(100 - fit) / 2}%`, borderRadius: 4 } : null) }} />;
    case 'card': return <CardFace def={art.def} />;
    case 'video': return <ClipArt clip={art.clip} poster={Math.min(2, CLIPS[art.clip].len / 2)} playing={playing} className={styles.clip} />;
    case 'aa': return <span className={styles.aa} style={{ fontFamily: art.font, fontWeight: art.weight, color: art.colour }}>Aa</span>;
    case 'chip': return <i className={styles.chip} style={{ background: art.bg, borderColor: art.ink }} />;
    case 'icon':
      return (
        <span className={styles.icon} style={{ color: art.colour, ...(iconPx ? { width: iconPx, height: iconPx } : null) }}>
          <svg viewBox="0 0 24 24" fill={art.soft ? 'currentColor' : 'none'} fillOpacity={art.soft ? 0.2 : undefined} stroke="currentColor" strokeWidth={1.75} strokeLinecap="round" strokeLinejoin="round" aria-hidden>
            <path d={iconPaths[art.name as IconName] || ''} />
          </svg>
        </span>
      );
    case 'poster':
      return (
        <>
          <i className={styles.tsw} style={{ background: art.bg || 'var(--bg)' }} />
          <span className={styles.play}><svg viewBox="0 0 24 24" width={10} height={10} fill="currentColor" aria-hidden><path d={iconPaths.play} /></svg></span>
        </>
      );
  }
}
