import { useRef, useState, type ReactNode, type RefObject } from 'react';
import { Button } from '@/components/Button';

/** Storybook helper: a trigger button that owns the open state of whatever hangs from it. */
export function AnchorDemo({
  label = 'Open',
  defaultOpen = true,
  children,
}: {
  label?: string;
  defaultOpen?: boolean;
  children: (api: { anchorRef: RefObject<HTMLButtonElement | null>; open: boolean; setOpen: (v: boolean) => void }) => ReactNode;
}) {
  const anchorRef = useRef<HTMLButtonElement>(null);
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div style={{ minHeight: 420, minWidth: 360, display: 'grid', placeItems: 'center' }}>
      <span ref={anchorRef as RefObject<HTMLSpanElement>} style={{ display: 'inline-flex' }}>
        <Button onClick={() => setOpen(!open)} aria-haspopup="menu" aria-expanded={open}>{label}</Button>
      </span>
      {children({ anchorRef, open, setOpen })}
    </div>
  );
}
