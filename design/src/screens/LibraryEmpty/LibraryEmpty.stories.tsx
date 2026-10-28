import type { Meta, StoryObj } from '@storybook/react-vite';
import { fullHeight } from '@/stories-support/storyDecorators';
import { EMPTY_COPY, LibraryEmpty, type LibraryEmptyKind } from './LibraryEmpty';
import styles from './LibraryEmpty.module.css';

const meta: Meta<typeof LibraryEmpty> = {
  title: 'Screens/LibraryEmpty',
  component: LibraryEmpty,
  parameters: { layout: 'fullscreen' },
  decorators: [fullHeight],
  args: { kind: 'library', withHeader: true },
  argTypes: { kind: { control: 'select', options: Object.keys(EMPTY_COPY) } },
};
export default meta;
type Story = StoryObj<typeof LibraryEmpty>;

export const Playground: Story = {};
export const EmptyLibrary: Story = { args: { kind: 'library' } };
export const EmptyFolder: Story = { args: { kind: 'folder' } };
export const NoFolders: Story = { args: { kind: 'folders' } };
export const NoSearchMatch: Story = { args: { kind: 'search' } };
export const NoFolderMatch: Story = { args: { kind: 'folderSearch' } };
export const NoFilterMatch: Story = { args: { kind: 'filters' } };
export const NothingDeleted: Story = { args: { kind: 'deleted', withHeader: false } };
export const NothingSelected: Story = { args: { kind: 'select', withHeader: false } };

export const All: Story = {
  decorators: [],
  parameters: { layout: 'padded' },
  render: () => (
    <div className={styles.grid} style={{ height: 'auto' }}>
      {(Object.keys(EMPTY_COPY) as LibraryEmptyKind[]).map((k) => (
        <div key={k} className={styles.cell}><LibraryEmpty kind={k} withHeader={false} /></div>
      ))}
    </div>
  ),
};
