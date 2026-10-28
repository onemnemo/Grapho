/* The wheel's pen drawing: a real pen with its tip in the live ink. Artwork, so the greys live here.
   The tip fills with var(--ink), which the parent sets (the wheel face, a pen card). */
import { useId, type Ref, type SVGProps } from 'react';

export type PenGlyphKind = 'pen' | 'highlighter';

/** Path of the tip for a size between 0 (finest) and 1 (broadest). */
export function nibTipPath(kind: PenGlyphKind, t: number) {
  if (kind === 'highlighter') {
    const h = 3.4 + 2.2 * t;
    return `M58 ${15 - h - 1.6}L72 ${15 - h + 0.4}v${(h - 0.4) * 2}L58 ${15 + h + 1.6}z`;
  }
  const h = 0.5 + 2.4 * t;
  return `M63 12.4L74 ${15 - h}v${h * 2}L63 17.6z`;
}

export interface PenGlyphProps extends Omit<SVGProps<SVGSVGElement>, 'ref'> {
  kind: PenGlyphKind;
  /** 0 to 1, the pen's size. Ignored when the tip is driven through tipRef. */
  size?: number;
  tipRef?: Ref<SVGPathElement>;
}

export function PenGlyph({ kind, size = 0.5, tipRef, ...rest }: PenGlyphProps) {
  const id = useId();
  const body = `${id}b`;
  const cone = `${id}c`;
  if (kind === 'highlighter') {
    return (
      <svg viewBox="0 0 76 30" width="76" height="30" aria-hidden="true" {...rest}>
        <defs>
          <linearGradient id={body} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stopColor="#FBFBFA" /><stop offset=".55" stopColor="#E6E6EA" /><stop offset="1" stopColor="#BDBDC6" />
          </linearGradient>
        </defs>
        <rect x="0" y="5" width="46" height="20" rx="6" fill={`url(#${body})`} />
        <rect x="4" y="7.4" width="38" height="2" rx="1" fill="#fff" opacity=".75" />
        <rect x="34" y="5" width="9" height="20" style={{ fill: 'var(--ink)' }} opacity=".9" />
        <path d="M46 5.2l12 2.6v14.4L46 24.8z" fill="#CFCFD6" />
        <path ref={tipRef} d={nibTipPath('highlighter', size)} style={{ fill: 'var(--ink)' }} />
        <path d="M58 7.8L72 11.6v6.8L58 22.2z" fill="none" stroke="rgba(255,255,255,.35)" strokeWidth=".8" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 76 30" width="76" height="30" aria-hidden="true" {...rest}>
      <defs>
        <linearGradient id={body} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#FBFBFA" /><stop offset=".55" stopColor="#E6E6EA" /><stop offset="1" stopColor="#B9B9C2" />
        </linearGradient>
        <linearGradient id={cone} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#EDEDF0" /><stop offset="1" stopColor="#A9A9B3" />
        </linearGradient>
      </defs>
      <rect x="0" y="7" width="48" height="16" rx="6" fill={`url(#${body})`} />
      <rect x="4" y="9" width="40" height="2" rx="1" fill="#fff" opacity=".75" />
      <rect x="32" y="7" width="7" height="16" style={{ fill: 'var(--ink)' }} opacity=".92" />
      <rect x="42" y="7" width="2.6" height="16" fill="#0E0E12" opacity=".75" />
      <path d="M48 7h4l11 5.4v5.2L52 23h-4z" fill={`url(#${cone})`} />
      <path ref={tipRef} d={nibTipPath('pen', size)} style={{ fill: 'var(--ink)' }} />
    </svg>
  );
}
