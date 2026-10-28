import { useEffect, useRef, useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { NoteStage } from '@/stories-support/NoteStage';
import { LassoBar } from '../LassoBar';
import { LineOptions } from './LineOptions';
import { LinePreview } from './LinePreview';
import { defaultLine, type LineValues } from './lineTypes';

function Demo({ start }: { start: LineValues }) {
  const [v, setV] = useState(start);
  const [open, setOpen] = useState(false);
  const bar = useRef<HTMLDivElement>(null);
  const anchor = useRef<HTMLElement | null>(null);
  useEffect(() => {
    anchor.current = bar.current?.querySelector<HTMLElement>('[aria-label="Line options"]') ?? null;
    setOpen(true);
  }, []);
  return (
    <NoteStage width={800} height={580} page={<LinePreview values={v} />}>
      <div ref={bar} style={{ display: 'contents' }}>
      <LassoBar
        x={250} y={132}
        onAction={(id) => (id === 'o' ? setOpen(!open) : id === 'swap' && setV({ ...v, start: v.end, end: v.start }))}
        items={[{ id: 'o', icon: 'shapes', label: 'Line options', active: open }, { id: 'swap', icon: 'swap', label: 'Swap ends' }, { id: 'c', icon: 'palette', label: 'Colour' }, { id: 'd', icon: 'duplicate', label: 'Duplicate' }, 'separator', { id: 'x', icon: 'trash', label: 'Delete', danger: true }]}
      />
      </div>
      <LineOptions open={open} onClose={() => setOpen(false)} anchorRef={anchor} boundary="container" values={v} onChange={setV} />
    </NoteStage>
  );
}

const meta: Meta<typeof Demo> = {
  title: 'Blocks/LineOptions',
  component: Demo,
  parameters: { layout: 'padded' },
  argTypes: { start: { control: 'object' } },
  args: { start: defaultLine },
  render: (a) => <Demo {...a} />,
};
export default meta;
type Story = StoryObj<typeof Demo>;

/** A popover on the toolbar button that opened it. No title, no close: press away to dismiss. Ends are two compact dropdowns with a swap between them. The arrowhead grows with the width. */
export const Arrow: Story = {};
export const PlainLine: Story = { args: { start: { ...defaultLine, end: 'none' } } };
export const DoubleArrow: Story = { args: { start: { ...defaultLine, start: 'arrow' } } };
export const Curved: Story = { args: { start: { ...defaultLine, path: 'curved', end: 'open', width: 7 } } };
export const Elbow: Story = { args: { start: { ...defaultLine, path: 'elbow', end: 'dot', start: 'square' } } };
export const Dashed: Story = { args: { start: { ...defaultLine, outline: 'dashed', end: 'diamond', width: 2 } } };
export const Heavy: Story = { args: { start: { ...defaultLine, width: 12, outline: 'dotted' } } };
