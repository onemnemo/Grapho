import { useEffect } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { DEFAULT_SETS } from '@/assets/wheel/defaults';
import type { WheelModel } from '@/blocks/PenWheel/engine/model';
import { PanelStage } from '@/stories-support/wheel/PanelStage';
import { ColourPicker } from './ColourPicker';

const meta: Meta<typeof ColourPicker> = {
  title: 'Blocks/ColourPicker',
  component: ColourPicker,
  parameters: { layout: 'centered' },
};
export default meta;
type Story = StoryObj<typeof ColourPicker>;

/** The pen takes the colour as you drag. Add to palette keeps it in the chosen set. */
export const Default: Story = {
  render: (a) => (
    <PanelStage height={900}>{({ model }) => <ColourPicker {...a} model={model} />}</PanelStage>
  ),
};
/** Reached from Manage colours: edits one colour of a set, with Cancel and Save. */
export const EditingAColour: Story = {
  render: (a) => (
    <PanelStage height={820}>{({ model }) => <ColourPicker {...a} model={model} edit={{ s: 0, i: 2 }} />}</PanelStage>
  ),
};
export const HighlighterInk: Story = {
  render: (a) => (
    <PanelStage height={900} options={{ mode: 'highlighter' }}>{({ model }) => <ColourPicker {...a} model={model} />}</PanelStage>
  ),
};

const manySets = Array.from({ length: 9 }, (_, i) => {
  const base = DEFAULT_SETS[i % DEFAULT_SETS.length];
  return { id: `set${i}`, name: i < 3 ? base.name : `${base.name} ${Math.floor(i / 3) + 1}`, colours: base.colours.slice() };
});
function WithManySets({ model, children }: { model: WheelModel; children: React.ReactNode }) {
  useEffect(() => { model.setSets(manySets); }, [model]);
  return <>{children}</>;
}

/** Nine sets cost the same room as two: the set is a menu behind the chevron. */
export const ManySets: Story = {
  render: (a) => (
    <PanelStage height={900}>{({ model }) => <WithManySets model={model}><ColourPicker {...a} model={model} /></WithManySets>}</PanelStage>
  ),
};
