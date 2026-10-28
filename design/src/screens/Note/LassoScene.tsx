import { useRef, useState } from 'react';
import { LassoBar, lassoActions } from '@/blocks/LassoBar';
import { InkLayer, SampleInk, SelectionOutline } from '@/blocks/NotePage';
import { SelectionMenu } from '@/blocks/SelectionMenu';
import { Popover } from '@/components/Popover';
import { Slider } from '@/components/Slider';
import { SwatchRow } from '@/components/Swatch';
import { useToast } from '@/components/Toast';
import { NoteLayout } from './NoteLayout';
import type { SceneProps } from './sceneTypes';

const INK_COLOURS = [
  { value: 'var(--paper-ink)', color: 'var(--paper-ink)', label: 'Ink' },
  { value: 'var(--pink)', color: 'var(--pink)', label: 'Pink' },
  { value: 'var(--red)', color: 'var(--red)', label: 'Red' },
  { value: 'var(--orange)', color: 'var(--orange)', label: 'Orange' },
  { value: 'var(--gold)', color: 'var(--gold)', label: 'Gold' },
  { value: 'var(--green)', color: 'var(--green)', label: 'Green' },
  { value: 'var(--blue)', color: 'var(--blue)', label: 'Blue' },
  { value: 'var(--lilac)', color: 'var(--lilac)', label: 'Lilac' },
];

type Pop = 'colour' | 'thickness' | 'more' | null;
const COLOURS = [...INK_COLOURS, { value: 'custom', kind: 'custom' as const, label: 'Custom colour' }];

/**
 * A lasso selection. One floating bar rides on the selection: colour, thickness, copy, duplicate and More. The toolbar stays as it is.
 * Delete is instant, with an Undo toast.
 */
export function LassoScene({ settings, ...rest }: SceneProps) {
  const toast = useToast();
  const [selected, setSelected] = useState(true);
  const [pop, setPop] = useState<Pop>(null);
  const [layer, setLayer] = useState('ink');
  const anchor = useRef<HTMLElement | null>(null);
  const close = () => setPop(null);

  const floating = (id: string, e?: { currentTarget: HTMLElement }) => {
    if ((id === 'colour' || id === 'thickness' || id === 'more') && e) {
      anchor.current = e.currentTarget;
      setPop(id);
    } else if (id === 'delete') {
      setSelected(false);
      toast.deleted('12 strokes', () => setSelected(true));
    } else toast.show({ cut: 'Cut', copy: 'Copied', duplicate: 'Duplicated', text: 'Converting to text' }[id] ?? id, { kind: 'ok', duration: 1600 });
  };

  return (
    <NoteLayout
      {...rest}
      settings={settings.values}
      hint={selected ? undefined : 'Nothing selected'}
      bar={{ activeTool: 'lasso' }}
      page={
        <>
          <SampleInk />
          {selected && <InkLayer><SelectionOutline x={48} y={150} width={372} height={192} /></InkLayer>}
        </>
      }
      floating={selected && <LassoBar x={234} y={112} items={lassoActions} onAction={floating} />}
      overlays={
        <>
          {rest.overlays}
          <Popover open={pop === 'colour'} onClose={close} anchorRef={anchor} kind="content" placement="bottom" aria-label="Colour">
            <SwatchRow aria-label="Colour" swatches={COLOURS} defaultValue="var(--paper-ink)" size="sm" gap={8} />
          </Popover>
          <Popover open={pop === 'thickness'} onClose={close} anchorRef={anchor} kind="content" placement="bottom" aria-label="Thickness">
            <Slider aria-label="Thickness" min={1} max={12} defaultValue={3} unit="dp" bubble />
          </Popover>
          <SelectionMenu
            open={pop === 'more'} onClose={close} anchorRef={anchor} placement="bottom" canPaste layer={layer}
            onAction={(a) => (a === 'delete' ? floating('delete') : a.startsWith('layer:') ? setLayer(a.slice(6) === 'l2' ? 'image' : 'ink') : toast.show('Done', { kind: 'ok', duration: 1400 }))}
          />
        </>
      }
    />
  );
}
