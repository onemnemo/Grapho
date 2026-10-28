import type { Meta, StoryObj } from '@storybook/react-vite';
import { AnchorDemo } from '@/stories-support/AnchorDemo';
import { InsertMenu } from './InsertMenu';

const meta: Meta<typeof InsertMenu> = {
  title: 'Blocks/InsertMenu',
  component: InsertMenu,
  parameters: { layout: 'fullscreen', docs: { story: { inline: false, height: '420px' } } },
  args: { canPasteImage: true, infinite: false, boundary: 'viewport' },
  argTypes: { open: { control: false }, onClose: { control: false }, anchorRef: { control: false } },
  render: (args) => (
    <AnchorDemo label="Insert">
      {({ anchorRef, open, setOpen }) => <InsertMenu {...args} anchorRef={anchorRef} open={open} onClose={() => setOpen(false)} />}
    </AnchorDemo>
  ),
};
export default meta;
type Story = StoryObj<typeof InsertMenu>;

export const Default: Story = {};
/** Paste image shows only when the clipboard holds a picture. */
export const NothingToPaste: Story = { args: { canPasteImage: false } };
/** An infinite canvas has no pages. */
export const InfiniteCanvas: Story = { args: { infinite: true, canPasteImage: false } };
