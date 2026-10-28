import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { SAVED_COLOURS } from '@/assets/studio';
import { ColourDisc } from './ColourDisc';

const meta: Meta<typeof ColourDisc> = { title: 'Components/ColourDisc', component: ColourDisc, parameters: { layout: 'centered' } };
export default meta;
type Story = StoryObj<typeof ColourDisc>;

export const Playground: Story = {
  args: { size: 196, value: SAVED_COLOURS[0] },
  render: (args) => {
    const [c, setC] = useState(args.value);
    return (
      <div style={{ width: 236, padding: 14, background: 'var(--overlay)', borderRadius: 'var(--r-md)', boxShadow: 'var(--e2)' }}>
        <ColourDisc {...args} value={c} onChange={setC} />
      </div>
    );
  },
};
export const Compact: Story = {
  render: () => {
    const [c, setC] = useState(SAVED_COLOURS[1]);
    return <div style={{ width: 150 }}><ColourDisc value={c} onChange={setC} size={150} /></div>;
  },
};
