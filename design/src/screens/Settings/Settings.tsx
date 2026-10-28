import { useState } from 'react';
import { SettingsDialogs, SettingsPanel, useSettings, type SettingsSectionId, type SettingsValues } from '@/blocks/SettingsOverlay';
import { ToastProvider } from '@/components/Toast';
import { DeviceFrame } from '@/components/DeviceFrame';
import { LocaleProvider, useLocale } from '@/i18n';
import styles from './Settings.module.css';

export interface SettingsProps {
  /** tablet: the section list on the left. phone: a stacked list, a section opens with a back arrow. */
  variant?: 'tablet' | 'phone';
  /** The open section. On a phone, leave it out to start on the list. */
  section?: SettingsSectionId | null;
  initial?: Partial<SettingsValues>;
}

function SettingsInner({ variant = 'tablet', section: start, initial }: SettingsProps) {
  const settings = useSettings(initial);
  const phone = variant === 'phone';
  const [section, setSection] = useState<SettingsSectionId | null>(start === undefined ? (phone ? null : 'general') : start);
  const panel = <SettingsPanel variant={variant} embedded={!phone} section={section} onSectionChange={setSection} settings={settings} placement="container" />;
  return (
    <DeviceFrame tone="screen" shape={phone ? 'phone' : 'tablet'} width={phone ? 390 : 1040} height={phone ? 760 : 700}>
      {phone ? panel : <div className={styles.pad}>{panel}</div>}
      <SettingsDialogs settings={settings} placement="container" />
    </DeviceFrame>
  );
}

/** Settings as a screen: general, appearance, language, pen and input, content by type, packs, extensions, sync and backup, about. Everything is live. */
export function Settings(props: SettingsProps) {
  const { locale } = useLocale();
  return (
    <div className={styles.root}>
      <LocaleProvider key={locale} defaultLocale={locale}>
        <ToastProvider placement="container">
          <SettingsInner {...props} />
        </ToastProvider>
      </LocaleProvider>
    </div>
  );
}
