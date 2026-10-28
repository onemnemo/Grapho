import type { Meta, StoryObj } from '@storybook/react-vite';
import { GlyphPicker, type GlyphOption } from './GlyphPicker';

const svg = (body: React.ReactNode) => (
  <svg width="32" height="18" viewBox="0 0 30 18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">{body}</svg>
);

const options: GlyphOption[] = [
  { value: 'none', label: 'None', glyph: svg(<path d="M2 9h22" />) },
  { value: 'arrow', label: 'Arrow', glyph: svg(<><path d="M2 9h18" /><path d="M18 3l8 6-8 6z" fill="currentColor" /></>) },
  { value: 'open', label: 'Open arrow', glyph: svg(<path d="M2 9h24M19 3l7 6-7 6" />) },
  { value: 'dot', label: 'Dot', glyph: svg(<><path d="M2 9h18" /><circle cx="24" cy="9" r="4" fill="currentColor" /></>) },
];

const meta: Meta<typeof GlyphPicker> = {
  title: 'Components/GlyphPicker',
  component: GlyphPicker,
  args: { options, defaultValue: 'arrow', 'aria-label': 'Line ending' },
  decorators: [(Story) => <div style={{ width: 280 }}><Story /></div>],
};
export default meta;
type Story = StoryObj<typeof GlyphPicker>;

export const Playground: Story = {};
