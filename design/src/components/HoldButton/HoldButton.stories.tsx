import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import { HoldButton } from './HoldButton';

const meta: Meta<typeof HoldButton> = {
  title: 'Components/HoldButton',
  component: HoldButton,
  args: { label: 'Hold to delete', icon: 'trash', size: 'md', duration: 900, disabled: false },
  argTypes: { size: { control: 'inline-radio', options: ['sm', 'md', 'lg'] }, duration: { control: { type: 'range', min: 300, max: 2000, step: 100 } } },
};
export default meta;
type Story = StoryObj<typeof HoldButton>;

export const Playground: Story = {};

export const Sizes: Story = {
  render: () => (
    <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
      <HoldButton size="sm" />
      <HoldButton size="md" />
      <HoldButton size="lg" />
    </div>
  ),
};

export const Disabled: Story = { args: { disabled: true } };

/** Hold for the full time to confirm. Let go early and the fill drains back. */
export const Confirm: Story = {
  render: function Render() {
    const [n, setN] = useState(0);
    const [confirmed, setConfirmed] = useState(false);
    return (
      <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
        <HoldButton key={n} label="Hold to delete forever" onConfirm={() => setConfirmed(true)} />
        <span style={{ color: 'var(--mute)', width: 80 }}>{confirmed ? 'Deleted' : ''}</span>
        <button type="button" onClick={() => { setN(n + 1); setConfirmed(false); }}>Reset</button>
      </div>
    );
  },
};
