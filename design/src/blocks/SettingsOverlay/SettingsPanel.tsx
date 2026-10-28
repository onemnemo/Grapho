import { useState, type HTMLAttributes } from 'react';
import { IconButton } from '@/components/IconButton';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import { ContentPage } from '../ContentPage';
import { ExtensionsPage } from '../ExtensionsPage';
import { PacksPage } from '../PacksPage';
import { AboutSection } from './sections/AboutSection';
import { AppearanceSection } from './sections/AppearanceSection';
import { ContentIndexSection } from './sections/ContentIndexSection';
import { GeneralSection } from './sections/GeneralSection';
import { LanguageSection } from './sections/LanguageSection';
import { PenSection } from './sections/PenSection';
import { SyncSection } from './sections/SyncSection';
import { SettingsNav } from './SettingsNav';
import { contentOf, isContentSection, sections, type SettingsSectionId } from './settingsTypes';
import { contentType } from '@/assets/content';
import type { SettingsState } from './useSettings';
import { cx } from '@/lib/cx';
import styles from './SettingsPanel.module.css';

export interface SettingsPanelProps extends HTMLAttributes<HTMLDivElement> {
  /** tablet: a section list on the left and the section on the right. phone: one stacked list, a section opens full width with a back arrow. */
  variant?: 'tablet' | 'phone';
  /** The open section. On a phone, null shows the list. */
  section: SettingsSectionId | null;
  onSectionChange: (id: SettingsSectionId | null) => void;
  /** The state from useSettings. */
  settings: SettingsState;
  /** Draws its own card, with a "Settings" title bar. Off when a Sheet around it supplies those. */
  embedded?: boolean;
  /** Phone: shows a close button in the title bar. */
  onClose?: () => void;
  onEditTools?: () => void;
  /** Where menus open: the window, or the nearest framed container. */
  placement?: OverlayPlacement;
}

/** Every customisation in one place: sections on the left and the open page beside it (or stacked, on a phone). */
export function SettingsPanel({ variant = 'tablet', section, onSectionChange, settings: s, embedded = false, onClose, onEditTools, placement = 'viewport', className, ...rest }: SettingsPanelProps) {
  const phone = variant === 'phone';
  const open = section ?? (phone ? null : 'general');
  const info = isContentSection(open) ? { label: contentType(contentOf(open)).label } : sections.find((x) => x.id === open);
  const [deep, setDeep] = useState<string | null>(null);
  const sub = { values: s.values, onChange: s.change, phone };
  const showTop = phone || embedded;
  const select = (id: SettingsSectionId | null) => { setDeep(null); onSectionChange(id); };
  const boundary = placement;
  const contentType_ = isContentSection(open) ? contentOf(open) : null;
  // a pack or extension page has its own heading, so the section title steps aside
  const showTitle = info && !phone && !(deep && (open === 'packs' || open === 'extensions'));

  return (
    <div
      className={cx(styles.set, phone && styles.phone, embedded && !phone && styles.card, showTop && styles.hasTop, className)}
      data-view={open ? 'detail' : 'list'}
      {...rest}
    >
      {showTop && (
        <div className={styles.top}>
          {phone && open && <IconButton icon="back" label="Back to settings" iconSize={20} onClick={() => select(contentType_ ? 'content' : null)} />}
          <h3>{phone && info ? info.label : 'Settings'}</h3>
          {phone && onClose && <IconButton icon="close" label="Close" iconSize={20} onClick={onClose} />}
        </div>
      )}
      <div className={styles.navCol}><SettingsNav open={open} phone={phone} onSelect={select} /></div>
      <div className={styles.main} key={open ?? 'list'}>
        <div className={styles.page}>
          {showTitle && <h2 className={styles.h}>{info.label}</h2>}
          {open === 'general' && <GeneralSection {...sub} onEditTools={onEditTools} />}
          {open === 'appearance' && <AppearanceSection {...sub} theme={s.theme} onTheme={s.chooseTheme} onMoreThemes={() => s.openBrowse('themes')} />}
          {open === 'language' && <LanguageSection />}
          {open === 'pen' && <PenSection {...sub} />}
          {open === 'content' && <ContentIndexSection items={s.visibleContent} onOpen={(t) => select(`content/${t}`)} />}
          {contentType_ && (
            <ContentPage
              key={contentType_} type={contentType_} items={s.content} sources={s.sources} phone={phone} boundary={boundary}
              onExportAsPack={(ids) => s.openNewPack(ids)} onImport={s.openImport} onDiscover={s.openBrowse}
            />
          )}
          {open === 'packs' && (
            <PacksPage
              packs={s.packs} items={s.content} phone={phone} boundary={boundary} onDetail={setDeep}
              onToggle={s.togglePack} onPackAction={s.packAction} onNewPack={() => s.openNewPack()} onImport={s.openImport} onBrowse={() => s.openBrowse()}
            />
          )}
          {open === 'extensions' && (
            <ExtensionsPage extensions={s.extensions} onDetail={setDeep} onToggle={s.toggleExtension} onRemove={s.removeExtension} onInstallFile={s.installExtensionFile} onBrowse={() => s.openBrowse('extensions')} />
          )}
          {open === 'sync' && <SyncSection values={s.values} onChange={s.change} status={s.sync.status} last={s.sync.last} onSyncNow={s.syncNow} />}
          {open === 'about' && <AboutSection />}
        </div>
      </div>
    </div>
  );
}
