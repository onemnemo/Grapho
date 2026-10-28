import { useEffect, useRef, useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { NoteStage } from '@/stories-support/NoteStage';
import { LassoBar } from '../LassoBar';
import { ShapeOptions } from './ShapeOptions';
import { ShapePreview } from './ShapePreview';
import { defaultShape, type ShapeValues } from './shapeTypes';

function Demo({ shape }: { shape: ShapeValues }) {
  const [v, setV] = useState(shape);
  const [open, setOpen] = useState(false);
  const bar = useRef<HTMLDivElement>(null);
  const anchor = useRef<HTMLElement | null>(null);
  useEffect(() => {
    anchor.current = bar.current?.querySelector<HTMLElement>('[aria-label="Shape options"]') ?? null;
    setOpen(true);
  }, []);
  return (
    <NoteStage width={800} height={580} page={<ShapePreview values={v} />}>
      <div ref={bar} style={{ display: 'contents' }}>
        <LassoBar x={200} y={132} onAction={(id) => id === 'o' && setOpen(!open)} items={[{ id: 'o', icon: 'shapes', label: 'Shape options', active: open }, { id: 'c', icon: 'palette', label: 'Colour' }, { id: 'd', icon: 'duplicate', label: 'Duplicate' }, 'separator', { id: 'x', icon: 'trash', label: 'Delete', danger: true }]} />
      </div>
      <ShapeOptions open={open} onClose={() => setOpen(false)} anchorRef={anchor} boundary="container" values={v} onChange={setV} />
    </NoteStage>
  );
}

const meta: Meta<typeof Demo> = {
  title: 'Blocks/ShapeOptions',
  component: Demo,
  parameters: { layout: 'padded' },
  argTypes: { shape: { control: 'object' } },
  args: { shape: defaultShape },
  render: (a) => <Demo {...a} />,
};
export default meta;
type Story = StoryObj<typeof Demo>;

export const Star: Story = {};
export const StarSharp: Story = { args: { shape: { ...defaultShape, points: 8, sharpness: 80 } } };
export const Polygon: Story = { args: { shape: { ...defaultShape, kind: 'polygon', sides: 6 } } };
export const Box: Story = { args: { shape: { ...defaultShape, kind: 'box', radius: 28, fill: true } } };
export const Oval: Story = { args: { shape: { ...defaultShape, kind: 'oval', outline: 'dashed' } } };
export const Dotted: Story = { args: { shape: { ...defaultShape, outline: 'dotted', width: 7 } } };
