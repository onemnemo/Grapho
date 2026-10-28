import { useRef, useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Surface } from '@/stories-support/Surface';
import { SortMenu, SORT_LABELS, type GroupBy, type SortDir, type SortKey } from './SortMenu';

const meta: Meta<typeof SortMenu> = {
  title: 'Blocks/SortMenu',
  component: SortMenu,
  parameters: { layout: 'padded' },
};
export default meta;
type Story = StoryObj<typeof SortMenu>;

function Demo({ scope, startKey }: { scope: 'items' | 'folders'; startKey: SortKey }) {
  const [key, setKey] = useState<SortKey>(startKey);
  const [dir, setDir] = useState<SortDir>('desc');
  const [group, setGroup] = useState<GroupBy>('none');
  const [size, setSize] = useState(50);
  const [open, setOpen] = useState(true);
  const ref = useRef<HTMLButtonElement>(null);
  return (
    <Surface style={{ minHeight: 520, display: 'flex', justifyContent: 'flex-end', alignItems: 'flex-start' }}>
      <button ref={ref} type="button" onClick={() => setOpen((v) => !v)} style={{ font: 'var(--t-label-sm)', background: 'var(--s2)', borderRadius: 999, height: 36, padding: '0 14px' }}>
        <span style={{ color: 'var(--mute)' }}>Sort </span>{SORT_LABELS[key]}
      </button>
      <SortMenu open={open} onClose={() => setOpen(false)} anchorRef={ref} scope={scope} sortKey={key} sortDir={dir} group={group} size={size}
        onSortKey={setKey} onSortDir={setDir} onGroup={setGroup} onSize={setSize} />
    </Surface>
  );
}

export const Items: Story = { render: () => <Demo scope="items" startKey="opened" /> };
export const SizeKey: Story = { render: () => <Demo scope="items" startKey="size" /> };
export const Folders: Story = { render: () => <Demo scope="folders" startKey="name" /> };
