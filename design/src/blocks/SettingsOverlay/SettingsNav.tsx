import { Fragment } from 'react';
import { Icon } from '@/assets/icons';
import { contentTypes } from '@/assets/content';
import { cx } from '@/lib/cx';
import { isContentSection, navGroups, type SettingsSectionId } from './settingsTypes';
import styles from './SettingsNav.module.css';

export interface SettingsNavProps {
  open: SettingsSectionId | null;
  phone: boolean;
  onSelect: (id: SettingsSectionId) => void;
}

/** The section list: an icon and a name per row, nothing underneath. Content opens into its seven types. */
export function SettingsNav({ open, phone, onSelect }: SettingsNavProps) {
  const inContent = open === 'content' || isContentSection(open);
  return (
    <nav className={styles.nav} aria-label="Settings sections">
      {navGroups.map((group, g) => (
        <div key={g} className={styles.group}>
          {group.map((s) => {
            const here = open === s.id || (s.id === 'content' && inContent && !phone);
            return (
              <Fragment key={s.id}>
                <button type="button" className={cx(styles.item, here && styles.on)} aria-current={open === s.id ? 'page' : undefined} onClick={() => onSelect(s.id)}>
                  <Icon name={s.icon} size={20} />
                  <span>{s.label}</span>
                  {phone && <Icon name="chevron_right" size={18} className={styles.go} />}
                </button>
                {s.id === 'content' && inContent && !phone && (
                  <div className={styles.sub}>
                    {contentTypes.map((t) => (
                      <button key={t.id} type="button" className={cx(styles.item, styles.child, open === `content/${t.id}` && styles.on)} aria-current={open === `content/${t.id}` ? 'page' : undefined} onClick={() => onSelect(`content/${t.id}`)}>
                        <Icon name={t.icon} size={18} />
                        <span>{t.label}</span>
                      </button>
                    ))}
                  </div>
                )}
              </Fragment>
            );
          })}
        </div>
      ))}
    </nav>
  );
}
