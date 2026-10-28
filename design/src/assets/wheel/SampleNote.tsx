/* A page of handwriting for stories: printed ink behind the wheel, not erasable. Artwork. */
import type { CSSProperties } from 'react';

export function SampleNote({ width, height, style }: { width: number; height: number; style?: CSSProperties }) {
  return (
    <svg
      width={width}
      height={height}
      viewBox="380 90 900 700"
      preserveAspectRatio="xMaxYMid slice"
      aria-hidden="true"
      style={{ position: 'absolute', left: 0, top: 0, pointerEvents: 'none', fontFamily: 'var(--hand)', fontWeight: 700, ...style }}
    >
      <g fill="#F5F5F4"><text x="430" y="150" fontSize="58" transform="rotate(-1.2 430 150)">Adding two vectors</text></g>
      <path d="M432 168c120 8 300 -4 470 4" stroke="#F5F5F4" strokeWidth="3" fill="none" strokeLinecap="round" />
      <g fill="#B9A6F0"><text x="430" y="238" fontSize="42" transform="rotate(-.6 430 238)">Geometric view</text></g>
      <path d="M442 300c60-34 330-44 440-18 86 22 70 84-40 100-120 18-330 14-400-2-70-16-56-62 0-80z" stroke="#F28A2E" strokeWidth="3" fill="none" strokeLinecap="round" />
      <g fill="#F28A2E"><text x="470" y="352" fontSize="44" transform="rotate(-.8 470 352)">a · b = |a| |b| cos θ</text></g>
      <g fill="none" stroke="#FF5A4A" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
        <path d="M520 590 L742 452" /><path d="M718 450 L744 450 L735 474" /><path d="M520 590 L780 590" /><path d="M758 580 L782 590 L758 600" />
        <path d="M590 590 c-2-18 6-34 20-42" />
      </g>
      <g fill="#FF5A4A"><text x="600" y="496" fontSize="40">a</text><text x="640" y="640" fontSize="40">b</text><text x="612" y="580" fontSize="32">θ</text></g>
      <g fill="#5C8DF0"><text x="860" y="480" fontSize="40" transform="rotate(-.7 860 480)">cos θ = </text><text x="1006" y="462" fontSize="38">adj</text><text x="1006" y="508" fontSize="38">hyp</text></g>
      <path d="M1000 476 h76" stroke="#5C8DF0" strokeWidth="3" strokeLinecap="round" />
      <g fill="#F5F5F4"><text x="860" y="590" fontSize="36" transform="rotate(-.5 860 590)">Which way do they point?</text></g>
      <g fill="#3FB27A"><text x="440" y="716" fontSize="38" transform="rotate(-.5 440 716)">x₁x₂ + y₁y₂ = |a| |b| cos θ</text></g>
      <g fill="#F5F5F4"><text x="990" y="716" fontSize="36">Dot product</text></g>
    </svg>
  );
}
