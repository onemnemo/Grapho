import { useEffect, useRef } from 'react';
import { Mascot, SPRITE_H, SPRITE_W, type AnimName } from './mascot';

export interface MascotSpriteProps {
  /** Animation to play; looping ones loop, one-shots hold their last frame. */
  anim?: AnimName;
  /** Whole-pixel scale. 5 is the empty-state size. */
  scale?: number;
  /** Same seed, same shimmer. */
  seed?: number;
  className?: string;
}

/** The pixel mascot. It has no name, and appears only in brand moments such as empty states. */
export function MascotSprite({ anim = 'idle', scale = 5, seed = 1, className }: MascotSpriteProps) {
  const ref = useRef<HTMLCanvasElement>(null);
  const mascot = useRef<Mascot | null>(null);

  useEffect(() => {
    if (!ref.current) return;
    mascot.current = new Mascot(ref.current, seed);
    return () => mascot.current?.destroy();
  }, [seed]);

  useEffect(() => {
    mascot.current?.play(anim);
  }, [anim, seed]);

  return (
    <canvas
      ref={ref}
      className={className}
      aria-hidden
      style={{ width: SPRITE_W * scale, height: SPRITE_H * scale, imageRendering: 'pixelated' }}
    />
  );
}
