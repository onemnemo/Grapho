import type { Meta, StoryObj } from '@storybook/react-vite';
import { SelectionBadge } from './SelectionBadge';

const meta: Meta<typeof SelectionBadge> = {
  title: 'Blocks/SelectionBadge',
  component: SelectionBadge,
  args: { visible: true, selected: false, size: 'md' },
  argTypes: { size: { control: 'inline-radio', options: ['md', 'sm'] } },
  render: (args) => (
    <div style={{ position: 'relative', width: 60, height: 60, ['--badge-left' as string]: '16px', ['--badge-top' as string]: '16px' }}>
      <SelectionBadge {...args} />
    </div>
  ),
};
export default meta;
type Story = StoryObj<typeof SelectionBadge>;

export const Playground: Story = {};
export const Hidden: Story = { args: { visible: false } };
export const Choosing: Story = { args: { visible: true, selected: false } };
export const Selected: Story = { args: { selected: true } };
export const Small: Story = { args: { selected: true, size: 'sm' } };
