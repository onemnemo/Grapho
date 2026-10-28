import type { Meta, StoryObj } from '@storybook/react-vite';
import { Surface } from '@/stories-support/Surface';
import { GroupHeading } from './GroupHeading';

const meta: Meta<typeof GroupHeading> = {
  title: 'Blocks/GroupHeading',
  component: GroupHeading,
  args: { label: 'A', count: 12, tone: 'default' },
  argTypes: { tone: { control: 'inline-radio', options: ['default', 'quiet'] } },
  render: (args) => <Surface style={{ width: 520 }}><GroupHeading {...args} /></Surface>,
};
export default meta;
type Story = StoryObj<typeof GroupHeading>;

export const Playground: Story = {};
export const Day: Story = { args: { label: 'Yesterday', count: 3 } };
export const Pinned: Story = { args: { label: 'Pinned', count: 3, tone: 'quiet' } };
