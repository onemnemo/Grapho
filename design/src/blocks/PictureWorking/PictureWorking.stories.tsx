import type { Meta, StoryObj } from '@storybook/react-vite';
import { ILLUS } from '@/assets/studio';
import { NoteStage } from '@/stories-support/NoteStage';
import { InkLayer } from '../NotePage';
import { PictureWorking } from './PictureWorking';

const BOX = { x: 250, y: 60, width: 300, height: 243 };

const meta: Meta<typeof PictureWorking> = {
  title: 'Blocks/PictureWorking',
  component: PictureWorking,
  parameters: { layout: 'padded' },
  args: { job: 'removing', box: BOX },
  argTypes: { job: { control: 'inline-radio', options: ['removing', 'converting'] } },
  render: (args) => (
    <NoteStage height={400} page={<img src={ILLUS} alt="" style={{ position: 'absolute', left: BOX.x, top: BOX.y, width: BOX.width, height: BOX.height }} />}>
      <PictureWorking {...args} onCancel={() => {}} />
    </NoteStage>
  ),
};
export default meta;
type Story = StoryObj<typeof PictureWorking>;

/** Removing background: a line sweeps down the picture. The pill carries the job's own icon. */
export const RemovingBackground: Story = {};
/** Converting to ink: the line sweeps across. */
export const ConvertingToInk: Story = { args: { job: 'converting' } };
/** Without Cancel the pill is only a status. */
export const NoCancel: Story = {
  render: (args) => (
    <NoteStage height={400} page={<InkLayer><text x="64" y="120" fontSize="40">Morning routine</text></InkLayer>}>
      <PictureWorking {...args} />
    </NoteStage>
  ),
};
