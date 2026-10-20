# 14. Native Python delta resolution for closure-changing edits: per-manager feasibility and scope

Date: 2026-10-20

## Status

Proposed

Amends [9. Native Python lock file regeneration](0009-native-python-lock-regeneration.md) (its Phase B) and
[10. Native uv.lock delta resolution](0010-native-uv-lock-delta-resolution.md). Decides which package managers get a
native resolver for edits that change the resolved closure (adds, closure-shrinking removals, non-leaf upgrades), in
what order, and what stays fail-loud.

## Context

### Where the engines stand

ADR 0009 shipped four minimal-update engines and named a shared `resolvelib`-style backtracking resolver as the
deferred Phase B. Since then:

- **uv** already has a delta resolver. ADR 0010 replaced the shared-resolver plan with a greedy-forward resolver that
  keeps every pin, opens a decision only for a package a requirement can no longer satisfy at its pin or a new
  package, picks the highest compatible version, and fails loud where uv would backtrack. It shipped in
  openrewrite/rewrite#8292 as `UvLockEngine`'s cascade and addition queues, verified byte-identical to uv 0.10.11 on
  recorded scenarios (`UvLockDeltaOfflineTest`, `UvLockCascadeLiveTest`). Of its 41 remaining `RESOLUTION_REQUIRED`
  sites, one is backtracking; the rest are named structural gates (fork induction, fork-duplicated packages,
  restricted environments, `exclude-newer`, extras on adds, declared-extras and marker changes, workspace locks,
  marker forms the engine cannot reproduce).
- **Poetry** and **PDM** engines (openrewrite/rewrite#8285) rewrite only a direct dependency that resolves to a leaf
  package; adds, removals that orphan a package, and non-leaf upgrades fail loud (7 sites each).
- **pipenv** (`PipenvLockEngine`) proves the closure unchanged for a version change and prunes on removal; adds and
  closure-changing upgrades fail loud (10 sites).

ADR 0010's corpus census is the only sizing data: over 55 uv repositories, 32% of direct dependencies are leaves,
and of decidable non-leaf upgrades to latest 23% cascade into `RESOLUTION_REQUIRED`. Adds are structurally 100%
deferred on the three non-uv managers. A corpus run of all four real tools over the same edits, in progress at the
time of writing, will replace these estimates.

### What byte identity requires

An engine's lock is byte-identical to the tool's only if it reproduces, for the same index state, (a) the tool's
*version choices*, (b) the tool's *dependency metadata source*, since the lock records each package's requirements,
and (c) the tool's *marker algebra*, since every edge or package marker is a string the tool normalized. The
version choice is the part a resolver decides; (b) and (c) are the parts a reimplementation most often gets wrong
while producing a plausible lock. The sections below were established by reading each tool's source at the version
the existing fixtures were recorded with (uv 0.10.11, Poetry 2.4.1 with poetry-core 2.4.0, PDM 2.28.0 with
resolvelib 1.2.1, pipenv 2026.6.2 with vendored pip 26.1 and resolvelib 1.2.1); citations are file paths in those
trees.

### The common shape of the three deterministic resolvers

uv, Poetry and PDM share the property the greedy-forward design relies on. Each decides a package by taking its
locked version if the current constraint intersection still admits it, otherwise the highest admissible version
under that tool's filters:

- uv: `CandidateSelector::select` tries the lock preference first (`candidate_selector.rs:78-161`, `172-364`), then
  `select_no_preference` iterates a `BTreeMap` of versions highest-first (`415-505`).
- Poetry: `VersionSolver._choose_package_version` takes `Provider.get_locked` unconditionally when it satisfies every
  derived term, else the first of `search_for`, sorted not-yanked, stable, highest (`mixology/version_solver.py:562-597`,
  `puzzle/provider.py:279-314`, `780-795`).
- PDM: `ReusePinProvider.find_matches` yields the locked pins that satisfy all requirements first, then the fresh
  candidate list in `PDMPackageFinder._sort_key` order, highest first (`resolver/providers.py:449-483`,
  `models/finder.py:85-111`).

When such a run finishes without a conflict, each package's final version is the highest version admitted by the
constraints of its chosen parents, or its pin, independent of decision order. The engines' decision order
(PubGrub priority classes in uv, "most remaining versions first" in Poetry, resolvelib's preference tuple in PDM)
matters only when a later constraint excludes an earlier decision, which is a conflict the tool resolves by
backtracking. A greedy engine that gathers the complete constraint set on a package before selecting, and refuses
the instant a new requirement would invalidate a package already decided, therefore reproduces the tool's choice on
every conflict-free edit and refuses on exactly the edits where the tool's conflict resolution decides the outcome.
uv's `wrong-backtracking-basic` scenario (`crates/uv/tests/it/lock_scenarios.rs:20-175`) and Poetry's
`test_update_with_use_latest_vs_lock` (`tests/puzzle/test_solver.py:4957-5026`) are both cases a greedy engine must
refuse, and both present as a requirement on an already-decided package.

What differs per tool, and must be encoded per tool, is the admissibility filter: prerelease policy, yanked policy,
the `Requires-Python` rule, and index precedence. These are small, explicit functions in each source tree.

## Per-manager assessment

### uv 0.10.11

**How it resolves.** PubGrub with lock preferences. An edited `pyproject.toml` classifies the lock as `Preferable`
(`commands/project/lock.rs:1225-1418`): every locked version becomes a preference and the lock's fork markers seed
the initial forks. `uv add` runs the same lock operation with no `--upgrade`, so an added package already locked
transitively keeps its version (`add.rs:1001-1024`). Prereleases are admitted only when a package has no stable
release at all on the index, or the root requirement names one (`prerelease.rs:67-130`); yanked files are usable
only when locked or `==`-pinned (`yanks.rs:17-52`); a version whose file `requires-python` does not contain the
whole project range is **forked**, not skipped (`resolver/mod.rs:1337-1378`).

**Determinism.** Index files are sorted by filename on parse; versions live in `BTreeMap`s; the lock is emitted from
sorted structures. The inputs that can move the output are the HTTP cache (an index page served stale), a relative
`exclude-newer` span (resolved against the clock at every run and rewritten into `[options]`), and the host
interpreter only when the project declares no `requires-python`. Metadata comes from the "best" wheel, the first
filename-sorted wheel passing the requires-python tag check, and uv assumes all files of a version agree
(`prioritized_distribution.rs:459-461`).

**Byte-identity risk.** Markers. uv renders every marker from its canonical decision diagram, which lowers
`python_version` to `python_full_version`, normalizes `platform_system` to `sys_platform`, and simplifies against
the requires-python bounds (`uv-pep508/src/marker/{lowering,algebra}.rs`). The engine reproduces a verified subset
and fails loud outside it.

**Verdict: achieved for the conflict-free subset; the residue is backtracking and fork structure.** The
engine already resolves adds and cascades. Of the remaining gates, several are bookkeeping rather than search
(adding a dependency with extras, a transitive pulled in with extras, a changed declared-extras list, a pinned
index on an add) and are likely the most frequent refusals in the field. Backtracking (ADR 0010's T3) is the only
gate that needs a search algorithm, and building it means porting PubGrub's conflict resolution plus uv's conflict
re-prioritization (`resolver/mod.rs:3087-3143`), since which package yields in a conflict is decided there.

### Poetry 2.4.1

**How it resolves.** `poetry lock` without `--regenerate` is `Installer._do_refresh`: the solver is seeded with every
locked package and `use_latest` holds only directory packages (`installation/installer.py:184-214`). That is the
oracle for a manifest edited by a recipe. `poetry add` and `poetry remove` instead whitelist the named packages into
`use_latest` (`console/commands/add.py:408-411`, `remove.py:169-179`), so a recorded `poetry add` can differ from
edit-then-lock when the added package was already locked transitively. Candidate versions come from the index sorted
not-yanked, stable, highest; prereleases are tri-state and used only when nothing stable satisfies the constraint
(`repositories/repository.py:36-65`, poetry-core `packages/dependency.py:238-246`); yanked versions only for an
exact constraint; and a version whose `Requires-Python` does not cover the whole project range is rejected and the
solver walks down (`puzzle/provider.py:401-454`). Poetry decides the package with the **most** remaining versions
first, the inverse of PubGrub's heuristic (`version_solver.py:478-486`); under the conflict-free argument above
this does not change the result.

**Determinism.** No environment markers and no interpreter facts enter a lock; everything set-derived is sorted on
emission. The inputs that can vary are Poetry's release-info cache, which never expires
(`repositories/cached_repository.py`, `utils/cache.py` with `MAX_DATE`), the HTTP cache, `solver.min-release-age`
(clock), and which repository answered.

**Byte-identity risks.**

1. *Metadata source.* On pypi.org Poetry reads the JSON API release record: `info.requires_dist`, `info.summary`,
   `info.requires_python`, and `urls[]` for files (`repositories/pypi_repository.py:130-193`). The engine's ladder
   reads a wheel's `METADATA`. The two usually agree, but for byte identity the Poetry engine must fetch what Poetry
   fetches; `index/PypiJsonApi` exists already (pipenv's hash path uses it). On a legacy index Poetry uses PEP 658,
   then a lazy wheel, then a download, with a fixed wheel-choice rule (universal, then a py2/py3 union with
   synthesized markers, then the first platform wheel in page order, `http_repository.py:304-436`).
2. *Markers.* A package's `markers` is the union over paths of parent marker intersected with edge marker, simplified
   by the root python constraint, rendered by poetry-core's normalizer, which picks the least complex of the
   unnormalized, CNF and DNF forms (`puzzle/solver.py:247-447`, poetry-core `version/markers.py:1170-1211`). Poetry's
   own tests carry two accepted strings for one case because the rendering changed between poetry-core releases.
3. *Duplicate requirements.* A package requiring the same name twice with different markers goes through
   `_resolve_overlapping_markers` and may trigger a multi-pass solve with overrides, which writes several
   `[[package]]` entries of one name (`provider.py:607-729`, `solver.py:151-173`).
4. *Dependency rendering.* `[package.dependencies]` constraints are poetry-core's printing of the parsed constraint,
   not the `Requires-Dist` text (`packages/locker.py:518-662`).

**Verdict: achievable for conflict-free adds, cascades and removals with unconditional closures.** Poetry is the
strongest candidate after uv: its oracle is the exact analogue of `uv lock`, it uses no host inputs, its
admissibility rules are simpler than uv's (reject instead of fork on `Requires-Python`), and its reader, writer and
content hash already exist. The marker algebra and duplicate handling are gates, not blockers, as they are for uv.

### PDM 2.28.0

**How it resolves.** resolvelib with PDM's provider. The default strategy of a bare `pdm lock` is `all`
(`cli/commands/lock.py:52-58`): locked pins are **not** preferred and every package may move. `pdm add`, `pdm remove`
and `pdm update` use `reuse` (`project/config.py:171`, `remove.py:126`), and `pdm add` additionally tracks the added
package so a locked version is discarded and its specifier rewritten (`add.py:147-183`, `cli/utils.py:530-566`). The
oracle for a recipe's manifest edit is therefore `pdm lock --update-reuse`, not `pdm lock`. Admissibility: yanked files only for a `==` requirement (`models/repositories/pypi.py:58-73`),
prereleases when the requirement, a pinned or locked prerelease, or the project allows them, else a retry allowing
them when nothing remains (`providers.py:231-284`, `repositories/base.py:146-255`), and a candidate whose
`requires-python` does not cover the project's is skipped with a warning. A default lock has one target,
`{requires_python = <project range>}`, and evaluates markers against it with platform and implementation markers
stripped, so only Python markers can exclude an edge (`models/markers.py:28-32`, `71-77`).

**Determinism.** Deterministic for a fixed index state; set-derived collections are sorted on emission. The inputs
that vary are config (`strategy.update`, `use_uv`, `pypi.json_api`, sources, binary preferences, `exclude-newer`),
caches (a metadata cache keyed by requires-python, an HTTP cache with a 7-day default), the host interpreter for
sdist builds, and marker term order, which follows resolver traversal order through dep-logic's simplifier.

**Byte-identity risks.** Markers again (`graph.py:_build_marker`, ANDing parents and ORing across them, rendered by
dep-logic); `requires_python` read from whichever wheel link won a sort tie; `dependencies` rendered through
`as_line` (name case preserved, extras sorted, `str(SpecifierSet)`); and `groups` propagated under
`inherit_metadata`. A lock written through `use_uv` lacks `inherit_metadata` and carries no `groups` or `marker`,
which identifies it. Metadata comes from the PEP 658 sidecar of the chosen link, else the downloaded file, which
matches the engine's ladder.

**Verdict: achievable on the same terms as Poetry, once the oracle is `--update-reuse`.** The metadata path already
matches, the format layer exists, and the admissibility rules are explicit. Non-default `strategy` flags, several
`targets`, and `use_uv` locks stay fail-loud.

### pipenv 2026.6.2

**How it resolves.** One entry point, `venv_resolve_deps`, spawns pip's resolvelib resolver under the project
virtualenv's interpreter, and nothing passes the existing lock's versions as constraints (`utils/resolver.py:1284`,
`391-421`; the only constraints are the default category's pins when locking another category,
`routines/lock.py:115-118`). `pipenv lock` and `pipenv install` with no arguments re-resolve every category to latest.
`pipenv install X`, `update X` and `upgrade X` run `upgrade()`: resolve the whole category fresh with no lock
constraints, then transplant only X's closure into the lock, with reverse dependents and pruning taken from
`pipdeptree` over the **installed virtualenv** (`routines/update.py:135-176`, `483-551`). `pipenv uninstall X` prunes
entries absent from a fresh resolution and leaves the rest verbatim.

**Determinism.** Markers are evaluated, not preserved: pip drops any dependency whose marker is false on the locking
virtualenv, with only `python_version` and `python_full_version` patched from `[requires]`
(`resolution/resolvelib/factory.py:525-546`, `pipenv/resolver.py:507-534`). Dependency metadata is read from the
best wheel for the locking platform. Entry markers are rebuilt from the chosen file's `Requires-Python` and overwritten
by the first-recorded parent chain's top-level Pipfile markers (`utils/resolver.py:765-882`). The output depends on
the virtualenv's interpreter version and platform tags, pip configuration and `PIP_*` environment, the HTTP cache,
and for `upgrade` the set of installed packages.

**Verdict: not achievable under the byte-identity bar.** No pipenv command performs a minimal update, so there is no
command whose output a minimal-update engine can match except pure pruning (`pipenv uninstall`), and the commands
closest in spirit read state the engine does not have (the installed virtualenv) and evaluate markers on a machine
the engine is not. The existing Phase A pipenv paths are already held to ADR 0009's weaker contract
(`pipenv verify` green, installable, minimal change) rather than to byte identity with `pipenv lock`. Extending that
weaker contract to adds and cascades is a product decision, not an engineering one; it is not recommended here
because a lock whose markers were composed for an assumed platform is exactly the plausible-but-different output the
accuracy contract exists to prevent.

## Decision

1. **The resolver is greedy-forward per manager, not a shared backtracking port.** ADR 0010's design, as derived
   above, is adopted for Poetry and PDM with each tool's own admissibility filter. Backtracking is not built for any
   manager now. ADR 0013's approach for Node, porting each resolver whole, is not taken for Python: uv's resolver
   spans several Rust crates including a marker decision-diagram library, Poetry's and PDM's would each need their
   marker libraries ported for marker strings alone, and pipenv's output is a function of a virtualenv. The greedy
   subset gives the same byte-exactness on the edits it accepts and a named refusal on the rest.
2. **Order: Poetry, then PDM, then uv gate closures. pipenv stays fail-loud.** The verdicts above carry the
   reasons. uv's remaining gates are closed individually as the corpus run ranks them; a backtracking T3 is decided
   only on that data.
3. **Oracles are fixed per manager**, and the fixture READMEs record them: uv `uv lock`; Poetry `poetry lock` (no
   `--regenerate`); PDM `pdm lock --update-reuse`; every recording from an empty-cache home with the pinned tool
   version. A golden recorded with `poetry add`, `pdm add` or a bare `pdm lock` is not a valid expectation for the
   engines, because those commands move packages a manifest edit does not.
4. **Metadata is fetched the way the tool fetches it.** The Poetry engine reads the PyPI JSON API release record when
   the index is pypi.org and the PEP 658 ladder otherwise; the PDM and uv engines keep the wheel-metadata ladder. The
   index and metadata layers are reused, not forked; the Poetry engine composes `PypiJsonApi` with the existing
   clients.
5. **Scope of the first Poetry and PDM increment**, each verified byte-identical against the real tool before it
   ships:
   - adds of a direct dependency whose closure has only unconditional edges, including transitive packages new to
     the lock;
   - cascades: a non-leaf upgrade whose new requirements force transitives to move or to be added;
   - removals that orphan packages (reachability sweep over the recorded graph, as the uv engine does), including
     group membership changes for packages that stay reachable from another group;
   - a Python-gated edge is accepted only when it is always true or always false under the project's Python range
     (both tools then drop or keep it unconditionally); every other marker fails loud until that tool's rendering of
     that form is reproduced and recorded.
6. **Named refusals for Poetry and PDM**, each a `RESOLUTION_REQUIRED` with its own detail string: a requirement on
   an already-decided package that its version does not satisfy (backtracking); a marker-gated edge outside the
   rule above; a package requiring one name twice; extras on a new or moved transitive; several locked entries of
   one name (Poetry's override splits); a locked package with an explicit `source`; Poetry legacy-index metadata that
   would take the py2/py3 union path; PDM locks whose `strategy` is not exactly `inherit_metadata`, that carry more
   than one target, or that were written through `use_uv`; prerelease or yanked selection where the tool's rule
   would admit one (recorded as a refusal until a fixture pins the behavior).
7. **The differential harness is the gate**, as ADR 0010 required for uv: for each manager a live test that applies
   the recipe's edit to a fixture project, runs the engine, runs the real tool with the oracle command in an
   empty-cache home, asserts byte identity, and records the HTTP exchange for an offline replay test. The corpus run
   in progress supplies the edits and the failure ranking.

## Consequences

- Poetry and PDM gain in-recipe adds, cascades and removals for the conflict-free, unconditional-closure majority,
  with the same by-construction accuracy argument uv has; what they refuse is named and counted in
  `PythonLockRegenerationFailures`.
- Three marker algebras remain the long tail. Each manager's gate widens only by recording the tool's output for a
  specific marker form and reproducing that rendering; no engine ever normalizes a marker it has not seen the tool
  render.
- pipenv's closure-changing edits keep deferring to a real relock. If pipenv coverage is wanted, the decision to
  write a lock no pipenv command would write has to be taken explicitly, with the install-correct contract stated in
  the data table row.
- The oracle choice for PDM changes what "correct" means for its existing golden: the `g-upgrade` pair must be
  re-recorded with `--update-reuse` (for a single leaf the bytes are the same, so no test changes, but the README and
  the recorder must say so).
- The tracked tool versions grow to Poetry 2.4.1 and PDM 2.28.0 for resolution behavior, not only for format. A
  newer release is a diff of the files cited here, replayed against the fixtures.
