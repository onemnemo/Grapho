import { ActionRow } from '@/components/ActionRow';
import { FieldRow } from '@/components/FieldRow';
import { Toggle } from '@/components/Toggle';
import type { ThemeChoice } from '@/lib/theme';
import { ThemeEditor } from '../../ThemeEditor';
import { SectionHead } from './SectionHead';
import type { SectionProps } from './ToolbarSection';
import styles from './sections.module.css';

export interface AppearanceSectionProps extends SectionProps {
  theme: ThemeChoice;
  onTheme: (choice: ThemeChoice) => void;
  /** Open Discover on themes. */
  onMoreThemes?: () => void;
}

/** Themes: pick one or make your own, choose whether it follows the device's light and dark mode, and find more. */
export function AppearanceSection({ theme, onTheme, values: v, onChange, onMoreThemes }: AppearanceSectionProps) {
  return (
    <>
      <SectionHead>Theme</SectionHead>
      <ThemeEditor value={theme} onChange={onTheme} />
      <div className={`${styles.rows} ${styles.below}`}>
        <FieldRow plain label="Match system" hint="Follows your set device theme">
          <Toggle variant="marked" aria-label="Match system" checked={v.matchDevice} onChange={(matchDevice) => onChange({ matchDevice })} />
        </FieldRow>
        <ActionRow icon="grid" onClick={onMoreThemes}>Discover themes</ActionRow>
      </div>
    </>
  );
}
