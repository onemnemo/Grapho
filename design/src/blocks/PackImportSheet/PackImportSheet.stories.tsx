import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '@/components/Button';
import { StoryStage } from '@/stories-support/StoryStage';
import { PackImportSheet } from './PackImportSheet';

const meta: Meta<typeof PackImportSheet> = {
  title: 'Blocks/PackImportSheet',
  component: PackImportSheet,
  parameters: { layout: 'fullscreen' },
  args: { open: true, placement: 'container', initialStage: 'drop', initialOpen: ['stickers'] },
  argTypes: { initialStage: { control: 'inline-radio', options: ['drop', 'review'] }, placement: { control: 'inline-radio', options: ['container', 'viewport'] } },
  decorators: [(Story) => <StoryStage height={720}><Story /></StoryStage>],
  render: (args) => {
    const [open, setOpen] = useState(args.open);
    return (
      <>
        <div style={{ position: 'absolute', bottom: 12, left: 12 }}><Button onClick={() => setOpen(true)}>Import pack</Button></div>
        {open && <PackImportSheet {...args} open onClose={() => setOpen(false)} />}
      </>
    );
  },
};
export default meta;
type Story = StoryObj<typeof PackImportSheet>;

/** Before a file is chosen. Choose file moves to the contents after a beat. */
export const ChooseFile: Story = {};
/** What is inside, grouped by type with counts. Group boxes go half filled when only some items are chosen. */
export const Contents: Story = { args: { initialStage: 'review' } };
export const AllGroupsOpen: Story = { args: { initialStage: 'review', initialOpen: ['stickers', 'covers', 'templates', 'pens', 'themes'] } };
