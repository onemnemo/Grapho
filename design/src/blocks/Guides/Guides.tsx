import { Protractor, type ProtractorProps } from './Protractor';
import { Ruler, type RulerProps } from './Ruler';

export interface GuidesProps {
  /** Props for the ruler. Leave out to keep it off the page. */
  ruler?: RulerProps;
  protractor?: ProtractorProps;
}

/** The guides on a page: the ruler and the protractor, each only while it is on. */
export function Guides({ ruler, protractor }: GuidesProps) {
  return (
    <>
      {ruler && <Ruler {...ruler} />}
      {protractor && <Protractor {...protractor} />}
    </>
  );
}
