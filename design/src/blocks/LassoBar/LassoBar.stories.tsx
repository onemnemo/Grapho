import type { Meta, StoryObj } from '@storybook/react-vite';
import { NoteStage } from '@/stories-support/NoteStage';
import { ILLUS } from '@/assets/studio';
import { InkLayer, SampleInk, SelectionOutline } from '../NotePage';
import { LassoBar, lassoActions, lassoImageActions } from './LassoBar';
import { PasteChip } from './PasteChip';

const meta: Meta<typeof LassoBar> = {
  title: 'Blocks/LassoBar',
  component: LassoBar,
  parameters: { layout: 'padded' },
  args: { items: lassoActions, x: 234, y: 112 },
  render: (args) => (
    <NoteStage
      height={420}
      page={
        <>
          <SampleInk />
          <InkLayer><SelectionOutline x={48} y={150} width={372} height={192} /></InkLayer>
        </>
      }
    >
      <LassoBar {...args} />
    </NoteStage>
  ),
};
export default meta;
type Story = StoryObj<typeof LassoBar>;

/** Ink, shapes and text. Colour, Cut, Copy, Duplicate and More weigh the same; Delete is red and set apart by space, not by a rule. */
export const Default: Story = {};

/** One picture. The three picture actions carry words because an icon alone would not say what they do. */
export const PictureSelected: Story = {
  args: { items: lassoImageActions, x: 400, y: 70 },
  render: (args) => (
    <NoteStage height={420} page={<><img src={ILLUS} alt="" style={{ position: 'absolute', left: 250, top: 130, width: 300, height: 243 }} /><InkLayer><SelectionOutline x={250} y={130} width={300} height={243} fill={false} /></InkLayer></>}>
      <LassoBar {...args} />
    </NoteStage>
  ),
};

export const ShapeSelected: Story = {
  args: {
    items: [
      { id: 'options', icon: 'shapes', label: 'Shape options', active: true },
      { id: 'colour', icon: 'palette', label: 'Colour' },
      { id: 'duplicate', icon: 'duplicate', label: 'Duplicate' },
      'separator',
      { id: 'delete', icon: 'trash', label: 'Delete', danger: true },
    ],
  },
};
export const LineSelected: Story = {
  args: {
    items: [
      { id: 'options', icon: 'shapes', label: 'Line options' },
      { id: 'swap', icon: 'swap', label: 'Swap ends' },
      { id: 'colour', icon: 'palette', label: 'Colour' },
      { id: 'duplicate', icon: 'duplicate', label: 'Duplicate' },
      'separator',
      { id: 'delete', icon: 'trash', label: 'Delete', danger: true },
    ],
  },
};

/** Tap empty page with something copied and one chip offers to paste there. */
export const Paste: Story = {
  render: () => (
    <NoteStage height={300}>
      <PasteChip x={400} y={170} />
    </NoteStage>
  ),
};
