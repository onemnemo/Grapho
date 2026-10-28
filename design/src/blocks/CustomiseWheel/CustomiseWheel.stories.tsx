import type { Meta, StoryObj } from '@storybook/react-vite';
import { PanelStage } from '@/stories-support/wheel/PanelStage';
import { CustomiseWheel } from './CustomiseWheel';

const meta: Meta<typeof CustomiseWheel> = {
  title: 'Blocks/CustomiseWheel',
  component: CustomiseWheel,
  parameters: { layout: 'centered' },
  argTypes: { tab: { control: 'inline-radio', options: ['tools', 'pens'] } },
};
export default meta;
type Story = StoryObj<typeof CustomiseWheel>;

const render: Story['render'] = (a) => (
  <PanelStage height={a.maxBody && a.maxBody > 500 ? 820 : 660}>{({ model, undoable }) => <CustomiseWheel {...a} model={model} onUndoable={undoable} />}</PanelStage>
);

/** Which tools the ring shows, and in what order. Drag a row, or turn it off and it travels to the Off list. */
export const Tools: Story = { render, args: { tab: 'tools' } };
export const Pens: Story = { render, args: { tab: 'pens' } };
/** A pen row opens into its editor: name, kind, size, line, opacity, colour. */
export const PenEditorOpen: Story = { render, args: { tab: 'pens', openPen: 3, maxBody: 700 } };
/** Some tools off: they sit in their own list, and the slot ring shows how many places are in use. */
export const ToolsSomeOff: Story = {
  args: { tab: 'tools' },
  render: (a) => (
    <PanelStage height={820} options={{ tools: ['pen', 'highlighter', 'eraser', 'lasso', 'text'] }}>{({ model, undoable }) => <CustomiseWheel {...a} model={model} onUndoable={undoable} />}</PanelStage>
  ),
};
/** Share as pack turns the rows into a checklist of pens to put in the pack. */
export const PensSharing: Story = { render, args: { tab: 'pens' }, play: async ({ canvasElement }) => {
  (Array.from(canvasElement.querySelectorAll('button')).find((b) => b.textContent === 'Share as pack') as HTMLButtonElement | undefined)?.click();
} };
