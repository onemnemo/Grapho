import type { Meta, StoryObj } from '@storybook/react-vite';
import { CV_CURRENT, CV_LIB, CV_NEW, type CanvasSettings } from '@/assets/studio';
import { SheetDemo } from '@/stories-support/studio/SheetDemo';
import { CanvasSheet, type CanvasSheetProps } from './CanvasSheet';

const meta: Meta<typeof CanvasSheet> = { title: 'Blocks/CanvasSheet', component: CanvasSheet, parameters: { layout: 'centered' } };
export default meta;
type Story = StoryObj<typeof CanvasSheet>;

const item = (c: CanvasSettings) => ({ name: c.name, meta: c.meta ?? '', canvas: c });

function Demo({ mode, tab, open = true, initial }: { mode: 'new' | 'edit'; tab?: CanvasSheetProps['initialTab']; open?: boolean; initial?: CanvasSettings }) {
  const start = initial ?? (mode === 'edit' ? CV_CURRENT : undefined);
  const items = (mode === 'edit' ? [start ?? CV_CURRENT, CV_LIB[1], CV_LIB[2]] : CV_LIB).map(item);
  return (
    <SheetDemo
      kind="canvas" mode={mode} items={items} startOpen={open}
      sheet={({ open: o, onClose, created, saved, removed }) => (
        <CanvasSheet
          open={o} onClose={onClose} mode={mode} placement="container" initial={start} initialTab={tab}
          onCreate={(c) => (mode === 'edit' ? saved(item(c)) : created(item(c)))} onDelete={removed}
        />
      )}
    />
  );
}

export const NewPresets: Story = { render: () => <Demo mode="new" /> };
export const NewBackground: Story = { render: () => <Demo mode="new" tab="colour" /> };
export const NewPattern: Story = { render: () => <Demo mode="new" tab="pattern" /> };
export const NewSize: Story = { render: () => <Demo mode="new" tab="size" /> };
export const EditPresets: Story = { render: () => <Demo mode="edit" /> };
export const EditSize: Story = { render: () => <Demo mode="edit" tab="size" /> };
export const EditFixed: Story = { render: () => <Demo mode="edit" tab="size" initial={{ ...CV_NEW, ...CV_LIB[1], where: 'Library' }} /> };
export const BlankPattern: Story = { render: () => <Demo mode="new" tab="pattern" initial={{ ...CV_NEW, tpl: 'blank' }} /> };
export const ClosedOverLibrary: Story = { render: () => <Demo mode="new" open={false} /> };
