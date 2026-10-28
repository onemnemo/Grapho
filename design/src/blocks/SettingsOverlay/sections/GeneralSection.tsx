import { BatterySection } from './BatterySection';
import { ToolbarSection, type SectionProps } from './ToolbarSection';

export function GeneralSection(props: SectionProps & { onEditTools?: () => void }) {
  return (
    <>
      <ToolbarSection {...props} />
      <BatterySection {...props} />
    </>
  );
}
