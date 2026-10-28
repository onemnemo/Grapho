import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { RemovableChip } from '@/components/Chip';
import { Button } from '@/components/Button';
import { Surface } from '@/stories-support/Surface';
import { SectionHeader } from './SectionHeader';
import type { SortDir, SortKey, ViewMode } from '../SortMenu';

const meta: Meta<typeof SectionHeader> = {
  title: 'Blocks/SectionHeader',
  component: SectionHeader,
  parameters: { layout: 'padded' },
  args: { title: 'Folders', count: 12 },
  render: (args) => <Surface style={{ width: 760 }}><SectionHeader {...args} /></Surface>,
};
export default meta;
type Story = StoryObj<typeof SectionHeader>;

export const Playground: Story = {};
export const WithSeeAll: Story = { args: { onSeeAll: () => {} } };

function Full({ open }: { open?: boolean }) {
  const [key, setKey] = useState<SortKey>('opened');
  const [dir, setDir] = useState<SortDir>('desc');
  const [view, setView] = useState<ViewMode>('grid');
  return (
    <Surface style={{ width: 760, minHeight: open ? 520 : undefined }}>
      <SectionHeader title="Recent" count="3 of 40" view={view} onView={setView} defaultSortOpen={open}
        sort={{ scope: 'items', sortKey: key, sortDir: dir, onSortKey: setKey, onSortDir: setDir }}>
        <RemovableChip onRemove={() => {}}>Notebooks</RemovableChip>
        <RemovableChip onRemove={() => {}}>exam</RemovableChip>
        <Button variant="text" size="sm">Clear all</Button>
      </SectionHeader>
    </Surface>
  );
}
export const WithSortAndView: Story = { render: () => <Full /> };
export const SortOpen: Story = { render: () => <Full open /> };
