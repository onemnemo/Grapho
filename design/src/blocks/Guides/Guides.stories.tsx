import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { NoteStage } from '@/stories-support/NoteStage';
import { Protractor } from './Protractor';
import { Ruler } from './Ruler';

const meta: Meta<typeof Ruler> = {
  title: 'Blocks/Guides',
  component: Ruler,
  parameters: { layout: 'padded' },
};
export default meta;
type Story = StoryObj<typeof Ruler>;

/** The ruler is a pale band with a measured edge; its handle turns it. */
export const RulerOnPage: Story = {
  render: () => {
    const [on, setOn] = useState(true);
    return <NoteStage height={320}>{on && <Ruler x={400} y={200} length={14} angle={-8} onClose={() => setOn(false)} />}</NoteStage>;
  },
};
/** While it is being turned, its angle shows above it. */
export const RulerTurning: Story = { render: () => <NoteStage height={320}><Ruler x={400} y={200} angle={24} turning /></NoteStage> };
/** A tick every 5 degrees, a number every 30. A reading draws a line from the centre and the angle at its end. */
export const ProtractorReading: Story = { render: () => <NoteStage height={320}><Protractor x={400} y={270} reading={62} /></NoteStage> };
export const ProtractorPlain: Story = { render: () => <NoteStage height={320}><Protractor x={400} y={270} onClose={() => {}} /></NoteStage> };
export const Both: Story = {
  render: () => (
    <NoteStage height={460}>
      <Ruler x={250} y={330} length={10} angle={-6} />
      <Protractor x={630} y={430} reading={62} />
    </NoteStage>
  ),
};
