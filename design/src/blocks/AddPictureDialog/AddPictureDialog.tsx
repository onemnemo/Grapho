import { useEffect, useState } from 'react';
import { ILLUS, outlineFilter } from '@/assets/studio';
import { Button } from '@/components/Button';
import { ChoiceCard } from '@/components/ChoiceCard';
import { Dialog } from '@/components/Dialog';
import type { OverlayPlacement } from '@/lib/OverlayLayer';
import { cutoutURL } from '@/lib/studio';
import { useBackgroundRemover, type StickerResult } from '../BackgroundRemover';
import { useCropper, type Crop } from '../CropDialog';
import styles from './AddPictureDialog.module.css';

export type AddPictureStep = 'choose' | 'crop' | 'remove';

export interface AddPictureDialogProps {
  open: boolean;
  onClose: () => void;
  src?: string;
  /** Words the question uses: "How should it sit on the cover?". */
  kind?: 'cover' | 'folder';
  /** Aspect of the cover or folder front, offered when cropping. */
  frameAspect?: number;
  onAddSticker?: (r: StickerResult) => void;
  onAddImage?: (crop: Crop, aspect: number) => void;
  /** Start on a later step, for stories. */
  initialStep?: AddPictureStep;
  placement?: OverlayPlacement;
}

/** Add a picture: choose Sticker or Image, each shown with what it will look like. An image goes on to a crop, a sticker to the background remover. */
export function AddPictureDialog({ open, onClose, src = ILLUS, kind = 'cover', frameAspect, onAddSticker, onAddImage, initialStep = 'choose', placement = 'viewport' }: AddPictureDialogProps) {
  const [step, setStep] = useState<AddPictureStep>(initialStep);
  const [use, setUse] = useState<'sticker' | 'image'>('sticker');
  const [cut, setCut] = useState<string>('');
  useEffect(() => { let live = true; cutoutURL(src).then((c) => { if (live) setCut(c.url); }); return () => { live = false; }; }, [src]);

  const remover = useBackgroundRemover({ src, onClose, onBack: () => setStep('choose'), onAdd: (r) => { onAddSticker?.(r); onClose(); } });
  const cropper = useCropper({ src, onClose, mode: 'adding', frameLabel: kind === 'cover' ? 'Cover' : 'Front', frameAspect, onBack: () => setStep('choose'), onApply: (c, a) => { onAddImage?.(c, a); onClose(); } });

  let title = 'Add picture';
  let body: React.ReactNode;
  let actions: React.ReactNode;
  if (step === 'remove') ({ title, body, actions } = remover);
  else if (step === 'crop') ({ title, body, actions } = cropper);
  else {
    body = (
      <>
        <p className={styles.lede}>How should it sit on the {kind}?</p>
        <div className={styles.cards}>
          <ChoiceCard
            title="Sticker" selected={use === 'sticker'} onClick={() => setUse('sticker')}
            preview={<span className={styles.surf}><img className={styles.sticker} src={cut || src} alt="" style={{ filter: `${outlineFilter(3)} drop-shadow(0 7px 7px var(--st-sticker-shadow))` }} /></span>}
          />
          <ChoiceCard
            title="Image" selected={use === 'image'} onClick={() => setUse('image')}
            preview={<span className={styles.surf}><span className={styles.frame}><img src={src} alt="" /><i className={styles.k1} /><i className={styles.k2} /><i className={styles.k3} /><i className={styles.k4} /></span></span>}
          />
        </div>
      </>
    );
    actions = (
      <>
        <Button variant="text" onClick={onClose}>Cancel</Button>
        <Button variant="primary" onClick={() => setStep(use === 'image' ? 'crop' : 'remove')}>Continue</Button>
      </>
    );
  }
  return <Dialog open={open} onClose={onClose} placement={placement} width={560} density="compact" title={title} actions={actions}>{body}</Dialog>;
}
