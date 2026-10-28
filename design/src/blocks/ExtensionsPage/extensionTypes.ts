import type { IconName } from '@/assets/icons';

/** built ships with the app. grapho is official, from the catalogue. user was installed from a .graphoext file. */
export type ExtensionGroup = 'built' | 'grapho' | 'user';

export interface ExtensionAccess {
  icon: IconName;
  label: string;
}

export interface ExtensionItem {
  id: string;
  icon: IconName;
  name: string;
  description: string;
  enabled: boolean;
  group: ExtensionGroup;
  version: string;
  /** What it can reach. Shown on its page. */
  access: ExtensionAccess[];
}

export const groupLabels: Record<ExtensionGroup, string> = { built: 'Built in', grapho: 'From Grapho', user: 'Installed by you' };
export const groupOrder: ExtensionGroup[] = ['built', 'grapho', 'user'];

export const sampleExtensions: ExtensionItem[] = [
  {
    id: 'maths', icon: 'calculator', name: 'Handwriting to maths', description: 'Turns written formulas into typeset maths.', enabled: true, group: 'built', version: '1.4',
    access: [{ icon: 'pen', label: 'Your strokes on the open page' }, { icon: 'copy', label: 'The clipboard, when you copy a formula' }],
  },
  {
    id: 'audio', icon: 'mic', name: 'Audio on pages', description: 'Record a lecture and replay it from your ink.', enabled: false, group: 'built', version: '1.4',
    access: [{ icon: 'mic', label: 'The microphone, while recording' }, { icon: 'page', label: 'The page you record on' }],
  },
  {
    id: 'cards', icon: 'card', name: 'Flashcards', description: 'Turn a page into a deck you can test yourself on.', enabled: true, group: 'grapho', version: '0.9',
    access: [{ icon: 'page', label: 'The pages you pick' }, { icon: 'clock', label: 'Study times, kept on this device' }],
  },
  {
    id: 'dav', icon: 'cloud', name: 'Sync with WebDAV', description: 'Back up notes to your own server.', enabled: false, group: 'grapho', version: '0.7',
    access: [{ icon: 'globe', label: 'The server address you enter' }, { icon: 'notebook', label: 'All notes, to upload them' }],
  },
  {
    id: 'timer', icon: 'clock', name: 'Study timer', description: 'A small timer that sits beside the page.', enabled: true, group: 'grapho', version: '1.1',
    access: [{ icon: 'bell', label: 'Notifications, when a session ends' }],
  },
  {
    id: 'refs', icon: 'link', name: 'Reference links', description: 'Cite sources from a library file on this tablet.', enabled: true, group: 'user', version: '0.3',
    access: [{ icon: 'doc', label: 'The library file you chose' }, { icon: 'text', label: 'Text on the open page' }],
  },
];
