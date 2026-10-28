import type { ButtonHTMLAttributes, HTMLAttributes } from 'react';
import { Icon } from '@/assets/icons';
import { THEME_INPUTS, type ThemeId, type ThemeInputs } from '@/assets/themes';
import { cx } from '@/lib/cx';
import { themeProps } from '@/lib/theme';
import { useT } from '@/i18n';
import styles from './ThemeTile.module.css';

export interface ThemePreviewProps extends HTMLAttributes<HTMLDivElement> {
  theme: ThemeId;
  /** The four colours of a custom theme. Ignored for the built-in themes. */
  inputs?: ThemeInputs;
}

/** A small picture of the Library drawn with the real tokens of a theme, so it can never drift from the app.
 *  It scales with its width (everything is in container units). */
export function ThemePreview({ theme, inputs = THEME_INPUTS.ink, className, ...rest }: ThemePreviewProps) {
  const t = useT();
  const attrs = themeProps({ id: theme, custom: inputs });
  return (
    <div className={cx(styles.frame, className)} aria-hidden="true" {...attrs} {...rest}>
      <div className={styles.screen}>
        <div className={styles.head}>
          <b className={styles.title}>{t('theme.previewTitle')}</b>
          <span className={styles.search}><Icon name="search" size={14} />{t('theme.previewSearch')}</span>
          <span className={styles.new}>{t('theme.previewNew')}</span>
        </div>
        <div className={styles.folders}>
          <i className={styles.folder} data-tone="indigo" />
          <i className={styles.folder} data-tone="rose" />
          <i className={styles.folder} data-tone="sea" />
        </div>
        <div className={styles.notes}>
          {[0, 1, 2, 3].map((i) => (
            <i key={i} className={styles.note} data-chosen={i === 1 ? '' : undefined}>
              <u /><u /><u />
            </i>
          ))}
        </div>
        <div className={styles.controls}>
          <span className={styles.toggle} />
          <span className={styles.slider}><i /></span>
        </div>
      </div>
    </div>
  );
}

export interface ThemeTileProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> {
  theme: ThemeId;
  inputs?: ThemeInputs;
  selected?: boolean;
}

/** One theme to choose: a live mini picture of the app and its name. A radio; put several in a radiogroup. */
export function ThemeTile({ theme, inputs, selected = false, className, type = 'button', ...rest }: ThemeTileProps) {
  const t = useT();
  return (
    <button type={type} role="radio" aria-checked={selected} className={cx(styles.tile, className)} {...rest}>
      <ThemePreview theme={theme} inputs={inputs} />
      <span className={styles.name}>
        {t(`theme.${theme}`)}
        <Icon name="check" size={16} className={styles.tick} />
      </span>
    </button>
  );
}
