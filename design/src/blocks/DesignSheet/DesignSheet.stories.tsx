import type { Meta, StoryObj } from '@storybook/react-vite';
import { buildTemplates, CURRENT_COVER, type Design } from '@/assets/studio';
import { SheetDemo } from '@/stories-support/studio/SheetDemo';
import { DesignSheet, make } from './DesignSheet';

const meta: Meta<typeof DesignSheet> = { title: 'Blocks/DesignSheet', component: DesignSheet, parameters: { layout: 'centered' } };
export default meta;
type Story = StoryObj<typeof DesignSheet>;

const item = (d: Design) => ({ name: d.name, meta: d.meta, design: d });

function Demo({ kind }: { kind: 'folder' | 'cover' }) {
  const T = buildTemplates();
  const list = (kind === 'cover' ? T.covers : T.folders).slice(0, 4);
  const items = [make(kind, CURRENT_COVER), ...list.slice(1)].map(item);
  return (
    <SheetDemo
      kind={kind} mode="edit" items={items}
      sheet={({ open, onClose, saved, removed }) => (
        <DesignSheet
          kind={kind} open={open} onClose={onClose} mode="edit" placement="container" initial={CURRENT_COVER}
          onCreate={(c, d) => saved(item({ ...d, name: c.name }))} onDelete={removed} onCustomise={() => onClose()}
        />
      )}
    />
  );
}

/** The sheet behind Notebook and Folder: presets, colour, texture and icon in one place. */
export const Cover: Story = { render: () => <Demo kind="cover" /> };
export const Folder: Story = { render: () => <Demo kind="folder" /> };
