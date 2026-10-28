import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import { Checkbox } from './Checkbox';

const meta: Meta<typeof Checkbox> = {
  title: 'Components/Checkbox',
  component: Checkbox,
  args: { 'aria-label': 'Select', checked: false, disabled: false },
  argTypes: { checked: { control: 'select', options: [false, true, 'mixed'] } },
};
export default meta;
type Story = StoryObj<typeof Checkbox>;

const row = { display: 'flex', gap: 24, alignItems: 'center' } as const;

export const Playground: Story = {};

export const States: Story = {
  render: () => (
    <div style={row}>
      <Checkbox aria-label="Unchecked" />
      <Checkbox aria-label="Checked" defaultChecked />
      <Checkbox aria-label="Mixed" defaultChecked="mixed" />
      <Checkbox aria-label="Disabled" disabled />
      <Checkbox aria-label="Disabled checked" defaultChecked disabled />
    </div>
  ),
};

export const WithLabel: Story = {
  render: () => (
    <div style={{ width: 320 }}>
      <Checkbox label="Include the images I use" defaultChecked />
      <Checkbox label="Include fonts" hint="Adds about 2 MB" />
      <Checkbox label="Include notes" disabled />
    </div>
  ),
};

/** A parent that shows mixed while only some children are selected. */
export const Mixed: Story = {
  render: function Render() {
    const [kids, setKids] = useState([true, false, false]);
    const all = kids.every(Boolean);
    const some = kids.some(Boolean);
    return (
      <div style={{ width: 320 }}>
        <Checkbox label="Import everything" checked={all ? true : some ? 'mixed' : false} onChange={(v) => setKids(kids.map(() => v))} />
        {['Stickers', 'Shapes', 'Textures'].map((n, i) => (
          <div key={n} style={{ paddingLeft: 24 }}>
            <Checkbox label={n} checked={kids[i]} onChange={(v) => setKids(kids.map((k, j) => (j === i ? v : k)))} />
          </div>
        ))}
      </div>
    );
  },
};
