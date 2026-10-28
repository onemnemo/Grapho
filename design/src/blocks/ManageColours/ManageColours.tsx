import { useState } from 'react';
import { Button } from '@/components/Button';
import { ReorderList } from '@/components/ReorderList';
import type { ColourSet, WheelModel } from '@/blocks/PenWheel/engine/model';
import { useWheelModel } from '@/blocks/PenWheel/useWheelModel';
import { WheelPanel } from '@/blocks/WheelPanel';
import { SetBlock } from './SetBlock';
import styles from './ManageColours.module.css';

export interface ManageColoursProps {
  model: WheelModel;
  /** The colour that starts out chosen. */
  pick?: { s: number; i: number } | null;
  onClose?: () => void;
  /** Edit the chosen colour: opens the picker for it. */
  onEditColour?: (at: { s: number; i: number }) => void;
  /** A colour or set was deleted; offer to put it back. */
  onUndoable?: (text: string, undo: () => void) => void;
  state?: 'open' | 'leave';
}

const move = <T,>(list: T[], from: number, to: number) => {
  const next = list.slice();
  const [x] = next.splice(from, 1);
  next.splice(to, 0, x);
  return next;
};

/** Your colour sets as one list of swatches. The first set leads the ring; drag a set to reorder. */
export function ManageColours({ model, pick = null, onClose, onEditColour, onUndoable, state = 'open' }: ManageColoursProps) {
  useWheelModel(model);
  const sets = model.sets;
  const [picked, setPicked] = useState<{ id: string; i: number } | null>(pick && sets[pick.s] ? { id: sets[pick.s].id, i: pick.i } : null);
  const setColours = (id: string, colours: string[]) => model.setSets(sets.map((s) => (s.id === id ? { ...s, colours } : s)));

  return (
    <WheelPanel width={380} variant="form" state={state} role="dialog" aria-label="Colours">
      <div className={styles.body}>
        <ReorderList<ColourSet>
          items={sets}
          getId={(s) => s.id}
          getLabel={(s) => s.name}
          itemClassName={styles.item}
          onChange={(next) => model.setSets(next)}
          renderItem={(set, { handle, index }) => (
            <SetBlock
              set={set}
              handle={handle}
              first={index === 0}
              chosen={picked?.id === set.id && set.colours[picked.i] != null ? picked.i : null}
              canDelete={sets.length > 1}
              onRename={(name) => model.renameSet(index, name)}
              onChoose={(i) => setPicked(i == null ? null : { id: set.id, i })}
              onAdd={() => {
                setColours(set.id, [...set.colours, model.setting.color]);
                setPicked({ id: set.id, i: set.colours.length });
              }}
              onEdit={() => picked && onEditColour?.({ s: index, i: picked.i })}
              onNudge={(by) => {
                if (!picked) return;
                setColours(set.id, move(set.colours, picked.i, picked.i + by));
                setPicked({ id: set.id, i: picked.i + by });
              }}
              onDeleteColour={() => {
                if (!picked) return;
                const at = picked.i;
                const gone = set.colours[at];
                setColours(set.id, set.colours.filter((_, k) => k !== at));
                setPicked(null);
                onUndoable?.('Colour deleted', () => model.setSets(model.sets.map((s) => (s.id === set.id ? { ...s, colours: s.colours.slice(0, at).concat(gone, s.colours.slice(at)) } : s))));
              }}
              onDeleteSet={() => {
                setPicked(null);
                model.setSets(sets.filter((s) => s.id !== set.id));
                onUndoable?.('Set deleted', () => model.setSets(model.sets.slice(0, index).concat(set, model.sets.slice(index))));
              }}
            />
          )}
        />
        <Button
          variant="text"
          icon="plus"
          className={styles.newSet}
          onClick={() => {
            const id = `s${Date.now()}`;
            model.setSets([...sets, { id, name: 'New set', colours: [] }]);
          }}
        >
          New set
        </Button>
      </div>
      <Button block variant="primary" onClick={onClose}>Done</Button>
    </WheelPanel>
  );
}
