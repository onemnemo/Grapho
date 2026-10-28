import { useEffect, useRef, type HTMLAttributes, type ReactNode, type RefObject } from 'react';
import { createPortal } from 'react-dom';
import { useAnchoredPosition, type Placement } from '@/lib/anchor';
import { usePresence } from '@/lib/usePresence';
import { cx } from '@/lib/cx';
import styles from './Popover.module.css';

export type PopoverKind = 'menu' | 'content' | 'compact';

export interface PopoverProps extends Omit<HTMLAttributes<HTMLDivElement>, 'children'> {
  open: boolean;
  onClose: () => void;
  /** The element the popover hangs from. Clicks on it do not count as outside. */
  anchorRef: RefObject<HTMLElement | null>;
  placement?: Placement;
  /** Gap between the anchor and the popover, in px. */
  offset?: number;
  /** viewport floats over the whole window; container sits inside the nearest positioned parent, for framed screens and previews. */
  boundary?: 'viewport' | 'container';
  /** menu is a tight 6px frame for a Menu; content has room for a title and controls; compact is a 240px frame for a few quiet controls, with no title. */
  kind?: PopoverKind;
  children: ReactNode;
}

const stack: symbol[] = [];

/**
 * A floating panel anchored to an element. It flips when there is no room, closes on Escape or an outside
 * press, and hands focus back to the anchor. Menus open inside it.
 */
export function Popover({
  open,
  onClose,
  anchorRef,
  placement = 'bottom-start',
  offset = 8,
  boundary = 'viewport',
  kind = 'menu',
  className,
  style,
  children,
  ...rest
}: PopoverProps) {
  const { mounted, leaving } = usePresence(open);
  const panelRef = useRef<HTMLDivElement>(null);
  const inside = useRef(false);
  const closeRef = useRef(onClose);
  closeRef.current = onClose;
  const pos = useAnchoredPosition(anchorRef, panelRef, mounted, placement, offset, boundary === 'container');

  useEffect(() => {
    if (!open) return;
    const id = Symbol('popover');
    stack.push(id);
    const anchor = anchorRef.current;
    const panel = panelRef.current;
    const first =
      panel?.querySelector<HTMLElement>('[role^="menuitem"]:not([aria-disabled="true"])') ??
      panel?.querySelector<HTMLElement>('[data-autofocus], button, input, [href]') ??
      panel;
    first?.focus({ preventScroll: true });

    const onKey = (e: KeyboardEvent) => {
      if (stack[stack.length - 1] !== id) return;
      if (e.key === 'Escape') {
        e.stopPropagation();
        closeRef.current();
      } else if (e.key === 'Tab' && kind === 'menu') {
        closeRef.current();
      }
    };
    const onDown = (e: PointerEvent) => {
      // React marks presses inside this popover or any popover rendered from it before this runs.
      if (inside.current) {
        inside.current = false;
        return;
      }
      if (anchor?.contains(e.target as Node)) return;
      closeRef.current();
    };
    window.addEventListener('keydown', onKey, true);
    document.addEventListener('pointerdown', onDown);
    return () => {
      window.removeEventListener('keydown', onKey, true);
      document.removeEventListener('pointerdown', onDown);
      stack.splice(stack.indexOf(id), 1);
      if (panelRef.current?.contains(document.activeElement) || document.activeElement === document.body) {
        anchor?.focus({ preventScroll: true });
      }
    };
  }, [open, anchorRef, kind]);

  if (!mounted) return null;
  const panel = (
    <div
      ref={panelRef}
      className={cx(styles.popover, styles[kind], className)}
      data-state={leaving ? 'leave' : 'open'}
      data-boundary={boundary}
      data-ready={pos ? 'true' : 'false'}
      data-placement={pos?.placement ?? placement}
      tabIndex={-1}
      onPointerDown={() => { inside.current = true; }}
      style={{ left: pos?.left ?? 0, top: pos?.top ?? 0, transformOrigin: pos?.origin, ...style }}
      {...rest}
    >
      {children}
    </div>
  );
  return boundary === 'viewport' ? createPortal(panel, document.body) : panel;
}
