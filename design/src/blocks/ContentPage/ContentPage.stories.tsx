import type { Meta, StoryObj } from '@storybook/react-vite';
import { buildSampleContent, sampleSources } from '@/assets/content';
import { ContentPage } from './ContentPage';

const items = buildSampleContent();

const meta: Meta<typeof ContentPage> = {
  title: 'Blocks/ContentPage',
  component: ContentPage,
  parameters: { layout: 'padded' },
  args: { type: 'stickers', items, sources: sampleSources, phone: false, initialView: 'grid', initialSort: 'recent', initialSource: 'all' },
  argTypes: {
    type: { control: 'select', options: ['pens', 'colours', 'covers', 'folders', 'stickers', 'templates', 'themes'] },
    initialView: { control: 'inline-radio', options: ['grid', 'list'] },
    initialSort: { control: 'inline-radio', options: ['recent', 'name', 'newest'] },
  },
  decorators: [(Story) => <div style={{ width: 720, background: 'var(--overlay)', padding: 24, borderRadius: 24, minHeight: 520 }}><Story /></div>],
  render: (args) => <ContentPage key={`${args.type}-${args.initialView}-${args.initialSource}-${args.initialSort}`} {...args} boundary="container" />,
};
export default meta;
type Story = StoryObj<typeof ContentPage>;

/** 120 stickers from built in, your own and five packs. */
export const Stickers: Story = {};
export const StickersFromAPack: Story = { args: { initialSource: 'botan' } };
export const List: Story = { args: { initialView: 'list' } };
export const Pens: Story = { args: { type: 'pens' } };
export const ColourSets: Story = { args: { type: 'colours' } };
export const Covers: Story = { args: { type: 'covers' } };
export const FolderStyles: Story = { args: { type: 'folders' } };
export const Templates: Story = { args: { type: 'templates' } };
export const Themes: Story = { args: { type: 'themes' } };
/** Select mode: Export as pack is the one solid button. */
export const Selecting: Story = { args: { initialSelected: items.filter((i) => i.type === 'stickers').slice(0, 14).map((i) => i.id) } };
export const Phone: Story = { args: { phone: true } };
