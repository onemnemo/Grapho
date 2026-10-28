import type { Meta, StoryObj } from '@storybook/react-vite';
import { fullHeight } from '@/stories-support/storyDecorators';
import { manyFolders } from '../Library/libraryData';
import { AllFolders } from './AllFolders';

const meta: Meta<typeof AllFolders> = {
  title: 'Screens/AllFolders',
  component: AllFolders,
  parameters: { layout: 'fullscreen' },
  decorators: [fullHeight],
  args: { items: [], folders: manyFolders() },
};
export default meta;
type Story = StoryObj<typeof AllFolders>;

export const Default: Story = {};
export const ListView: Story = { args: { view: 'list' } };
export const Searching: Story = { args: { searching: true, query: 'ph' } };
export const NothingFound: Story = { args: { searching: true, query: 'zzz' } };
export const Selecting: Story = { args: { selecting: true, selected: ['f:mf3', 'f:mf9'] } };
export const FewFolders: Story = { args: { folders: manyFolders().slice(0, 6) } };
export const NoFolders: Story = { args: { folders: [] } };
