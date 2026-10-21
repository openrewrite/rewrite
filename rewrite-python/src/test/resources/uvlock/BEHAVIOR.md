# uv behavioral contracts (empirical, uv 0.10.11)

Companion to FORMAT.md; same environment and provenance (2026-07-17, macOS x86_64, pypi.org).
Every claim cites a fixture in this directory or a doc URL. Doc cross-checks against
docs.astral.sh/uv/concepts/projects/sync/ and /concepts/indexes/ — all agreements unless noted.
Development-time artifacts (diff files, verbose lock output, one negative probe) were not checked
in; the before/after fixture pairs they summarized are cited instead.

## 1. `uv lock --check` — the exact oracle

The oracle is: **re-derive the declared metadata from the manifest(s) and compare it with what the
lockfile records** — i.e. `requires-python`, `[manifest].members`, and each root/member package's
`[package.metadata]` (`requires-dist`, `requires-dev`, `provides-extras`) plus the ability of the
locked graph to satisfy those requirements. The resolved graph is **not** revalidated against the
registry and package content hashes are not rechecked.

Test matrix (`j-check/`, files `uv.lock.baseline`, `uv.lock.pristine`, `uv.lock.tampered-metadata`,
`uv.lock.handbumped`):

| test | result |
|------|--------|
| fresh lock | exit 0 |
| comment/whitespace-only pyproject.toml edit | exit 0 (metadata unchanged) |
| semantic edit (constraint `>=1.16`→`>=1.17`) | exit 1, `The lockfile at \`uv.lock\` needs to be updated, but \`--check\` was provided. To update the lockfile, run \`uv lock\`.` |
| uv.lock missing | exit 2, `error: Unable to find lockfile at \`uv.lock\`, …` |
| hand-tampered `[package.metadata]` specifier in the lock (`>=1.16`→`>=1.15`, manifest untouched) | exit 1 — --check catches metadata drift in either direction |
| hand-swapped package entry to a different version that still satisfies constraints (six 1.17.0→1.16.0 with correct 1.16.0 URLs/hashes) | **exit 0 — passes** |

Doc agreement: "uv will check if it matches the project metadata … will not consider lockfiles
outdated when new versions of packages are released" (docs.astral.sh/uv/concepts/projects/sync/).

**Engine consequence**: a surgical hand-edit of a `[[package]]` entry (version + sdist + wheels)
that keeps `[package.metadata]` in sync with pyproject.toml passes `--check` and needs no uv run.

## 2. Minimal-update / locked-version preference

- **No-op relock is byte-identical**: running `uv lock` again with no manifest change rewrites
  nothing — no timestamp, no revision churn (`i-minimal-update/`).
- **Pin edit** `six==1.16.0`→`==1.17.0` then `uv lock`: diff touches only (a) the requires-dist
  specifier line and (b) the six `[[package]]` entry's `version`/`sdist`/`wheels` lines; the
  unrelated `iniconfig` entry is untouched (compare `i-minimal-update/uv.lock.v1` and `.v2`).
- **Constraint relaxation** (`==1.16.0`→`>=1.16`) then `uv lock`: resolution keeps 1.16.0 (locked
  preference); only the requires-dist line changes (`i2-upgrade-package/uv.lock.pinned` vs
  `.relaxed-plain` = 1 line). Doc agreement: "uv will prefer the previously locked versions …
  versions will only change if the project's dependency constraints exclude the previous, locked
  version."
- `uv lock --upgrade-package six` then moves 1.16.0→1.17.0, again touching only that entry
  (`i2-upgrade-package/uv.lock.relaxed-plain` vs `.upgraded`, 12 diff lines).
- **Hand-edited locks are adopted as state**: after hand-downgrading six in the lock, a plain
  `uv lock` leaves the file byte-identical — uv treats the hand-written entry as the locked
  preference (`j-check/uv.lock.handbumped`).

## 3. requires-python bump (the UpgradePythonVersion surgical path)

`h-requires-python/uv.lock.before`→`.after` (3.10→3.12) and `h2-bump-wheels/uv.lock.before`→
`.after` (3.10→3.13):
1. header `requires-python` line replaced;
2. dependency edges whose markers become always-false are removed from `dependencies` arrays;
3. `[[package]]` entries no longer reachable are deleted wholesale;
4. wheel arrays are **re-filtered**: rows whose python tag falls below the new lower bound are
   deleted (exactly the cp310/311/312 rows for 3.10→3.13; nothing else changes);
5. `[package.metadata].requires-dist` is **left untouched**, still listing deps whose markers can
   never fire (it mirrors declared metadata, not the resolution).

## 4. Forks and markers

- Conflicting constraints across python ranges create duplicate `[[package]]` entries +
  top-level and per-package `resolution-markers` (`e2-true-fork/`).
- A bare environment marker (`sys_platform == 'win32'`) does NOT fork; the dep simply carries the
  marker (`e-markers-forks/` — no resolution-markers despite three conditional deps).
- A dependency marker that raises the Python lower bound above `requires-python` (`>=`, `>` or `==`
  on `python_version`/`python_full_version`, at any precision, alone or and-ed with other clauses)
  forks the resolution even without version conflicts (`n2-inline-width/`, `off15-python-lower-bound-marker/`).
  An upper bound or exclusion (`<`, `<=`, `!=`) does not fork, even at patch precision
  (`off17-python-upper-bound-marker/`). Every corpus lock with such a lower-bound edge records
  `resolution-markers`.

- Since 0.11.25 uv restricts every edge marker by the marker under which its parent is reachable (the union
  over all paths of the edge markers along them): inside the `aiofileish` entry forked at `< '3.11'` the edge
  `caioish; python_full_version < '3.11'` loses its marker, and `zippish; python_full_version < '3.12'` under an
  `importlibish` only reachable below 3.12 does too. A lock written by 0.10.11 still carries them, and any
  change relocked by 0.13.4 rewrites them all (`off37-parent-reachability-markers/`, before by 0.10.11). The
  engine applies the same pass to a changed revision-3-or-later lock and fails loud on marker shapes outside
  python clauses and equality atoms, on a disjunction the pass would narrow, and on restricted environments.

## 5. Lockfile revision migration

- Current uv **preserves** old-format locks as long as nothing must change: `--check` passes and
  plain `uv lock` leaves the 0.5.0-format (no revision, no upload-time) and 0.7.0-format
  (revision 2) files **byte-identical** (`o-old-uv/proj-0.5.0/`, `proj-0.7.0/`).
- The moment any semantic change forces a write, uv rewrites the **entire file** at
  `revision = 3`, adding `upload-time` to every artifact of every package, including untouched
  ones (`o-old-uv/proj-0.5.0/uv.lock.after-change-current-uv` — iniconfig gained upload-time while
  only six changed).
- There is no way to force modernization (issue 15220 closed as not-planned).

**Engine consequence**: a Java surgical editor must (a) detect the lock's revision (absent/2/3) and
emit the matching style for edited entries — same-revision edits must NOT add upload-time to a
rev-none file; and (b) accept that its output will differ from what a real `uv lock` would produce
for old-revision files (uv would rewrite everything to rev 3). Matching the existing file's
revision is the diff-minimal choice and still passes `--check` (§1 oracle ignores these fields).
(`o-old-uv/proj-0.5.0/uv.lock.engine-edited` is the engine-defined expectation for this case —
produced by `UvLockEngine`, not uv, and hand-reviewed; see the test referencing it.)

## 6. Index behavior

- Named index + `explicit = true`: only packages pinned via `[tool.uv.sources]` resolve from it;
  the pin is recorded as `index = "<declared url>"` inside the requires-dist entry, and the package
  `source.registry` is the declared URL **verbatim** (trailing slash preserved:
  `https://test.pypi.org/simple/`; the slash-less declared default stays slash-less)
  (`k-multi-index/uv.lock`). No index catalog is stored at top level.
- **Flat/file index**: a plain directory of wheels requires `format = "flat"` on the
  `[[tool.uv.index]]` entry; without it resolution fails with "was not found in the package
  registry" (negative probe, dev-time only — not checked in). With it:
  `source = { registry = "assets/shared" }` (relative path verbatim) and
  `{ path = "<filename>.whl" }` wheels with no hash/size/upload-time (`l-flat-index/uv.lock`).
  `uv lock --check` works normally against such locks (exit 0). Identical representation under
  uv 0.7.0 (`o-old-uv/proj-0.7.0-flat/`).
  Doc agreement: flat indexes are "local directories … that contain flat lists of wheels and
  source distributions" (docs.astral.sh/uv/concepts/indexes/); docs do not state the lock
  representation — empirical above fills that in.

## 7. sdist metadata acquisition (scenario c)

For `pickle-mixin==1.0.2` (2017 sdist, Metadata-Version 1.1), uv could not use static metadata:
`DEBUG No static PKG-INFO available … (UnsupportedMetadataVersion("1.1"))` → it **downloads the
sdist, creates a PEP 517 build environment (setuptools), and calls
`prepare_metadata`/`get_requires_for_build_wheel`** to obtain dependencies (observed in dev-time
verbose lock output). The lock entry itself is just the `sdist` table — no `wheels` key at all
(`c-sdist-only/uv.lock`).

**Engine consequence**: a native Java engine can cover packages with (a) wheels (METADATA from
PyPI JSON / PEP 658), or (b) sdists with static PKG-INFO ≥ 2.2; legacy/dynamic sdists would
require executing a Python build backend — the engine must detect this case and refuse/defer
rather than guess dependencies.

## 8. Extras and groups

- `requests[socks]` does not create pseudo-packages; the extra is an `extra = ["socks"]`
  attribute on the edge, plus `[package.optional-dependencies]` on the providing package listing
  the extra's own deps (`d-extras/uv.lock`).
- `[dependency-groups]` → `[package.dev-dependencies]` (resolved) + `[package.metadata.requires-dev]`
  (declared) on the root package; groups are NOT top-level lock concepts (`f-dep-groups/uv.lock`).
- `[project.optional-dependencies]` → `[package.optional-dependencies]` on the root +
  `marker = "extra == '<name>'"` entries in requires-dist + `provides-extras` (`g-optional-deps/uv.lock`).
- The legacy `[tool.uv] dev-dependencies` list locks exactly like the same list under
  `[dependency-groups] dev` (`off16-tool-uv-dev-dependencies/uv.lock` was produced from the former; the
  latter gives a byte-identical file).
- A non-root package's groups follow its activated extras (local-index scenarios, uv 0.13.4):
  upgrading `alpha[c]` rebuilds group `c` from the new version's `extra == 'c'` requirements, an entry
  keeping the rest of its and-chain as marker (`alphaw; extra == 'c' and sys_platform == 'win32'` →
  `{ name = "alphaw", marker = "sys_platform == 'win32'" }`), and a group entry needing a newer version
  cascades like a base edge, here in lockstep with alphac's `alpha==2.0`
  (`off19-extra-group-lockstep-upgrade/`).
- A transitive requirement with extras (`gamma[fast]`) records the extra on the edge and only the
  activated group on gamma; the unactivated `slow` extra leaves no group and pulls nothing
  (`off20-transitive-add-with-extras/`).
- A new version activating an extra on an already-locked package (`reqlike 2.0` requiring
  `urlish[socks]`) keeps urlish's version and adds its `socks` group and that group's closure
  (`off21-upgrade-activates-extra/`).
- An extra-gated marker is read per disjunct: `zeta; extra == 'fast' or extra == 'slow'` lands in each
  activated extra's group without a marker, next to the plain `extra == 'fast'` entries
  (`off22-or-extras-marker/`); `(sys_platform == 'win32') and extra == 'fast'` keeps the platform
  clause as the entry marker, and a disjunction whose every term needs an unactivated extra
  (`(sys_platform == 'emscripten' and extra == 'all') or (sys_platform == 'win32' and extra == 'all')`)
  leaves no trace (`off23-extra-dnf-markers/`).
- The root's declared extras work the same way: adding `reqlike[socks]` records `extra = ["socks"]` on the
  root edge and the new entry's `socks` group (`off25-add-with-extras/`); turning a declared `urlish` into
  `urlish[socks]` keeps its version and adds the group and its closure, and the reverse edit drops both
  without consulting the index (`off26-declared-extras-added/`, `off27-declared-extras-removed/`).
- An add pinned to a named index through `[tool.uv.sources]` resolves from that index alone, even when the
  default index lists the package too, and records `index =` on its requires-dist entry
  (`off28-add-pinned-index/`, two indexes under one host).
- A term without an extra clause (`delta; extra == 'fast' or sys_platform == 'win32'`) is split by uv
  into a base edge `sys_platform == 'win32'` and a group entry `sys_platform != 'win32'`
  (`off24-extra-or-platform-marker/uv.lock.after`); the engine fails loud on that shape.

## 9. Which requirements bound a package (local-index scenarios, uv 0.10.11 and 0.13.4)

- A dependent's requirement whose marker is always false under `requires-python` does not constrain
  the target: with `requires-python = ">=3.10"`, `urlish<2.1; python_version < "3.10"` next to
  `urlish<3; python_version >= "3.10"` lets a cascade move urlish to 2.5.0 (`off10-dead-marker-cascade/`).
- An extra-gated requirement (`urlish<2; extra == "old"`) constrains the target only while some edge
  activates that extra on the dependent (`off12-inactive-extra-constraint/`: nothing activates `old`, urlish
  moves to 2.5.0).
- A `[dependency-groups]` declaration bounds a target like a `[project]` one: `dev = ["urlish<2.4"]`
  keeps urlish at 2.0.0 although 2.5.0 is listed (`off13-dev-group-constraint/`).
- A non-root package's `[package.optional-dependencies]` group exists only while an edge activates the
  extra: removing `reqlike[socks]` from the root while `other` still depends on `reqlike` keeps the
  package, drops the `socks` group and the `socklike` entry only it reached
  (`off11-extras-removal-keeps-package/`).
- The same holds within one edit: dropping `reqlike[socks]` while `keeper` still depends on reqlike
  releases reqlike's `urlish<2; extra == "socks"` bound on a cascade the edit also triggers, so urlish
  moves to 2.5 rather than 1.9 (`off18-removed-extra-unbinds-cascade/`).

- A declared package is bounded by its live dependents as much as by its declaration: with `other==1.0`
  requiring `reqlike<1.5`, the edit `reqlike>=1.2` resolves to 1.4 although 2.0 is listed
  (`off33-dependent-bound-shapes-selection/`), and `reqlike==1.5` is "unsatisfiable" rather than a reason
  to move anything (`off32-upgrade-past-dependent-bound/`, no `.after`). The engine selects under the
  dependents' bounds, falls back to the declaration alone when nothing satisfies both (the lockstep pair
  in `off19`, where the dependent is the one moving), and then fails loud on any untouched dependent the
  new version no longer satisfies.

- An exact-minor `requires-python = "==3.12.*"` bounds the lock from above as well: wheels tagged for another
  minor (`cp311`, `cp313`) are dropped while `cp39-abi3`, `cp313-abi3` and `py3-none-any` stay, in filename
  order (`off35-exact-minor-wheel-filter/`); `python_version < '3.14'` is always true and vanishes from the
  edge, `python_version < '3.12'` never fires and takes its edge with it (`off36-exact-minor-marker-drop/`).

## 9a. `[tool.uv] constraint-dependencies` (local-index scenarios, uv 0.13.4)

- A constraint bounds every resolution like a requirement without adding the package, and the lock
  records the full list under `[manifest] constraints` as `{ name, specifier }` entries sorted by name,
  one inline and several multiline (`off29-transitive-constraint/`, `off30-two-constraints/`). Adding
  `urlish==2.5` once 2.5 exists moves urlish from its 1.0 pin, which a plain relock would have kept.
- The lock is rewritten only when the constraints on packages it holds differ from the recorded ones:
  a constraint naming only an absent package leaves an otherwise satisfied lock untouched and
  `uv lock --check` passes, while a changed constraint on a held package rewrites the manifest with
  every constraint, absent names included, even when the pin already satisfies it. Removing the last
  held-package constraint removes `[manifest]`.
- Markered constraints were not reproduced and are refused by the engine.

## 10. Edits that change the fork structure (fail loud)

- Upgrading `reqlike[socks]` to a version that no longer provides `socks` makes uv drop the extra from
  the root edge, the group and its closure, with a warning (`off14-upgrade-drops-extra/`, no `.after`).
- A new version gating a dependency on a raised Python lower bound (`urlish; python_full_version >= '3.11'`)
  makes uv record lock-level `resolution-markers` at 3.11 even though every package resolves to one
  version (`off15-python-lower-bound-marker/`, no `.after`), as §4 describes.
- Removing a dependency whose closure was the only cause of a fork leaves the top-level
  `resolution-markers` in place: uv seeds its forks from the existing lock
  (`s3` probe, dev-time only; the engine's unchanged header matched byte for byte).

## 11. uv 0.13.4

- Writes `revision = 5`; for every scenario in §9 and §10 the file is otherwise byte-identical to
  uv 0.10.11's (`z-revision-5/uv.lock` is a 0.13.4 lock). A relative `exclude-newer` is written as the
  sentinel `0001-01-01T00:00:00Z` with a trailing comment plus `exclude-newer-span`
  (`m4-exclude-newer-span/uv.lock`, since 0.11.8) and reads and writes back verbatim, as do
  `[options.exclude-newer-package]` and `[manifest] build-constraints`
  (`off34-exclude-newer-package-and-build-constraints/`); selecting a version of a package named with a
  timestamp under `exclude-newer-package` fails loud, a `false` entry restricts nothing.
- Rewrites a revision-3 file at revision 5 when anything changes, as §5 describes for older revisions, and
  the file is otherwise byte-identical (`off18-removed-extra-unbinds-cascade/uv.lock.after.uv-0.13.4`);
  0.12.21 does the reverse to a revision-5 file (`off21-upgrade-activates-extra/uv.lock.after.uv-0.12.21`).
- Omits `version` on a root with `dynamic = ["version"]` (also 0.10.11: `z2-dynamic-version-root/uv.lock`).
