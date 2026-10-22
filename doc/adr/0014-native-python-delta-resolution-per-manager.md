# 14. Native Python delta resolution for closure-changing edits: per-manager feasibility and scope

Date: 2026-10-20

## Status

Proposed. Amended 2026-10-21: Poetry and PDM regenerate through ports of their resolvers, and uv's marker
algebra becomes a port, following [ADR 0013](0013-lock-regeneration-follows-each-package-manager.md).

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

ADR 0010's corpus census gave the first sizing: over 55 uv repositories, 32% of direct dependencies are leaves,
and of decidable non-leaf upgrades to latest 23% cascade into `RESOLUTION_REQUIRED`. A second run on 2026-10-20
applied real edits over a 70-repository corpus per manager and compared the engines with the real tools (uv 0.10.11
and 0.13.4, pipenv 2026.6.1, Poetry 2.4.1, PDM 2.28.0):

- uv, 9 real upgrades and 16 removals that both the engine and uv could perform: every engine success was
  byte-identical to uv 0.13.4, and 7 of them differed from 0.10.11 only on lines the edit did not touch; the
  refusals were the `[package.optional-dependencies]` rebuild (3 upgrades), a transitive pulled in with extras (1),
  and `[tool.uv] environments` (1). No edit was refused for backtracking.
- pipenv, 6 upgrades: 4 refused because the new version's requirements were not satisfied by existing pins
  (`boto3`/`botocore`, `fastapi`/`starlette`, `myst-parser`/`docutils`, `asfpy`/`cffi`), 1 byte-identical to
  `pipenv lock` on the edited entry.
- Poetry and PDM: no closure-changing edits were attempted; the no-op round trip failed loud on 25 of 70 Poetry
  locks (lock-version 2.0 or no `@generated` header) and 35 of 70 PDM locks (`lock_version` 4.4.x or 4.5.1).

Adds remain structurally 100% deferred on the three non-uv managers.

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

### Current releases versus the pinned versions

The fixtures pin uv 0.10.11, Poetry 2.4.1 and PDM 2.28.0. On 2026-10-20 the current releases are uv 0.13.4,
Poetry 2.5.1 and PDM 2.29.3. Each was run from an empty-cache home over the existing leaf-upgrade goldens, and the
resolver and lock-writer sources were diffed from the pinned tag to the current one. A change a release introduced
and a later release in the range fixed is not listed; only the net behavior at the current release counts.

Every tool stamps its own release into the lock, and that stamp is the one difference in the goldens:

| Tool | Golden | Current release's output |
|---|---|---|
| uv 0.13.4 | `i-minimal-update` | identical, except `revision = 3` becomes `revision = 5` on any change; an unchanged lock is preserved and `uv lock --check` passes |
| Poetry 2.5.1 | `j-modern-upgrade` | identical, except the header comment names 2.5.1 |
| PDM 2.29.3 (`--update-reuse`) | `g-upgrade` | identical, except `lock_version = "4.5.1"` |

The engines handled those stamps unevenly when this assessment was written: `UvLockReader` rejected any revision
above 3 and `PdmLockReader` any `lock_version` other than `4.5.0`, both as `MALFORMED_LOCK`, so a lock last written
by uv 0.12.22 or later or by PDM 2.28.1 or later was refused before any edit was attempted. Both readers now accept
the current stamps (Decision 9). `PoetryLockWriter` preserves the header and lock-version it read.

What the stamps encode, and the other net changes that reach lock bytes:

- **uv.** Revision 4 was only ever written by the metadata-free preview (0.11.33, #20695); the default writer went
  from 3 to 5 in one step at 0.12.22 (#22010, #22044), where revision 5 records workspace default groups and group
  `requires-python`, written only when configured away from the default.
  The reader accepts any revision and leaves a satisfied older lock untouched (`uv-lock/src/lock/mod.rs:124-138`,
  `6582`). Beyond the stamp: a relative `exclude-newer` is written as a sentinel
  timestamp plus `exclude-newer-span` (0.11.8); prerelease selection became "stable first within the range, then
  prerelease" for every package, so a range with no stable candidate now resolves instead of failing (0.12.0,
  #19993); a `Requires-Python` known only from fetched metadata now forks instead of marking the version unavailable
  (0.11.32, #20586); dependency markers are simplified under the parent's reachability marker (0.11.25, #19971) and
  rendered from a new irredundant DNF cover (0.13.2, #22608), so a conditional edge's string can differ from the
  0.10.11 form the engine reproduces. Unchanged by diff: preference lookup, yank allowances, PubGrub priority
  classes, the conflict threshold, fork detection, package and edge sort order, `hash = max(hashes)`, and `uv add`
  passing no implicit upgrade. The code moved (`uv-lock`, `uv-lock-operations`, `uv-resolver-types` crates), so the
  line citations in the uv section refer to 0.10.11.
- **Poetry.** `lock-version` is still 2.1 and both releases read 1.0 through 2.1 silently; the header flips only when
  data changes (`Locker._should_write` compares parsed dicts). poetry-core 2.5.0 (#939) renders a constraint that
  combines `!=` with a bound as `>=1,!=2` where 2.4.0 wrote `>=1,<2 || >2`, and Poetry re-renders every package's
  `[package.dependencies]` on any rewrite, so a 2.5.1 relock changes those strings file-wide while the engine
  preserves untouched entries. The same change admits prereleases differently at `<V` and `!=V` boundaries.
  poetry-core 2.4.1 (#943, first shipped with Poetry 2.5.0) changes `without_extras()` for disjunctions containing an
  `extra ==` term, which feeds per-package `markers` and `[package.extras]`. Poetry 2.5.0 (#10944) resolves
  mutually exclusive marker conflicts by splitting into override runs that 2.4.1 failed on. Unchanged: the solver's
  preference order, `search_for`, `get_locked`, `_do_refresh`, JSON API usage, wheel choice for metadata, and
  `content-hash` inputs.
- **PDM.** `lock_version` 4.5.1 (2.28.1, #3821) encodes the opt-in `metadata.lock_inputs` table, which replaces
  `content_hash` when enabled; both releases read the other's stamp with a warning only. A lock with two or more
  platform targets gains `sys_platform` markers on later targets (2.29.0, #3261); `python_version <= "X.Y"` is now
  converted to `<X.Y+1` in the Python-range arithmetic that decides marker dropping (2.29.3, #3902). The marker and
  specifier library dep-logic is pinned only as `>=0.5`, so the installed 0.5.2 or 0.7.x is an input: it changes
  the rendering of a `targets[].requires_python` with a hole and one marker-union shape. Resolution code for named
  requirements, `strategy.update = "reuse"`, and the `[metadata]` key set are unchanged.
- **pipenv** was not re-examined; its verdict does not depend on the release.

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
index on an add) and are likely the most frequent refusals in the field. The two the corpus hit, rebuilding a
package's `[package.optional-dependencies]` groups and a transitive pulled in with extras, are closed with this
ADR: groups follow the extras live edges activate, and an extra-gated requirement outside a single-clause
`extra ==` and-chain fails loud by name. Backtracking (ADR 0010's T3) is the only
gate that needs a search algorithm, and building it means porting PubGrub's conflict resolution plus uv's conflict
re-prioritization (`resolver/mod.rs:3087-3143`), since which package yields in a conflict is decided there.

**Closed after the first corpus replay (2026-10-21).** Extra-gated markers read as a disjunction of and-chains
with parentheses, the uv release stamp (item 8's `PythonExecutionContextView.setUvVersion`), direct adds with
extras, changed declared extras, index-pinned adds, `[tool.uv] constraint-dependencies` (the
UpgradeTransitiveDependencyVersion edit, which the engine had silently ignored), a declared upgrade bounded by
its live dependents (a wrong-lock path), `[options.exclude-newer-package]` and `[manifest] build-constraints`
read through, and `requires-python = "==3.X.*"` parsed with its real ceiling (wheel filtering, marker dropping).
Writing as uv 0.13.4, 26 of the 29 corpus edits both tools performed were byte-identical before the last four
of those landed. Marker re-simplification under parent reachability (uv 0.11.25+) is reproduced for python
clauses and equality atoms on any changed lock, with named refusals outside that algebra. The residue is a
relative `exclude-newer` (time-dependent by construction) and recorded marker forms outside the catalogued
subset.

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
accuracy contract exists to prevent. The demand is real, though: 4 of the 6 corpus upgrades needed it, so the
decision should be taken explicitly rather than left to the deferral.

## Decision

1. **The resolver is greedy-forward per manager, not a shared backtracking port.** ADR 0010's design, as derived
   above, is adopted for Poetry and PDM with each tool's own admissibility filter. Backtracking is not built for any
   manager now. ADR 0013's approach for Node, porting each resolver whole, is not taken for Python: uv's resolver
   spans several Rust crates including a marker decision-diagram library, Poetry's and PDM's would each need their
   marker libraries ported for marker strings alone, and pipenv's output is a function of a virtualenv. The greedy
   subset gives the same byte-exactness on the edits it accepts and a named refusal on the rest.
2. **Order: the readers (item 9), then uv's optional-dependencies bookkeeping, then Poetry, then PDM. pipenv stays
   fail-loud.** The verdicts above carry the reasons; the corpus ranked the uv bookkeeping gates first because they
   were the only field refusals left for uv (see the uv verdict). uv's other gates are closed individually as the
   corpus run ranks them; a backtracking T3 is decided only on that data.
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
   in progress supplies the edits and the failure ranking. Goldens are recorded with the current release and kept
   per release, as ADR 0013 keeps Node scenarios, so an older release's output stays a checked expectation.
8. **An engine writes as one named release, and reads any stamp up to the newest it reproduces.** The release is
   taken from the host through `PythonExecutionContextView` when the host knows which tool the project runs; without
   that, the release the lock's own stamp names (Poetry's header exactly; for uv and PDM the newest reproduced release
   that writes that stamp). Writing as a release means its stamp and its renderings: uv revision 5, Poetry's header
   plus poetry-core 2.5.0's constraint rendering across the file, PDM `lock_version` 4.5.1. The lock's own stamp is the
   default because every current release accepts the previous stamp (`uv lock --check` passes on revision 3, Poetry
   reads 2.1, PDM warns on 4.5.0), so the output is byte-identical to the release that evidently wrote the lock and
   still passes the current one's check. A stamp newer than the newest reproduced release fails loud, because the
   lock may carry keys the engine does not model.
9. **Before any resolver work, the readers accept the current stamps.** `UvLockReader` accepts revisions 4 and 5 and
   fails loud only on the keys they can carry (`default-groups`, `group-requires-python`, `[package.metadata]`
   omitted for a registry package); `PdmLockReader` accepts 4.5.1 and fails loud on a `lock_inputs` table. The
   corpus adds the other direction: `UvLockReader` parses the commented `exclude-newer` line 0.11.8 writes (15 of 70
   locks) so the edit reaches the `exclude-newer` gate instead of `MALFORMED_LOCK`, and PDM 4.4.x and Poetry 2.0
   locks are refused with a named reason, since the current tools rewrite those wholesale with per-package keys the
   engine would have to compute for every entry. This is reader work independent of resolution and precedes every
   item above.

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
- The tracked tool versions grow to Poetry and PDM for resolution behavior, not only for format, and move from the
  fixture pins to the current releases. A newer release is a diff of the files cited here, replayed against the
  fixtures.
- The uv engine's reproduced marker forms are 0.10.11's, and 0.13.2's DNF cover can render the same Boolean function
  differently, so each form is re-recorded against the current release before it stays accepted.

## Amendment 2026-10-21: follow ADR 0013 and port each resolver

The Node engines went through the architecture Decision 1 chose and replaced it. ADR 0011 regenerated a Node lock
with one shared resolver, a per-dependency proof that an edit leaves the tree unchanged, a byte-preserving patcher,
and a refusal for every shape the proof could not express.

ADR 0013 records 383 refusals on the npm path alone, and a recording of real npm that contradicted the proof's
premise. Every Node package manager is now a Java port of its own resolver modules, with their names, control flow
and iteration order, and writes the whole lock.

The uv corpus loop of 2026-10-21 followed ADR 0011's curve. Each round closed a gap by reimplementing one more part of
uv: marker lowering, extra-gated disjunctions, simplification under parent reachability, a declared upgrade bounded by
its dependents, constraint handling. Each reimplementation covers a subset and refuses at its edge. Most of what remains
is marker algebra that `uv-pep508` already defines.

### Decision

1. **Poetry and PDM are ports, not greedy engines.** This replaces Decision 1 for those two. Each upstream module the
   lock depends on becomes one Java class named after it, with its control flow and iteration order, and its class
   Javadoc names the file and release. For Poetry that is the mixology solver and the puzzle provider and solver
   (about 4,000 lines), poetry-core's markers and constraints (about 4,300), and the locker. For PDM it is resolvelib's
   resolution, PDM's resolver and providers, and dep-logic's markers (about 3,400 together), and the lockfile writer.
   What both need from Python itself (dict ordering, `sorted` stability, `str` comparison, `json.dumps`) is ported
   once in a shared package, as `EcmaScript` and `EventLoop` are for Node.
2. **uv keeps greedy-forward selection, and its marker algebra becomes a port.** No corpus edit across 375 locks was
   refused for backtracking, so a PubGrub port does not earn its cost yet, and Decision 1 stands for selection. The
   marker module of `uv-pep508` (the decision diagram in `algebra.rs`, `simplify.rs`, `lowering.rs` and `tree.rs`,
   about 7,000 lines) is ported as one library and replaces `UvLockMarkers` and `UvLockReachability`. The lock
   construction in `uv-lock` (dependency simplification and serialization) follows it. PubGrub is reconsidered when
   the corpus shows backtracking refusals.
3. **One target release per manager, the newest reproduced.** A changed lock is written as that release writes it,
   whatever stamp the lock carries, since a relock by any current release rewrites the whole file in its own shape and
   most projects run a recent release. This replaces item 8's default of the lock's own stamp. It is a calculated
   assumption, wrong for a project pinned to an older release, and that project's host names its release through
   `PythonExecutionContextView` (`setUvVersion` for uv) or, once the parser records it, a marker beside
   `PythonResolutionResult`. Releases are not swept one by one: goldens are recorded with the target release, and a
   difference an older release shows is ported only when a host that names that release meets it.
4. **A written lock is a fixpoint.** After writing, the engine runs again over the edited manifest and the lock it
   wrote, with no index access, and fails if the result differs. This is the Python form of ADR 0013's
   `LockOnlyInstall.verify`.
5. **Scenarios live in `openrewrite/rewrite-python-lock-fixtures`**, as Node's live in
   `openrewrite/rewrite-javascript-lock-fixtures`. The repository holds every manager's scenarios with an `expect`
   line for each that is not byte-exact, the recorder scripts, and the corpus tools. The `rewrite-python` build
   downloads a pinned commit of it and puts `lock/` on the test classpath, and a change that needs new recordings
   moves the pin with it.
6. **A refusal says what kind it is.** `RESOLUTION_REQUIRED` covers both "not ported yet" and "the engine cannot know".
   The two are separated, so the corpus ranks work items apart from the true limits.

Order: the fixtures wiring, then the `uv-pep508` marker port, then pipenv under item 7, then fork and backtracking
support in the uv engine, then the fixpoint check. The Poetry and PDM ports follow later.

7. **pipenv's contract is an install-correct minimal change.** No pipenv command makes a minimal update, and its
   output depends on the locking machine (the pipenv 2026.6.2 section above), so byte identity with pipenv has no
   fixed meaning. The engine edits only the closure an edit moves or adds, resolved greedily over the index in the
   Linux lock environment it already uses, with every file's hash and `_meta.hash` recomputed. A lock it writes must
   pass `pipenv verify` and `pipenv sync` in a clean Linux container with the Pipfile's Python, which the pipenv
   corpus checks with the fixtures repository's `record/pipenv-install-oracle.sh`. Where pipenv would also move
   unrelated packages, the engine does not. This replaces Decision 2's fail-loud stance for pipenv.

### Consequences

- Poetry and PDM regeneration covers what their resolvers do, at the cost of tracking two more upstreams release by
  release.
- The uv engine's marker handling stops growing by hand. Code written for marker forms since ADR 0010 is replaced, not
  extended.
- Adding a scenario means adding a directory to the fixtures repository, and a test expectation changes only when a
  recording says so.
