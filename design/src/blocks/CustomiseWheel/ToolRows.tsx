import { Icon } from '@/assets/icons';
import { ReorderGroups } from '@/components/ReorderList';
import { Toggle } from '@/components/Toggle';
import { TOOLS, TOOL_ORDER, type ToolId, type WheelModel } from '@/blocks/PenWheel/engine/model';
import { SlotRing } from './SlotRing';
import styles from './CustomiseWheel.module.css';

const ids: ToolId[] = TOOL_ORDER;

/** The wheel's tools in two lists. Drag a row to reorder it or to move it between lists; the switch moves it too, and it travels. */
export function ToolRows({ model }: { model: WheelModel }) {
  const on = model.tools;
  const off = ids.filter((id) => !on.includes(id));
  const toggle = (id: ToolId, isOn: boolean) => model.setTools(isOn ? on.filter((x) => x !== id) : [...on, id]);
  return (
    <ReorderGroups<ToolId>
      sections={[
        {
          id: 'on',
          items: on,
          min: 1,
          heading: <div className={styles.listHead}>On the wheel<SlotRing filled={on.length} total={ids.length} /></div>,
        },
        {
          id: 'off',
          items: off,
          heading: <div className={styles.listHead}>Off</div>,
          empty: <div className={styles.slotEmpty}>None</div>,
        },
      ]}
      getId={(id) => id}
      getLabel={(id) => TOOLS[id].label}
      itemClassName={styles.toolItem}
      onChange={(next) => model.setTools(next[0].items)}
      renderItem={(id, { handle, section }) => {
        const isOn = section === 'on';
        return (
          <div className={styles.toolRow} data-off={!isOn || undefined}>
            {handle}
            <span className={styles.toolIcon}><Icon name={TOOLS[id].icon} size={20} /></span>
            <b>{TOOLS[id].label}</b>
            <Toggle aria-label={TOOLS[id].label} checked={isOn} disabled={isOn && on.length <= 1} onChange={() => toggle(id, isOn)} />
          </div>
        );
      }}
    />
  );
}
