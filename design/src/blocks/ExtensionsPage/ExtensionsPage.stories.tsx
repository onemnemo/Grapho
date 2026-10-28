import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { ExtensionsPage, type ExtensionsPageProps } from './ExtensionsPage';
import { ExtensionRow } from './ExtensionRow';
import { sampleExtensions, type ExtensionItem } from './extensionTypes';

function Live(props: Partial<ExtensionsPageProps>) {
  const [list, setList] = useState<ExtensionItem[]>(sampleExtensions);
  return (
    <ExtensionsPage
      extensions={list} {...props}
      onToggle={(id, enabled) => setList(list.map((e) => (e.id === id ? { ...e, enabled } : e)))}
      onRemove={(id) => setList(list.filter((e) => e.id !== id))}
    />
  );
}

const meta: Meta<typeof ExtensionsPage> = {
  title: 'Blocks/ExtensionsPage',
  component: ExtensionsPage,
  parameters: { layout: 'padded' },
  decorators: [(Story) => <div style={{ width: 640, background: 'var(--overlay)', padding: 24, borderRadius: 24 }}><Story /></div>],
  render: () => <Live />,
};
export default meta;
type Story = StoryObj<typeof ExtensionsPage>;

/** Built in, From Grapho, Installed by you. The group is the source. */
export const Groups: Story = {};
/** What it can access, an off switch, and Remove for anything that is not built in. */
export const DetailBuiltIn: Story = { render: () => <Live initialOpen="audio" /> };
export const DetailInstalled: Story = { render: () => <Live initialOpen="refs" /> };
export const Row: Story = {
  render: () => (
    <div style={{ width: 560 }}>
      <ExtensionRow extension={sampleExtensions[0]} />
      <ExtensionRow extension={sampleExtensions[1]} />
    </div>
  ),
};
