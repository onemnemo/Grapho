import type { ReactNode } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Icon } from '@/assets/icons';
import { CARDS, allPackItems, clipItems, packItems, photoItems, type PanelItem } from '@/assets/studio';
import { StickerTile } from './StickerTile';
import styles from './StickerTile.stories.module.css';

const meta: Meta<typeof StickerTile> = {
  title: 'Blocks/StickerTile',
  component: StickerTile,
  parameters: { layout: 'centered' },
  argTypes: { size: { control: 'inline-radio', options: ['normal', 'wide', 'sm', 'swatch'] }, state: { control: 'inline-radio', options: ['rest', 'hover'] } },
};
export default meta;
type Story = StoryObj<typeof StickerTile>;

const tiles = (items: PanelItem[], caption = false) => items.map((it) => <StickerTile key={it.spec} art={it.art} name={it.name} size={it.size} caption={caption && it.cell} />);
const Panel = ({ cols, children }: { cols: 2 | 3 | 4 | 5; children: ReactNode }) => <div className={styles.panel}><div className={styles[`g${cols}`]}>{children}</div></div>;

export const Playground: Story = {
  args: { art: packItems('stickers', 'grapho', 'marks')[0].art, name: 'Star', size: 'normal', state: 'rest' },
  render: (a) => <div style={{ width: 96 }}><StickerTile {...a} /></div>,
};
export const Doodles: Story = { render: () => <Panel cols={3}>{tiles(packItems('stickers', 'grapho', 'marks'))}</Panel> };
export const Pixel: Story = { render: () => <Panel cols={3}>{tiles(packItems('stickers', 'grapho', 'pixel').slice(0, 9))}</Panel> };
export const Labels: Story = { render: () => <Panel cols={3}>{tiles(packItems('stickers', 'grapho', 'study'))}</Panel> };
export const Tape: Story = { render: () => <Panel cols={3}>{tiles(allPackItems('stickers', 'tape'))}</Panel> };
export const Shapes: Story = { render: () => <Panel cols={4}>{tiles(allPackItems('shapes', 'grapho'))}</Panel> };
export const Icons: Story = {
  render: () => <Panel cols={5}>{['star', 'book', 'atom', 'leaf', 'moon'].map((n) => <StickerTile key={n} name={n} size="sm" art={{ t: 'icon', name: n, colour: 'var(--fg)' }} />)}
    <StickerTile name="Import SVG" size="sm" art={{ t: 'icon', name: 'plus', colour: 'var(--fg)' }} add={<Icon name="plus" size={18} />} /></Panel>,
};
export const Photos: Story = { render: () => <Panel cols={2}>{tiles(photoItems().slice(0, 4))}</Panel> };
export const Cards: Story = { render: () => <Panel cols={2}>{CARDS.slice(0, 4).map((c) => <StickerTile key={c[0]} name={c[1]} art={{ t: 'card', def: c }} />)}</Panel> };
export const Textures: Story = { render: () => <Panel cols={2}>{tiles(packItems('textures', 'grapho', 'textures').slice(0, 4), true)}</Panel> };
export const Patterns: Story = { render: () => <Panel cols={2}>{tiles(packItems('textures', 'grapho', 'patterns').slice(0, 4), true)}</Panel> };
export const Clips: Story = {
  render: () => <Panel cols={2}>{tiles(clipItems().slice(0, 4), true)}<StickerTile name="From device" size="swatch" art={{ t: 'icon', name: 'film', colour: 'var(--fg)' }} add={<><Icon name="film" size={20} /><span>Import</span></>} /></Panel>,
};
export const Carried: Story = { render: () => <div style={{ width: 96 }}><StickerTile art={packItems('stickers', 'grapho', 'marks')[1].art} name="Heart" carried /></div> };
