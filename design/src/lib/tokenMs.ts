/** Reads a duration token such as `--d-toast` from the document, in ms. Falls back when the page has no tokens (tests, SSR). */
export function tokenMs(name: string, fallback: number): number {
  if (typeof document === 'undefined') return fallback;
  const raw = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  const n = parseFloat(raw);
  if (Number.isNaN(n)) return fallback;
  return raw.endsWith('ms') ? n : raw.endsWith('s') ? n * 1000 : fallback;
}
