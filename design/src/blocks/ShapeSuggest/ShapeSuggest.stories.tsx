import type { Meta, StoryObj } from '@storybook/react-vite';
import { NoteStage } from '@/stories-support/NoteStage';
import { InkLayer } from '../NotePage';
import { ShapeSuggest } from './ShapeSuggest';
import { shapeGlyphs } from './shapeGlyphs';

const meta: Meta<typeof ShapeSuggest> = {
  title: 'Blocks/ShapeSuggest',
  component: ShapeSuggest,
  parameters: { layout: 'padded' },
  args: {
    x: 400, y: 312, durationMs: 5000,
    best: { value: 'circle', label: 'Circle', glyph: shapeGlyphs.circle },
    others: [{ value: 'ellipse', label: 'Ellipse', glyph: shapeGlyphs.ellipse }],
  },
  argTypes: { durationMs: { control: { type: 'number', min: 1000, max: 15000, step: 500 } } },
  render: (args) => (
    <NoteStage
      height={400}
      page={<InkLayer width={800} height={400}><circle cx="400" cy="190" r="90" fill="none" stroke="currentColor" strokeWidth="4" strokeLinecap="round" /></InkLayer>}
    >
      <ShapeSuggest {...args} onChoose={() => {}} onDismiss={() => {}} />
    </NoteStage>
  ),
};
export default meta;
type Story = StoryObj<typeof ShapeSuggest>;

/** Counts down for five seconds and pauses while the pointer is over it. */
export const Circle: Story = {};
export const Rectangle: Story = { args: { best: { value: 'rect', label: 'Rectangle', glyph: shapeGlyphs.rect }, others: [{ value: 'rrect', label: 'Rounded', glyph: shapeGlyphs.rrect }] } };
export const Triangle: Story = { args: { best: { value: 'tri', label: 'Triangle', glyph: shapeGlyphs.tri }, others: [{ value: 'poly', label: 'Polygon', glyph: shapeGlyphs.poly }] } };
export const Arrow: Story = { args: { best: { value: 'arrow', label: 'Arrow', glyph: shapeGlyphs.arrow }, others: [{ value: 'line', label: 'Line', glyph: shapeGlyphs.line }] } };
export const OnlyOneMatch: Story = { args: { others: [] } };
