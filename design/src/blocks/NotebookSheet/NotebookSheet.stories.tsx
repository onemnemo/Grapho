import type { Meta, StoryObj } from '@storybook/react-vite';
import { buildTemplates, CURRENT_COVER, type Design } from '@/assets/studio';
import { makeDesign, type DesignChoice } from '../DesignSheet';
import { SheetDemo } from '@/stories-support/studio/SheetDemo';
import { NotebookSheet } from './NotebookSheet';

const meta: Meta<typeof NotebookSheet> = { title: 'Blocks/NotebookSheet', component: NotebookSheet, parameters: { layout: 'centered' } };
export default meta;
type Story = StoryObj<typeof NotebookSheet>;

const CURRENT: DesignChoice = CURRENT_COVER;
const item = (d: Design) => ({ name: d.name, meta: d.meta, design: d });

function Demo({ mode, tab, open = true }: { mode: 'new' | 'edit'; tab?: string; open?: boolean }) {
  const T = buildTemplates().covers;
  const items = (mode === 'edit' ? [makeDesign('cover', CURRENT), T[1], T[2], T[4]] : [T[0], T[1], T[2], T[4]]).map(item);
  return (
    <SheetDemo
      kind="cover" mode={mode} items={items} startOpen={open}
      sheet={({ open: o, onClose, created, saved, removed }) => (
        <NotebookSheet
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
export const NewTexture: Story = { render: () => <Demo mode="new" tab="finish" /> };
export const NewIcon: Story = { render: () => <Demo mode="new" tab="icon" /> };
export const NewPaper: Story = { render: () => <Demo mode="new" tab="paper" /> };
export const EditPresets: Story = { render: () => <Demo mode="edit" /> };
export const EditColour: Story = { render: () => <Demo mode="edit" tab="colour" /> };
export const EditTexture: Story = { render: () => <Demo mode="edit" tab="finish" /> };
export const EditIcon: Story = { render: () => <Demo mode="edit" tab="icon" /> };
export const ClosedOverLibrary: Story = { render: () => <Demo mode="new" open={false} /> };
