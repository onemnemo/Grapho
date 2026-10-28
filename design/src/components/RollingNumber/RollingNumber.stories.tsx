import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '../Button';
import { RollingNumber } from './RollingNumber';

const meta: Meta<typeof RollingNumber> = {
  title: 'Components/RollingNumber',
  component: RollingNumber,
  parameters: { layout: 'centered' },
  args: { value: 4 },
};
export default meta;
type Story = StoryObj<typeof RollingNumber>;

export const Static: Story = {};

/** The old digit leaves one way and the new one arrives. */
export const Counting: Story = {
  render: function Render() {
    const [n, setN] = useState(4);
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <Button size="sm" onClick={() => setN((v) => Math.max(0, v - 1))}>Fewer</Button>
        <b style={{ font: 'var(--t-title)' }}><RollingNumber value={n} /></b>
        <Button size="sm" onClick={() => setN((v) => v + 1)}>More</Button>
      </div>
    );
  },
};
