import type { Meta, StoryObj } from '@storybook/react-vite';
import { fullHeight } from '@/stories-support/storyDecorators';
import { Library } from './Library';
import { bigFolders, bigItems, homeDeleted, homeFolders, homeItems } from './libraryData';

const meta: Meta<typeof Library> = {
  title: 'Screens/Library',
  component: Library,
  parameters: { layout: 'fullscreen' },
  decorators: [fullHeight],
  args: { items: homeItems(), folders: homeFolders(), deleted: homeDeleted() },
};
export default meta;
type Story = StoryObj<typeof Library>;

export const Home: Story = {};

export const Selecting: Story = { args: { selecting: true, selected: ['f:sem1', 'lec6', 'bio', 'spa'] } };

export const Searching: Story = { args: { searching: true, query: 'phys' } };
export const SearchBeforeTyping: Story = { args: { searching: true } };

export const ListView: Story = { args: { view: 'list', group: 'date' } };

export const FiltersApplied: Story = {
  args: { items: bigItems(), folders: bigFolders(), scopeAll: true, pinnedRow: true, filters: { types: ['notebook'], edited: 'any', starred: false, tags: ['exam', 'lab'] } },
};

export const WithNotice: Story = {
  args: { banner: { title: 'You are offline', text: 'Changes are saved on this tablet and sync when you are back.', action: 'Try again' } },
};

export const BigLibrary: Story = {
  args: { items: bigItems(), folders: bigFolders(), scopeAll: true, pinnedRow: true, group: 'letter', sortKey: 'name', sortDir: 'asc', size: 12 },
};

export const Empty: Story = { args: { items: [], folders: [] } };

/** Recently deleted, from the end of All folders. Everything deleted stays 30 days and comes back with Restore. */
export const RecentlyDeleted: Story = { args: { startOnDeleted: true } };
export const RecentlyDeletedEmpty: Story = { args: { startOnDeleted: true, deleted: [] } };
/** All folders ends with the way into Recently deleted. */
export const AllFoldersList: Story = { args: { startOnFolders: true, view: 'list' } };
