import { useRef, useState, type InputHTMLAttributes } from 'react';
import { Icon } from '@/assets/icons';
import { cx } from '@/lib/cx';
import styles from './SearchField.module.css';

export interface SearchFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size' | 'onChange' | 'type'> {
  value?: string;
  defaultValue?: string;
  onChange?: (value: string) => void;
  /** md is 44px (library header), sm is 36px (sheets and pickers). */
  size?: 'sm' | 'md';
  /** Width at rest in px. The field widens when focused. */
  width?: number;
  /** Width when focused in px. Defaults to a little wider than at rest. */
  focusWidth?: number;
}

/** One surface, no inner box: icon, text, a clear button, and a tail-gradient line while typing. */
export function SearchField({
  value,
  defaultValue = '',
  onChange,
  size = 'md',
  width = 240,
  focusWidth,
  placeholder = 'Search',
  className,
  style,
  ...rest
}: SearchFieldProps) {
  const [inner, setInner] = useState(defaultValue);
  const text = value ?? inner;
  const input = useRef<HTMLInputElement>(null);
  const set = (v: string) => {
    if (value === undefined) setInner(v);
    onChange?.(v);
  };
  const vars = { '--w': `${width}px`, '--wf': `${focusWidth ?? width + 100}px`, ...style } as React.CSSProperties;
  return (
    <label className={cx(styles.search, size === 'sm' && styles.sm, text && styles.has, className)} style={vars}>
      <Icon name="search" size={18} className={styles.icon} />
      <input
        ref={input}
        type="search"
        value={text}
        placeholder={placeholder}
        aria-label={placeholder}
        autoComplete="off"
        spellCheck={false}
        onChange={(e) => set(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Escape' && text) {
            e.stopPropagation();
            set('');
          }
        }}
        {...rest}
      />
      <button
        type="button"
        className={styles.clear}
        aria-label="Clear search"
        tabIndex={text ? 0 : -1}
        onClick={() => {
          set('');
          input.current?.focus();
        }}
      >
        <Icon name="close" size={14} strokeWidth={2.2} />
      </button>
    </label>
  );
}
