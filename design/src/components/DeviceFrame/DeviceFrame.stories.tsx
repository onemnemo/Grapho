import type { Meta, StoryObj } from '@storybook/react-vite';
import { DeviceFrame } from './DeviceFrame';

const meta: Meta<typeof DeviceFrame> = {
  title: 'Components/DeviceFrame',
  component: DeviceFrame,
  parameters: { layout: 'centered' },
  args: { width: 480, height: 300, tone: 'stage', shape: 'tablet' },
  argTypes: {
    tone: { control: 'inline-radio', options: ['stage', 'screen'] },
    shape: { control: 'inline-radio', options: ['tablet', 'phone'] },
  },
};
export default meta;
type Story = StoryObj<typeof DeviceFrame>;

/** A fixed-size window that overlays and the note page are placed inside. */
export const Stage: Story = {};
export const Screen: Story = { args: { tone: 'screen' } };
export const Phone: Story = { args: { width: 240, height: 420, shape: 'phone' } };
