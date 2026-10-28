// The legacy alias layer: every variable name the old src/tokens/tokens.css defined, now written in terms
// of the new tokens. Names with no equivalent keep their old value, read from legacy-tokens.css (a frozen
// copy of the old file). Used by build-tokens.mjs to write src/tokens/tokens.css.
import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { here } from './tokens-model.mjs';

const folders = ['indigo', 'rose', 'ember', 'forest', 'sea', 'gold', 'sky', 'slate'];
export const MAP = {
  // surfaces, text, edges, fills
  bg: 'surface-base', s1: 'surface-raised', s2: 'surface-control', 's2-hover': 'surface-control-hover', s3: 'surface-strong', overlay: 'surface-overlay', stage: 'surface-sunken',
  paper: 'canvas-page', scrim: 'scrim-default', fg: 'text-default', mute: 'text-muted', dim: 'text-subtle', 'on-fg': 'text-on-action', off: 'text-disabled', tint: 'text-default', 'on-fg-mute': 'text-muted',
  'solid-hover': 'action-primary-bg-hover', 'solid-press': 'action-primary-bg-pressed', 'solid-off': 'action-primary-bg-disabled', 'on-solid-off': 'action-primary-fg-disabled',
  accent: 'action-primary-bg', line: 'border-subtle', line2: 'border-strong', 'dot-edge': 'border-strong', hl: 'hover-fill',
  hover: 'hover-fill', press: 'press-fill', press2: 'press-fill', sel: 'selection-fill', 'sel-fill': 'selection-fill', 'sel-row': 'selection-fill', 'sel-fill-hover': 'selection-fill-hover',
  'box-edge': 'border-control', 'box-edge-hover': 'border-control-hover', 'knob-halo': 'hover-fill', 'knob-halo-drag': 'press-fill',
  danger: 'state-danger-fg', 'danger-bg': 'state-danger-bg', 'danger-bg-hover': 'state-danger-bg-hover', 'danger-line': 'state-danger-line', 'warn-line': 'state-warning-line', 'ok-line': 'state-success-line',
  'ok-fg': 'state-success-fg', 'ok-bg': 'state-success-bg', ok: 'brand-green', tip: 'action-primary-bg', 'checker-a': 'surface-strong', 'checker-b': 'surface-control', white: 'state-danger-on-solid',
  // brand and content
  pink: 'brand-pink', red: 'brand-red', orange: 'brand-orange', navy: 'brand-navy', gold: 'brand-gold', blue: 'brand-blue', green: 'brand-green', lilac: 'brand-lilac', tail: 'brand-tail',
  'page-white': 'content-paper-white', 'page-cream': 'content-paper-cream', 'page-slate': 'content-paper-slate', 'page-dark': 'content-paper-dark', 'page-black': 'content-paper-black',
  'paper-ink': 'canvas-ink-default', 'paper-ink-light': 'content-ink-on-dark', 'paper-rule': 'canvas-rule', 'paper-sel': 'canvas-selection-fill', mark: 'highlight-match',
  'on-art': 'media-text', 'art-edge': 'media-edge', 'on-media': 'media-chip', 'on-media-hover': 'media-chip-hover',
  // type, space, shape, motion, layers
  sans: 'font-sans', hand: 'font-hand', 't-display': 'type-display', 't-title': 'type-title', 't-heading': 'type-heading', 't-body': 'type-body', 't-label': 'type-label', 't-caption': 'type-caption',
  'track-display': 'type-display-tracking', 'track-title': 'type-title-tracking', 'track-heading': 'type-heading-tracking',
  'sp-1': 'space-xs', 'sp-2': 'space-sm', 'sp-3': 'space-md', 'sp-4': 'space-lg', 'sp-5': 'space-xl', 'sp-6': 'space-2xl', 'sp-8': 'space-3xl', 'sp-12': 'space-4xl', 'sp-16': 'space-5xl',
  'r-xs': 'radius-key', 'r-sm': 'radius-item', 'r-md': 'radius-surface', 'r-lg': 'radius-container', 'r-full': 'radius-pill', hit: 'size-hit-touch',
  ease: 'motion-easing-standard', 'ease-in': 'motion-easing-exit', 'd-press': 'motion-duration-press', 'd-fast': 'motion-duration-fast', 'd-base': 'motion-duration-base', 'd-slow': 'motion-duration-slow',
  'd-exit': 'motion-duration-exit', 'd-sheet': 'motion-duration-slow', 'd-pop': 'motion-duration-pop', 'd-toast': 'toast-duration-undo', 'd-toast-plain': 'toast-duration-plain',
  'z-bar': 'z-floating-bar', 'z-menu': 'z-popover', 'z-tip': 'z-tooltip', 'toast-max': 'toast-width-max',
  'wh-ring': 'wheel-ring', 'wh-hub': 'wheel-hub', 'wh-hub-edge': 'wheel-hub-edge', 'wh-plate': 'wheel-plate', 'wh-dot': 'wheel-dot',
  'wh-divider': 'wheel-divider', 'wh-band-edge': 'wheel-band-edge',
  'wh-well': 'wheel-well', 'wh-grip': 'wheel-grip', 'wh-scroll-track': 'wheel-scroll-track', 'wh-glyph-ring': 'wheel-glyph-ring',
  'wh-checker-dark': 'wheel-checker-dark', 'wh-checker-light': 'wheel-checker-light', 'wh-field': 'wheel-field', 'wh-field-focus': 'wheel-field-focus', 'wh-hint-edge': 'wheel-edge',
};
for (const f of folders) { MAP[`f-${f}-b`] = `content-folder-${f}-back`; MAP[`f-${f}-1`] = `content-folder-${f}-top`; MAP[`f-${f}-2`] = `content-folder-${f}-bottom`; }
// Names that must keep their ring or composition, written out
const SPECIAL = {
  e1: 'var(--elevation-float)',
  e2: '0 0 0 1px var(--border-strong), var(--elevation-popover)',
  e3: '0 0 0 1px var(--border-strong), var(--elevation-sheet)',
};
// Defined by the new tokens already with the same meaning, or replaced there: do not emit
const SKIP = new Set(['hover-fill', 'z-sheet', 'z-dialog', 'z-toast', 'wheel-hub']);

export const legacyLayer = (cssNames) => {
  const old = readFileSync(join(here, 'legacy-tokens.css'), 'utf8').replace(/\/\*[\s\S]*?\*\//g, '');
  const decls = new Map();
  for (const m of old.matchAll(/--([a-zA-Z0-9-]+)\s*:\s*([^;]+);/g)) if (!decls.has(m[1])) decls.set(m[1], m[2].trim());
  const lines = [], errors = [];
  let aliased = 0, kept = 0;
  for (const [name, value] of decls) {
    if (SKIP.has(name) || name.startsWith('p-') || name.startsWith('th-')) continue;
    if (SPECIAL[name]) { lines.push(`--${name}: ${SPECIAL[name]};`); aliased++; continue; }
    if (MAP[name]) {
      if (!cssNames.has(`--${MAP[name]}`)) errors.push(`legacy --${name} maps to undefined --${MAP[name]}`);
      lines.push(`--${name}: var(--${MAP[name]});`); aliased++;
    } else { lines.push(`--${name}: ${value};`); kept++; }
  }
  for (const k of Object.keys(MAP)) if (!decls.has(k) && !SKIP.has(k)) errors.push(`legacy map names --${k}, which the old file never defined`);
  return { lines, aliased, kept, errors };
};
