import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { packItems } from '@/assets/studio';
import { StickerTile } from '../StickerTile';
import { PackCategory } from './PackCategory';
import styles from './PackCategory.stories.module.css';

const meta: Meta<typeof PackCategory> = { title: 'Blocks/PackCategory', component: PackCategory, parameters: { layout: 'centered' } };
export default meta;
type Story = StoryObj<typeof PackCategory>;

const marks = packItems('stickers', 'grapho', 'marks');
const nature = packItems('stickers', 'grapho', 'nature');
const grid = (items: typeof marks) => (
  <div className={styles.grid}>{items.map((it) => <StickerTile key={it.spec} art={it.art} name={it.name} size={it.size} />)}</div>
);

export const Closed: Story = {
  render: () => <div className={styles.panel}><PackCategory title="Marks" count={marks.length} open={false} onToggle={() => undefined} previews={marks.map((m) => m.art)} /></div>,
};
export const Open: Story = {
  render: () => <div className={styles.panel}><PackCategory title="Marks" count={marks.length} open onToggle={() => undefined}>{grid(marks)}</PackCategory></div>,
};
export const Group: Story = {
  render: () => {
    const [open, setOpen] = useState<string[]>(['marks']);
    const flip = (k: string) => setOpen((o) => (o.includes(k) ? o.filter((x) => x !== k) : [...o, k]));
    return (
      <div className={`${styles.panel} ${styles.stack}`}>
        <PackCategory title="Marks" count={marks.length} open={open.includes('marks')} onToggle={() => flip('marks')} previews={marks.map((m) => m.art)}>{grid(marks)}</PackCategory>
        <PackCategory title="Nature" count={nature.length} open={open.includes('nature')} onToggle={() => flip('nature')} previews={nature.map((m) => m.art)}>{grid(nature)}</PackCategory>
      </div>
    );
  },
};

/** Without boxes, as in Settings: the pack's contents sit straight on the panel. */
export const Plain: Story = {
  render: () => (
    <div className={`${styles.panel} ${styles.stack}`}>
      <PackCategory variant="plain" icon="sticker" title="Stickers" count={marks.length} open onToggle={() => undefined}>{grid(marks)}</PackCategory>
      <PackCategory variant="plain" icon="sticker" title="Nature" count={nature.length} open={false} onToggle={() => undefined} previews={nature.map((m) => m.art)} />
    </div>
  ),
};
