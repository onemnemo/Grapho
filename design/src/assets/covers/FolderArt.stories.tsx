import type { Meta, StoryObj } from '@storybook/react-vite';
import { iconPaths, type IconName } from '../icons';
import { FolderArt, FOLDER_COLOURS } from './FolderArt';
import s from './Covers.stories.module.css';

const meta: Meta<typeof FolderArt> = {
  title: 'Assets/Covers/FolderArt',
  component: FolderArt,
  parameters: { layout: 'padded' },
  args: { colour: 'indigo', name: 'Semester 1', meta: '12 items', pinned: false, interactive: true, state: 'rest' },
  argTypes: {
    colour: { control: 'select', options: FOLDER_COLOURS },
    icon: { control: 'select', options: [undefined, ...(Object.keys(iconPaths) as IconName[])] },
    state: { control: 'inline-radio', options: ['rest', 'hover', 'over'] },
  },
  render: (args) => <div className={s.folder}><FolderArt {...args} /></div>,
};
export default meta;
type Story = StoryObj<typeof FolderArt>;

export const Playground: Story = {};

export const Colours: Story = {
  render: () => (
    <div className={s.row}>
      {FOLDER_COLOURS.map((c) => (
        <figure key={c} className={s.cell}>
          <div className={s.folder}><FolderArt colour={c} name={c[0].toUpperCase() + c.slice(1)} meta="8 items" papers={['calculus', 'plain-paper', 'biology']} interactive /></div>
          {c}
        </figure>
      ))}
    </div>
  ),
};

export const WithIconAndPin: Story = {
  render: () => (
    <div className={s.row}>
      <div className={s.folder}><FolderArt colour="sea" name="Labs" meta="4 items" icon="flask" /></div>
      <div className={s.folder}><FolderArt colour="rose" name="Personal" meta="9 items" pinned /></div>
      <div className={s.folder}><FolderArt colour="gold" /></div>
    </div>
  ),
};

export const States: Story = {
  render: () => (
    <div className={s.row}>
      {(['rest', 'hover', 'over'] as const).map((st) => (
        <figure key={st} className={s.cell}>
          <div className={s.folder}><FolderArt colour="forest" name="Readings" meta="3 items" papers={['physics', 'plain-paper', 'dusk']} state={st} /></div>
          {st}
        </figure>
      ))}
    </div>
  ),
};
