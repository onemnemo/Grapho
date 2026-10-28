import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { buildSampleContent, sampleSources } from '@/assets/content';
import { PacksPage, type PacksPageProps } from './PacksPage';
import type { PackItem } from '../PackRow';

const items = buildSampleContent();
const initial: PackItem[] = sampleSources.filter((s) => s.kind === 'pack').map((s) => ({ id: s.id, name: s.name, art: s.art as PackItem['art'], enabled: s.enabled }));

function Live(props: Partial<PacksPageProps>) {
  const [packs, setPacks] = useState(initial);
  return (
    <PacksPage
      packs={packs} items={items} boundary="container" {...props}
      onToggle={(id, enabled) => setPacks(packs.map((p) => (p.id === id ? { ...p, enabled } : p)))}
      onPackAction={(id, a) => a === 'remove' && setPacks(packs.filter((p) => p.id !== id))}
    />
  );
}

const meta: Meta<typeof PacksPage> = {
  title: 'Blocks/PacksPage',
  component: PacksPage,
  parameters: { layout: 'padded' },
  decorators: [(Story) => <div style={{ width: 640, background: 'var(--overlay)', padding: 24, borderRadius: 24 }}><Story /></div>],
  render: () => <Live />,
};
export default meta;
type Story = StoryObj<typeof PacksPage>;

/** Packs as sources. Switch one off and it leaves the pickers; notes keep what they use. */
export const List: Story = {};
/** A pack's contents grouped by type. */
export const Detail: Story = { render: () => <Live initialOpen="botan" /> };
export const DetailOff: Story = { render: () => <Live initialOpen="mid" /> };
export const Empty: Story = { render: () => <PacksPage packs={[]} items={[]} /> };
