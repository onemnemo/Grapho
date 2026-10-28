import { Icon } from '@/assets/icons';
import { ActionRow } from '@/components/ActionRow';
import { Toggle } from '@/components/Toggle';
import { groupLabels, type ExtensionItem } from './extensionTypes';
import styles from './ExtensionDetail.module.css';

export interface ExtensionDetailProps {
  extension: ExtensionItem;
  onBack: () => void;
  onToggle: (enabled: boolean) => void;
  onRemove: () => void;
}

/** One extension: what it can reach, an off switch, and Remove unless it ships with the app. */
export function ExtensionDetail({ extension: e, onBack, onToggle, onRemove }: ExtensionDetailProps) {
  return (
    <div className={styles.detail}>
      <button type="button" className={styles.back} onClick={onBack}>
        <Icon name="back" size={18} />
        Extensions
      </button>
      <header className={styles.head}>
        <span className={styles.plate}><Icon name={e.icon} size={28} /></span>
        <div className={styles.title}>
          <h3>{e.name}</h3>
          <small>{groupLabels[e.group]} · Version {e.version}</small>
        </div>
        <Toggle variant="marked" checked={e.enabled} onChange={onToggle} aria-label={`${e.name} on`} />
      </header>
      <p className={styles.about}>{e.description}</p>
      <section>
        <h4>Can access</h4>
        <ul className={styles.access}>
          {e.access.map((a) => (
            <li key={a.label}><Icon name={a.icon} size={20} /><span>{a.label}</span></li>
          ))}
        </ul>
      </section>
      {e.group !== 'built' && (
        <div className={styles.remove}>
          <ActionRow icon="trash" tone="danger" onClick={onRemove}>Remove extension</ActionRow>
        </div>
      )}
    </div>
  );
}
