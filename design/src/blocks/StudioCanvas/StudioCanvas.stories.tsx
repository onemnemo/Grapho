import { useEffect, useRef, useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { buildTemplates, newLayer, type Design } from '@/assets/studio';
import { useStudioEditor } from '@/lib/studio';
import { StudioCanvas } from './StudioCanvas';

const meta: Meta<typeof StudioCanvas> = { title: 'Blocks/StudioCanvas', component: StudioCanvas, parameters: { layout: 'centered' } };
export default meta;
type Story = StoryObj<typeof StudioCanvas>;

interface DemoProps {
  kind?: 'cover' | 'folder';
  /** Panel specs added on load; the last one is selected. */
  specs?: string[];
  lock?: boolean;
  nameAt?: 'left' | 'centre' | 'below' | 'hidden';
  playing?: boolean;
}

/** A real canvas: drag, scale, rotate, double click text, and watch the guides. */
function Demo({ kind = 'cover', specs = [], lock = false, nameAt, playing = false }: DemoProps) {
  const [start] = useState<Design>(() => { const t = buildTemplates(); const d = kind === 'cover' ? t.covers[2] : t.folders[0]; if (nameAt && d.folder) d.folder.name = nameAt; return d; });
  const ed = useStudioEditor(start);
  const [editing, setEditing] = useState<string | null>(null);
  const [play, setPlay] = useState(playing);
  const done = useRef(false);
  useEffect(() => {
    if (done.current) return;
    done.current = true;
    specs.forEach((s, i) => { const l = newLayer(s, ed.design); if (lock && i === specs.length - 1) l.lock = true; ed.addLayer(l); });
  }, [specs, lock, ed]);
  return (
    <div style={{ width: 760, height: 676, display: 'flex' }}>
      <StudioCanvas
        design={ed.design} selectedId={ed.selectedId} onSelect={ed.select} onLive={ed.live} onCommit={ed.commit}
        playing={play} onPlaying={setPlay} editingId={editing}
        onEditStart={(id) => { ed.select(id); setEditing(id); }}
        onEditInput={(text) => ed.live((d) => { const l = d.layers.find((x) => x.id === editing); if (l) { l.text = text; delete l.bind; } })}
        onEditEnd={() => { setEditing(null); ed.commit(); }}
        onPresets={() => {}}
      />
    </div>
  );
}

export const Cover: Story = { render: () => <Demo /> };
export const CoverSelected: Story = { render: () => <Demo specs={['st:doodle:star']} /> };
export const TextSelected: Story = { render: () => <Demo specs={['tx:serif']} /> };
export const Locked: Story = { render: () => <Demo specs={['st:doodle:heart']} lock /> };
export const Folder: Story = { render: () => <Demo kind="folder" nameAt="left" /> };
export const FolderCentreName: Story = { render: () => <Demo kind="folder" nameAt="centre" /> };
export const FolderSticker: Story = { render: () => <Demo kind="folder" specs={['st:doodle:sun']} /> };
export const WithVideo: Story = { render: () => <Demo specs={['vd:aurora']} /> };
export const VideoPlaying: Story = { render: () => <Demo specs={['vd:aurora']} playing /> };
export const Shape: Story = { render: () => <Demo specs={['sh:rounded']} /> };
