import { Sheet } from '@/components/Sheet';
import { OverlayLayer, type OverlayPlacement } from '@/lib/OverlayLayer';
import { SettingsPanel, type SettingsPanelProps } from './SettingsPanel';
import styles from './SettingsOverlay.module.css';

export interface SettingsOverlayProps extends Omit<SettingsPanelProps, 'embedded' | 'onClose' | 'placement'> {
  open: boolean;
  onClose: () => void;
  /** viewport covers the window; container fills the nearest positioned parent. */
  placement?: OverlayPlacement;
}

/**
 * Settings over whatever is behind it. On a tablet a centred sheet with the section list on the left; on a phone it fills the screen
 * and stacks, with a back arrow inside a section.
 */
export function SettingsOverlay({ open, onClose, placement = 'viewport', variant = 'tablet', ...panel }: SettingsOverlayProps) {
  if (variant === 'phone') {
    return (
      <OverlayLayer open={open} onClose={onClose} placement={placement}>
        <section role="dialog" aria-modal="true" aria-label="Settings" tabIndex={-1} className={styles.full}>
          <SettingsPanel variant="phone" onClose={onClose} placement={placement} {...panel} />
        </section>
      </OverlayLayer>
    );
  }
  return (
    <Sheet open={open} onClose={onClose} title="Settings" width={1000} flush placement={placement} className={styles.sheet}>
      <SettingsPanel variant="tablet" placement={placement} {...panel} />
    </Sheet>
  );
}
