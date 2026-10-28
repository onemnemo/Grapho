import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Button } from '@/components/Button';
import { WheelNoteStage as NoteStage } from '@/stories-support/wheel/WheelNoteStage';
import type { Line } from '@/blocks/PenWheel/engine/model';
import { WheelPanel, PanelModal } from './WheelPanel';
import { LineTiles } from './LineTiles';

const meta: Meta<typeof WheelPanel> = {
  title: 'Blocks/WheelPanel',
  component: WheelPanel,
  parameters: { layout: 'centered' },
  argTypes: { variant: { control: 'inline-radio', options: ['list', 'dialog', 'form'] }, state: { control: 'inline-radio', options: ['open', 'leave'] } },
};
export default meta;
type Story = StoryObj<typeof WheelPanel>;

/** The overlay surface everything that opens from the wheel shares. */
export const Surface: Story = {
  args: { width: 300, variant: 'list', children: <p style={{ margin: 12 }}>Panel content</p> },
};
export const Dialog: Story = {
  args: { width: 340, variant: 'dialog', children: <p style={{ margin: 0 }}>Centred panel, 14 padding.</p> },
};
export const Form: Story = {
  args: { width: 340, variant: 'form', children: <p style={{ margin: 0 }}>Roomier panel, 22 padding, for the picker and colours.</p> },
};
export const OnScrim: Story = {
  render: () => (
    <NoteStage width={640} height={420}>
      <PanelModal>
        <WheelPanel width={300} variant="dialog">
          <p style={{ margin: 0 }}>A scrim over the note, the panel centred.</p>
          <Button variant="primary" block>Done</Button>
        </WheelPanel>
      </PanelModal>
    </NoteStage>
  ),
};

export const LineStyle: StoryObj<typeof LineTiles> = {
  render: function Render() {
    const [line, setLine] = useState<Line>('solid');
    return <div style={{ width: 280 }}><LineTiles value={line} onChange={setLine} /></div>;
  },
};
