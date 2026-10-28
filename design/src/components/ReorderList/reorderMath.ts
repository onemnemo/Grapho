/* Pure helpers for ReorderList: where a dragged item lands and how far every other item moves to make room. */

export interface Slot<T> {
  id: string;
  items: T[];
}

export interface Target {
  /** Index into the sections. */
  section: number;
  /** Position inside that section, among the items that are not being dragged. */
  index: number;
  /** Position in the flat list of every other item, in reading order. */
  flat: number;
}

export interface Box {
  top: number;
  bottom: number;
}

/**
 * Finds the section and slot for a dragged item whose centre is at `y`.
 * `sectionBoxes` are the sections' boxes; `mids` are the vertical centres of the other items, per section.
 */
export function targetAt(y: number, sectionBoxes: Box[], mids: number[][]): Target {
  let section = 0;
  let best = Infinity;
  sectionBoxes.forEach((b, i) => {
    const d = y < b.top ? b.top - y : y > b.bottom ? y - b.bottom : 0;
    if (d < best) { best = d; section = i; }
  });
  const index = mids[section].filter((m) => m < y).length;
  const flat = mids.slice(0, section).reduce((n, list) => n + list.length, 0) + index;
  return { section, index, flat };
}

/**
 * How far each of the other items moves while one is dragged.
 * Items after the lifted item close the gap; items from the drop slot on open one.
 */
export function shiftFor(flatIndex: number, removedAt: number, insertAt: number, room: number): number {
  return (flatIndex >= insertAt ? room : 0) - (flatIndex >= removedAt ? room : 0);
}

/** Moves one item to a new section and slot. */
export function moveItem<T>(sections: Array<Slot<T>>, id: string, getId: (t: T) => string, to: { section: number; index: number }): Array<Slot<T>> {
  let moved: T | undefined;
  const without = sections.map((s) => ({
    ...s,
    items: s.items.filter((t) => {
      if (getId(t) !== id) return true;
      moved = t;
      return false;
    }),
  }));
  if (moved === undefined) return sections;
  return without.map((s, i) => {
    if (i !== to.section) return s;
    const items = s.items.slice();
    items.splice(to.index, 0, moved as T);
    return { ...s, items };
  });
}
