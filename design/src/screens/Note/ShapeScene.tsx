import { useRef, useState } from 'react';
import { LassoBar } from '@/blocks/LassoBar';
import { defaultShape, ShapeOptions, ShapePreview, type ShapeValues } from '@/blocks/ShapeOptions';
import { useToast } from '@/components/Toast';
import { NoteLayout } from './NoteLayout';
import type { SceneProps } from './sceneTypes';

/** A selected shape: the floating bar gains one extra button, Shape, which opens the options beside the page. Changes apply live. */
export function ShapeScene({ settings, ...rest }: SceneProps) {
  const toast = useToast();
  const [shape, setShape] = useState<ShapeValues>(defaultShape);
  const [open, setOpen] = useState(true);
  const bar = useRef<HTMLDivElement>(null);
  const anchor = useRef<HTMLElement | null>(null);
  const anchored = () => { anchor.current = bar.current?.querySelector<HTMLElement>('[aria-label="Shape options"]') ?? null; return anchor; };
  return (
    <NoteLayout
      {...rest}
      settings={settings.values}
      bar={{ activeTool: 'shapes' }}
      page={<ShapePreview values={shape} />}
      floating={
        <>
          <div ref={(el) => { bar.current = el; anchored(); }} style={{ display: 'contents' }}>
          <LassoBar
            x={200} y={132}
            items={[{ id: 'options', icon: 'shapes', label: 'Shape options', active: open }, { id: 'colour', icon: 'palette', label: 'Colour' }, { id: 'duplicate', icon: 'duplicate', label: 'Duplicate' }, 'separator', { id: 'delete', icon: 'trash', label: 'Delete', danger: true }]}
            onAction={(id) => (id === 'options' ? setOpen(!open) : toast.show(id === 'delete' ? 'Shape deleted' : id === 'duplicate' ? 'Duplicated' : 'Colour', { kind: 'info', duration: 1400 }))}
          />
          </div>
          <ShapeOptions open={open} onClose={() => setOpen(false)} anchorRef={anchor} boundary="container" values={shape} onChange={setShape} />
        </>
      }
    />
  );
}
