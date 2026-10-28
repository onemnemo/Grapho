/* Patterns and textures that can sit on any cover. Artwork data, so raw colours are fine. */

export type CoverPattern = 'none' | 'dots' | 'grid' | 'stripes' | 'diagonal' | 'check' | 'waves';
export type CoverTexture = 'none' | 'paper' | 'grain' | 'linen' | 'felt' | 'leather';
export const COVER_PATTERNS: CoverPattern[] = ['none', 'dots', 'grid', 'stripes', 'diagonal', 'check', 'waves'];
export const COVER_TEXTURES: CoverTexture[] = ['none', 'paper', 'grain', 'linen', 'felt', 'leather'];

interface LayerProps { id: string; className?: string }

export function PatternLayer({ id, pattern, colour = '#ffffff', className }: LayerProps & { pattern: CoverPattern; colour?: string }) {
  const pid = `${id}-pat`;
  const marks = (() => {
    switch (pattern) {
      case 'dots': return { w: 12, h: 12, el: <circle cx="6" cy="6" r="1.6" fill={colour} /> };
      case 'grid': return { w: 12, h: 12, el: <path d="M12 0V12H0" fill="none" stroke={colour} strokeWidth=".7" /> };
      case 'stripes': return { w: 12, h: 12, el: <rect width="6" height="12" fill={colour} /> };
      case 'diagonal': return { w: 10, h: 10, el: <path d="M-2 12L12 -2M-2 7L7 -2M3 12L12 3" stroke={colour} strokeWidth="1.6" /> };
      case 'check': return { w: 20, h: 20, el: <><rect width="10" height="10" fill={colour} /><rect x="10" y="10" width="10" height="10" fill={colour} /></> };
      case 'waves': return { w: 24, h: 10, el: <path d="M0 5Q6 0 12 5T24 5" fill="none" stroke={colour} strokeWidth="1.4" /> };
      default: return null;
    }
  })();
  if (!marks) return null;
  return (
    <svg className={className} viewBox="0 0 120 160" preserveAspectRatio="xMidYMid slice" aria-hidden="true" style={{ opacity: 0.28 }}>
      <defs>
        <pattern id={pid} width={marks.w} height={marks.h} patternUnits="userSpaceOnUse">{marks.el}</pattern>
      </defs>
      <rect width="120" height="160" fill={`url(#${pid})`} />
    </svg>
  );
}

const TEX: Record<Exclude<CoverTexture, 'none'>, { freq: string; octaves: number; alpha: number; light: boolean; blend: string }> = {
  paper: { freq: '0.9', octaves: 3, alpha: 0.22, light: true, blend: 'multiply' },
  grain: { freq: '1.6', octaves: 2, alpha: 0.3, light: false, blend: 'overlay' },
  linen: { freq: '0.06 0.9', octaves: 2, alpha: 0.38, light: true, blend: 'multiply' },
  felt: { freq: '0.55', octaves: 4, alpha: 0.4, light: true, blend: 'overlay' },
  leather: { freq: '0.12', octaves: 4, alpha: 0.5, light: true, blend: 'multiply' },
};

export function TextureLayer({ id, texture, className }: LayerProps & { texture: Exclude<CoverTexture, 'none'> }) {
  const t = TEX[texture];
  const fid = `${id}-tex`;
  return (
    <svg className={className} aria-hidden="true" style={{ mixBlendMode: t.blend as 'multiply', opacity: t.alpha, pointerEvents: 'none' }}>
      <filter id={fid} x="0" y="0" width="100%" height="100%">
        <feTurbulence type="fractalNoise" baseFrequency={t.freq} numOctaves={t.octaves} seed="4" result="n" />
        {t.light ? (
          <feDiffuseLighting in="n" lightingColor="#ffffff" surfaceScale="2">
            <feDistantLight azimuth="45" elevation="60" />
          </feDiffuseLighting>
        ) : (
          <feColorMatrix type="saturate" values="0" />
        )}
      </filter>
      <rect width="100%" height="100%" filter={`url(#${fid})`} />
    </svg>
  );
}
