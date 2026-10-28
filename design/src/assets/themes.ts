/* Starting values for the four colours a custom theme is made from. They match the built-in themes in
   tokens.css, so "Customise" begins from the look the person was already using. Artwork data like the
   cover palettes, so raw values are allowed here. */

export type BuiltInThemeId = 'ink' | 'graphite' | 'paper';
export type ThemeId = BuiltInThemeId | 'custom';

/** The four user-editable inputs. Every other colour of a custom theme is derived from them in CSS. */
export interface ThemeInputs {
  /** Focus ring, switches and sliders. */
  accent: string;
  /** The page behind everything. Its lightness decides whether text is light or dark. */
  bg: string;
  /** Cards, fields and controls at rest. */
  surface: string;
  /** The note page. */
  paper: string;
}

export const THEME_IDS: ThemeId[] = ['ink', 'graphite', 'paper', 'custom'];

export const THEME_INPUTS: Record<BuiltInThemeId, ThemeInputs> = {
  ink: { accent: '#F5F5F4', bg: '#08080A', surface: '#18181C', paper: '#F3F1EC' },
  graphite: { accent: '#ECECEA', bg: '#131418', surface: '#23242A', paper: '#EFEDE8' },
  paper: { accent: '#17171A', bg: '#FBFAF7', surface: '#EBE8E1', paper: '#FFFFFF' },
};

/** A first custom theme when nothing was chosen yet: Ink with a blue accent. */
export const DEFAULT_CUSTOM: ThemeInputs = { ...THEME_INPUTS.ink, accent: '#5C8DF0' };

/** Example custom themes for stories. */
export const SAMPLE_CUSTOM = {
  /** Graphite with a blue accent. */
  blue: { ...THEME_INPUTS.graphite, accent: '#5C8DF0' } as ThemeInputs,
  /** A light, cool grey-blue with a pink accent. */
  harbour: { accent: '#D9487A', bg: '#EAF0F4', surface: '#D5E0E8', paper: '#FFFFFF' } as ThemeInputs,
  /** A deep navy dark. */
  night: { ...THEME_INPUTS.ink, accent: '#5C8DF0', surface: '#1B2230', bg: '#0B1018' } as ThemeInputs,
};
