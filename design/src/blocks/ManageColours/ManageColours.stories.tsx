import { useEffect } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { DEFAULT_SETS } from '@/assets/wheel/defaults';
import type { WheelModel } from '@/blocks/PenWheel/engine/model';
import { PanelStage } from '@/stories-support/wheel/PanelStage';
import { ManageColours } from './ManageColours';

const meta: Meta<typeof ManageColours> = {
  title: 'Blocks/ManageColours',
  component: ManageColours,
  parameters: { layout: 'centered' },
};
export default meta;
type Story = StoryObj<typeof ManageColours>;

/** Sets as one list of swatches. The name edits in place, the grip shows on hover, delete is in the menu, and a deleted set or colour comes back with Undo. */
export const Default: Story = {
  render: (a) => (
    <PanelStage height={800}>{({ model, undoable }) => <ManageColours {...a} model={model} onUndoable={undoable} />}</PanelStage>
  ),
};
export const ColourChosen: Story = {
  args: { pick: { s: 0, i: 2 } },
  render: (a) => (
    <PanelStage height={800}>{({ model, undoable }) => <ManageColours {...a} model={model} onUndoable={undoable} />}</PanelStage>
  ),
};

const manySets = Array.from({ length: 6 }, (_, i) => {
  const base = DEFAULT_SETS[i % DEFAULT_SETS.length];
  return { id: `set${i}`, name: i < 3 ? base.name : `${base.name} ${Math.floor(i / 3) + 1}`, colours: base.colours.slice() };
});
function WithManySets({ model, children }: { model: WheelModel; children: React.ReactNode }) {
  useEffect(() => { model.setSets(manySets); }, [model]);
  return <>{children}</>;
}

/** Six sets: the list scrolls, the grid stays aligned from one set to the next. */
export const ManySets: Story = {
  render: (a) => (
    <PanelStage height={800}>{({ model, undoable }) => <WithManySets model={model}><ManageColours {...a} model={model} onUndoable={undoable} /></WithManySets>}</PanelStage>
  ),
};
