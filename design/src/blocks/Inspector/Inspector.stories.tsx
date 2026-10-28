import { useEffect, useRef, useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { buildTemplates, newLayer, STRAP_COLOURS, type Design } from '@/assets/studio';
import { applyProp, useStudioEditor } from '@/lib/studio';
import { Inspector } from './Inspector';
import styles from './Inspector.stories.module.css';

const meta: Meta<typeof Inspector> = { title: 'Blocks/Inspector', component: Inspector, parameters: { layout: 'centered' } };
export default meta;
type Story = StoryObj<typeof Inspector>;

/** A live inspector on a real design, with one layer added from the panel spec. */
function Demo({ kind = 'cover', spec, strap }: { kind?: 'cover' | 'folder'; spec?: string; strap?: string }) {
  const [start] = useState<Design>(() => {
    const t = buildTemplates();
    const d = kind === 'cover' ? t.covers[2] : t.folders[0];
    return strap ? { ...d, cover: { ...d.cover, strap } } : d;
  });
  const ed = useStudioEditor(start);
  const [safe, setSafe] = useState(true);
  const added = useRef(false);
  useEffect(() => {
    if (spec && !added.current) {
      added.current = true;
      ed.addLayer(newLayer(spec, ed.design));
    }
  }, [spec, ed]);
  return (
    <div className={styles.frame}>
      <Inspector
        design={ed.design}
        layer={ed.selected}
        safe={safe}
        onSafe={setSafe}
        onEdit={(path, value, final) => { ed.live((d) => applyProp(d, ed.selected, path, value)); if (final) ed.commit(); }}
        onAction={(a) => {
          const id = ed.selectedId;
          if (!id) return;
          if (a === 'dup') ed.duplicate(id);
          else if (a === 'del') ed.deleteLayer(id);
          else if (a === 'fwd') ed.move(id, 1);
          else if (a === 'bwd') ed.move(id, -1);
          else if (a === 'lock') ed.patchLayer(id, { lock: !ed.selected?.lock }, true);
        }}
      />
    </div>
  );
}

export const CoverSettings: Story = { render: () => <Demo /> };
/** The strap takes any colour: a preset, or the colour ring for your own. */
export const CustomStrap: Story = { render: () => <Demo strap={STRAP_COLOURS[5]} /> };
export const FolderSettings: Story = { render: () => <Demo kind="folder" /> };
export const Text: Story = { render: () => <Demo spec="tx:serif" /> };
export const Card: Story = { render: () => <Demo spec="cd:sticky" /> };
export const Shape: Story = { render: () => <Demo spec="sh:rounded" /> };
export const LineShape: Story = { render: () => <Demo spec="sh:squiggle" /> };
export const Icon: Story = { render: () => <Demo spec="ic:book" /> };
export const Sticker: Story = { render: () => <Demo spec="st:doodle:star" /> };
export const Image: Story = { render: () => <Demo spec="im:hills" /> };
export const Texture: Story = { render: () => <Demo spec="tex:leather" /> };
export const Pattern: Story = { render: () => <Demo spec="pt:dots" /> };
export const PaintedFill: Story = { render: () => <Demo spec="pn:ember" /> };
export const Video: Story = { render: () => <Demo spec="vd:aurora" /> };
export const FolderLayer: Story = { render: () => <Demo kind="folder" spec="st:doodle:heart" /> };
