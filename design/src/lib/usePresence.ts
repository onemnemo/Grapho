import { useEffect, useState } from 'react';

const reduced = () => typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/**
 * Keeps something mounted while its exit animation plays.
 * `mounted` is true from open until `exitMs` after close; `leaving` is true during that exit.
 */
export function usePresence(open: boolean, exitMs = 160) {
  const [mounted, setMounted] = useState(open);
  const [leaving, setLeaving] = useState(false);

  useEffect(() => {
    if (open) {
      setMounted(true);
      setLeaving(false);
      return;
    }
    if (!mounted) return;
    if (reduced()) {
      setMounted(false);
      return;
    }
    setLeaving(true);
    const t = window.setTimeout(() => {
      setMounted(false);
      setLeaving(false);
    }, exitMs);
    return () => window.clearTimeout(t);
  }, [open, exitMs, mounted]);

  return { mounted: open || mounted, leaving: !open && leaving };
}
