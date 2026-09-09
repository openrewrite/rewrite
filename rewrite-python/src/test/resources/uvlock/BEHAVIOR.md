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
- A `python_full_version` comparison against a **patch version** (`>= '3.10.2'`) forks the
  resolution even without version conflicts (`n2-inline-width/`).

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
