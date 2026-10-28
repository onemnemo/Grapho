import { useRef, type PointerEvent } from 'react';

/** Long press (450 ms, without moving) starts selection, like on a touch screen. Returns props to spread on a tile. */
export function useLongPress(onLong: (id: string) => void, enabled: boolean) {
  const timer = useRef<number>(0);
  const origin = useRef<[number, number]>([0, 0]);
  const fired = useRef(false);
  const cancel = () => window.clearTimeout(timer.current);
  return (id: string) => ({
    onPointerDown: (e: PointerEvent) => {
      fired.current = false;
      if (!enabled || e.button > 0) return;
      origin.current = [e.clientX, e.clientY];
      timer.current = window.setTimeout(() => { fired.current = true; onLong(id); }, 450);
    },
    onPointerMove: (e: PointerEvent) => {
      if (Math.hypot(e.clientX - origin.current[0], e.clientY - origin.current[1]) > 8) cancel();
    },
    onPointerUp: cancel,
    onPointerLeave: cancel,
    onPointerCancel: cancel,
    /** True when the press that just ended was a long press, so the click that follows should be ignored. */
    wasLong: () => fired.current,
  });
}
