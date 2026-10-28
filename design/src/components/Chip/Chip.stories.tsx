import type { Meta, StoryObj } from '@storybook/react-vite';
import { useState } from 'react';
import { Chip, RemovableChip } from './Chip';

const meta: Meta<typeof Chip> = {
  title: 'Components/Chip',
  component: Chip,
  args: { children: 'Notebooks', count: 12, size: 'md', selected: false, disabled: false },
  argTypes: { size: { control: 'inline-radio', options: ['sm', 'md'] } },
};
export default meta;
type Story = StoryObj<typeof Chip>;

const row = { display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' } as const;

export const Playground: Story = {};

export const States: Story = {
  render: () => (
    <div style={row}>
      <Chip>Plain</Chip>
      <Chip selected>Selected</Chip>
      <Chip count={12}>With count</Chip>
      <Chip selected count={12}>Selected count</Chip>
      <Chip disabled>Disabled</Chip>
    </div>
  ),
};

export const Sizes: Story = {
  render: () => (
    <div style={row}>
      <Chip size="md" defaultSelected>Medium</Chip>
      <Chip size="md">Medium</Chip>
      <Chip size="sm" defaultSelected>Small</Chip>
      <Chip size="sm">Small</Chip>
    </div>
  ),
};

export const TypeFilter: Story = {
  render: () => (
    <div style={row}>
      <Chip count={36} defaultSelected>All</Chip>
      <Chip count={22}>Notebooks</Chip>
      <Chip count={9}>Canvases</Chip>
      <Chip count={5}>Documents</Chip>
    </div>
  ),
};

export const Removable: Story = {
  render: function Render() {
    const [tags, setTags] = useState(['maths', 'biology', 'exam week']);
    return (
      <div style={row}>
        {tags.map((t) => (
          <RemovableChip key={t} dot="var(--pink)" removeLabel={`Remove ${t}`} onRemove={() => setTags(tags.filter((x) => x !== t))}>
            {t}
          </RemovableChip>
        ))}
      </div>
    );
  },
};
