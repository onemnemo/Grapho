import markOnDark from './grapho-mark-on-dark.svg';
import markOnLight from './grapho-mark-on-light.svg';
import markMonoWhite from './grapho-mark-mono-white.svg';
import iconRounded from './grapho-icon-rounded.svg';

const sources = {
  'mark-on-dark': markOnDark,
  'mark-on-light': markOnLight,
  'mark-mono-white': markMonoWhite,
  'app-icon': iconRounded,
} as const;

export type LogoVariant = keyof typeof sources;

export interface LogoProps {
  variant?: LogoVariant;
  /** Height in px; the width follows the artwork. */
  height?: number;
}

/** The Grapho swash. Never redraw or recolour it; pick the variant for the background. */
export function Logo({ variant = 'mark-on-dark', height = 32 }: LogoProps) {
  return <img src={sources[variant]} alt="Grapho" style={{ height, width: 'auto', display: 'block' }} />;
}
