import type { Meta, StoryObj } from '@storybook/react-vite';
import { Icon } from './Icon';
import { iconPaths, type IconName } from './iconPaths';
import styles from './Icons.stories.module.css';

const names = Object.keys(iconPaths) as IconName[];

const meta: Meta<typeof Icon> = {
  title: 'Assets/Icons',
  component: Icon,
  args: { name: 'pen', size: 24, active: false },
  argTypes: { name: { control: 'select', options: names } },
};
export default meta;

export const Single: StoryObj<typeof Icon> = {};

export const AllIcons: StoryObj<typeof Icon> = {
  parameters: { layout: 'padded' },
  render: (args) => (
    <div className={styles.grid}>
      {names.map((n) => (
        <figure key={n} className={styles.cell}>
          <Icon name={n} size={args.size} active={args.active} />
          <figcaption>{n}</figcaption>
        </figure>
      ))}
    </div>
  ),
};
