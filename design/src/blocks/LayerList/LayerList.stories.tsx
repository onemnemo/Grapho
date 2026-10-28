import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { buildTemplates, deep, type Design } from '@/assets/studio';
import { LayerList } from './LayerList';
import styles from './LayerList.stories.module.css';

const meta: Meta<typeof LayerList> = { title: 'Blocks/LayerList', component: LayerList, parameters: { layout: 'centered' } };
export default meta;
type Story = StoryObj<typeof LayerList>;

function Live({ make, selected = null }: { make: (t: ReturnType<typeof buildTemplates>) => Design; selected?: number | null }) {
  const [d, setD] = useState<Design>(() => deep(make(buildTemplates())));
  const [sel, setSel] = useState<string | null>(selected == null ? null : d.layers[selected]?.id ?? null);
  const flip = (id: string, k: 'lock' | 'hide') => setD((x) => ({ ...x, layers: x.layers.map((l) => (l.id === id ? { ...l, [k]: !l[k] } : l)) }));
  const reorder = (ids: string[]) => setD((x) => {
    const by = new Map(x.layers.map((l) => [l.id, l]));
    const slots = x.layers.map((l, i) => (ids.includes(l.id) ? i : -1)).filter((i) => i >= 0);
    const next = x.layers.slice();
    ids.slice().reverse().forEach((id, k) => { next[slots[k]] = by.get(id)!; });
    return { ...x, layers: next };
  });
  return <div className={styles.frame}><LayerList design={d} selectedId={sel} onSelect={setSel} onToggleLock={(id) => flip(id, 'lock')} onToggleHide={(id) => flip(id, 'hide')} onReorder={reorder} /></div>;
}

export const Cover: Story = { render: () => <Live make={(t) => t.covers[2]} selected={1} /> };
export const CoverSettingsSelected: Story = { render: () => <Live make={(t) => t.covers[2]} /> };
export const Folder: Story = { render: () => <Live make={(t) => t.folders[0]} selected={0} /> };
export const FolderWithLayers: Story = { render: () => <Live make={(t) => t.folders[3]} selected={1} /> };
export const Locked: Story = {
  render: () => <Live make={(t) => { const d = deep(t.covers[1]); d.layers.forEach((l, i) => { l.lock = i === 0; l.hide = i === 1; }); return d; }} />,
};
