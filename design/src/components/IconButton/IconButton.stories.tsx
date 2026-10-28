import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import { iconPaths, type IconName } from '@/assets/icons';
import { IconButton } from './IconButton';

const meta: Meta<typeof IconButton> = {
  title: 'Components/IconButton',
  component: IconButton,
  args: { icon: 'pen', label: 'Pen', variant: 'quiet', size: 'md', active: false, dot: false, disabled: false },
  argTypes: {
    icon: { control: 'select', options: Object.keys(iconPaths) as IconName[] },
    variant: { control: 'inline-radio', options: ['quiet', 'solid', 'ghost'] },
    size: { control: 'inline-radio', options: ['sm', 'md', 'lg'] },
  },
};
export default meta;
type Story = StoryObj<typeof IconButton>;

const row = { display: 'flex', gap: 16, alignItems: 'center' } as const;

export const Playground: Story = {};

export const Variants: Story = {
  render: () => (
    <div style={row}>
      <IconButton icon="settings" label="Quiet" variant="quiet" />
      <IconButton icon="settings" label="Solid" variant="solid" />
      <IconButton icon="settings" label="Ghost" variant="ghost" />
    </div>
  ),
};

export const Sizes: Story = {
  render: () => (
    <div style={row}>
      {(['sm', 'md', 'lg'] as const).map((s) => (
        <IconButton key={s} icon="search" label={s} size={s} variant="solid" />
      ))}
    </div>
  ),
};

export const States: Story = {
  render: () => (
    <div style={row}>
      <IconButton icon="filter" label="Rest" />
      <IconButton icon="filter" label="Active" active />
      <IconButton icon="filter" label="Dot" dot />
      <IconButton icon="filter" label="Disabled" disabled />
    </div>
  ),
};

/** The note bar: the selected tool turns white and its body fills softly. */
export const ToolRow: Story = {
  render: function Render() {
    const tools: IconName[] = ['pen', 'highlighter', 'eraser', 'lasso', 'shapes'];
    const [tool, setTool] = useState<IconName>('pen');
    return (
      <div style={row}>
        {tools.map((t) => (
          <IconButton key={t} icon={t} label={t} active={t === tool} onClick={() => setTool(t)} />
        ))}
      </div>
    );
  },
};
