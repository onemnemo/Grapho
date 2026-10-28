import type { Meta, StoryObj } from '@storybook/react-vite';
import { COVER_PRESETS } from '@/assets/covers';
import { Cells, Surface } from '@/stories-support/Surface';
import { CoverTile } from './CoverTile';

const meta: Meta<typeof CoverTile> = {
  title: 'Blocks/CoverTile',
  component: CoverTile,
  args: { kind: 'notebook', cover: 'calculus', doc: 'chart', title: 'Calculus', meta: 'Notebook · 32 pages', starred: false, selecting: false, selected: false, state: 'rest' },
  argTypes: {
    kind: { control: 'inline-radio', options: ['notebook', 'pdf', 'canvas'] },
    cover: { control: 'select', options: COVER_PRESETS },
    doc: { control: 'inline-radio', options: ['plain', 'chart', 'highlight'] },
    state: { control: 'inline-radio', options: ['rest', 'hover', 'press', 'hold'] },
  },
  render: (args) => <Surface><div style={{ width: 174 }}><CoverTile {...args} /></div></Surface>,
};
export default meta;
type Story = StoryObj<typeof CoverTile>;

export const Playground: Story = {};

export const Kinds: Story = {
  render: () => (
    <Cells width={174} items={[
      { caption: 'notebook', node: <CoverTile cover="biology" title="Biology" meta="Notebook · 24 pages" /> },
      { caption: 'pdf with chart', node: <CoverTile kind="pdf" doc="chart" title="Lecture 6 slides" meta="PDF · 18 pages" /> },
      { caption: 'pdf with highlight', node: <CoverTile kind="pdf" doc="highlight" title="Worksheet 3" meta="PDF · 4 pages" /> },
      { caption: 'canvas', node: <CoverTile kind="canvas" title="Mood board" meta="Canvas" /> },
    ]} />
  ),
};

export const Starred: Story = { args: { starred: true } };

const sel = (props: Partial<React.ComponentProps<typeof CoverTile>>) => <CoverTile cover="calculus" title="Calculus" meta="Notebook · 32 pages" {...props} />;

export const States: Story = {
  render: () => (
    <Cells width={174} items={[
      { caption: 'rest', node: sel({}) },
      { caption: 'hover', node: sel({ state: 'hover' }) },
      { caption: 'press', node: sel({ state: 'press' }) },
      { caption: 'long press', node: sel({ state: 'hold' }) },
      { caption: 'choosing, not picked', node: sel({ selecting: true }) },
      { caption: 'picked', node: sel({ selecting: true, selected: true }) },
      { caption: 'being dragged', node: sel({ lifted: true }) },
    ]} />
  ),
};

export const Dimmed: Story = {
  render: () => (
    <Cells width={174} items={[
      { caption: 'picked', node: <CoverTile cover="biology" title="Biology" meta="Notebook" selecting selected /> },
      { caption: 'dimmed', node: <CoverTile cover="physics" title="Physics" meta="Notebook" selecting /> },
      { caption: 'dimmed pdf', node: <CoverTile kind="pdf" doc="chart" title="Lecture 6" meta="PDF" selecting /> },
    ]} />
  ),
};

export const Warning: Story = { args: { warning: '3 days left', title: 'Garden plan v1', cover: 'moss' } };
export const SearchMatch: Story = { args: { query: 'calc' } };

/** The ••• menu, the same one folders have: Open, Edit, Star, Move to, Duplicate, Select and Delete. */
export const Menu: Story = {
  args: { state: 'hover', starred: true, onMenuAction: () => {}, moveTargets: [{ id: 'sem1', name: 'Semester 1', colour: 'indigo' }, { id: 'personal', name: 'Personal', colour: 'rose' }], onMove: () => {} },
  render: (args) => <Surface><div style={{ width: 174, height: 420 }}><CoverTile {...args} /></div></Surface>,
  play: async ({ canvasElement }) => { canvasElement.querySelector<HTMLButtonElement>('[aria-haspopup="menu"]')?.click(); },
};
