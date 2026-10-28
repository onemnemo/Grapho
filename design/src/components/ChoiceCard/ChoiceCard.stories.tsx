import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { CV_PRESETS, CV_NEW, ILLUS } from '@/assets/studio';
import { CanvasPreview } from '@/assets/covers/CanvasPreview';
import { ChoiceCard } from './ChoiceCard';

const meta: Meta<typeof ChoiceCard> = {
  title: 'Components/ChoiceCard',
  component: ChoiceCard,
  parameters: { layout: 'centered' },
  args: { title: 'Image', description: 'A rectangle you can crop.', selected: false, size: 'lg' },
  argTypes: { size: { control: 'inline-radio', options: ['lg', 'sm'] } },
};
export default meta;
type Story = StoryObj<typeof ChoiceCard>;

const picture = (
  <span style={{ display: 'grid', placeItems: 'center', width: '100%', height: '100%' }}>
    <img src={ILLUS} alt="" style={{ width: '70%', aspectRatio: '3 / 2', objectFit: 'cover', borderRadius: 4 }} />
  </span>
);
const wrap = { width: 236 } as const;

export const Playground: Story = { render: (a) => <div style={wrap}><ChoiceCard {...a} preview={picture} /></div> };
export const Selected: Story = { render: (a) => <div style={wrap}><ChoiceCard {...a} selected preview={picture} /></div> };
export const Disabled: Story = { render: (a) => <div style={wrap}><ChoiceCard {...a} disabled preview={picture} /></div> };

export const Pair: Story = {
  render: () => {
    const [pick, setPick] = useState('image');
    return (
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, width: 472 }}>
        <ChoiceCard title="Sticker" description="Cut out, with an outline and a shadow." selected={pick === 'sticker'} onClick={() => setPick('sticker')} preview={picture} />
        <ChoiceCard title="Image" description="A rectangle you can crop." selected={pick === 'image'} onClick={() => setPick('image')} preview={picture} />
      </div>
    );
  },
};

export const Small: Story = {
  render: () => {
    const [pick, setPick] = useState('inf');
    const cv = { ...CV_NEW, bg: CV_PRESETS[1][1] };
    return (
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, width: 400 }}>
        <ChoiceCard size="sm" title="Infinite" description="Grows as you write." selected={pick === 'inf'} onClick={() => setPick('inf')}
          preview={<span style={{ display: 'grid', placeItems: 'center', height: '100%' }}><CanvasPreview settings={cv} width={96} height={60} plain flat /></span>} />
        <ChoiceCard size="sm" title="Fixed" description="A set size, like a page." selected={pick === 'fixed'} onClick={() => setPick('fixed')}
          preview={<span style={{ display: 'grid', placeItems: 'center', height: '100%' }}><CanvasPreview settings={{ ...cv, size: 'fixed' }} width={96} height={60} plain flat /></span>} />
      </div>
    );
  },
};
