import { useEffect, useRef, useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { buildTemplates, newLayer } from '@/assets/studio';
import { applyProp, useStudioEditor } from '@/lib/studio';
import { StudioSide } from './StudioSide';

const meta: Meta<typeof StudioSide> = { title: 'Blocks/StudioSide', component: StudioSide, parameters: { layout: 'centered' } };
export default meta;
type Story = StoryObj<typeof StudioSide>;

function Demo({ kind = 'cover', spec }: { kind?: 'cover' | 'folder'; spec?: string }) {
  const [start] = useState(() => { const t = buildTemplates(); return kind === 'cover' ? t.covers[2] : t.folders[0]; });
  const ed = useStudioEditor(start);
  const [safe, setSafe] = useState(true);
  const done = useRef(false);
  useEffect(() => { if (spec && !done.current) { done.current = true; ed.addLayer(newLayer(spec, ed.design)); } }, [spec, ed]);
  return (
    <div style={{ width: 300, height: 720 }}>
      <StudioSide
        design={ed.design} selectedId={ed.selectedId} onSelect={ed.select} layer={ed.selected} safe={safe} onSafe={setSafe}
        onToggleLock={(id) => ed.patchLayer(id, { lock: !ed.design.layers.find((l) => l.id === id)?.lock }, true)}
        onToggleHide={(id) => ed.patchLayer(id, { hide: !ed.design.layers.find((l) => l.id === id)?.hide }, true)}
        onReorder={ed.reorder}
        onEdit={(path, value, final) => { ed.live((d) => applyProp(d, ed.selected, path, value)); if (final) ed.commit(); }}
        onAction={(a) => { const id = ed.selectedId; if (!id) return; if (a === 'dup') ed.duplicate(id); else if (a === 'del') ed.deleteLayer(id); else if (a === 'fwd') ed.move(id, 1); else if (a === 'bwd') ed.move(id, -1); }}
      />
    </div>
  );
}

export const CoverSettings: Story = { render: () => <Demo /> };
export const FolderSettings: Story = { render: () => <Demo kind="folder" /> };
export const SelectedText: Story = { render: () => <Demo spec="tx:serif" /> };
export const SelectedSticker: Story = { render: () => <Demo spec="st:doodle:star" /> };
