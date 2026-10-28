/** Languages a person can pick, each in its own name (`name`) with the English name for orientation.
 *  Only the codes in `LOCALES` have a catalogue; the rest are listed so the picker can be shown at scale. */
export interface Language {
  code: string;
  name: string;
  english: string;
}

export const LANGUAGES: Language[] = [
  { code: 'en', name: 'English', english: 'English' },
  { code: 'nb', name: 'Norsk bokmål', english: 'Norwegian Bokmål' },
  { code: 'fr', name: 'Français', english: 'French' },
  { code: 'de', name: 'Deutsch', english: 'German' },
  { code: 'es', name: 'Español', english: 'Spanish' },
  { code: 'hi', name: 'हिन्दी', english: 'Hindi' },
  { code: 'it', name: 'Italiano', english: 'Italian' },
  { code: 'sv', name: 'Svenska', english: 'Swedish' },
  { code: 'da', name: 'Dansk', english: 'Danish' },
  { code: 'fi', name: 'Suomi', english: 'Finnish' },
  { code: 'nl', name: 'Nederlands', english: 'Dutch' },
  { code: 'pt', name: 'Português', english: 'Portuguese' },
  { code: 'pl', name: 'Polski', english: 'Polish' },
  { code: 'cs', name: 'Čeština', english: 'Czech' },
  { code: 'sk', name: 'Slovenčina', english: 'Slovak' },
  { code: 'hu', name: 'Magyar', english: 'Hungarian' },
  { code: 'ro', name: 'Română', english: 'Romanian' },
  { code: 'bg', name: 'Български', english: 'Bulgarian' },
  { code: 'el', name: 'Ελληνικά', english: 'Greek' },
  { code: 'tr', name: 'Türkçe', english: 'Turkish' },
  { code: 'ru', name: 'Русский', english: 'Russian' },
  { code: 'uk', name: 'Українська', english: 'Ukrainian' },
  { code: 'ja', name: '日本語', english: 'Japanese' },
  { code: 'ko', name: '한국어', english: 'Korean' },
  { code: 'zh', name: '简体中文', english: 'Chinese (Simplified)' },
  { code: 'ar', name: 'العربية', english: 'Arabic' },
  { code: 'he', name: 'עברית', english: 'Hebrew' },
  { code: 'th', name: 'ไทย', english: 'Thai' },
  { code: 'vi', name: 'Tiếng Việt', english: 'Vietnamese' },
  { code: 'id', name: 'Bahasa Indonesia', english: 'Indonesian' },
  { code: 'ms', name: 'Bahasa Melayu', english: 'Malay' },
  { code: 'bn', name: 'বাংলা', english: 'Bengali' },
  { code: 'ta', name: 'தமிழ்', english: 'Tamil' },
  { code: 'te', name: 'తెలుగు', english: 'Telugu' },
  { code: 'mr', name: 'मराठी', english: 'Marathi' },
  { code: 'ca', name: 'Català', english: 'Catalan' },
  { code: 'is', name: 'Íslenska', english: 'Icelandic' },
  { code: 'et', name: 'Eesti', english: 'Estonian' },
  { code: 'hr', name: 'Hrvatski', english: 'Croatian' },
  { code: 'sl', name: 'Slovenščina', english: 'Slovenian' },
];
