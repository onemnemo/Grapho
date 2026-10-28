/* Sample library content for stories: a home library, a big one and a long list of folders. */
import type { CoverPreset, DocKind } from '@/assets/covers';
import type { CoverTileKind } from '@/blocks/CoverTile';
import type { BinEntry, LibFolder, LibItem } from './libraryModel';

type Opts = { ed?: number; cr?: number; folder?: string; star?: boolean; tags?: string[]; notes?: LibItem['notes'] };
const KINDS: Record<string, CoverTileKind> = { nb: 'notebook', pdf: 'pdf', cv: 'canvas' };
const mk = (id: string, k: string, art: string, title: string, pages: number, op: number, o: Opts = {}): LibItem => ({
  id, kind: KINDS[k], cover: (k === 'nb' ? art : 'plain') as CoverPreset, doc: (art === 'hl' ? 'highlight' : art === 'chart' ? 'chart' : 'plain') as DocKind,
  title, pages, op, ed: o.ed ?? op, cr: o.cr ?? op + 480, folder: o.folder ?? null, star: !!o.star, tags: o.tags ?? [], notes: o.notes ?? [],
});

export const TAGS = ['exam', 'lab', 'reading', 'draft', 'group work', 'summary', 'homework', 'formulas', 'revision', 'project', 'semester 1', 'semester 2', 'lecture', 'sketch', 'ideas', 'research', 'essay', 'quiz', 'theory', 'practice', 'solutions', 'cheat sheet', 'presentation', 'journal', 'planning', 'review', 'diagram', 'vocabulary', 'maths', 'physics', 'biology', 'history'];

/** A few things in Recently deleted. */
export const homeDeleted = (): BinEntry[] => [
  { id: 'old-chem', items: [mk('old-chem', 'nb', 'chem', 'Chemistry draft', 12, 300)], folders: [], left: 28 },
  { id: 'f:trip', folders: [{ id: 'trip', colour: 'sky', name: 'Trip planning' }], items: [mk('trip1', 'nb', 'plain', 'Packing list', 3, 400, { folder: 'trip' }), mk('trip2', 'pdf', 'plain', 'Tickets', 2, 400, { folder: 'trip' })], left: 19 },
  { id: 'scan4', items: [mk('scan4', 'pdf', 'hl', 'Scan 4', 1, 600)], folders: [], left: 3 },
];

export const homeItems = (): LibItem[] => [
  mk('calc', 'nb', 'calculus', 'Calculus', 32, 0.6, { star: true, tags: ['exam'], notes: [{ p: 9, t: 'Partial derivatives of a scalar field' }] }),
  mk('lec6', 'pdf', 'chart', 'Lecture 6 slides', 18, 2.5, { tags: ['reading'] }),
  mk('mood', 'cv', '', 'Mood board', 0, 4),
  mk('bio', 'nb', 'biology', 'Biology', 24, 6, { tags: ['exam', 'lab'], notes: [{ p: 6, t: 'Physical properties of water and why they matter' }] }),
  mk('ws3', 'pdf', 'hl', 'Worksheet 3', 4, 20),
  mk('phys', 'nb', 'physics', 'Physics', 61, 27, { tags: ['exam'], notes: [{ p: 3, t: 'Physics of motion: force is a vector with direction and size' }] }),
  mk('chem', 'nb', 'chem', 'Chemistry', 18, 50, { tags: ['lab'] }),
  mk('sk', 'nb', 'sketches', 'Sketches', 9, 74, { star: true }),
  mk('spa', 'nb', 'spanish', 'Spanish', 27, 100),
  mk('hist', 'nb', 'history', 'Art history', 14, 200, { tags: ['reading'] }),
  mk('gar', 'nb', 'moss', 'Garden plan', 6, 400, { tags: ['draft'] }),
  mk('map', 'cv', '', 'Project map', 0, 600, { tags: ['group work'] }),
  mk('la', 'nb', 'journal', 'Linear algebra', 40, 30, { folder: 'sem1', tags: ['exam'] }),
  mk('vc', 'nb', 'dusk', 'Vector calculus', 36, 52, { folder: 'sem1' }),
  mk('v4', 'pdf', 'chart', 'Vectors, ch. 4', 20, 80, { folder: 'sem1', tags: ['reading'] }),
  mk('stat', 'nb', 'history', 'Statistics', 28, 130, { folder: 'sem1' }),
  mk('l5', 'pdf', 'hl', 'Lecture 5 slides', 22, 150, { folder: 'sem1' }),
  mk('pn', 'nb', 'plain', 'Study plan', 4, 330, { folder: 'sem1', tags: ['draft'] }),
  mk('opt', 'nb', 'physics', 'Optics', 22, 30, { folder: 'phys', tags: ['exam'] }),
  mk('fm', 'cv', '', 'Formula map', 0, 55, { folder: 'phys' }),
  mk('pe', 'pdf', 'hl', 'Past exam 2024', 12, 100, { folder: 'phys', tags: ['exam'] }),
  mk('mech', 'nb', 'dusk', 'Mechanics notes', 38, 3, { folder: 'phys', star: true, tags: ['exam'] }),
  mk('pfs', 'pdf', 'chart', 'Physics formula sheet', 2, 5, { folder: 'phys' }),
  mk('th', 'nb', 'chem', 'Thermodynamics', 16, 150, { folder: 'phys' }),
  mk('lr2', 'pdf', 'hl', 'Lab report 2', 8, 200, { folder: 'phys', tags: ['lab'] }),
  mk('wv', 'nb', 'journal', 'Waves', 19, 400, { folder: 'phys' }),
  mk('j1', 'nb', 'journal', 'Journal', 80, 12, { folder: 'personal' }), mk('j2', 'nb', 'dusk', 'Goals', 10, 90, { folder: 'personal' }), mk('j3', 'cv', '', 'Trip ideas', 0, 300, { folder: 'personal' }),
  mk('a1', 'nb', 'sketches', 'Doodles', 30, 40, { folder: 'art' }), mk('a2', 'nb', 'history', 'Colour theory', 12, 120, { folder: 'art' }), mk('a3', 'pdf', 'hl', 'Brief', 3, 130, { folder: 'art' }),
  mk('r1', 'pdf', 'chart', 'Reading 1', 14, 70, { folder: 'readings' }), mk('r2', 'pdf', 'hl', 'Reading 2', 16, 71, { folder: 'readings' }), mk('r3', 'pdf', 'chart', 'Reading 3', 11, 72, { folder: 'readings' }),
  mk('p1', 'nb', 'chem', 'Project notes', 20, 55, { folder: 'projects' }), mk('p2', 'cv', '', 'Roadmap', 0, 260, { folder: 'projects' }),
  mk('sp2', 'nb', 'spanish', 'Spanish II', 12, 60, { folder: 'lang' }), mk('fr', 'nb', 'sketches', 'French', 9, 210, { folder: 'lang' }),
];

export const homeFolders = (): LibFolder[] => [
  { id: 'sem1', colour: 'indigo', name: 'Semester 1', pin: true }, { id: 'personal', colour: 'rose', name: 'Personal' },
  { id: 'art', colour: 'ember', name: 'Art class' }, { id: 'readings', colour: 'forest', name: 'Readings' },
  { id: 'projects', colour: 'sea', name: 'Projects' }, { id: 'lang', colour: 'gold', name: 'Languages' },
  { id: 'phys', colour: 'sky', name: 'Physics', parent: 'sem1' }, { id: 'maths', colour: 'sea', name: 'Maths', parent: 'sem1' },
];

const FOLDER_NAMES = 'Algebra;Algorithms;Anatomy;Archive;Art class;Astronomy;Biology;Botany;Brainstorms;Calculus;Chemistry;Circuits;Cooking;Data structures;Design;Drafts;Economics;Electronics;Essays;Ethics;Exams;Finance;French;Garden;Genetics;Geography;Geology;German;Grammar;History;Ideas;Journal;Labs;Languages;Lectures;Linear algebra;Literature;Logic;Maths;Mechanics;Meetings;Music;Neuroscience;Optics;Painting;Personal;Philosophy;Physics;Poetry;Projects;Psychology;Readings;Semester 1;Semester 2;Sketches;Sociology;Spanish;Statistics;Thermodynamics;Travel;Waves;Work'.split(';');

/** 62 folders with fixed counts and dates, for the All folders screen. */
export const manyFolders = (): LibFolder[] => {
  const cols = ['indigo', 'rose', 'ember', 'forest', 'sea', 'gold', 'sky', 'slate'] as const;
  let s = 11;
  const rnd = () => (s = (s * 16807) % 2147483647) / 2147483647;
  return FOLDER_NAMES.map((n, i) => ({
    id: `mf${i}`, colour: cols[Math.floor(rnd() * cols.length)], name: n, pin: ['Semester 1', 'Personal', 'Physics'].includes(n),
    n: n === 'Archive' || n === 'Drafts' ? 0 : 1 + Math.floor(rnd() * 24), ed: Math.round(rnd() * 900) + 1,
  }));
};

const BIG = 'Anatomy;Algebra notes;Astronomy;Atlas, ch. 3:pdf;Art history;Biochem lab;Biology;Botany;Brainstorm:cv;Calculus;Calculus II;Chem formulas:pdf;Chemistry;Circuits;Class map:cv;Cooking;Cell biology;Data structures;Design sketches;Doodles;Economics;Electronics;Essay drafts;Ethics;Exam plan:cv;Finance basics:pdf;French;Garden plan;Genetics;Geography;Geology;German;Grammar guide:pdf;History;Ideas:cv;Journal;Kinematics;Lab report 1:pdf;Lab report 2:pdf;Lecture 1 slides:pdf;Lecture 2 slides:pdf;Lecture 3 slides:pdf;Lecture 4 slides:pdf;Lecture 5 slides:pdf;Lecture 6 slides:pdf;Linear algebra;Literature;Logic;Mechanics;Meeting notes;Mood board:cv;Music theory;Neuroscience;Optics;Organic chemistry;Painting;Past exam 2024:pdf;Philosophy;Physics;Poetry;Project map:cv;Psychology;Reading list:pdf;Roadmap:cv;Sketches;Sociology;Spanish;Statistics;Study plan;Thermodynamics;Travel;Trigonometry;Vector calculus;Vectors, ch. 4:pdf;Waves;Worksheet 3:pdf;Writing'.split(';');

/** Around eighty notes, spread over the folders, for the big library. */
export const bigItems = (): LibItem[] => {
  const arts = ['calculus', 'biology', 'physics', 'journal', 'chem', 'sketches', 'spanish', 'history', 'moss', 'dusk', 'plain'];
  let s = 7;
  const rnd = () => (s = (s * 16807) % 2147483647) / 2147483647;
  const homes = ['sem1', 'personal', 'art', 'readings', 'projects', 'lang', 'work', 'arch', 'garden'];
  return BIG.map((n, i) => {
    const [title, k] = n.split(':');
    const kind = k || 'nb';
    const op = Math.round(rnd() * 900 + 1);
    const art = kind === 'pdf' ? (i % 2 ? 'chart' : 'hl') : kind === 'cv' ? '' : arts[Math.floor(rnd() * arts.length)];
    const tag = () => TAGS[Math.floor(rnd() * rnd() * TAGS.length)];
    const tags = rnd() < 0.12 ? [] : [...new Set([tag(), rnd() < 0.4 ? tag() : ''].filter(Boolean))];
    return mk(`b${i}`, kind, art, title, kind === 'cv' ? 0 : 4 + Math.floor(rnd() * 70), op, { folder: i % 3 ? homes[i % 9] : undefined, ed: op, cr: op + 300 + Math.floor(rnd() * 900), star: rnd() < 0.08, tags });
  });
};

export const bigFolders = (): LibFolder[] => ([
  ...homeFolders().filter((f) => !f.parent),
  { id: 'work', colour: 'sky', name: 'Work' }, { id: 'arch', colour: 'slate', name: 'Archive' }, { id: 'garden', colour: 'forest', name: 'Garden' },
] as LibFolder[]).map((f) => ({ ...f, pin: true }));
