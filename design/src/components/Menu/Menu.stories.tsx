import { useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { AnchorDemo } from '@/stories-support/AnchorDemo';
import { Popover } from '../Popover';
import { Menu, MenuSection, MenuSeparator } from './Menu';
import { MenuItem } from './MenuItem';

const meta: Meta<typeof MenuItem> = {
  title: 'Components/Menu',
  component: MenuItem,
  parameters: { layout: 'fullscreen', docs: { story: { inline: false, height: '520px' } } },
  args: { children: 'Rename', icon: 'rename', tone: 'default', disabled: false, keepOpen: false },
  argTypes: {
    tone: { control: 'inline-radio', options: ['default', 'danger'] },
    checked: { control: 'select', options: [undefined, true, false] },
    value: { control: 'text' },
    description: { control: 'text' },
    submenu: { control: false },
  },
};
export default meta;
type Story = StoryObj<typeof MenuItem>;

/** The note's ••• menu: plain actions, a trailing value, a submenu and a danger item. */
export const Playground: Story = {
  render: (args) => (
    <AnchorDemo label="More">
      {({ anchorRef, open, setOpen }) => (
        <Popover anchorRef={anchorRef} open={open} onClose={() => setOpen(false)} placement="bottom-start">
          <Menu label="Note options" onClose={() => setOpen(false)}>
            <MenuItem {...args} />
            <MenuItem icon="duplicate">Duplicate</MenuItem>
            <MenuItem icon="move" value="Work">Move to</MenuItem>
            <MenuItem icon="share" submenu={<><MenuItem icon="pdf">PDF</MenuItem><MenuItem icon="image">Image</MenuItem><MenuItem icon="pack">Pack</MenuItem></>}>
              Export
            </MenuItem>
            <MenuSeparator />
            <MenuItem icon="trash" tone="danger">Delete</MenuItem>
          </Menu>
        </Popover>
      )}
    </AnchorDemo>
  ),
};

/** The sort menu: a label, then one current choice with a check. */
export const SingleChoiceWithSections: Story = {
  render: () => <SortMenu />,
};

function SortMenu() {
  const [by, setBy] = useState('Modified');
  const options = ['Modified', 'Created', 'Name'];
  return (
    <AnchorDemo label="Sort">
      {({ anchorRef, open, setOpen }) => (
        <Popover anchorRef={anchorRef} open={open} onClose={() => setOpen(false)} placement="bottom-end">
          <Menu label="Sort" onClose={() => setOpen(false)}>
            <MenuSection label="Sort by">
              {options.map((o) => (
                <MenuItem key={o} checked={by === o} onSelect={() => setBy(o)}>{o}</MenuItem>
              ))}
            </MenuSection>
            <MenuSeparator />
            <MenuSection label="Order">
              <MenuItem icon="sort" checked>Newest first</MenuItem>
              <MenuItem icon="sort" checked={false}>Oldest first</MenuItem>
            </MenuSection>
          </Menu>
        </Popover>
      )}
    </AnchorDemo>
  );
}

/** The New menu: sections with labels, descriptions and a separator. */
export const NewMenu: Story = {
  render: () => (
    <AnchorDemo label="New">
      {({ anchorRef, open, setOpen }) => (
        <Popover anchorRef={anchorRef} open={open} onClose={() => setOpen(false)}>
          <Menu label="New" onClose={() => setOpen(false)}>
            <MenuItem icon="notebook">Notebook</MenuItem>
            <MenuItem icon="canvas">Canvas</MenuItem>
            <MenuItem icon="folder">Folder</MenuItem>
            <MenuSeparator />
            <MenuSection label="Import">
              <MenuItem icon="pdf" description="Open a PDF as pages">PDF</MenuItem>
              <MenuItem icon="image">Image</MenuItem>
              <MenuItem icon="pack">Pack</MenuItem>
            </MenuSection>
          </Menu>
        </Popover>
      )}
    </AnchorDemo>
  ),
};

export const Danger: Story = { args: { children: 'Delete', icon: 'trash', tone: 'danger' }, render: Playground.render };
export const Disabled: Story = { args: { children: 'Rename', disabled: true }, render: Playground.render };
