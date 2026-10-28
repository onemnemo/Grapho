export interface NoteLayer {
  id: string;
  name: string;
  visible: boolean;
  locked: boolean;
  /** 0 to 1. */
  opacity: number;
}

export const sampleLayers: NoteLayer[] = [
  { id: 'l3', name: 'Highlights', visible: true, locked: false, opacity: 0.6 },
  { id: 'l2', name: 'Diagram', visible: true, locked: true, opacity: 1 },
  { id: 'l1', name: 'Layer 1', visible: true, locked: false, opacity: 1 },
];

export const nextLayerName = (layers: NoteLayer[]) => {
  const used = new Set(layers.map((l) => l.name));
  let n = layers.length + 1;
  while (used.has(`Layer ${n}`)) n += 1;
  return `Layer ${n}`;
};
