import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Surface } from '@/stories-support/Surface';
import { LibraryHeader } from './LibraryHeader';

const meta: Meta<typeof LibraryHeader> = {
  title: 'Blocks/LibraryHeader',
  component: LibraryHeader,
  parameters: { layout: 'fullscreen' },
  args: {
    title: 'Library', selecting: false, selectedCount: 0, searching: false, filterActive: false, scrolled: false,
    onFilter: () => {}, onSettings: () => {}, onNew: () => {}, onSearchOpen: () => {}, onSearchClose: () => {},
  },
  render: (args) => <Surface style={{ padding: 0, borderRadius: 0, minHeight: 440 }}><LibraryHeader {...args} /></Surface>,
};
export default meta;
type Story = StoryObj<typeof LibraryHeader>;

export const Playground: Story = {};
export const WithCaption: Story = { args: { caption: '148 items' } };
export const FilterActive: Story = { args: { filterActive: true } };
export const NewMenuOpen: Story = { args: { defaultNewOpen: true } };
export const InAFolder: Story = {
  args: { path: [{ label: 'Library', onClick: () => {} }, { label: 'Semester 1', onClick: () => {} }], current: 'Physics', onBack: () => {} },
};
export const AllFolders: Story = {
  args: { path: [{ label: 'Library', onClick: () => {} }], current: 'All folders', onBack: () => {}, onFilter: undefined },
};
export const Selecting: Story = { args: { selecting: true, selectedCount: 4, onSelectAll: () => {}, onDone: () => {} } };
export const AllSelected: Story = { args: { selecting: true, selectedCount: 12, allSelected: true, onSelectAll: () => {}, onDone: () => {} } };
export const Searching: Story = { args: { searching: true, query: 'phys' } };
export const Scrolled: Story = { args: { scrolled: true } };

export const Live: Story = {
  render: () => {
    const [searching, setSearching] = useState(false);
    const [q, setQ] = useState('');
    const [sel, setSel] = useState(false);
    return (
      <Surface style={{ padding: 0, borderRadius: 0, minHeight: 440 }}>
        <LibraryHeader
          searching={searching} query={q} onQueryChange={setQ}
          onSearchOpen={() => setSearching(true)} onSearchClose={() => { setSearching(false); setQ(''); }}
          selecting={sel} selectedCount={sel ? 3 : 0} onDone={() => setSel(false)} onSelectAll={() => {}}
          onFilter={() => setSel(true)} onSettings={() => {}} onNew={() => {}}
        />
      </Surface>
    );
  },
};
