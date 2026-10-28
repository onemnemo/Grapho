import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { Toggle } from '../Toggle';
import { ReorderGroups, ReorderList } from './ReorderList';
import styles from './ReorderList.stories.module.css';

const meta: Meta<typeof ReorderList> = {
  title: 'Components/ReorderList',
  component: ReorderList,
  parameters: { layout: 'centered' },
  argTypes: { items: { control: false }, onChange: { control: false }, renderItem: { control: false }, getId: { control: false }, getLabel: { control: false } },
};
export default meta;
type Story = StoryObj<typeof ReorderList>;

interface Item { id: string; name: string }
const names = ['Pen', 'Highlighter', 'Eraser', 'Fill', 'Lasso', 'Shapes'];
const make = (list: string[]): Item[] => list.map((n) => ({ id: n.toLowerCase(), name: n }));

/** Hover a row to see its grip. Drag it, or focus the grip and use the arrow keys. On touch, press and hold a row. */
export const Single: Story = {
  render: () => {
    const [items, setItems] = useState(() => make(names));
    return (
      <div className={styles.stage}>
        <ReorderList
          items={items}
          onChange={setItems}
          getId={(i) => i.id}
          getLabel={(i) => i.name}
          renderItem={(i, { handle }) => <div className={styles.row}>{handle}<b>{i.name}</b></div>}
        />
      </div>
    );
  },
};

/** Two sections. Turning a row off, or dragging it across, moves it to the other list and it travels there. */
export const TwoSections: Story = {
  render: () => {
    const [on, setOn] = useState(() => make(names.slice(0, 4)));
    const [off, setOff] = useState(() => make(names.slice(4)));
    const toggle = (item: Item, to: 'on' | 'off') => {
      if (to === 'off') { setOn(on.filter((x) => x.id !== item.id)); setOff([item, ...off]); }
      else { setOff(off.filter((x) => x.id !== item.id)); setOn([...on, item]); }
    };
    return (
      <div className={styles.stage}>
        <ReorderGroups
          sections={[
            { id: 'on', items: on, heading: <div className={styles.title}>On the wheel</div>, min: 1 },
            { id: 'off', items: off, heading: <div className={styles.title}>Off</div>, empty: <div className={styles.empty}>Drag a tool here to turn it off</div> },
          ]}
          getId={(i) => i.id}
          getLabel={(i) => i.name}
          onChange={(next) => { setOn(next[0].items); setOff(next[1].items); }}
          renderItem={(i, { handle, section }) => (
            <div className={styles.row} data-off={section === 'off' || undefined}>
              {handle}<b>{i.name}</b>
              <Toggle aria-label={i.name} checked={section === 'on'} disabled={section === 'on' && on.length <= 1} onChange={() => toggle(i, section === 'on' ? 'off' : 'on')} />
            </div>
          )}
        />
      </div>
    );
  },
};
