# 10. Native uv.lock delta resolution: sizing and a greedy-forward approach

Date: 2026-07-17

## Status

Proposed

Builds on [9. Native Python lock file regeneration](0009-native-python-lock-regeneration.md).
Refines that ADR's Phase B for the `uv` backend specifically: where 0009 §5 sketched a
`resolvelib` backtracking port shared across package managers, this ADR records what a
corpus sizing measurement found and proposes a narrower, accuracy-safe resolver built
uv-first. 0009's Phase A (the native minimal-update engines, now shipped for
`Pipfile.lock`, `uv.lock`, `poetry.lock`, and `pdm.lock`) is unchanged.

## Context

### What the uv engine is today

The shipped
[`UvLockEngine`](../../rewrite-python/src/main/java/org/openrewrite/python/internal/uvlock/UvLockEngine.java)
is a **minimal-update diff engine, not a resolver**. It treats the existing lock as fixed
resolution state, diffs the edited `pyproject.toml` against the root package's recorded
`[package.metadata]` (uv's own `--check` oracle), and rewrites only what provably changed.
It handles a version-change of a leaf-resolving direct dependency, a removal (via the
recorded graph plus a reachability sweep), and an upward `requires-python` bump (as a wheel
list filter). Every edit whose result cannot be proven correct fails loud with a structured
`Failure` — 37 `RESOLUTION_REQUIRED`/etc. throw sites.

Crucially, the engine already contains most of what a *single-package* resolution step
needs. `applyChange` fetches a package's index listing (`fetchListing`), groups files by
version, selects the highest version satisfying the collected constraints under
`requires-python` and yank filtering (`selectVersion`), fetches its dependency metadata
through the PEP 658 → lazy-wheel → sdist ladder (`fetchMetadata`), and derives its new lock
edges (`buildEdges`). `selectVersion`'s candidate ordering is already differential-verified
byte-identical against real `uv lock` in Phase A. What the engine never does is **search**:
open a *new* decision variable, expand a closure, or reconsider a choice.

The closure-changing edits it fails loud on, and where:

| Edit | Seam | Current behavior |
|---|---|---|
| Add a new direct dependency | `diffSection` (`UvLockEngine.java:776`) | `RESOLUTION_REQUIRED` |
| New version needs a transitive **not in the lock** | `buildEdges` (`:1666`) | `RESOLUTION_REQUIRED` |
| New version needs a transitive the **pin doesn't satisfy** | `buildEdges` (`:1674`) | `RESOLUTION_REQUIRED` |
| New version needs an **extra** the transitive wasn't locked with | `buildEdges` (`:1685`) | `RESOLUTION_REQUIRED` |

### The founding tension

A resolver *approximates* uv's PubGrub solver. The moment it emits a lock a subsequent
real `uv lock` would disagree with, it breaks the accuracy contract that 0009 §4 holds by
construction ("fail loud, never guess"). Any Phase B for uv must preserve that contract,
not trade it for coverage.

### Sizing measurement

Before committing to the build, we sized the opportunity on a corpus (0009's decision
sequence: measure how often `RESOLUTION_REQUIRED` actually fires before building the
resolver). We fetched the `uv.lock` for the 55 corpus uv repositories at their pinned
revisions and ran two passes; scripts and data are retained with the session.

**Structural census** (offline; 52 analyzable — 2 empty stubs, 1 workspace excluded):

- **81% (42/52) are forked/universal locks** carrying `resolution-markers` — uv's *default*
  output across Python/platform environments. 25/52 additionally have fork-*duplicated*
  packages. Only 15% (8/52) are clean single-environment locks.
- **Per-dependency editability is nonetheless high.** Of 2227 direct dependencies, 95% are
  single-entry and registry-sourced (the engine can attempt an upgrade); only 4% are
  fork-duplicated (edit bails) and 1% non-registry. A forked lock does not block ordinary
  single-dependency upgrades — it blocks native `requires-python` bumps (`applyPythonBump`
  throws on any `resolution-markers`) and edits to the fork-duplicated packages.
- Of all direct dependencies, **32% are leaves** (a Tier-0 upgrade is safe) and **63% are
  non-leaf** (an upgrade *may* cascade). Graphs are deep: median 101 packages, 31 direct
  dependencies, 29 shared transitives (in-degree > 1 — the cascade amplifiers).

**Live calibration** (PyPI; 183 non-leaf direct-dep "upgrade to latest" simulations across
47 repos, checking exactly what `buildEdges` checks — whether the new version's
`requires_dist` stays satisfied by the lock's existing pins):

- 45% were already at latest (the freshly-resolved corpus understates field demand; a
  stale lock jumps further and cascades more).
- Of the 99 decidable upgrades, **77% stay a Tier-0 change and 23% cascade** into
  `RESOLUTION_REQUIRED`. This 23% is a *lower* bound — a "satisfied" upgrade can still bail
  on marker reproduction. Blended across leaf and non-leaf upgrades the cascade rate is
  ~15–16%. The cascades are ordinary version bumps: `huggingface-hub 0.36→1.24` (pulls in
  `click`/`httpx`/`hf-xet`), `matplotlib 3.5→3.11`, `boto3`/`botocore` lockstep, `sphinx 6→9`.

**Verdict:** cascade demand is **real but moderate** (~15–23%, higher in the field on
stale locks), not the dominant edit. The defer-to-CI outcome already handles it correctly —
the recipe applies the manifest edit and fails loud, leaving the stale lock to fail
`uv lock --check` in CI. In-recipe cascade resolution is a coverage improvement, not a
correctness fix. Separately, the census surfaced a larger coverage lever — forked-lock
support, which would unlock native `requires-python` bumps on the 81% of locks that block
them today — but that is the marker-space (fork) resolution axis, orthogonal to and harder
than the cascade resolver, and out of scope here.

## Decision

Adopt a **greedy-forward delta resolver** for uv, gated behind the existing
`LockFileRegeneration` / `UvLockEngine.regenerate` seam. The product decision that motivated
the sizing is settled: customers need the lock updated **in-recipe for the vast majority of
cases**, so the defer-to-CI outcome is a fallback for the residual the resolver cannot prove,
not the primary behavior. T1 and T2 (below) are therefore both in scope. The design preserves
0009's accuracy contract by construction rather than by conformance testing alone.

Because 81% of real locks are forked/universal, "vast majority" coverage requires the
resolver to operate *inside* forked locks, not only the 15% clean ones. That is feasible
without solving marker-space resolution: see "Forked locks" below.

### Accuracy contract: greedy-forward, keep pins fixed, fail loud on backtrack

uv's default resolution (no `--upgrade`) uses a highest-version strategy seeded with
preferences from the existing lock: it keeps every locked version unless forced to move
one, and only *backtracks* — revises an already-made choice — when a forced move conflicts
with an existing pin. The resolver mirrors exactly the non-backtracking part of this:

1. Keep every existing pin fixed.
2. Open a new decision variable only for a package a new requirement can no longer satisfy
   at its current pin (or a newly added package with no pin).
3. Choose the highest compatible version via the existing, already-verified `selectVersion`
   / `fetchMetadata` / `buildEdges` machinery, and recurse into its closure.
4. **Fail loud the instant a forced move would invalidate an already-decided package** —
   i.e. exactly where uv would begin to backtrack.

This is accuracy-safe *by construction*: it produces byte-identical output to `uv lock` on
every edit uv itself resolves without backtracking, and refuses (fail loud → CI) on exactly
the edits where uv backtracks. It never emits a lock a real relock disagrees with. The
expensive part of a full PubGrub port — backtracking — is therefore *also* the
accuracy-risky part; deferring it is both cheaper and safer.

### Tiers

| Tier | Capability | Status / effort |
|---|---|---|
| **T0** | Leaf version-change, removal, `requires-python` bump | Shipped (0009 Phase A) |
| **T1** | Greedy cascading upgrade: a changed package's new version forces a transitive to move; recurse forward, keep other pins fixed, fail loud on backtrack | ~2–3 days; reuses per-node machinery, adds a worklist + a "would this move a prior decision?" guard |
| **T2** | Add / remove of a direct dependency with closure expansion (adds are structurally 100% unhandled today), including a dependency gated on a non-version environment marker | ~1–2 days on top of T1 — same worklist, seeded differently |
| **T3** | Full backtracking PubGrub | **Deferred** — 1.5–2.5k lines, the accuracy-risky cases; build only if T1/T2 leave real demand |

### Forked locks

The resolver operates inside forked/universal locks (the 81% majority) without solving
marker-space resolution, by drawing the line at fork *structure changes*:

- **In scope:** cascading or adding a package that resolves to a **single version across the
  lock's entire marker space**. Its entry is emitted once; its edges reuse the existing
  `edgeMarker` / `recordMarker` machinery, which already reproduces uv's recorded marker form
  for the verified subset and fails loud outside it. Editing a package that is not itself
  fork-duplicated, in a forked lock, is already the T0 behavior — T1/T2 extend it to the
  closures such edits pull in.
- **Fail loud (out of scope):** any edit that would **create or alter a fork-duplication** —
  a new/moved package that resolves to different versions in different environments, or an
  edit to an already fork-duplicated package (`resolution-markers` on the package entry).
  That is per-fork (marker-space) resolution, the T3+/forked-resolution effort, and the
  engine continues to `RESOLUTION_REQUIRED` there.

This keeps the accuracy contract intact — the engine never invents a fork it did not resolve
— while covering the common case where a newly pulled transitive has one universal version.

### `requires-python` bumps that collapse a fork

Raising the `requires-python` floor past every fork boundary — the common "drop the oldest
Python" bump — is a *mechanical* transformation of the existing lock, not a re-resolution: uv
minimizes forks, so once only one environment survives the higher floor, each fork-duplicated
package keeps exactly the version already locked for that environment. `applyPythonBump` now
supports this on forked locks. It confirms the top-level `resolution-markers` collapse to a
single always-true branch (every other branch is always-false under the new range), then drops
the eliminated fork entries, strips the surviving entry's `resolution-markers`, reverts its
now-redundant fork-disambiguated edges (dropping `version` / `source`), filters wheels, prunes
edges whose Python markers no longer fire, and sweeps newly-unreachable transitives. It fails
loud when the bump leaves *more than one* branch (the lock stays forked — marker-space
resolution) or when the lock is environment-restricted. Verified byte-identical to real uv
(`soupsieve` 2.7/2.8.4 collapse; `importlib-resources` three-way collapse dropping `zipp`).

### Markered direct adds

Adding a dependency gated on an environment marker (`foo ; sys_platform == 'linux'`) is a T2
add whose marker lands only on the root edge and requires-dist; the `[[package]]` entry and the
pulled-in closure are byte-identical to the unmarkered add (verified against real uv). The
narrow boundaries that keep this accuracy-safe:

- **Python-version markers fail loud.** `foo ; python_version >= '3.11'` makes uv record
  lock-level `resolution-markers` (a fork boundary at 3.11) even when `foo` resolves to a single
  version — a fork-*structure* change fork-induction detection does not catch, so it is rejected.
- **A conditional transitive in the closure fails loud.** If the added closure has a marker-gated
  edge (e.g. `portalocker ; sys_platform == 'linux'` needs `pywin32 ; sys_platform == 'win32'`,
  which uv intersects to empty and drops), reproducing it needs root/edge marker intersection,
  which the engine does not do; a closure of only unconditional edges is byte-identical.
- **`platform_system` normalizes to `sys_platform`.** uv rewrites `platform_system == 'Windows'`
  / `'Linux'` / `'Darwin'` to `sys_platform == 'win32'` / `'linux'` / `'darwin'` on both edges and
  requires-dist; `recordMarker` now reproduces that for those three values (others fail loud).
  This also closes a latent gap in unmarkered adds/bumps of packages with a `platform_system`-gated
  transitive.

**Fork-induction detection is mandatory, not optional.** A greedy single-version resolver
that is unaware of forking will *silently* mis-resolve a package uv would fork — e.g. adding
`click` to a `>=3.8` lock, where uv locks 8.1.8 for `<3.10` and 8.4.2 for `>=3.10`, but the
naive engine locks only the floor-compatible 8.1.8 and reports success. That is exactly the
wrong-lock outcome the accuracy contract forbids. The resolver therefore checks, whenever it
selects a version, whether a *newer* constraint-matching version exists whose `requires-python`
floor lands inside the lock's range but above its floor; if so uv would fork it, and the engine
fails loud instead. This guard covers adds, cascades, and plain upgrades alike.

### Differential harness (gates any build)

There is no uv live/differential test today — Phase A's uv fixtures were hand-recorded, and
only Poetry has a
[`PoetryLockEngineLiveTest`](../../rewrite-python/src/test/java/org/openrewrite/python/internal/poetrylock/PoetryLockEngineLiveTest.java).
T1/T2 are unsafe to ship without a uv analog: generate realistic edits across the corpus,
run the engine, run real `uv lock` over the same edit, and assert byte-identity (with a
recorded-HTTP offline replay for CI, live re-record `@Disabled`). This harness — ~2–3 days —
is a prerequisite, not an add-on, and doubles as the sizing instrument for future PMs.

### Relationship to defer-to-CI

The greedy resolver and the defer-to-CI outcome compose: T1/T2 resolve the subset they can
prove, and every other closure-changing edit still fails loud into
`PythonLockRegenerationFailures` and a CI relock. No edit becomes less safe; a bounded
fraction becomes resolved in-recipe instead of in CI.

## Consequences

- **Accuracy preserved.** The resolver never emits a lock uv would disagree with, because
  it only acts where uv does not backtrack and fails loud otherwise — the same guarantee
  0009 makes, extended to a strictly larger set of edits.
- **Bounded coverage gain.** In-recipe resolution recovers the ~15–23% of upgrades (higher
  on stale locks) that cascade today, plus adds/removals with conflict-free closures (T2).
  Backtracking-required edits, forked locks, and workspace locks remain fail-loud.
- **Reuse over new code.** T1/T2 add a search loop over machinery that already exists and is
  already differential-verified; the net new surface is the worklist, the prior-decision
  guard, and the differential harness — not a resolver from scratch.
- **Per-package-manager, not shared.** Unlike 0009 §5's shared `resolvelib` port, the
  accuracy-safe subset is defined by each tool's own default resolution discipline (uv:
  PubGrub with lock preferences). A future pipenv/poetry/pdm resolver would reproduce *its*
  tool's non-backtracking behavior, seeded and gated the same way, but is not literally the
  same code.
- **Platform forks are handled, not residual.** A ground-truth investigation against real
  `uv lock` 0.10.11 (local-index scenarios exercising `sys_platform`-partitioned constraints, wheel
  availability, and `tool.uv.environments`) established that uv *fork-duplicates a package to
  multiple versions if and only if no single version satisfies the union of its constraints*. When a
  single version does satisfy the union, uv locks that one version — the highest satisfying the
  union — even across an existing fork and even under restricted environments; it does not maximize
  each fork independently, and it does not fork on wheel-platform availability (a version with only
  a linux wheel is locked as-is, not backfilled per platform). This is exactly what the engine's
  `gatherConstraints` (union) + `selectVersion` (highest satisfying) computes, so a `sys_platform`
  fork is never mis-resolved silently: the engine reproduces uv's single-version choice byte-for-byte
  when one exists, and otherwise fails loud — `selectVersion` returns null → `RESOLUTION_CONFLICT`
  (mutually-exclusive constraints), `wouldFork` (a `requires-python`-driven fork), or
  `findSinglePackage` throwing (an edit touching an already fork-duplicated package).
- **The one residual that needed closing — restricted environments.** A lock restricted to a subset
  of environments via `[tool.uv] environments` / `required-environments` (recorded as
  `supported-markers` / `required-markers`) is resolved *per environment*: uv drops edges gated on an
  unsupported platform (a linux-only lock omits a `sys_platform == 'win32'` transitive entirely) and
  can fork-duplicate where a universal resolution would error. The engine simplifies markers only
  against `requires-python`, so it would keep such edges and emit a wrong lock. Resolving into a lock
  with `supported-markers` / `required-markers` (or under a manifest declaring those `[tool.uv]`
  keys) now fails loud; restricted-environment locks are uncommon, so the coverage cost is small and
  the accuracy guarantee is again by construction.
- **Justified by the product need.** Customers need in-recipe lock updates for the vast
  majority of cases, so the residual defer-to-CI cases must be genuinely residual — which is
  why T1 and T2 are both built and why the resolver must work inside forked locks. The
  remaining fail-loud surface (backtracking, new fork-duplications, workspace locks,
  `requires-python` bumps that leave the lock forked) is the explicit boundary of this ADR; each
  is a named, separately-scoped follow-on rather than an open-ended gap.
