import type { Meta, StoryObj } from '@storybook/react-vite';
import { CoverArt } from './CoverArt';
import { COVER_PRESETS } from './coverPresets';
import { COVER_PATTERNS, COVER_TEXTURES } from './coverFinish';
import { DocArt, CanvasArt } from './ThumbArt';
import s from './Covers.stories.module.css';

const meta: Meta<typeof CoverArt> = {
  title: 'Assets/Covers/CoverArt',
  component: CoverArt,
  parameters: { layout: 'padded' },
  args: { preset: 'calculus', label: 'Calculus', pattern: 'none', texture: 'none', artwork: true, spine: true, strap: true },
  argTypes: {
    preset: { control: 'select', options: COVER_PRESETS },
    pattern: { control: 'select', options: COVER_PATTERNS },
    texture: { control: 'select', options: COVER_TEXTURES },
    color: { control: 'color' },
    patternColor: { control: 'color' },
  },
  render: (args) => <div className={s.cover}><CoverArt {...args} /></div>,
};
export default meta;
type Story = StoryObj<typeof CoverArt>;

export const Playground: Story = {};

export const Presets: Story = {
  render: () => (
    <div className={s.row}>
      {COVER_PRESETS.map((p) => (
        <figure key={p} className={s.cell}>
          <div className={s.cover}><CoverArt preset={p} label={p[0].toUpperCase() + p.slice(1)} /></div>
          {p}
        </figure>
      ))}
    </div>
  ),
};

export const Textures: Story = {
  render: () => (
    <div className={s.row}>
      {COVER_TEXTURES.map((t) => (
        <figure key={t} className={s.cell}>
          <div className={s.cover}><CoverArt preset="moss" texture={t} label="Garden" /></div>
          {t}
        </figure>
      ))}
    </div>
  ),
};

export const Patterns: Story = {
  render: () => (
    <div className={s.row}>
      {COVER_PATTERNS.map((p) => (
        <figure key={p} className={s.cell}>
          <div className={s.cover}><CoverArt preset="dusk" artwork={false} pattern={p} label="Journal" /></div>
          {p}
        </figure>
      ))}
    </div>
  ),
};

export const SpineAndStrap: Story = {
  render: () => (
    <div className={s.row}>
      {[[true, true], [true, false], [false, true], [false, false]].map(([sp, st]) => (
        <figure key={`${sp}${st}`} className={s.cell}>
          <div className={s.cover}><CoverArt preset="history" spine={sp} strap={st} label="Art" /></div>
          {sp ? 'spine' : 'no spine'}, {st ? 'strap' : 'no strap'}
        </figure>
      ))}
    </div>
  ),
};

export const OtherItems: Story = {
  render: () => (
    <div className={s.row}>
      {(['plain', 'chart', 'highlight'] as const).map((k) => (
        <figure key={k} className={s.cell}><div className={s.cover}><DocArt kind={k} /></div>PDF {k}</figure>
      ))}
      <figure className={s.cell}><div className={s.cover}><CanvasArt /></div>canvas</figure>
    </div>
  ),
};
