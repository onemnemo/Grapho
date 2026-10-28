import { DOODLES, TAPES, pixSVG } from './stickers';
import { L, covFull, froFull, tT, tShape, tCard, tIcon, tTex, tPat, tSt, tPx, tVid, dCover, dFolder } from './layers';
import type { Design } from './types';

/** Eight covers and eight folders, all built from the same few tools. */
export function buildTemplates(): { covers: Design[]; folders: Design[] } {
  const covers: Design[] = [
    dCover('Calculus', { c1: '#F2C14E' }, [
      tShape('rect', 150, 262, 300, 6, '#17181C', { name: 'Rule' }),
      tShape('circle', 92, 322, 120, 120, '#E0402E'),
      tShape('rect', 210, 322, 96, 96, '#3B3470'),
      tCard('label', 150, 74, { text: 'Calculus', r: -2, sh: 1 }),
      tTex('grain', covFull(), { op: 0.5 }),
    ]),
    dCover('Biology', { c1: '#B7A5F2', c2: '#9C86EA', ang: 170 }, [
      tPat('dots', covFull(), '#ffffff', { op: 0.38, scale: 1.3 }),
      tPx('wave', 150, 232, 11),
      tSt(pixSVG('star', '#F6C453'), 70, 330, 40, { cut: 0, sh: 0, r: -8 }),
      tSt(pixSVG('heart', '#E0402E'), 236, 316, 34, { cut: 0, sh: 0, r: 10 }),
      tCard('label', 150, 74, { text: 'Biology', r: 2, sh: 1 }),
      tTex('grain', covFull(), { op: 0.45 }),
    ]),
    dCover('Physics', { c1: '#27275A' }, [
      tPat('grid', covFull(), '#3d3d82', { scale: 1.3 }),
      tIcon('atom', 150, 236, 200, '#F28A2E', { sw: 1.1 }),
      tIcon('atom', 150, 236, 200, '#D9487A', { sw: 1.1, r: 90 }),
      tCard('label', 150, 74, { text: 'Physics', r: -2, sh: 1 }),
    ]),
    dCover('Journal', { c1: '#5B1F2B' }, [
      tTex('leather', covFull(), { op: 1 }),
      tShape('rect', 150, 200, 246, 346, 'none', { stroke: '#D9B870', sw: 1.5, radius: 6 }),
      tT('Journal', 150, 176, 220, { size: 52, font: 'serif', weight: 400, colour: '#E7C98A', ls: 0.01, sh: 1 }),
      tShape('rect', 150, 236, 70, 1.5, '#D9B870'),
      tT('2026', 150, 262, 200, { size: 15, font: 'mono', colour: '#D9B870', ls: 0.35, weight: 500 }),
      tIcon('star', 150, 318, 30, '#D9B870', { sw: 1.5 }),
    ], { strap: '#D9B870' }),
    dCover('Spanish', { c1: '#E9873A' }, [
      tPat('scallop', covFull(), '#ffffff', { op: 0.34, scale: 1.4 }),
      tShape('circle', 150, 190, 176, 176, '#FFFFFF', { sh: 2 }),
      tT('Español', 150, 172, 160, { size: 44, font: 'serif', weight: 400, colour: '#B8431A' }),
      tT('SPANISH', 150, 220, 160, { size: 13, font: 'mono', colour: '#B8431A', ls: 0.3, weight: 500 }),
      tSt(DOODLES.sun, 232, 76, 72, { r: 12 }),
      tCard('ribbon', 142, 330, { text: 'Lección 4', colour: '#17181C', ink: '#fff', w: 170, r: -3, sh: 1 }),
    ]),
    dCover('Chemistry', { c1: '#10302f' }, [
      L('paint', { key: 'sea', ...covFull() }),
      tIcon('flask', 150, 196, 150, '#FFFFFF', { sw: 1.1 }),
      tSt(TAPES.tape2, 150, 46, 140, { r: -4, cut: 0, sh: 1 }),
      tSt(DOODLES.sparkle, 226, 128, 34, { r: 10 }),
      tSt(DOODLES.sparkle, 78, 112, 24, { r: -8 }),
      tT('CHEMISTRY', 150, 322, 250, { size: 24, font: 'mono', weight: 500, colour: '#fff', ls: 0.14 }),
      tT('Periodic table and bonds', 150, 352, 230, { size: 12, font: 'mono', colour: 'rgba(255,255,255,.72)', ls: 0.04, weight: 400 }),
      tTex('grain', covFull(), { op: 0.4 }),
    ]),
    dCover('Sketches', { c1: '#B98B58' }, [
      tTex('kraft', covFull(), { op: 1 }),
      tCard('polaroid', 150, 188, { text: 'Summer 24', w: 190, h: 228, r: -4, sh: 2, photo: 'hills' }),
      tSt(TAPES.tape1, 150, 82, 104, { r: -4, cut: 0, sh: 1 }),
      tT('Sketches', 150, 332, 250, { size: 62, font: 'hand', weight: 700, colour: '#3a2410', r: -3 }),
      tSt(DOODLES.heart, 244, 296, 44, { r: 14 }),
    ]),
    dCover('Night notes', { c1: '#0c1226' }, [
      tVid('aurora', covFull(), 3),
      tSt(DOODLES.moon, 150, 108, 60, { r: -12, cut: 0, sh: 1 }),
      tT('Night notes', 150, 292, 250, { size: 50, font: 'serif', weight: 400, colour: '#fff' }),
      tShape('rect', 150, 330, 44, 1.5, 'rgba(255,255,255,.7)'),
      tT('Lecture series', 150, 352, 220, { size: 13, font: 'mono', colour: 'rgba(255,255,255,.7)', ls: 0.2, caps: true, weight: 400 }),
    ], { strap: false }),
  ];
  const folders: Design[] = [
    dFolder('Semester 1', '14 items', { c1: '#6A61B4', c2: '#51489A' }, { back: '#3B3470', papers: ['#F2C14E', '#27275A', '#B7A5F2'] }, [
      tTex('linen', froFull(), { op: 0.95 }),
      tIcon('cap', 428, 142, 46, 'rgba(255,255,255,.9)'),
    ]),
    dFolder('Personal', '6 items', { c1: '#B04D76', c2: '#93395F' }, { back: '#7A2E50', papers: ['#3E3A6D', '#F5F1E6', '#F2C14E'] }, [
      tTex('felt', froFull(), { op: 1 }),
      tShape('rect', 240, 200, 444, 184, 'none', { stroke: '#F9D6E4', sw: 2.5, dash: true, radius: 20, op: 0.85, name: 'Stitching' }),
      tSt(DOODLES.heart, 408, 96, 66, { r: 12, pop: 'above', sh: 2 }),
    ]),
    dFolder('Art class', '5 items', { c1: '#CC7330', c2: '#B35F22' }, { back: '#8A4A16', papers: ['#E8A2BE', '#F2C14E', '#F5F1E6'] }, [
      L('paint', { key: 'ember', ...froFull(), blend: 'soft-light', op: 0.8 }),
      tIcon('palette', 428, 142, 48, 'rgba(255,255,255,.92)'),
      tPx('lookat', 120, 62, 8, { pop: 'behind' }),
    ]),
    dFolder('Readings', '9 items', { c1: '#3F9A74', c2: '#2F7F5E' }, { back: '#25604A', papers: ['#F5F1E6', '#B7A5F2', '#F2C14E'] }, [
      tPat('stripes', froFull(), '#ffffff', { op: 0.11, scale: 1 }),
      tIcon('book', 428, 142, 46, 'rgba(255,255,255,.92)'),
      tSt(TAPES.tape4, 96, 96, 118, { r: -8, pop: 'above', cut: 0, sh: 1 }),
    ]),
    dFolder('Marble', '11 items', { c1: '#E9E7E3' }, { back: '#BDB9B2', ink: 'dark', papers: ['#F2C14E', '#3E3A6D', '#E8A2BE'] }, [
      tTex('marble', froFull(), { colour: '#ECE9E4' }),
      tShape('rect', 240, 196, 452, 190, 'none', { stroke: '#C9A24E', sw: 1.5, radius: 18, name: 'Gold line' }),
      tIcon('globe', 424, 146, 40, '#9B7A2E', { sw: 1.5 }),
    ]),
    dFolder('Languages', '7 items', { c1: '#D9A933', c2: '#BC8F25' }, { back: '#8A6A1A', ink: 'dark', papers: ['#E9873A', '#F5F1E6', '#5A8CF0'] }, [
      tPat('scallop', froFull(), '#ffffff', { op: 0.3, scale: 1.2 }),
      tIcon('flag', 428, 142, 44, '#3a2c0a'),
    ]),
    dFolder('Night', '3 items', { c1: '#1b1936' }, { back: '#141a33', papers: ['#5C8DF0', '#B9A6F0', '#F5F1E6'] }, [
      tVid('aurora', froFull(), 4),
      tIcon('moon', 428, 142, 44, 'rgba(255,255,255,.92)'),
      tSt(DOODLES.sparkle, 98, 96, 34, { pop: 'above', r: 10 }),
    ]),
    dFolder('Holo', '4 items', { c1: '#6A6D7B' }, { back: '#2A2B33', ink: 'dark', papers: ['#F2C14E', '#E8A2BE', '#B7A5F2'] }, [
      tTex('holo', froFull(), {}),
      tIcon('sparkle', 428, 142, 44, '#17181C', { sw: 1.5 }),
    ]),
  ];
  [...covers, ...folders].forEach((d) => d.layers.forEach((l) => { if ((l.type === 'text' || l.type === 'card') && l.text === d.name) l.bind = 'name'; }));
  return { covers, folders };
}
