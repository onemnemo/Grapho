import { useState } from 'react';
import { DEVICE, LanguagePicker } from '../../LanguagePicker';
import { LANGUAGES, LOCALES, useLocale, type LocaleCode } from '@/i18n';
import styles from './sections.module.css';

/** Only the languages Grapho is translated into, so every row works. */
const TRANSLATED = LANGUAGES.filter((l) => LOCALES.includes(l.code as LocaleCode));
const DEVICE_LANGUAGE: LocaleCode = 'nb';

/** App language. Choosing one switches the app at once. */
export function LanguageSection() {
  const { locale, setLocale } = useLocale();
  const [value, setValue] = useState<string>(locale === DEVICE_LANGUAGE ? DEVICE : locale);
  const choose = (code: string) => {
    setValue(code);
    setLocale((code === DEVICE ? DEVICE_LANGUAGE : code) as LocaleCode);
  };
  return (
    <div className={styles.languages}>
      <LanguagePicker languages={TRANSLATED} searchAbove={0} value={value} onChange={choose} deviceLanguage={DEVICE_LANGUAGE} />
    </div>
  );
}
