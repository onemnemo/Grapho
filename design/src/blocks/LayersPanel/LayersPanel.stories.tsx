import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { LayersPanel } from './LayersPanel';
import { sampleLayers, type NoteLayer } from './layers';

const meta: Meta<typeof LayersPanel> = {
  title: 'Blocks/LayersPanel',
  component: LayersPanel,
  parameters: { layout: 'centered' },
  argTypes: { layers: { control: false }, onChange: { control: false }, onActiveChange: { control: false } },
};
export default meta;
type Story = StoryObj<typeof LayersPanel>;

const Live = ({ initial, boundary }: { initial: NoteLayer[]; boundary?: 'viewport' | 'container' }) => {
  const [layers, setLayers] = useState(initial);
  const [active, setActive] = useState(initial[initial.length - 1].id);
  return (
    <div style={{ width: 320, height: 420, position: 'relative' }}>
      <LayersPanel layers={layers} activeId={active} onChange={setLayers} onActiveChange={setActive} boundary={boundary ?? 'viewport'} style={{ height: '100%' }} />
    </div>
  );
};

/** Drag the grip to reorder (touch: press and hold). Eye hides, padlock locks, dots hold Rename, Opacity, Merge down and Delete. Double-click a name to rename it. */
export const Default: Story = { render: () => <Live initial={sampleLayers} /> };
export const OneLayer: Story = { render: () => <Live initial={[sampleLayers[2]]} /> };
export const HiddenAndLocked: Story = {
  render: () => <Live initial={[{ id: 'a', name: 'Notes', visible: false, locked: false, opacity: 1 }, { id: 'b', name: 'Scan', visible: true, locked: true, opacity: 0.4 }, { id: 'c', name: 'Layer 1', visible: true, locked: false, opacity: 1 }]} />,
};
