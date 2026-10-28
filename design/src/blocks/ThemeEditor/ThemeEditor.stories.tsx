import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { SAMPLE_CUSTOM, THEME_INPUTS } from '@/assets/themes';
import { DEFAULT_THEME, themeProps, type ThemeChoice } from '@/lib/theme';
import { ThemeEditor } from './ThemeEditor';

const meta: Meta<typeof ThemeEditor> = {
  title: 'Blocks/ThemeEditor',
  component: ThemeEditor,
  parameters: { layout: 'padded' },
  decorators: [
    (Story) => (
      <div style={{ maxWidth: 760 }}>
        <Story />
      </div>
    ),
  ],
};
export default meta;
type Story = StoryObj<typeof ThemeEditor>;

/** The default: Ink chosen, three built-in themes and the custom one as tiles, Customise as the one action. */
export const Default: Story = { args: { defaultValue: DEFAULT_THEME } };

export const PaperChosen: Story = { args: { defaultValue: { id: 'paper', custom: THEME_INPUTS.paper } } };

/** Custom: live preview on the left, four colours to edit on the right. Changing a colour updates the preview and the tile. */
export const Customising: Story = {
  args: { defaultValue: { id: 'custom', custom: SAMPLE_CUSTOM.blue } },
};

/** The choice applied to a small surface next to the editor, to show what the rest of the app will do. */
export const AppliedToASurface: Story = {
  render: () => {
    const [choice, setChoice] = useState<ThemeChoice>({ id: 'custom', custom: SAMPLE_CUSTOM.harbour });
    return (
      <div style={{ display: 'grid', gap: 24 }}>
        <ThemeEditor value={choice} onChange={setChoice} />
        <div {...themeProps(choice)} style={{ ...themeProps(choice).style, background: 'var(--s1)', color: 'var(--fg)', padding: 20, borderRadius: 'var(--r-lg)' }}>
          <p style={{ margin: 0, font: 'var(--t-heading)' }}>Same tokens as the app</p>
          <p style={{ margin: '4px 0 0', font: 'var(--t-caption)', color: 'var(--mute)' }}>Surface, text and lines follow the four colours.</p>
        </div>
      </div>
    );
  },
};
