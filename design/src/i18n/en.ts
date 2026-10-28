/** English, the source catalogue. Every other catalogue is checked against these keys.
 *  Keys are grouped by the component that shows them: `area.thing`. Values may hold {name} placeholders. */
export const en = {
  'common.remove': 'Remove',
  'common.close': 'Close',
  'common.cancel': 'Cancel',
  'common.done': 'Done',
  'common.reset': 'Reset',

  'colour.hueSat': 'Hue and saturation',
  'colour.brightness': 'Brightness',
  'colour.hex': 'Hex colour',
  'colour.pick': 'Pick from screen',

  'language.title': 'Language',
  'language.search': 'Search languages',
  'language.device': 'Device language',
  'language.all': 'All languages',
  'language.none': 'No language matches "{query}"',
  'language.count': '{n} languages',

  'theme.title': 'Theme',
  'theme.ink': 'Ink',
  'theme.graphite': 'Graphite',
  'theme.paper': 'Paper',
  'theme.custom': 'Custom',
  'theme.customise': 'Customise',
  'theme.accent': 'Accent',
  'theme.background': 'Background',
  'theme.surface': 'Surface',
  'theme.page': 'Page',
  'theme.preview': 'Preview',
  'theme.previewTitle': 'Notes',
  'theme.previewNew': 'New',
  'theme.previewSearch': 'Search',
} as const;

export type Key = keyof typeof en;
export type Catalogue = Partial<Record<Key, string>>;
