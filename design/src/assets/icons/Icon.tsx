import type { SVGProps } from 'react';
import { iconFills, iconPaths, type IconName } from './iconPaths';

export type IconSize = 14 | 16 | 18 | 20 | 24 | 28;

export interface IconProps extends Omit<SVGProps<SVGSVGElement>, 'name'> {
  name: IconName;
  /** Rendered size in px. UI chrome uses 18 or 20; 24 is the drawing grid. */
  size?: IconSize;
  /** Selected tools fill their body softly. */
  active?: boolean;
  /** Accessible label. Without it the icon is decorative and hidden from assistive tech. */
  label?: string;
}

export function Icon({ name, size = 20, active = false, label, ...rest }: IconProps) {
  const fill = active ? iconFills[name] : undefined;
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.75}
      strokeLinecap="round"
      strokeLinejoin="round"
      role={label ? 'img' : undefined}
      aria-label={label}
      aria-hidden={label ? undefined : true}
      focusable="false"
      {...rest}
    >
      {fill && <path d={fill} fill="currentColor" fillOpacity={0.22} stroke="none" />}
      <path d={iconPaths[name]} />
    </svg>
  );
}
