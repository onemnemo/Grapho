import { Icon } from '@/assets/icons';
import { EmptyState } from '@/components/EmptyState';
import { FieldRow } from '@/components/FieldRow';
import styles from './sections.module.css';

export function AboutSection() {
  return (
    <>
      <div className={styles.about}>
        <EmptyState size="sm" anim="wave" title="Grapho">Version 1.4.0, build 214</EmptyState>
      </div>
      {['What is new', 'Licences', 'Privacy', 'Send feedback'].map((l) => (
        <FieldRow plain key={l} label={l} onClick={() => {}}>
          <Icon name="chevron_right" size={18} />
        </FieldRow>
      ))}
    </>
  );
}
