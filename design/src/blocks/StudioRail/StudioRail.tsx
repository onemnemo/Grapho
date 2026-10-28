import { Icon } from '@/assets/icons';
import { TOOLS, type ToolKey } from '@/assets/studio';
import styles from './StudioRail.module.css';

export interface StudioRailProps {
  tool: ToolKey;
  onTool: (tool: ToolKey) => void;
  className?: string;
}

/** The eight things to insert, as a narrow rail. The open one has a soft fill. */
export function StudioRail({ tool, onTool, className }: StudioRailProps) {
  return (
    <nav className={[styles.rail, className].filter(Boolean).join(' ')} aria-label="Insert">
      {TOOLS.map(([key, name, icon]) => (
        <button key={key} type="button" className={styles.tool} aria-current={tool === key} title={name} onClick={() => onTool(key)}>
          <Icon name={icon} size={20} />
          <span className={styles.label}>{name}</span>
        </button>
      ))}
    </nav>
  );
}
