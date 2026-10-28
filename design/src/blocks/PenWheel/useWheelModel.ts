import { useSyncExternalStore } from 'react';
import type { WheelModel } from './engine/model';

/** Re-render when anything in the wheel's state changes. Panels read the model directly after this. */
export function useWheelModel(model: WheelModel) {
  useSyncExternalStore(model.subscribe, () => model.version);
  return model;
}
