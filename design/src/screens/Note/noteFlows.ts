import { useRef } from 'react';
import type { InsertAction } from '@/blocks/InsertMenu';
import { useToast } from '@/components/Toast';

/**
 * The messages of the insert and export flows, in the order the app shows them: a working line, then the result. Timers stand in for the work.
 * Failure is chosen from the story so every message can be seen.
 */
export function useNoteFlows(fail: boolean) {
  const toast = useToast();
  const timer = useRef(0);
  const later = (ms: number, run: () => void) => {
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(run, ms);
  };
  const insert = (a: InsertAction) => {
    if (a === 'takePhoto' && fail) { toast.show('No camera app found.', { kind: 'error' }); return; }
    if (a === 'blankPage') { toast.show('Added 1 page', { kind: 'ok', duration: 1800 }); return; }
    const pages = a === 'pdfPages';
    toast.show(pages ? 'Adding pages' : 'Adding image', { kind: 'info', icon: 'spinner', duration: 1400 });
    later(1400, () => {
      if (fail) toast.show(pages ? "Couldn't add those pages." : "Couldn't add that image.", { kind: 'error', duration: 2600 });
      else toast.show(pages ? 'Added 8 pages' : 'Image added', { kind: 'ok', duration: 1800 });
    });
  };
  const exportNote = (done: () => void) => {
    later(1500, () => {
      done();
      toast.show(fail ? "Couldn't export the note." : 'Exported', { kind: fail ? 'error' : 'ok', duration: 2200 });
    });
  };
  return { insert, exportNote };
}
