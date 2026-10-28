import type { Meta, StoryObj } from '@storybook/react-vite';
import { DeviceFrame } from '@/components/DeviceFrame';
import { NotePage } from './NotePage';
import { SampleInk } from './Ink';

const meta: Meta<typeof NotePage> = {
  title: 'Blocks/NotePage',
  component: NotePage,
  parameters: { layout: 'padded' },
  render: (args) => <DeviceFrame width={800} height={360}><NotePage {...args}><SampleInk /></NotePage></DeviceFrame>,
};
export default meta;
type Story = StoryObj<typeof NotePage>;

export const EdgeToEdge: Story = {};
/** A full line bar at the top takes 40dp of the page. */
export const ReservingSpaceForALine: Story = { args: { inset: { top: 40 } } };
export const ReservingSpaceOnTheLeft: Story = { args: { inset: { left: 40 } } };
