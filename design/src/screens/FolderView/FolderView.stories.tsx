import type { Meta, StoryObj } from '@storybook/react-vite';
import { fullHeight } from '@/stories-support/storyDecorators';
import { homeFolders, homeItems } from '../Library/libraryData';
import { FolderView } from './FolderView';

const meta: Meta<typeof FolderView> = {
  title: 'Screens/FolderView',
  component: FolderView,
  parameters: { layout: 'fullscreen' },
  decorators: [fullHeight],
  args: { items: homeItems(), folders: homeFolders(), path: ['sem1'] },
};
export default meta;
type Story = StoryObj<typeof FolderView>;

export const Default: Story = {};
export const Nested: Story = { args: { path: ['sem1', 'phys'], view: 'list', group: 'date', sortKey: 'edited' } };
export const Selecting: Story = { args: { path: ['sem1', 'phys'], selecting: true, selected: ['opt', 'mech'] } };
export const EmptyFolder: Story = { args: { path: ['sem1', 'maths'] } };
