import type { Meta, StoryObj } from '@storybook/react-vite';
import { Icon } from '@/assets/icons';
import { allPackItems, packArt, PACK_NOUN } from '@/assets/studio';
import { PackTile } from './PackTile';

const meta: Meta<typeof PackTile> = {
  title: 'Blocks/PackTile',
  component: PackTile,
  parameters: { layout: 'centered' },
  argTypes: { variant: { control: 'inline-radio', options: ['pack', 'import'] }, state: { control: 'inline-radio', options: ['rest', 'hover'] } },
  decorators: [(Story) => <div style={{ width: 96 * 2 + 30 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof PackTile>;

const count = (tool: 'stickers' | 'shapes' | 'textures', id: string) => `${allPackItems(tool, id).length} ${PACK_NOUN[tool]}`;

export const Playground: Story = { args: { name: 'Grapho', detail: count('stickers', 'grapho'), art: packArt('stickers', 'grapho'), variant: 'pack', state: 'rest' } };
export const Hover: Story = { args: { name: 'Washi tape', detail: count('stickers', 'tape'), art: packArt('stickers', 'tape'), state: 'hover' } };
export const Shapes: Story = { args: { name: 'Grapho', detail: count('shapes', 'grapho'), art: packArt('shapes', 'grapho') } };
export const Materials: Story = { args: { name: 'Grapho', detail: count('textures', 'grapho'), art: packArt('textures', 'grapho') } };
export const Import: Story = { args: { name: 'Import', detail: '.graphopack file', variant: 'import', icon: <Icon name="plus" size={24} /> } };
