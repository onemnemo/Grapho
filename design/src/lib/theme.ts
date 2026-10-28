import type { CSSProperties } from 'react';
import { DEFAULT_CUSTOM, type ThemeId, type ThemeInputs } from '@/assets/themes';

/** What a person chose: a built-in theme, or custom with its four inputs. */
export interface ThemeChoice {
  id: ThemeId;
  custom: ThemeInputs;
}

export const DEFAULT_THEME: ThemeChoice = { id: 'ink', custom: DEFAULT_CUSTOM };

/** The runtime values of a custom theme, set on the element that carries data-theme="custom". */
export function customVars(inputs: ThemeInputs): CSSProperties {
  return {
    '--th-accent': inputs.accent,
    '--th-bg': inputs.bg,
    '--th-surface': inputs.surface,
    '--th-paper': inputs.paper,
  } as CSSProperties;
}

/** Attributes for the root of the app (or a preview): `<div {...themeProps(choice)}>`. */
export function themeProps(choice: ThemeChoice): { 'data-theme': ThemeId; style?: CSSProperties } {
  return choice.id === 'custom' ? { 'data-theme': 'custom', style: customVars(choice.custom) } : { 'data-theme': choice.id };
}

/** Apply a theme to the whole document (the app shell and Storybook do this). */
export function applyTheme(choice: ThemeChoice, root: HTMLElement = document.documentElement): void {
  root.dataset.theme = choice.id;
  const vars = customVars(choice.custom) as Record<string, string>;
  for (const k of Object.keys(vars)) {
    if (choice.id === 'custom') root.style.setProperty(k, vars[k]);
    else root.style.removeProperty(k);
  }
}
