import type { Meta, StoryObj } from '@storybook/react-vite';
import { buildTemplates, CURRENT_FOLDER, type Design } from '@/assets/studio';
import { makeDesign, type DesignChoice } from '../DesignSheet';
import { SheetDemo } from '@/stories-support/studio/SheetDemo';
import { FolderSheet } from './FolderSheet';

const meta: Meta<typeof FolderSheet> = { title: 'Blocks/FolderSheet', component: FolderSheet, parameters: { layout: 'centered' } };
export default meta;
type Story = StoryObj<typeof FolderSheet>;

const CURRENT: DesignChoice = CURRENT_FOLDER;
const item = (d: Design) => ({ name: d.name, meta: d.meta, design: d });

function Demo({ mode, tab, open = true }: { mode: 'new' | 'edit'; tab?: string; open?: boolean }) {
  const T = buildTemplates().folders;
  const items = (mode === 'edit' ? [makeDesign('folder', CURRENT), T[2], T[3]] : [T[0], T[2], T[3]]).map(item);
  return (
    <SheetDemo
      kind="folder" mode={mode} items={items} startOpen={open}
      sheet={({ open: o, onClose, created, saved, removed }) => (
        <FolderSheet
          open={o} onClose={onClose} mode={mode} placement="container" initial={mode === 'edit' ? CURRENT : undefined} initialTab={tab}
          onCreate={(c, d) => (mode === 'edit' ? saved(item({ ...d, name: c.name })) : created(item({ ...d, name: c.name })))}
          onDelete={removed} onCustomise={() => onClose()}
        />
      )}
    />
  );
}

export const NewPresets: Story = { render: () => <Demo mode="new" /> };
export const NewColour: Story = { render: () => <Demo mode="new" tab="colour" /> };
export const NewIcon: Story = { render: () => <Demo mode="new" tab="icon" /> };
export const NewFinish: Story = { render: () => <Demo mode="new" tab="finish" /> };
export const EditPresets: Story = { render: () => <Demo mode="edit" /> };
export const EditColour: Story = { render: () => <Demo mode="edit" tab="colour" /> };
export const EditIcon: Story = { render: () => <Demo mode="edit" tab="icon" /> };
export const EditFinish: Story = { render: () => <Demo mode="edit" tab="finish" /> };
export const ClosedOverLibrary: Story = { render: () => <Demo mode="new" open={false} /> };
