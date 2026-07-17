# uv.lock emission-rule catalog (empirical)

All rules derived by running **uv 0.10.11 (Homebrew 2026-03-16)** on macOS x86_64 against pypi.org,
2026-07-17, while designing `UvLockWriter`/`UvLockEngine`. Every rule cites a fixture in this
directory; the fixtures are the primary artifacts and this catalog is the index to them.
Cross-version data was produced with standalone uv 0.5.0 and 0.7.0 binaries (locks retained under
`o-old-uv/`; the binaries themselves were not checked in). Where docs.astral.sh/uv describes
behavior, agreement/divergence is noted; empirical output wins. Development-time artifacts
(diff files, verbose lock output, probe scratch dirs) were not checked in — where a rule rested on
one, the checked-in before/after fixture pair it summarized is cited instead.

## 1. File-level lexical rules

- **Encoding/EOL**: ASCII (UTF-8 superset), LF line endings, file ends with exactly one `\n`
  (last bytes `0a 5d 0a` = `…]\n` in `a-multi-package/uv.lock`). No trailing blank line. No CRLF.
- **No comments** are ever emitted.
- **Indentation**: multiline array elements are indented with exactly **4 spaces**; the closing `]`
  is at column 0. Every element line, including the last, ends with a trailing comma
  (`a-multi-package/uv.lock` wheels/dependencies).
- **Inline tables**: `{ key = value, key2 = value2 }` — one space after `{`, one before `}`,
  `, ` between pairs, ` = ` around equals. (all fixtures)
- **Strings**: always basic double-quoted strings. Markers use single quotes internally
  (`sys_platform == 'win32'`) so no escaping is ever needed; no escape sequences observed in any
  fixture. `upload-time` is a **quoted string**, not a TOML datetime
  (`upload-time = "2026-06-17T10:31:07.894Z"`, `a-multi-package/uv.lock:9`).
- **Integers**: `size` plain decimal, no separators.
- **Blank lines**: exactly one blank line between every top-level table/array-of-table entry
  (header block, `[options]`, `[manifest]`, each `[[package]]`, each `[package.*]` subtable).
  No blank line after a table header before its keys.

## 2. Header

Order (each key only when present):

```toml
version = 1
revision = 3
requires-python = ">=3.12"
resolution-markers = [ ... ]        # only when the resolution forked
supported-markers = [ ... ]         # only with [tool.uv] environments set
required-markers = [ ... ]          # only with [tool.uv] required-environments set
conflicts = [[ ... ]]               # only with [tool.uv] conflicts declared (§9)
```

then, separated by blank lines, optional top-level tables in this order:

- `[options]` — emitted only for non-default options; observed `exclude-newer = "2024-01-01T00:00:00Z"`
  (string, verbatim as passed) — `m2-options/uv.lock`.
- `[manifest]` — only for workspaces; `members` is a **multiline sorted array** of member package
  names — `q-workspace/uv.lock`.

Rules:
- `version = 1` always (all fixtures, all uv versions tested).
- `revision`: 0.10.11 writes `revision = 3`. uv 0.7.0 writes `revision = 2`. uv 0.5.0 writes **no
  revision key** — `o-old-uv/proj-0.5.0/uv.lock.as-0.5.0`, `o-old-uv/proj-0.7.0/uv.lock.as-0.7.0`.
- `requires-python`: normalized from the manifest, but with **different rules than specifier
  strings elsewhere**: `">= 3.10, <3.15"` → `">=3.10, <3.15"` — space around operators removed,
  but the `", "` comma-space separator kept (`m3-lexical/uv.lock:3`). Compare §7: `requires-dist`
  specifiers use `,` with **no** space. Clauses are **sorted ascending by version**, not kept in
  declaration order: `"<3.15,>=3.10"` → `">=3.10, <3.15"` (`h3-requires-python-order/`).
- `resolution-markers` (top level): multiline array of marker strings, one per fork; observed order
  puts the higher/newer python-range fork first and the complement last; complements are emitted
  with parentheses and `or`-chains:
  `"(python_full_version < '3.10.2' and platform_machine != 'aarch64') or platform_machine == 'aarch64' or sys_platform == 'win32'"`
  (`n2-inline-width/uv.lock:4-7`, `e2-true-fork/uv.lock:4-7`).
- `supported-markers` / `required-markers`: same multiline-string-array shape as `resolution-markers`,
  emitted **immediately after** it in that order (`supported-markers` then `required-markers`).
  `supported-markers` comes from `[tool.uv] environments`, `required-markers` from
  `required-environments` (`x-supported-required-markers/uv.lock`, both present; real-world:
  `supported-markers` alone in spotDL/spotify-downloader, `required-markers` alone in
  CorentinJ/Real-Time-Voice-Cloning).

## 3. Package ordering

`[[package]]` entries are sorted **alphabetically by normalized name**, with the **root/workspace
project entries participating in the same sort** (no special placement): `fixture-a` sits between
`colorama` and `idna` (`a-multi-package/uv.lock`). Duplicate names from forks are ordered by
**version ascending** (`typing-extensions` 4.5.0 before 4.16.0, `e2-true-fork/uv.lock`).

## 4. `[[package]]` key order

```
name
version
source
resolution-markers      # only on fork-duplicated packages; multiline even for 1 element
dependencies            # omitted when empty
sdist                   # omitted for wheel-only/flat-index packages
wheels                  # omitted for sdist-only packages (c-sdist-only/uv.lock)
```
followed (after blank lines) by subtables in this order:
```
[package.optional-dependencies]   # d-extras/uv.lock (on the providing package, e.g. requests)
[package.dev-dependencies]        # f-dep-groups/uv.lock (root/workspace members only)
[package.metadata]                # root/workspace members only
[package.metadata.requires-dev]   # f-dep-groups/uv.lock
```
`[package.metadata]` is emitted even when empty if `[package.metadata.requires-dev]` follows
(`m3-lexical/uv.lock`). When the root has **no declarations at all** (all deps removed, no groups),
uv omits `[package.metadata]` entirely — the root entry ends at `source`
(`s-remove-last-dep/uv.lock.after`).

## 5. `source` values

- Registry: `source = { registry = "https://pypi.org/simple" }` — the URL is recorded **verbatim
  as declared** in the manifest, including presence/absence of trailing slash
  (`https://test.pypi.org/simple/` kept with slash, `https://pypi.org/simple` without —
  `k-multi-index/uv.lock`). Default when no index declared: `https://pypi.org/simple`.
- Flat local index: `source = { registry = "assets/shared" }` — **relative path preserved
  verbatim** (`l-flat-index/uv.lock`).
- Root project without `[build-system]`: `source = { virtual = "." }` (`a-multi-package/uv.lock`).
- Root/workspace member with `[build-system]`: `source = { editable = "." }`
  (`p-editable-root/uv.lock`) or `{ editable = "packages/libone" }` (`q-workspace/uv.lock`).
- Direct URL (`[tool.uv.sources]` `url = …`): `source = { url = "https://…/six-1.17.0.tar.gz" }` — the
  download URL recorded **verbatim** (`t-url-source/uv.lock`). The artifact is not repeated on the
  URL: an sdist-URL package carries a **hash-only** `sdist = { hash = "sha256:…" }` (§8); a
  wheel-URL package carries the wheel with its own `url` + `hash` in `wheels`.
- Git (`[tool.uv.sources]` `git = …`): `source = { git = "https://…/six?tag=1.17.0#<40-hex-commit>" }`
  — the requested ref query (`?tag=`/`?rev=`/`?branch=`) is kept and the **resolved commit is appended
  as `#<sha>`** (`u-git-source/uv.lock`). Git packages carry no `sdist`/`wheels`.
- Local directory (`[tool.uv.sources]` `path = …` pointing at a **directory**, not a file):
  `source = { directory = "libs/foo" }` — the relative path recorded **verbatim** (`v-directory/uv.lock`;
  real-world: LizardByte/Sunshine, e.g. `{ directory = "third-party/glad" }`). Distinct from `path`,
  which uv uses when the `[tool.uv.sources]` path points at a wheel/sdist **file** (the `path`
  key appears on flat-index artifacts, §8). Directory packages carry no `sdist`/`wheels`.
- Single string value throughout: the source table always has **exactly one key** whose value is a
  string; `url`/`git`/`directory` fit the same `{ key = "value" }` shape as `registry`/`editable`.

## 6. `dependencies` arrays (resolved graph edges)

- **Always multiline**, one inline table per line, even for a single element
  (`c-sdist-only/uv.lock:9-11`).
- Sorted by name; duplicated names (forks) then by version ascending (`e2-true-fork/uv.lock:13-16`).
- Inline-table key order: `name`, `version`, `source`, `extra`, `marker`. `version` + `source` appear
  **only** when the name is ambiguous in the file (fork duplicates):
  `{ name = "typing-extensions", version = "4.5.0", source = { registry = "https://pypi.org/simple" }, marker = "python_full_version < '3.11'" }`.
- Extras on an edge: `extra = ["socks"]` (`d-extras/uv.lock:80`). No separate `package[extra]`
  pseudo-packages are created (default, non-conflicting configuration). Multi-extra edges record
  the extras **sorted**: declared `requests[use_chardet_on_py3,socks]` → edge
  `extra = ["socks", "use-chardet-on-py3"]` (`g3-multi-extras/uv.lock`) — contrast §7, where the
  requires-dist `extras` key keeps declaration order.
- **Marker simplification**: markers always true under `requires-python` are dropped entirely
  (`six; python_version >= '3.8'` with `requires-python = ">=3.12"` → `{ name = "six" }`,
  `n-normalization/uv.lock`); `python_version` is rewritten to `python_full_version`
  (`e-markers-forks/uv.lock:20-21`).

## 7. `[package.metadata]` (declared inputs, the --check oracle)

- `requires-dist`: **inline single-bracket array when it has exactly 1 element, multiline when ≥2**.
  The rule is element count, not line width — a 200-char single entry stays inline
  (`n2-inline-width/uv.lock`, `c-sdist-only/uv.lock:14`).
- Entries sorted by name. Inline-table key order: `name`, `extras`, `marker`,
  `editable`/`url`/`git`/`directory`, `specifier`, `index` — the direct-source keys
  `editable`/`url`/`git`/`directory` all sit **after `marker`** and stand in for `specifier`
  (a direct-source requirement has no version specifier). editable+marker together:
  `{ name = "foo", marker = "sys_platform == 'darwin'", editable = "libs/foo" }` (`y-editable-marker/uv.lock`):
  - `{ name = "requests", extras = ["socks"], specifier = ">=2.31" }` (`d-extras/uv.lock:84`)
  - `{ name = "importlib-metadata", marker = "python_full_version < '3.11'", specifier = ">=6.0" }` (`e-markers-forks/uv.lock:27`)
  - `{ name = "six", specifier = ">=1.16", index = "https://test.pypi.org/simple/" }` (`k-multi-index/uv.lock`)
  - `{ name = "libone", editable = "packages/libone" }` (`q-workspace/uv.lock`)
  - `{ name = "six", url = "https://…/six-1.17.0.tar.gz" }` (`t-url-source/uv.lock`) — the URL is the
    download URL **without** the package source's `#<commit>`; extras/marker still precede it, e.g.
    probed `{ name = "six", extras = ["all"], marker = "python_full_version >= '3.10'", url = "…" }`
  - `{ name = "six", git = "https://github.com/benjaminp/six?tag=1.17.0" }` (`u-git-source/uv.lock`) —
    the git URL **without** the resolved `#<commit>` that the package source carries
  - `{ name = "foo", directory = "libs/foo" }` (`v-directory/uv.lock`); with a marker:
    `{ name = "bar", marker = "extra == 'cuda'", directory = "libs/bar" }` (`w-conflicts/uv.lock`)
- Keys are omitted when absent (no `specifier` key for unconstrained deps, `e-markers-forks/uv.lock:26`).
- The `extras` key keeps **declaration order** (canonicalized, not sorted):
  `requests[use_chardet_on_py3,socks]` → `extras = ["use-chardet-on-py3", "socks"]`
  (`g3-multi-extras/uv.lock`); the resolved edge sorts them instead (§6).
- **Normalization** (`n-normalization/uv.lock`, `n2-inline-width/uv.lock`):
  - names PEP 503-canonicalized and lowercased (`Requests` → `requests`, `Charset_Normalizer` → `charset-normalizer`);
  - specifier: all whitespace removed, clauses joined by `,` (no space), clauses **sorted by
    version** (`>=3.0,<4,!=3.1.0,…` → `>=3.0,!=3.1.0,!=3.2.0,!=3.3.0,!=3.3.1,<4`);
  - markers: normalized spacing `a == 'b'`, single quotes, `python_version` → `python_full_version`,
    and clauses of an `and`-chain **sorted alphabetically by variable**
    (`sys_platform… and platform_machine…` → `platform_machine… and sys_platform…`);
  - declared markers are preserved (normalized) here even when always/never true under
    `requires-python` — contrast §6 (`h-requires-python/uv.lock.after` still lists the
    `python_full_version < '3.12'` dep under `requires-python = ">=3.12"`).
- Optional deps add `marker = "extra == 'cli'"` entries plus
  `provides-extras = ["cli", "yaml"]` — inline string array in **declaration order**, not sorted
  (`g2-extras-order/uv.lock`: `["zeta", "alpha"]`); it is the last key of `[package.metadata]`.
- `[package.metadata.requires-dev]`: one key per dependency group, sorted; each value follows the
  same 1-element-inline / n-element-multiline rule (`f-dep-groups/uv.lock:24-26`, `m3-lexical/uv.lock`).
- `[package.dev-dependencies]` (resolved counterpart on the package): group keys sorted; arrays
  always multiline like §6 (`f-dep-groups/uv.lock:13-19`).
- `[package.optional-dependencies]`: extra names as keys, **sorted** (alpha before zeta in
  `g2-extras-order/uv.lock` despite declaration order zeta,alpha); arrays always multiline (§6 style).

## 8. `sdist` and `wheels`

- `sdist` inline-table key order: `url`, `hash`, `size`, `upload-time`
  (`a-multi-package/uv.lock:9`). `hash = "sha256:<hex>"`.
- Direct-URL sdist packages carry a **hash-only** `sdist = { hash = "sha256:<hex>" }` — no `url`
  (it is the package `source`, §5), no `size`, no `upload-time` (`t-url-source/uv.lock`).
- `wheels`: **always multiline** (one wheel per line), even for a single wheel. Same key order per
  wheel: `url`, `hash`, `size`, `upload-time`.
- `upload-time` format: RFC3339 UTC with `Z`, fractional seconds **truncated to milliseconds,
  then trailing zeros trimmed** (`"2026-07-07T14:33:57.9Z"`, `"2026-07-07T14:34:12.47Z"`,
  `"2026-07-07T14:34:03.04Z"` — `a-multi-package/uv.lock`). Truncation, not rounding: PyPI's
  PEP 700 value `2024-12-04T17:35:26.475808Z` is recorded as `.475Z` (six 1.17.0 wheel,
  `i-minimal-update/uv.lock.v2`). uv 0.5.0 locks have **no** `upload-time` and none is added until the
  file is otherwise rewritten (see BEHAVIOR.md §5).
- Filenames inside URLs keep the index's original casing/underscores
  (`PySocks-1.7.1-py3-none-any.whl`, `charset_normalizer-…`), while `name` is normalized.
- **Wheel ordering**: matches the simple-index file order, which on PyPI equals sorting by filename;
  both hypotheses verified identical for the 79-wheel `charset-normalizer` list
  (`b-many-wheels/uv.lock`). For a byte-exact emitter against PyPI, sort by filename.
- **Wheel filtering**: wheels are dropped when their Python tag is incompatible with the lock's
  `requires-python` lower bound. With `>=3.10`, cp39 wheels are excluded but cp310–cp314(+t) and
  `py3-none-any` are all kept — **no platform filtering** (win/macos/manylinux/musllinux/armv7l/
  riscv64/s390x/ppc64le all present) (`b-many-wheels/uv.lock`: 79 of PyPI's 92 wheels; the 13
  missing are exactly the cp39 set). Re-filtering happens on `requires-python` bump: 3.10→3.13
  removed exactly the cp310/311/312 lines (compare `h2-bump-wheels/uv.lock.before` and `.after`).
- **Flat (file) index artifacts**: `wheels = [ { path = "six-1.17.0-py2.py3-none-any.whl" } ]` —
  path relative to the index directory, **no hash, no size, no upload-time**; no `sdist` key
  (`l-flat-index/uv.lock`). Same shape emitted by uv 0.7.0 (`o-old-uv/proj-0.7.0-flat/uv.lock`).

## 9. Forks (`resolution-markers`)

`e2-true-fork/uv.lock` (conflicting constraints across python versions):
- top-level `resolution-markers` (§2);
- one `[[package]]` per resolved version, each with its own per-package `resolution-markers`
  multiline array (placed after `source`) — multiline **even with one element**;
- dependency edges disambiguated with `version` + `source` keys (§6).
A fork can be triggered by a marker split alone (no duplicate versions): `n2-inline-width/uv.lock`
has top-level `resolution-markers` but a single `charset-normalizer` entry with no per-package
markers.

**`conflicts`** (top-level header key, from `[tool.uv] conflicts`): an **array of arrays** of inline
tables, each `{ package = "…", extra = "…" }` or `{ package = "…", group = "…" }` (package plus
exactly one of `extra`/`group`), declaring a mutually-exclusive set. Each set is a multiline bracket
run (one item per 4-space-indented line, trailing comma) and the outer brackets abut so a single set
reads `conflicts = [[ … ]]`; multiple sets are joined by `], [`:
```toml
conflicts = [[
    { package = "confproj", extra = "cpu" },
    { package = "confproj", extra = "cuda" },
], [
    { package = "confproj", group = "g1" },
    { package = "confproj", group = "g2" },
]]
```
Single-set (`w-conflicts/uv.lock`; real-world CorentinJ/Real-Time-Voice-Cloning, plotly/plotly.py),
two-set with a `group` set (`w2-conflicts-groups/uv.lock`). It is the **last header key**, before the
blank line that precedes `[options]`/`[manifest]`/`[[package]]`.

## 10. Cross-version drift (scenario o)

Same manifest locked by three uv versions (`o-old-uv/proj-*/`):

| uv | header | artifacts |
|----|--------|-----------|
| 0.5.0 | `version = 1`, no `revision` | no `upload-time` |
| 0.7.0 | `revision = 2` | `upload-time` present |
| 0.10.11 | `revision = 3` | `upload-time` present |

All other formatting (ordering, indentation, inline rules) is identical across the three.
Docs/issue cross-check: revision is bumped for backwards-compatible field additions; there is no
`--force` to modernize a lockfile (github.com/astral-sh/uv/issues/15220, closed not-planned);
issue #13738 shows rev 1 ↔ no upload-time, rev 2 ↔ upload-time in the wild. Migration semantics
are in BEHAVIOR.md §5.
