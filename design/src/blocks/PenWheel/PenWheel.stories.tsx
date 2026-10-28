import { useMemo, useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-vite';
import { SAMPLE_RED } from '@/assets/wheel/defaults';
import { Button } from '@/components/Button';
import { PenWheel, type PenWheelProps } from './PenWheel';
import { WheelModel, type ModelOptions } from './engine/model';
import { WheelNoteStage as NoteStage } from '@/stories-support/wheel/WheelNoteStage';
import styles from '@/stories-support/wheel/WheelStories.module.css';

const meta: Meta<typeof PenWheel> = {
  title: 'Blocks/PenWheel',
  component: PenWheel,
  parameters: { layout: 'centered' },
  argTypes: {
    open: { control: 'boolean', description: 'Open as the wheel, or closed as the tab' },
    panels: { control: 'boolean' },
    placement: { control: 'object' },
  },
};
export default meta;
type Story = StoryObj<typeof PenWheel>;

const W = 640;
const H = 520;

/** A note with the wheel on it, as the specimen boards show it. */
function Frame(props: PenWheelProps & { width?: number; height?: number }) {
  const { width = W, height = H, ...rest } = props;
  return (
    <NoteStage width={width} height={height}>
      <PenWheel {...rest} />
    </NoteStage>
  );
}

export const DockedLeft: Story = {
  render: (a) => <Frame {...a} />,
  args: { placement: { docked: true, edge: 'left', along: 270 } },
};
export const DockedRight: Story = {
  render: (a) => <Frame {...a} />,
  args: { placement: { docked: true, edge: 'right', along: 270 } },
};
export const DockedTop: Story = {
  render: (a) => <Frame {...a} width={780} height={360} />,
  args: { placement: { docked: true, edge: 'top', along: 400 } },
};
export const DockedBottom: Story = {
  render: (a) => <Frame {...a} width={780} height={360} />,
  args: { placement: { docked: true, edge: 'bottom', along: 400 } },
};
export const Floating: Story = {
  render: (a) => <Frame {...a} />,
  args: { placement: { docked: false, floatX: 360, floatY: 290 } },
};

const edit = (o: ModelOptions): Partial<PenWheelProps> => ({ modelOptions: o });

/** Tap the chosen pen again: the other tools fold into it, its plate grows to the rim, size dots one side, opacity arc the other. */
export const EditingPen: Story = {
  render: (a) => <Frame {...a} />,
  args: { ...edit({ sizeMode: true, pen: { color: SAMPLE_RED, w: 3.2 }, penAlpha: 190 }), placement: { docked: true, edge: 'left', along: 270 } },
};
export const EditingHighlighter: Story = {
  render: (a) => <Frame {...a} />,
  args: { ...edit({ mode: 'highlighter', sizeMode: true }), placement: { docked: true, edge: 'left', along: 270 } },
};
export const EditingFloating: Story = {
  render: (a) => <Frame {...a} />,
  args: { ...edit({ sizeMode: true }), placement: { docked: false, floatX: 360, floatY: 290 } },
};
/** The eraser opens its controls straight away: six sizes and whole or part of a stroke. */
export const Eraser: Story = {
  render: (a) => <Frame {...a} />,
  args: { ...edit({ mode: 'eraser' }), placement: { docked: true, edge: 'left', along: 270 } },
};
export const EraserFloating: Story = {
  render: (a) => <Frame {...a} />,
  args: { ...edit({ mode: 'eraser', eraserArea: true }), placement: { docked: false, floatX: 360, floatY: 290 } },
};
/** Eight tools do not fit on the half ring, so it turns; a grey marker on the band shows where you are. */
export const RingsTurned: Story = {
  render: (a) => <Frame {...a} />,
  args: { turn: { colour: 12, tools: 5 }, placement: { docked: true, edge: 'left', along: 270 } },
};
export const RingsTurnedFloating: Story = {
  render: (a) => <Frame {...a} />,
  args: { turn: { colour: 9, tools: 3 }, placement: { docked: false, floatX: 360, floatY: 290 } },
};
/** Put away, the wheel is a quiet tab that carries the pen's ink colour. Drag it along its edge. */
export const ClosedTab: Story = {
  render: (a) => <Frame {...a} />,
  args: { open: false, placement: { docked: true, edge: 'left', along: 270 } },
};
export const ClosedTabRight: Story = {
  render: (a) => <Frame {...a} />,
  args: { open: false, placement: { docked: true, edge: 'right', along: 200 } },
};
export const Pinned: Story = {
  render: (a) => <Frame {...a} />,
  args: { ...edit({ pinned: true }), placement: { docked: true, edge: 'left', along: 270 } },
};
/** Held without a finger: lifted, scaled up, a deeper shadow. */
export const Carrying: Story = {
  render: (a) => <Frame {...a} />,
  args: { pose: { shape: 1, lift: 1, centre: { x: 330, y: 280 } }, placement: { docked: false, floatX: 330, floatY: 280 } },
};

/** Tap the pen in the middle: your pens open beside the wheel. */
export const PensOpen: Story = {
  render: (a) => <Frame {...a} height={560} />,
  args: { initialPanel: 'pens', placement: { docked: true, edge: 'left', along: 280 } },
};
export const PensOpenFloating: Story = {
  render: (a) => <Frame {...a} height={560} />,
  args: { initialPanel: 'pens', placement: { docked: false, floatX: 220, floatY: 290 } },
};
/** The rainbow button opens the picker over a scrim; the pen takes the colour as you drag. */
export const PickerOpen: Story = {
  render: (a) => <Frame {...a} height={900} />,
  args: { initialPanel: 'picker', placement: { docked: true, edge: 'left', along: 280 } },
};
export const ColoursOpen: Story = {
  render: (a) => <Frame {...a} height={800} />,
  args: { initialPanel: 'colours', placement: { docked: true, edge: 'left', along: 280 } },
};
export const CustomiseTools: Story = {
  render: (a) => <Frame {...a} height={660} />,
  args: { initialPanel: 'customise-tools', placement: { docked: true, edge: 'left', along: 280 } },
};
export const CustomisePens: Story = {
  render: (a) => <Frame {...a} height={820} />,
  args: { initialPanel: 'customise-pens', placement: { docked: true, edge: 'left', along: 320 } },
};

/** The real wheel on a note. Draw, turn the rings, tap the chosen pen again, tap the pen in the middle, hold to carry. */
export const Playground: Story = {
  parameters: { layout: 'fullscreen' },
  render: function Playground() {
    const model = useMemo(() => new WheelModel(), []);
    const [open, setOpen] = useState(true);
    const [key, setKey] = useState(0);
    const [place, setPlace] = useState<PenWheelProps['placement']>({ docked: true, edge: 'left', along: 280 });
    return (
      <div className={styles.page}>
        <NoteStage width={1000} height={680} model={model} bar onInkDown={() => { if (open && !model.pinned) setOpen(false); }}>
          <PenWheel key={key} model={model} open={open} onOpenChange={setOpen} placement={place} onPlacementChange={setPlace} />
        </NoteStage>
        <div className={styles.row}>
          <Button onClick={() => { setPlace({ docked: true, edge: 'left', along: 280 }); setKey((k) => k + 1); }}>Reset position</Button>
          <Button onClick={() => { setPlace({ docked: false, floatX: 500, floatY: 360 }); setKey((k) => k + 1); }}>Open as floating circle</Button>
          <Button onClick={() => setOpen((o) => !o)}>Close or open</Button>
          <p className={styles.hint}>Unpinned, the wheel closes the moment you write. Escape does not close panels; press the page.</p>
        </div>
      </div>
    );
  },
};
