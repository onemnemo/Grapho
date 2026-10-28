import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { packArt } from '@/assets/packs';
import { PackRow } from './PackRow';
import { PackCover } from './PackCover';
import type { PackItem } from './packTypes';

const pack: PackItem = { id: 'pixel', name: 'Pixel friends', art: packArt.amber, enabled: true };

const meta: Meta<typeof PackRow> = {
  title: 'Blocks/PackRow',
  component: PackRow,
  parameters: { layout: 'padded' },
  args: { pack, detail: '48 items', compact: false },
  decorators: [(Story) => <div style={{ width: 560 }}><Story /></div>],
  render: (args) => {
    const [p, setP] = useState(args.pack);
    return <PackRow {...args} pack={p} onToggle={(enabled) => setP({ ...p, enabled })} />;
  },
};
export default meta;
type Story = StoryObj<typeof PackRow>;

export const On: Story = {};
export const Off: Story = { args: { pack: { ...pack, name: 'Ink and brush', art: packArt.graphite, enabled: false }, detail: '8 items' } };
export const Compact: Story = { args: { compact: true } };
export const List: Story = {
  render: () => (
    <div>
      <PackRow pack={pack} detail="48 items" />
      <PackRow pack={{ id: 'b', name: 'Botanical', art: packArt.forest, enabled: true }} detail="28 items" />
      <PackRow pack={{ id: 'm', name: 'Midnight', art: packArt.midnight, enabled: false }} detail="3 items" />
    </div>
  ),
};
export const Cover: Story = {
  render: () => (
    <div style={{ display: 'flex', gap: 12 }}>
      {Object.values(packArt).slice(0, 6).map((a, i) => <PackCover key={i} art={a} />)}
    </div>
  ),
};
