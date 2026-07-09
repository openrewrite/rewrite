# 9. Recipe execution stages

Date: 2026-07-09

## Status

Proposed (prototype on branch `stages-prototype`)

## Context

A `ScanningRecipe` studies a repository in a scan pass and then transforms it in an edit pass.
Increasingly we have recipes that, having scanned, need to *run other recipes* whose configuration is
only known after scanning — for example a recipe that studies a project's dependencies and then wants
to apply a set of `UpgradeDependencyVersion` / `UpgradeTransitiveDependencyVersion` upgrades, one per
dependency it decided to change, at versions it computed.

Today there is no good way to express that:

- **`getRecipeList()` is static.** A recipe's sub-recipes are declared up front with fixed options.
  You cannot say "run whatever the scan decided," because the targets and versions aren't known at
  recipe-construction time.
- **Driving a sub-recipe's visitor inline drops deferred edits.** The workaround has been to call
  `new SubRecipe(...).getVisitor(acc).visitNonNull(tree, ctx)` from within the outer recipe's edit
  visitor. This runs only that visitor, once, as a nested visitor. Recipes whose real work is
  expressed through `doAfterVisit(...)` — which is drained by the scheduler across the recipe
  lifecycle, not by a bare nested `visit` — silently make no change. Maven's `UpgradeDependencyVersion`
  is the canonical case: parent-POM and `<properties>`-managed versions are edited through deferred
  `ChangePropertyValue` / managed-dependency sub-visitors, so an inline invocation reports the intended
  change but never applies it.
- **Cycles cannot express a data dependency.** The scheduler already re-runs a *fixed* recipe set until
  it stops changing (cycles). But "run set B, where B is a function of set A's scan" is a *sequential
  data dependency*, not a fixpoint over a fixed set. Forcing it onto the cycle axis requires
  manufacturing edits to earn another cycle and threading state by hand.

### A cycle *is* a stage

The insight that resolves this is that "cycle" and "stage" are not two axes — they are the same thing.
A cycle was always just *one more whole pass over the source set* (scan/generate/edit); a stage is
exactly that. So instead of a stage loop wrapping a cycle loop, there is a single axis:

- A **stage** is one pass over the source set. A run is a sequence of stages.
- The next stage is chosen *data-dependently* from what this stage found: its edits and any recipes it
  schedules flow forward into the next pass.

The old cycle behavior — "re-run the fixed set until it stops changing" — is the special case where a
stage re-emits itself. When a recipe previously "caused another cycle," that now *equates to scheduling
the stage root as the next stage*, gated on having made a change. Two framings from the design:

1. A convergent, mutually-interacting recipe set is a *strongly-connected component*; the stage graph
   is its condensation. This suggested (incorrectly) that cycles were an irreducible primitive that
   stages could only sit above.
2. The correction: because the next stage is produced lazily and data-dependently, the stage graph is a
   *lazily-unrolled* loop, and lazy unrolling expresses iteration. A back-cascade is just a stage whose
   next stage is itself — re-emit the interacting set while it keeps changing. Idempotency is the
   natural convergence contract: a stage re-emitted with the same effect makes no change, so nothing is
   re-emitted and the run stops.

The vocabulary we landed on: **a run is a sequence of stages; each stage is one pass over the source
set; convergence is a stage's *self-edge*.** The classic recipe is a single self-looping stage. There
is one bound, `maxStages`, on the total number of passes.

## Decision

Collapse the cycle loop into the stage worklist so there is a single axis, and move the "run again"
decision out of `RecipeScheduler` into the recipe, so the scheduler is aware only of stages.

1. **Convergence is a default `Recipe.nextStage(RecipeList next, ExecutionContext ctx)`.** After a
   stage's pass, each recipe in the tree gets this hook. The default expresses the old cycle rule as a
   self-edge: a recipe that `causesAnotherCycle()` and made a change this pass re-emits the **stage
   root** (read from `ctx.getStageDetails().getRecipe()`), so the whole set runs again. This preserves
   the long-standing contract that *any* recipe requesting another cycle re-runs *all* of them. Because
   several recipes in a composite may each re-emit the same root, `RecipeList.addIfAbsent` deduplicates
   so the root is scheduled once. "Made a change" is read from the live stage via
   `getStageDetails().getMadeChangesInThisCycle()`.

2. **Data-dependent successors are `ScanningRecipe.nextStage(RecipeList next, ExecutionContext ctx, T acc)`.**
   Once a stage converges (nothing re-emitted), a scanning recipe may append recipes configured from its
   now-populated accumulator; they run as the next stage. Adding at least one schedules it.

3. **Rename `RecipeRunCycle` → `RecipeRunStage`** and its `ExecutionContext` accessors
   `getCycleDetails()` → `getStageDetails()`, `putCycle()` → `putStage()`. `getCycle()` keeps its name
   but now returns the stage number — the pass count so far — since a cycle *is* a stage. The rename is
   mechanical and internal to `rewrite-core` plus its RPC request classes.

4. **`RecipeScheduler` is a flat stage worklist that is agnostic to *why* something runs next.**
   `runRecipeStages` drains a queue of recipes seeded with the root; because each iteration polls and
   runs exactly one pass, the drain loop's counter *is* the stage number (no queue-item wrapper needed).
   `runStage` runs one pass, then a single `collectNextStage` walk asks each recipe what runs next and
   collects it into one `next` list — a re-emitted stage root (the cycle) and scanning recipes'
   data-dependent successors land there identically; the scheduler does not distinguish them. Scanning
   accumulators are read via `getMessage`, so a scanner that never ran contributes nothing; multiple
   recipes wrap into one synthetic `StageRecipe`. Whatever is in `next` is enqueued as the following
   stage. There is no inner cycle loop and no self-edge special case. `maxStages` bounds the total number
   of passes — the single cap; there is no separate `maxCycles` or `MAX_STAGES`.

Key design points captured for review:

- **`lastCycleOfRun` is a whole-run signal.** `LargeSourceSet.afterCycle(lastCycle)` runs end-of-run
  bookkeeping (the test harness validates change-cycle counts here). Since everything the recipes emit is
  enqueued, the run's last pass is simply the one after which `stageQueue` is empty. With a single
  converged stage the queue is empty there, identical to the classic behavior.
- **`minStages` is the only cycle-shaped residue left in the scheduler, and it is a genuine run bound.**
  When a stage produces no next work but the run's stage floor isn't met, the root is re-run once more to
  confirm stability (how tests assert idempotency). Because this fires only when `next` is empty, a stage
  that scheduled a successor is never starved — the successor supplies the next pass. This lets one stage
  counter drive both floor and cap even under the common test setting `minStages == maxStages`.
- **Successors are gathered every pass, not gated on whole-stage convergence.** Sealing the cycle into
  the `nextStage` default means a scanning recipe appends successors whenever its accumulator is live,
  even if a *sibling* recipe re-emitted the root. This is idempotency-safe and unobservable for current
  recipes (only the two stage tests use the successor hook, neither alongside a cycling sibling); flag if
  a global-convergence gate is later wanted.
- **Idempotency is the convergence contract.** A re-emitted stage that makes no change re-emits nothing,
  so the run stops. Real recipes (e.g. `UpgradeDependencyVersion`) are already idempotent, which is what
  removes any need for equality-based dedup of *scheduled* recipes (distinct from the root dedup above).
- **Parity is the gate.** The no-successor path must be, and is, byte-identical.

## Consequences

### Validated in this repo

- **Parity:** the full `rewrite-core` test suite passes (964 tests, 0 failures) with the stage
  generalization in place — a single-stage run is unchanged.
- **Mechanism:** `rewrite-core/.../scheduling/NextStageTest` proves a scanning recipe can discover work
  in one stage and schedule a genuine downstream recipe that acts on it in the next, using a value (a
  cross-file count) no single-file pass could know until scanning finished.
- **The motivating fix:** `rewrite-maven/.../NextStageUpgradeTest` proves a scanning recipe that makes
  *no edits itself* can schedule `UpgradeDependencyVersion` via `nextStage` and land a `<properties>` /
  `<dependencyManagement>`-managed version upgrade — the exact edit that a nested `visitNonNull`
  invocation drops, because the scheduler now drives the sub-recipe's deferred `doAfterVisit`
  lifecycle.

### Compatibility

The stage lifecycle lives entirely inside `RecipeScheduler`, and a single-stage run is byte-identical
to the old cycle loop, so existing callers gain stages simply by building against this version with no
source change. The positional `scheduleRun(recipe, sourceSet, ctx, …)` / `Recipe.run(…)` signatures are
unchanged; the last two int parameters are renamed `maxCycles`/`minCycles` → `maxStages`/`minStages`
(the concept is now a pass over the source set, and there is one cap, not two). The type rename touches
only `rewrite-core`-internal API: `RecipeRunCycle` → `RecipeRunStage` and `getCycleDetails()` /
`putCycle()` → `getStageDetails()` / `putStage()`. `getCycle()` keeps its name (now the stage number),
so recipes that key on it (data-table write-gating, cycle-1 guards) still compile and, for the common
single-stage case, behave identically. `Recipe.maxCycles()` (a per-recipe participation limit, distinct
from the scheduler cap) is untouched.

### Open questions for review

- **`getCycle()` across stages.** It now returns the run-global stage number, so `getCycle() <= 1`
  (data-table write-gating) means "first pass of the whole run" — successor stages don't write data
  tables. Confirm that's the intended semantics, or introduce a per-stage-lineage index if "first pass
  of this stage" is wanted instead.
- **Persistence of scheduled recipes.** Whether a scheduled stage's recipes should run once, or persist
  and re-run (idempotently) across later stages, is a policy choice not yet exercised beyond the
  two-stage case.
- **Termination ownership.** `maxStages` is the sole backstop for total passes. Whether a recipe should
  be able to signal "another stage" independent of emitting recipes is open.
- **Naming.** The `getCycle()` family and the "cycle" vocabulary in visitors/tests could follow the type
  rename to "stage"; kept as-is here to minimize churn.

## Where the prototype lives

- `rewrite-core`: `Recipe.nextStage` (convergence default) and `ScanningRecipe.nextStage` (successors),
  `RecipeList.addIfAbsent`, `RecipeScheduler` (`runRecipeStages` flat worklist — a recipe queue whose
  drain loop's counter is the stage number — `runStage`, one `collectNextStage` walk, `StageRecipe`),
  `RecipeRunStage` (renamed).
- Tests: `rewrite-core/.../scheduling/NextStageTest`, `rewrite-maven/.../NextStageUpgradeTest`.
