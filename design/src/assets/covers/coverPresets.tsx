/* Cover artwork data. Raw colours are allowed here: this is artwork, not chrome.
   Each preset is a base colour plus inline SVG drawn on a 120 x 160 sheet. */
import type { ReactNode } from 'react';

export type CoverPreset =
  | 'calculus' | 'biology' | 'physics' | 'journal' | 'chem' | 'sketches'
  | 'spanish' | 'history' | 'moss' | 'dusk' | 'plain';

export const COVER_PRESETS: CoverPreset[] = [
  'calculus', 'biology', 'physics', 'journal', 'chem', 'sketches', 'spanish', 'history', 'moss', 'dusk', 'plain',
];

export const COVER_COLOURS: Record<CoverPreset, string> = {
  calculus: '#F2C14E', biology: '#B7A5F2', physics: '#27275A', journal: '#23244F', chem: '#0F3328',
  sketches: '#E8A2BE', spanish: '#E9873A', history: '#5A8CF0', moss: '#2F7F5E', dusk: '#3E3A6D', plain: '#3A3C46',
};

const gridLines = (stroke: string, w: number, opacity?: number) => (
  <g stroke={stroke} strokeWidth={w} opacity={opacity}>
    {Array.from({ length: 14 }, (_, i) => <path key={i} d={`M${i * 9} 0V160M0 ${i * 12}H120`} />)}
  </g>
);

/** Drawn art per preset. The base colour is painted by CoverArt behind it. */
export const COVER_ART: Record<CoverPreset, (id: string) => ReactNode> = {
  calculus: () => (
    <>
      <rect x="0" y="86" width="120" height="3" fill="#17181C" />
      <circle cx="38" cy="108" r="22" fill="#E0402E" />
      <rect x="56" y="88" width="42" height="40" fill="#3B3470" />
    </>
  ),
  biology: () => (
    <g fill="none" stroke="#2a1f5c" strokeWidth="2.2">
      <circle cx="46" cy="104" r="30" fill="#D9CFFA" />
      <circle cx="46" cy="104" r="30" />
      <circle cx="54" cy="98" r="10" fill="#7c5ee0" />
      <circle cx="88" cy="128" r="14" fill="#D9CFFA" />
      <circle cx="88" cy="128" r="14" />
      <circle cx="92" cy="125" r="5" fill="#7c5ee0" />
    </g>
  ),
  physics: () => (
    <>
      {gridLines('#3d3d82', 0.6, 0.7)}
      <g fill="none" strokeWidth="2.4">
        <ellipse cx="56" cy="100" rx="34" ry="12" stroke="#F28A2E" />
        <ellipse cx="56" cy="100" rx="34" ry="12" stroke="#D9487A" transform="rotate(60 56 100)" />
        <ellipse cx="56" cy="100" rx="34" ry="12" stroke="#F6C453" transform="rotate(-60 56 100)" />
      </g>
      <circle cx="56" cy="100" r="4" fill="#fff" />
    </>
  ),
  journal: () => gridLines('#3a3b78', 0.6),
  chem: () => (
    <>
      <circle cx="56" cy="104" r="34" fill="#1f6b4f" opacity=".5" />
      <circle cx="56" cy="104" r="20" fill="#3FB27A" opacity=".55" />
      <circle cx="56" cy="104" r="8" fill="#9ED8F2" opacity=".8" />
    </>
  ),
  sketches: (id) => (
    <>
      <defs>
        <pattern id={`${id}-ck`} width="20" height="20" patternUnits="userSpaceOnUse">
          <rect width="10" height="10" fill="#fff" opacity=".85" />
          <rect x="10" y="10" width="10" height="10" fill="#fff" opacity=".85" />
        </pattern>
      </defs>
      <rect y="70" width="120" height="90" fill={`url(#${id}-ck)`} />
    </>
  ),
  spanish: (id) => (
    <>
      <defs>
        <pattern id={`${id}-pl`} width="16" height="16" patternUnits="userSpaceOnUse">
          <rect width="16" height="16" fill="#F2A55E" />
          <rect width="8" height="16" fill="#fff" opacity=".35" />
          <rect width="16" height="8" fill="#fff" opacity=".35" />
        </pattern>
      </defs>
      <rect y="70" width="120" height="90" fill={`url(#${id}-pl)`} />
    </>
  ),
  history: () => (
    <>
      <circle cx="70" cy="110" r="30" fill="#F6C453" />
      <rect x="20" y="96" width="40" height="40" fill="#1B1936" />
    </>
  ),
  moss: () => <path d="M0 120 Q30 90 60 112 T120 100 V160 H0Z" fill="#25604A" />,
  dusk: () => (
    <>
      <circle cx="60" cy="100" r="26" fill="#D9487A" />
      <circle cx="60" cy="100" r="26" fill="#F28A2E" opacity=".6" transform="translate(8 8)" />
    </>
  ),
  plain: () => null,
};
