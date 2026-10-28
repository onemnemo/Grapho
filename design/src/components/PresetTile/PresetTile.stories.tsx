import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { DesignArt, CV_PRESETS, QUICK_COVERS, QUICK_FOLDERS, coverFrom, folderFrom } from '@/assets/studio';
import { CanvasPreview } from '@/assets/covers/CanvasPreview';
import { TileArtView, texArt } from '@/assets/studio';
import { PresetTile } from './PresetTile';

const meta: Meta<typeof PresetTile> = {
  title: 'Components/PresetTile',
  component: PresetTile,
  parameters: { layout: 'centered' },
  args: { label: 'Indigo', selected: false, variant: 'sheet', shape: 'folder', selecting: false },
  argTypes: {
    variant: { control: 'inline-radio', options: ['sheet', 'picker', 'swatch'] },
    shape: { control: 'inline-radio', options: ['folder', 'cover', 'canvas'] },
  },
};
export default meta;
type Story = StoryObj<typeof PresetTile>;

const folder = (w = 96) => <div style={{ width: w }}><DesignArt design={folderFrom(QUICK_FOLDERS[0][1], { finish: QUICK_FOLDERS[0][2], name: 'Semester 2' })} /></div>;
const cover = (w = 56) => <div style={{ width: w }}><DesignArt design={coverFrom(QUICK_COVERS[2][1], { tex: QUICK_COVERS[2][2], name: 'Chemistry', cover: { spine: true } })} /></div>;
const canvas = (w = 96, h = 68) => <CanvasPreview settings={{ bg: CV_PRESETS[1][1], tpl: CV_PRESETS[1][2], gap: 'M', size: 'inf' }} width={w} height={h} plain flat />;
const cell = { width: 112 } as const;
const row = { display: 'flex', gap: 10, flexWrap: 'wrap' } as const;

export const Playground: Story = { render: (a) => <div style={cell}><PresetTile {...a} preview={folder()} /></div> };

export const States: Story = {
  render: () => (
    <div style={row}>
      {[['Rest', undefined], ['Hover', 'hover'], ['Press', 'press'], ['Focus', 'focus']].map(([n, s]) => (
        <div key={n} style={cell}><PresetTile label={n as string} preview={folder()} {...(s ? { 'data-state': s } : {})} /></div>
      ))}
      <div style={cell}><PresetTile label="Selected" selected preview={folder()} /></div>
      <div style={cell}><PresetTile label="Disabled" disabled preview={folder()} /></div>
    </div>
  ),
};

export const Shapes: Story = {
  render: () => (
    <div style={row}>
      <div style={cell}><PresetTile label="Indigo" shape="folder" preview={folder()} /></div>
      <div style={cell}><PresetTile label="Navy" shape="cover" preview={cover()} selected /></div>
      <div style={cell}><PresetTile label="Dot grid" shape="canvas" preview={canvas()} /></div>
    </div>
  ),
};

/** A texture or pattern tile: the picture fills a rounded well in its own colour, or overflows when bare. */
export const Swatch: Story = {
  render: () => (
    <div style={{ ...row, width: 360 }}>
      <div style={cell}><PresetTile variant="swatch" slotHeight={84} label="Linen" selected preview={<TileArtView art={texArt('linen', QUICK_FOLDERS[0][1])} />} /></div>
      <div style={cell}><PresetTile variant="swatch" slotHeight={84} label="Grain" preview={<TileArtView art={texArt('grain', QUICK_FOLDERS[0][1])} />} /></div>
      <div style={cell}><PresetTile variant="swatch" bare slotHeight={68} label="Dots" preview={canvas(84, 62)} /></div>
    </div>
  ),
};

export const Picker: Story = {
  render: () => {
    const [on, setOn] = useState<number | null>(1);
    return (
      <div style={{ ...row, width: 640 }}>
        {[0, 1, 2].map((i) => (
          <div key={i} style={{ width: 180 }}>
            <PresetTile variant="picker" shape="cover" label={QUICK_COVERS[i][0]} caption={i === 0 ? 'Saved just now' : 'Built in'} selected={on === i} onClick={() => setOn(i)}
              preview={cover(104)} />
          </div>
        ))}
      </div>
    );
  },
};

export const PickerSelecting: Story = {
  render: () => (
    <div style={{ ...row, width: 640 }}>
      {[0, 1, 2].map((i) => (
        <div key={i} style={{ width: 180 }}>
          <PresetTile variant="picker" shape="folder" selecting selected={i === 1} label={QUICK_FOLDERS[i][0]} caption="Built in" preview={folder(150)} />
        </div>
      ))}
    </div>
  ),
};
