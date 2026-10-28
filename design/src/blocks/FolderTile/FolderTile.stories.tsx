import type { Meta, StoryObj } from '@storybook/react-vite';
import { FOLDER_COLOURS } from '@/assets/covers';
import { iconPaths, type IconName } from '@/assets/icons';
import { Cells, Surface } from '@/stories-support/Surface';
import { FolderTile } from './FolderTile';

const meta: Meta<typeof FolderTile> = {
  title: 'Blocks/FolderTile',
  component: FolderTile,
  args: { name: 'Semester 1', meta: '12 items · 3 folders', colour: 'indigo', papers: ['calculus', 'biology', 'plain-paper'], pinned: false, selecting: false, selected: false, dropTarget: false, state: 'rest', width: 250 },
  argTypes: {
    colour: { control: 'select', options: FOLDER_COLOURS },
    icon: { control: 'select', options: [undefined, ...(Object.keys(iconPaths) as IconName[])] },
    state: { control: 'inline-radio', options: ['rest', 'hover'] },
    width: { control: { type: 'range', min: 120, max: 320 } },
  },
  render: (args) => <Surface style={{ paddingTop: 56 }}><FolderTile {...args} /></Surface>,
};
export default meta;
type Story = StoryObj<typeof FolderTile>;

export const Playground: Story = {};

const f = (props: Partial<React.ComponentProps<typeof FolderTile>>) => <FolderTile name="Art class" meta="6 items" colour="ember" papers={['sketches', 'history', 'plain-paper']} width={220} {...props} />;

export const States: Story = {
  render: () => (
    <Cells items={[
      { caption: 'rest', node: f({}) },
      { caption: 'hover, with the ••• button', node: f({ state: 'hover' }) },
      { caption: 'pinned', node: f({ pinned: true }) },
      { caption: 'choosing, not picked', node: f({ selecting: true }) },
      { caption: 'picked: shrunk with a white ring', node: f({ selecting: true, selected: true }) },
      { caption: 'drop target', node: f({ dropTarget: true }) },
      { caption: 'being dragged', node: f({ lifted: true }) },
    ]} />
  ),
};

export const Colours: Story = {
  render: () => (
    <Cells items={FOLDER_COLOURS.map((c) => ({ caption: c, node: f({ colour: c, name: c[0].toUpperCase() + c.slice(1), width: 200 }) }))} />
  ),
};

export const WithIcon: Story = { args: { icon: 'flask', name: 'Labs', colour: 'sea' } };
export const Small: Story = { args: { width: 150, smallMenuButton: true, meta: undefined, pinned: true } };
export const SearchMatch: Story = { args: { query: 'sem' } };
