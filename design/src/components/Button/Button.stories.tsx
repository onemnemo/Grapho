import type { Meta, StoryObj } from '@storybook/react-vite';
import { iconPaths, type IconName } from '@/assets/icons';
import { Button } from './Button';

const meta: Meta<typeof Button> = {
  title: 'Components/Button',
  component: Button,
  args: { children: 'Create folder', variant: 'primary', size: 'md', icon: 'plus', disabled: false },
  argTypes: { icon: { control: 'select', options: [undefined, ...(Object.keys(iconPaths) as IconName[])] } },
};
export default meta;
type Story = StoryObj<typeof Button>;

export const Playground: Story = {};

export const Variants: Story = {
  render: () => (
    <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
      <Button variant="primary" icon="plus">Create folder</Button>
      <Button variant="secondary">Customise</Button>
      <Button variant="text">Cancel</Button>
      <Button variant="danger" icon="trash">Delete</Button>
    </div>
  ),
};

export const Sizes: Story = {
  render: () => (
    <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
      <Button variant="primary" size="sm">Small</Button>
      <Button variant="primary">Medium</Button>
      <Button variant="primary" size="lg">Large</Button>
    </div>
  ),
};

export const Disabled: Story = { args: { disabled: true } };
