import type { IconName } from '@/assets/icons';
import type { ContentType } from '@/assets/content';
import type { BarPosition, BarVariant } from '../NoteBar';

export type AutoHide = 'off' | 'while' | 'always';
export type WheelEdge = 'left' | 'right' | 'bottom';
export type ShapeDetection = 'suggest' | 'snap' | 'off';
export type BatteryAuto = 'off' | '20' | '10';

export interface SettingsValues {
  barStyle: BarVariant;
  barPosition: BarPosition;
  autoHide: AutoHide;
  showTitle: boolean;
  wheelEdge: WheelEdge;
  detection: ShapeDetection;
  batterySaver: boolean;
  batteryAuto: BatteryAuto;
  syncOn: boolean;
  syncWifiOnly: boolean;
  /** Use Paper while the device is in light mode and the chosen theme while it is dark. */
  matchDevice: boolean;
}

export const defaultSettings: SettingsValues = {
  barStyle: 'pill', barPosition: 'top', autoHide: 'off', showTitle: true, wheelEdge: 'right', detection: 'suggest', batterySaver: false, batteryAuto: 'off',
  syncOn: true, syncWifiOnly: true, matchDevice: false,
};

export type ContentSectionId = `content/${ContentType}`;
export type SettingsSectionId = 'general' | 'appearance' | 'language' | 'pen' | 'content' | ContentSectionId | 'packs' | 'extensions' | 'sync' | 'about';

export interface SectionInfo {
  id: SettingsSectionId;
  icon: IconName;
  label: string;
}

/** The list on the left. Groups are separated by space, not lines or labels. */
export const navGroups: SectionInfo[][] = [
  [
    { id: 'general', icon: 'settings', label: 'General' },
    { id: 'appearance', icon: 'sun', label: 'Appearance' },
    { id: 'language', icon: 'globe', label: 'Language' },
    { id: 'pen', icon: 'pen', label: 'Pen and input' },
  ],
  [
    { id: 'content', icon: 'layers', label: 'Content' },
    { id: 'packs', icon: 'pack', label: 'Packs' },
    { id: 'extensions', icon: 'puzzle', label: 'Extensions' },
  ],
  [
    { id: 'sync', icon: 'cloud', label: 'Sync and backup' },
    { id: 'about', icon: 'info', label: 'About' },
  ],
];

export const sections: SectionInfo[] = navGroups.flat();

export const isContentSection = (id: SettingsSectionId | null): id is ContentSectionId => !!id && id.startsWith('content/');
export const contentOf = (id: ContentSectionId) => id.slice('content/'.length) as ContentType;

export type SyncStatus = 'ok' | 'syncing' | 'offline' | 'error';
