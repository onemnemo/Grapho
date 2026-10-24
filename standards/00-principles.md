# Engineering principles

The rules in the other files follow from these. When a rule and a principle appear to
disagree, the principle wins and the rule is wrong; say so and fix the rule.

## 1. Build for the Grapho that exists in three years

Design for the product's goal, not for today's feature list.

- Grapho is meant to hold notebooks with hundreds of pages and millions of ink points, drawn
  on Android, Windows and Apple devices from one engine. A design that works for one short
  page on one tablet is a description of the starting point, not the target.
- Ask what the surface is trying to become before concluding that a piece of engineering is
  unnecessary.
- Prefer the shape that keeps growing over the shape that is smallest today. A table of tools
  or pattern kinds that new entries are appended to beats a branch repeated in six places.

## 2. No temporary solutions

A stopgap that ships becomes the architecture. If a shortcut is genuinely unavoidable, it is
not finished until all three of these are true:

1. It is isolated behind one seam, so replacing it touches one file.
2. A comment at that seam says what it is standing in for and what would replace it.
3. The follow-up is filed as an issue, with what is wrong, why it matters, and what a fix
   involves.

Never leave a to-do, fix-me or hack marker in the tree. An untracked marker is a promise
nobody can find.

Corollaries:

- Do not write code whose purpose is to be replaced next sprint, unless it is a spike, in
  which case it is deleted rather than merged.
- Do not paper over a data problem in the view. Fix it where the data is produced or
  normalized, not where it is rendered.
- Do not add a compatibility shim without an owner and a removal condition.
- Existing hard-coded strings and similar leftovers are being migrated. Never add to them, and
  fix the ones in code you touch.

## 3. Modular by default

A reader should be able to swap one file without reading its neighbours.

- Small, focused modules with clear seams. A file passing roughly 400 lines because of your
  change is a signal to split it, not a reason to keep appending.
- Keep engine logic free of any UI or platform framework so it runs unchanged on every shell.
- One definition, many consumers. When the same list, menu, or rule appears in two places,
  export one builder and have both call it.
- Name and structure so a later engineer can replace an implementation without touching call
  sites.

## 4. Efficiency is a feature, and it is measured

Ink latency and frame time are the headline differentiator, not a polish item. That does not
license premature optimization; it means the cost of a design is part of choosing it.

- Choose the algorithm and the data shape with scale in mind, before writing the code.
- Stroke building runs once per input sample. Keep that path free of needless allocation.
- Optimize an existing path only after profiling it.
- Every performance claim needs a proof of correct output taken in the same run that produced
  the numbers. See `05-testing-and-verification.md`.
- Cache when the cost is real and the invalidation is understood.

## 5. Own what defines the product

Reach for a dependency when the problem is solved, boring, and not ours. Build it ourselves
when the thing is part of what makes Grapho good.

Build it ourselves when:

- The behaviour is a differentiator (stroke modelling, the stabiliser, outlines, the ink
  renderer, the pen wheel).
- The library forces a data model we would have to fight or convert on every call.
- The library is heavy relative to the slice we need.
- The license is incompatible with a permissively licensed open source product, or the
  provenance is unclear.
- We would end up wrapping it so thoroughly that the wrapper is the real implementation.

Take the dependency when:

- It is a well-tested solution to a hard, generic problem (ZIP, JSON, a PDF engine).
- It is a build or test tool rather than shipped behaviour.
- Writing it ourselves would mean reimplementing a specification, not a feature.

Either way, put it behind our own seam so the choice is reversible. Icons go through
`GraphoIcon`, never a direct import of an icon library, so the icon set is one file's decision.

## 6. Fidelity to intent, not to the old implementation

Samsung Notes is the baseline for how the interface works, and its annoyances are the list of
things to fix. Match the intent of what people rely on, not its warts. Never reproduce
something bad because the reference does it.

## 7. One rule everywhere

The same naming, the same layout, the same conventions across every module and both
languages. A contributor who has read one module should be able to predict the shape of the
next one. Follow the existing pattern even when you would have chosen differently, and change
it everywhere at once when it is genuinely wrong.

## 8. Claims need evidence

State what you ran and what it produced. "Tests pass" means you ran them. A latency number
without a correctness proof is void. A conclusion drawn from one arm of a comparison is a
guess wearing a number.
