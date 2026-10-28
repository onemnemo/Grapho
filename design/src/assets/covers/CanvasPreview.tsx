import type { CSSProperties } from 'react';
import { CV_GAP, type CanvasSettings } from '../studio/canvases';
import { lum } from '../studio/colour';
import styles from './CanvasPreview.module.css';

export interface CanvasPreviewProps {
  /** Background, pattern, spacing and size of the canvas. */
  settings: Pick<CanvasSettings, 'bg' | 'tpl' | 'gap' | 'size'>;
  width: number;
  height: number;
  /** Draw only the paper and its pattern: no sketch, no infinity mark. For small option tiles. */
  plain?: boolean;
  /** No drop shadow, for tiles that sit on a card. */
  flat?: boolean;
  className?: string;
}

const dark = (bg: string) => lum(bg) <= 0.55;

/** Pattern marks are scaled with the width, so a small tile shows the same look as the big preview. */
function pattern(c: CanvasPreviewProps['settings'], k: number): { img: string; size: string } {
  const g = CV_GAP[c.gap || 'M'] * k;
  const col = dark(c.bg) ? 'rgba(255,255,255,.2)' : 'rgba(23,24,28,.24)';
  const r = Math.max(0.8, 1.25 * k);
  if (c.tpl === 'dots') return { img: `radial-gradient(circle,${col} 0 ${r}px,transparent ${r + 0.6}px)`, size: `${g}px ${g}px` };
  if (c.tpl === 'grid') return { img: `linear-gradient(${col} 1px,transparent 1px),linear-gradient(90deg,${col} 1px,transparent 1px)`, size: `${g}px ${g}px` };
  if (c.tpl === 'lines') return { img: `linear-gradient(${col} 1px,transparent 1px)`, size: `${g}px ${g}px` };
  return { img: 'none', size: 'auto' };
}

/** A canvas drawn straight from its settings, so the preview is the real thing. */
export function CanvasPreview({ settings, width, height, plain = false, flat = false, className }: CanvasPreviewProps) {
  const p = pattern(settings, width / 220);
  const inf = settings.size !== 'fixed';
  const dk = dark(settings.bg);
  const style: CSSProperties = {
    width, height, backgroundColor: settings.bg, backgroundImage: p.img, backgroundSize: p.size, backgroundPosition: 'center',
  };
  return (
    <div className={[styles.cvt, inf ? styles.inf : styles.fixed, dk ? styles.dk : '', flat ? styles.flat : '', className].filter(Boolean).join(' ')} style={style}>
      {!plain && (
        <svg className={styles.sketch} viewBox="0 0 100 70" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
          <path d="M14 46c6-22 22-28 30-18s-4 26-14 22" fill="none" stroke="#E0517F" strokeWidth="2.2" strokeLinecap="round" />
          <path d="M50 22h30M50 30h22" fill="none" stroke={dk ? 'rgba(255,255,255,.85)' : 'rgba(23,24,28,.8)'} strokeWidth="2" strokeLinecap="round" />
          <path d="M56 48l10-12 8 8 10-14" fill="none" stroke="#5C8DF0" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      )}
      {inf && !plain && (
        <span className={styles.mark} title="Infinite">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" aria-hidden="true">
            <path d="M12 12c-1.6-2.4-3.2-3.6-5-3.6a3.6 3.6 0 0 0 0 7.2c1.8 0 3.4-1.2 5-3.6zm0 0c1.6 2.4 3.2 3.6 5 3.6a3.6 3.6 0 0 0 0-7.2c-1.8 0-3.4 1.2-5 3.6z" />
          </svg>
        </span>
      )}
    </div>
  );
}
