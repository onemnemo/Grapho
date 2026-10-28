import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { StoryStage } from '@/stories-support/StoryStage';
import { PackBrowseSheet } from './PackBrowseSheet';

const meta: Meta<typeof PackBrowseSheet> = {
  title: 'Blocks/PackBrowseSheet',
  component: PackBrowseSheet,
  parameters: { layout: 'fullscreen' },
  args: { open: true, placement: 'container', onClose: () => {} },
  decorators: [(Story) => <StoryStage height={640}><Story /></StoryStage>],
  render: (args) => {
    const [done, setDone] = useState<string[]>(args.installed ?? []);
    return <PackBrowseSheet {...args} installed={done} onInstall={(p) => setDone([...done, p.id])} />;
  },
};
export default meta;
type Story = StoryObj<typeof PackBrowseSheet>;

export const Catalogue: Story = {};
export const SomeInstalled: Story = { args: { installed: ['c1', 'c4'] } };
