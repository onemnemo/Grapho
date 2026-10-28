import { useEffect, useRef, type ReactNode, type RefObject } from 'react';
import { createPortal } from 'react-dom';
import { usePresence } from './usePresence';
import styles from './OverlayLayer.module.css';

export type OverlayPlacement = 'viewport' | 'container';

export interface OverlayLayerProps {
  open: boolean;
  onClose: () => void;
  /** viewport covers the window; container fills the nearest positioned parent (for stories and previews). */
  placement?: OverlayPlacement;
  layer?: 'sheet' | 'dialog';
  /** Clicking the scrim closes. Turn off for dialogs that must be answered. */
  dismissOnScrim?: boolean;
  /** Escape closes. */
  dismissOnEscape?: boolean;
  initialFocus?: RefObject<HTMLElement | null>;
  children: ReactNode;
}

const FOCUSABLE = 'a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])';
const stack: symbol[] = [];

/**
 * Scrim, enter and exit, Escape, focus trap and focus return, shared by Sheet and Dialog.
 * The panel is a child; it reads `[data-state="leave"]` from this layer to play its exit.
 */
export function OverlayLayer({
  open,
  onClose,
  placement = 'viewport',
  layer = 'sheet',
  dismissOnScrim = true,
  dismissOnEscape = true,
  initialFocus,
  children,
}: OverlayLayerProps) {
  const { mounted, leaving } = usePresence(open);
  const ref = useRef<HTMLDivElement>(null);
  const closeRef = useRef(onClose);
  closeRef.current = onClose;

  useEffect(() => {
    if (!open) return;
    const root = ref.current;
    if (!root) return;
    const id = Symbol('layer');
    stack.push(id);
    const previous = document.activeElement as HTMLElement | null;
    const lock = placement === 'viewport';
    const overflow = document.body.style.overflow;
    if (lock) document.body.style.overflow = 'hidden';

    const target =
      initialFocus?.current ??
      root.querySelector<HTMLElement>('[data-autofocus]') ??
      root.querySelector<HTMLElement>('[role="dialog"], [role="alertdialog"]');
    target?.focus({ preventScroll: true });

    const onKey = (e: KeyboardEvent) => {
      if (stack[stack.length - 1] !== id) return;
      if (e.key === 'Escape' && dismissOnEscape) {
        e.stopPropagation();
        closeRef.current();
        return;
      }
      if (e.key !== 'Tab') return;
      const items = [...root.querySelectorAll<HTMLElement>(FOCUSABLE)].filter((el) => el.offsetParent !== null);
      if (!items.length) {
        e.preventDefault();
        return;
      }
      const first = items[0];
      const last = items[items.length - 1];
      const active = document.activeElement;
      if (e.shiftKey && (active === first || !root.contains(active))) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && (active === last || !root.contains(active))) {
        e.preventDefault();
        first.focus();
      }
    };
    document.addEventListener('keydown', onKey, true);
    return () => {
      document.removeEventListener('keydown', onKey, true);
      stack.splice(stack.indexOf(id), 1);
      if (lock) document.body.style.overflow = overflow;
      if (previous && document.contains(previous)) previous.focus({ preventScroll: true });
    };
  }, [open, placement, dismissOnEscape, initialFocus]);

  if (!mounted) return null;
  const node = (
    <div
      ref={ref}
      className={styles.layer}
      data-state={leaving ? 'leave' : 'enter'}
      data-placement={placement}
      data-layer={layer}
    >
      <div className={styles.scrim} onClick={dismissOnScrim ? onClose : undefined} aria-hidden />
      {children}
    </div>
  );
  return placement === 'viewport' ? createPortal(node, document.body) : node;
}
