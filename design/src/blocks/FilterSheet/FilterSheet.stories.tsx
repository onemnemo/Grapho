import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { StoryStage } from '@/stories-support/StoryStage';
import { FilterSheet, NO_FILTERS, type FilterState } from './FilterSheet';

const TAGS = [
  { name: 'exam', count: 9 }, { name: 'lab', count: 5 }, { name: 'reading', count: 4 }, { name: 'draft', count: 3 }, { name: 'group work', count: 2 },
  { name: 'summary', count: 2 }, { name: 'homework', count: 2 }, { name: 'formulas', count: 1 }, { name: 'revision', count: 1 }, { name: 'project', count: 1 },
];

const meta: Meta<typeof FilterSheet> = {
  title: 'Blocks/FilterSheet',
  component: FilterSheet,
  parameters: { layout: 'padded' },
  args: { open: true, tags: TAGS, typeCounts: { notebook: 27, pdf: 11, canvas: 4 }, resultCount: 27, value: NO_FILTERS, placement: 'container' },
  render: (args) => {
    const [value, setValue] = useState<FilterState>(args.value);
    const [open, setOpen] = useState(true);
    return (
      <StoryStage height={720}>
        <button type="button" onClick={() => setOpen(true)} style={{ position: 'absolute', top: 16, left: 16, color: 'var(--mute)', font: 'var(--t-label-sm)' }}>Open filters</button>
        <FilterSheet {...args} open={open} onClose={() => setOpen(false)} value={value} onChange={setValue} />
      </StoryStage>
    );
  },
};
export default meta;
type Story = StoryObj<typeof FilterSheet>;

export const Playground: Story = {};
export const WithChoices: Story = { args: { value: { types: ['notebook'], edited: 'month', starred: true, tags: ['exam', 'lab'] }, resultCount: 6 } };
export const OneResult: Story = { args: { resultCount: 1 } };
export const NoItemsMatch: Story = { args: { resultCount: 0, value: { types: ['canvas'], edited: 'week', starred: true, tags: ['exam'] } } };
