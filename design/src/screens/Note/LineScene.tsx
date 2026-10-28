import { useRef, useState } from 'react';
import { LassoBar } from '@/blocks/LassoBar';
import { defaultLine, LineOptions, LinePreview, type LineValues } from '@/blocks/LineOptions';
import { useToast } from '@/components/Toast';
import { NoteLayout } from './NoteLayout';
import type { SceneProps } from './sceneTypes';

/** A selected line or arrow. One tool: two plain ends is a line, one arrow end is an arrow. Swap exchanges the ends. */
export function LineScene({ settings, ...rest }: SceneProps) {
  const toast = useToast();
  const [line, setLine] = useState<LineValues>(defaultLine);
  const [open, setOpen] = useState(true);
  const bar = useRef<HTMLDivElement>(null);
  const anchor = useRef<HTMLElement | null>(null);
  const anchored = () => { anchor.current = bar.current?.querySelector<HTMLElement>('[aria-label="Line options"]') ?? null; return anchor; };
  return (
    <NoteLayout
      {...rest}
      settings={settings.values}
      bar={{ activeTool: 'shapes' }}
      page={<LinePreview values={line} />}
      floating={
        <>
          <div ref={(el) => { bar.current = el; anchored(); }} style={{ display: 'contents' }}>
          <LassoBar
            x={250} y={132}
            items={[{ id: 'options', icon: 'shapes', label: 'Line options', active: open }, { id: 'swap', icon: 'swap', label: 'Swap ends' }, { id: 'colour', icon: 'palette', label: 'Colour' }, { id: 'duplicate', icon: 'duplicate', label: 'Duplicate' }, 'separator', { id: 'delete', icon: 'trash', label: 'Delete', danger: true }]}
            onAction={(id) => {
              if (id === 'options') setOpen(!open);
              else if (id === 'swap') setLine({ ...line, start: line.end, end: line.start });
              else toast.show(id === 'delete' ? 'Line deleted' : id === 'duplicate' ? 'Duplicated' : 'Colour', { kind: 'info', duration: 1400 });
            }}
          />
          </div>
          <LineOptions open={open} onClose={() => setOpen(false)} anchorRef={anchor} boundary="container" values={line} onChange={setLine} />
        </>
      }
    />
  );
}
