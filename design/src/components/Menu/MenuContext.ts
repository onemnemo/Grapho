import { createContext } from 'react';

export interface MenuContextValue {
  /** Close the whole menu after a choice. */
  close: () => void;
  /** Which submenu is open in this menu, if any. */
  activeSub: string | null;
  setActiveSub: (id: string | null) => void;
  /** Set inside a submenu: close just that level. */
  closeSelf?: () => void;
}

export const MenuContext = createContext<MenuContextValue | null>(null);
