import type { Meta, StoryObj } from '@storybook/react-vite';
import { Surface } from '@/stories-support/Surface';
import { LibraryRow } from './LibraryRow';

const meta: Meta<typeof LibraryRow> = {
  title: 'Blocks/LibraryRow',
  component: LibraryRow,
  args: { kind: 'notebook', title: 'Linear algebra', path: 'Library / Semester 1', cover: 'journal', typeLabel: 'Notebook', size: '40 pages', when: '2 h ago', selecting: false, selected: false, state: 'rest' },
  argTypes: { kind: { control: 'inline-radio', options: ['notebook', 'pdf', 'canvas', 'folder'] }, state: { control: 'inline-radio', options: ['rest', 'hover'] } },
  render: (args) => <Surface style={{ width: 880 }}><LibraryRow {...args} /></Surface>,
};
export default meta;
type Story = StoryObj<typeof LibraryRow>;

export const Playground: Story = {};

export const Kinds: Story = {
  render: () => (
    <Surface style={{ width: 880, display: 'grid', gap: 2 }}>
      <LibraryRow kind="notebook" title="Linear algebra" path="Library / Semester 1" cover="journal" typeLabel="Notebook" size="40 pages" when="2 h ago" />
      <LibraryRow kind="pdf" doc="chart" title="Vectors, ch. 4" path="Library / Semester 1" typeLabel="PDF" size="20 pages" when="Yesterday" />
      <LibraryRow kind="canvas" title="Formula map" path="Library / Physics" typeLabel="Canvas" size="Infinite" when="Mon" />
      <LibraryRow kind="folder" colour="sky" title="Physics" path="Semester 1" typeLabel="8 items" size="3 folders" when="12 Mar" pinned />
    </Surface>
  ),
};

export const Selection: Story = {
  render: () => (
    <Surface style={{ width: 880, display: 'grid', gap: 2 }}>
      <LibraryRow kind="notebook" title="Picked" cover="dusk" typeLabel="Notebook" size="36 pages" when="Today" selecting selected />
      <LibraryRow kind="notebook" title="Not picked" cover="physics" typeLabel="Notebook" size="61 pages" when="Today" selecting />
      <LibraryRow kind="pdf" title="Hovered" typeLabel="PDF" size="4 pages" when="Today" state="hover" />
    </Surface>
  ),
};

export const Compact: Story = { args: { compact: true, path: 'Physics' } };
export const SearchMatch: Story = { args: { query: 'alg' } };
