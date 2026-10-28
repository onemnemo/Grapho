import type { Meta, StoryObj } from '@storybook/react-vite';
import { ANIM_NAMES } from './mascot';
import { MascotSprite } from './MascotSprite';

const meta: Meta<typeof MascotSprite> = {
  title: 'Assets/Mascot',
  component: MascotSprite,
  args: { anim: 'wave', scale: 8, seed: 1 },
  argTypes: { anim: { control: 'select', options: ANIM_NAMES }, scale: { control: { type: 'range', min: 2, max: 12 } } },
};
export default meta;

export const Playground: StoryObj<typeof MascotSprite> = {};

export const EveryAnimation: StoryObj<typeof MascotSprite> = {
  parameters: { layout: 'padded' },
  render: () => (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(120px, 1fr))', gap: 16 }}>
      {ANIM_NAMES.map((a) => (
        <figure key={a} style={{ margin: 0, display: 'grid', justifyItems: 'center', gap: 8 }}>
          <MascotSprite anim={a} scale={6} />
          <figcaption style={{ font: 'var(--t-caption)', color: 'var(--dim)' }}>{a}</figcaption>
        </figure>
      ))}
    </div>
  ),
};
