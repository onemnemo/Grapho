import type { Meta, StoryObj } from '@storybook/react-vite';
import { StudioPanel } from './StudioPanel';
import styles from './StudioPanel.stories.module.css';

const meta: Meta<typeof StudioPanel> = {
  title: 'Blocks/StudioPanel',
  component: StudioPanel,
  parameters: { layout: 'centered' },
  args: { tool: 'stickers', kind: 'cover' },
  argTypes: { tool: { control: 'select', options: ['stickers', 'images', 'text', 'shapes', 'cards', 'icons', 'textures', 'video'] }, kind: { control: 'inline-radio', options: ['cover', 'folder'] } },
  decorators: [(Story) => <div className={styles.frame}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof StudioPanel>;

export const StickerPacks: Story = {};
export const StickerPackOpen: Story = { args: { initialPack: 'grapho' } };
export const StickerSearch: Story = { args: { initialPack: 'grapho', initialQuery: 'st' } };
export const StickerSearchAllPacks: Story = { args: { initialQuery: 'leaf' } };
export const NothingFound: Story = { args: { initialQuery: 'zzz' } };
export const TapePack: Story = { args: { initialPack: 'tape' } };
export const Images: Story = { args: { tool: 'images' } };
export const Text: Story = { args: { tool: 'text' } };
export const TextForFolder: Story = { args: { tool: 'text', kind: 'folder' } };
export const Shapes: Story = { args: { tool: 'shapes' } };
export const ShapesPackOpen: Story = { args: { tool: 'shapes', initialPack: 'grapho' } };
export const Cards: Story = { args: { tool: 'cards' } };
export const Icons: Story = { args: { tool: 'icons' } };
export const IconsSoft: Story = { args: { tool: 'icons', iconStyle: 'soft' } };
export const Textures: Story = { args: { tool: 'textures' } };
export const TexturesPackOpen: Story = { args: { tool: 'textures', initialPack: 'grapho' } };
export const Video: Story = { args: { tool: 'video' } };
