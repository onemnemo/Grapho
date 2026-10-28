import { Icon, type IconName } from '@/assets/icons';
import { ActionRow } from '@/components/ActionRow';
import { Button } from '@/components/Button';
import { FieldRow } from '@/components/FieldRow';
import { Toggle } from '@/components/Toggle';
import type { SettingsValues, SyncStatus } from '../settingsTypes';
import { SectionHead } from './SectionHead';
import styles from './SyncSection.module.css';
import rows from './sections.module.css';

export interface SyncSectionProps {
  values: SettingsValues;
  onChange: (patch: Partial<SettingsValues>) => void;
  status: SyncStatus;
  /** "2 minutes ago" */
  last: string;
  lastBackup?: string;
  onSyncNow?: () => void;
  onBackUp?: () => void;
  onRestore?: () => void;
}

const LINE: Record<SyncStatus, [IconName, (last: string) => string]> = {
  ok: ['cloud_check', (last) => `Up to date. Last synced ${last}.`],
  syncing: ['sync', () => 'Syncing'],
  offline: ['cloud_off', (last) => `Offline. Last synced ${last}.`],
  error: ['alert_circle', () => 'Could not sync. Try again.'],
};

/** Sync and backup, quietly: one line for the state, a manual action beside it, and the few switches that matter. */
export function SyncSection({ values: v, onChange, status, last, lastBackup = 'Yesterday, 21:40', onSyncNow, onBackUp, onRestore }: SyncSectionProps) {
  const [icon, text] = LINE[status];
  return (
    <>
      <div className={styles.status} data-status={status} role="status">
        <Icon name={icon} size={20} className={status === 'syncing' ? styles.spin : undefined} />
        <span>{text(last)}</span>
        <Button variant="secondary" size="sm" disabled={status === 'syncing' || status === 'offline'} onClick={onSyncNow}>Sync now</Button>
      </div>

      <SectionHead>Sync</SectionHead>
      <div className={rows.rows}>
        <FieldRow plain label="Sync automatically">
          <Toggle variant="marked" aria-label="Sync automatically" checked={v.syncOn} onChange={(syncOn) => onChange({ syncOn })} />
        </FieldRow>
        <FieldRow plain label="Only on Wi-Fi">
          <Toggle variant="marked" aria-label="Only on Wi-Fi" checked={v.syncWifiOnly} disabled={!v.syncOn} onChange={(syncWifiOnly) => onChange({ syncWifiOnly })} />
        </FieldRow>
      </div>

      <SectionHead>Backup</SectionHead>
      <div className={rows.rows}>
        <ActionRow icon="upload" value={lastBackup} onClick={onBackUp}>Back up now</ActionRow>
        <ActionRow icon="restore" onClick={onRestore}>Restore from a backup</ActionRow>
      </div>
    </>
  );
}
