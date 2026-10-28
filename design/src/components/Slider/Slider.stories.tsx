import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import { Slider } from './Slider';

const meta: Meta<typeof Slider> = {
  title: 'Components/Slider',
  component: Slider,
  args: { 'aria-label': 'Opacity', min: 0, max: 100, step: 1, defaultValue: 60, unit: '%', bubble: true, disabled: false },
  decorators: [(Story) => <div style={{ width: 320, paddingTop: 36 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof Slider>;

export const Playground: Story = {};

export const WithBubble: Story = { args: { bubble: true } };
export const WithoutBubble: Story = { args: { bubble: false } };
export const Stepped: Story = { args: { min: 1, max: 10, step: 1, defaultValue: 4, unit: 'pt' } };
export const Disabled: Story = { args: { disabled: true } };

/** The inspector row: label, slider and a live value. */
export const InRow: Story = {
  render: function Render() {
    const [v, setV] = useState(35);
    return (
      <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
        <span style={{ width: 72 }}>Size</span>
        <Slider aria-label="Size" value={v} onChange={setV} />
        <span style={{ width: 38, textAlign: 'right' }}>{v}</span>
      </div>
    );
  },
};
