import type { Meta, StoryObj } from '@storybook/react-vite';
import { ExportDialog } from './ExportDialog';

const meta: Meta<typeof ExportDialog> = {
  title: 'Blocks/ExportDialog',
  component: ExportDialog,
  parameters: { layout: 'fullscreen' },
  args: { open: true, pageCount: 12, infinite: false, exporting: false },
  argTypes: { open: { control: false }, onClose: { control: false } },
};
export default meta;
type Story = StoryObj<typeof ExportDialog>;

/** Format is three tiles, pages are a segment, and the backgrounds switch is the last row. Export is the only solid button. */
export const Default: Story = {};
export const Images: Story = { args: { initial: { format: 'images', pages: 'current' } } };
/** Range takes page numbers like 1-3, 5. A bad one says what is allowed and holds Export. */
export const RangeError: Story = { args: { initial: { pages: 'range', range: '3-20' } } };
export const RangeOk: Story = { args: { initial: { pages: 'range', range: '1-3, 5' } } };
/** A notebook keeps everything, so pages and backgrounds are not asked. */
export const Notebook: Story = { args: { initial: { format: 'notebook' } } };
/** An infinite canvas exports as one page. */
export const InfiniteCanvas: Story = { args: { infinite: true } };
/** Running: the choices freeze and the button says Exporting. */
export const Exporting: Story = { args: { exporting: true } };
