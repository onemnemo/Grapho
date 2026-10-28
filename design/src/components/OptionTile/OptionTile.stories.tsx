import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Icon } from '@/assets/icons';
import { OptionIcon, OptionTile, OptionTileGroup } from './OptionTile';

const meta: Meta<typeof OptionTile> = {
  title: 'Components/OptionTile',
  component: OptionTile,
  parameters: { layout: 'padded' },
  args: { label: 'Suggest', hint: 'Pause after drawing and a small chip offers the clean shape.', selected: false, layout: 'row' },
  decorators: [(Story) => <div style={{ width: 520 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof OptionTile>;

export const Playground: Story = {
  render: (a) => <OptionTile {...a} visual={<OptionIcon><Icon name="sparkle" size={20} /></OptionIcon>} />,
};

export const RowGroup: Story = {
  render: () => {
    const [v, setV] = useState('ask');
    const rows = [['ask', 'sparkle', 'Suggest', 'Pause after drawing and a small chip offers the clean shape.'], ['snap', 'shapes', 'Snap on hold', 'Hold the pen at the end of a stroke and it straightens.'], ['off', 'slash', 'Off', 'Strokes stay exactly as you wrote them.']] as const;
    return (
      <OptionTileGroup columns={1} aria-label="Shape detection">
        {rows.map(([id, icon, label, hint]) => (
          <OptionTile key={id} layout="row" selected={v === id} onClick={() => setV(id)} label={label} hint={hint} visual={<OptionIcon><Icon name={icon} size={20} /></OptionIcon>} />
        ))}
      </OptionTileGroup>
    );
  },
};

export const TileGrid: Story = {
  render: () => {
    const [v, setV] = useState('top');
    return (
      <OptionTileGroup columns={4} aria-label="Position">
        {['top', 'bottom', 'left', 'right'].map((p) => (
          <OptionTile key={p} layout="tile" selected={v === p} onClick={() => setV(p)} label={p[0].toUpperCase() + p.slice(1)} visual={<OptionIcon><Icon name="sliders" size={20} /></OptionIcon>} />
        ))}
      </OptionTileGroup>
    );
  },
};
