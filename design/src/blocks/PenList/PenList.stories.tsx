import type { Meta, StoryObj } from '@storybook/react-vite';
import { PanelStage } from '@/stories-support/wheel/PanelStage';
import { PenList } from './PenList';

const meta: Meta<typeof PenList> = {
  title: 'Blocks/PenList',
  component: PenList,
  parameters: { layout: 'centered' },
  argTypes: { edge: { control: 'inline-radio', options: ['left', 'right', 'top', 'bottom', 'free'] } },
};
export default meta;
type Story = StoryObj<typeof PenList>;

/** Tap a pen to use it, hold one to delete it (with Undo), Save current pen adds the live ink. */
export const Default: Story = {
  render: (a) => (
    <PanelStage modal={false} height={560}>
      {({ model, undoable }) => <PenList {...a} model={model} maxHeight={528} onDeleted={(pen, i) => undoable('Pen deleted', () => model.setPens(model.pens.slice(0, i).concat(pen, model.pens.slice(i))))} />}
    </PanelStage>
  ),
};
export const Highlighter: Story = {
  render: (a) => (
    <PanelStage modal={false} height={560} options={{ mode: 'highlighter' }}>
      {({ model }) => <PenList {...a} model={model} maxHeight={528} />}
    </PanelStage>
  ),
};
export const GrowsFromTheRight: Story = {
  args: { edge: 'right' },
  render: (a) => (
    <PanelStage modal={false} height={560}>
      {({ model }) => <PenList {...a} model={model} maxHeight={528} />}
    </PanelStage>
  ),
};
