# uv.lock fixture corpus

The empirically derived emission rules and behavioral contracts these fixtures witness are
cataloged in [FORMAT.md](FORMAT.md) and [BEHAVIOR.md](BEHAVIOR.md) — read those first when
extending `UvLockWriter`/`UvLockEngine` or regenerating fixtures against a newer uv.

Locks generated 2026-07-17 with uv 0.10.11 (Homebrew 2026-03-16) on macOS x86_64 against live
pypi.org, except `o-old-uv/proj-0.5.0/uv.lock.as-0.5.0` (uv 0.5.0: no `revision`, no `upload-time`)
and `o-old-uv/proj-0.7.0*/uv.lock*` (uv 0.7.0: `revision = 2`). Each scenario directory carries the
`pyproject.toml` the lock was derived from. Variant suffixes (`.before`/`.after`, `.v1`/`.v2`,
`.pinned`/`.relaxed-plain`/`.upgraded`, `.baseline`/`.pristine`/`.tampered-metadata`/`.handbumped`)
are before/after states of the scenarios described in the format catalog. `r-removal` is the
dependency-removal golden pair (requests removed; its orphaned transitives dropped wholesale), and
`http/` holds recorded pypi.org simple-API/PEP 658 responses (trimmed to the versions the engine
tests exercise) so `UvLockEngineTest` replays the golden scenarios offline.

Later probes (same uv 0.10.11 environment): `s-remove-last-dep` is the remove-last-dependency
golden pair — uv omits `[package.metadata]` entirely when the root has no declarations (the
checked-in pyproject is the edited, dependency-less state); `h3-requires-python-order` locks a
manifest declaring `requires-python = "<3.15,>=3.10"` — uv sorts the clauses ascending by version
(`">=3.10, <3.15"`); `g3-multi-extras` locks `requests[use_chardet_on_py3,socks]` — requires-dist
records extras in declaration order while dependency edges record them sorted.
`t-url-source` locks a `[tool.uv.sources]` direct-URL dependency (`six` from an sdist URL on
files.pythonhosted.org) — the package `source = { url = … }` with a hash-only `sdist` and a
`requires-dist` `url` key (FORMAT.md §5/§7/§8); `u-git-source` locks a `git` source
(`six` from `github.com/benjaminp/six?tag=1.17.0`) — package `source = { git = …#<commit> }` and a
`requires-dist` `git` key without the commit.

Follow-up constructs (same uv 0.10.11, generated **offline** against local path sub-packages so no
network was touched): `v-directory` locks a `[tool.uv.sources]` local-directory dependency
(`foo = { path = "libs/foo" }` pointing at a directory) — package `source = { directory = "libs/foo" }`
and a `requires-dist` `directory` key (FORMAT.md §5/§7). `w-conflicts` locks a single conflicting-extra
set (`[tool.uv] conflicts = [[{ extra = "cpu" }, { extra = "cuda" }]]`) — the top-level
`conflicts = [[ … ]]` header key (FORMAT.md §9) plus `directory` sources.
`w2-conflicts-groups` locks two conflict sets, one over extras and one over
`[dependency-groups]`, exercising the multi-set `[[ … ], [ … ]]` shape and the `group` key.
`x-supported-required-markers` locks `[tool.uv] environments`/`required-environments`, producing the
`supported-markers` and `required-markers` header arrays (FORMAT.md §2). These four match, byte for
byte, the same constructs found in the wild in real committed locks: `conflicts`/`required-markers`
in CorentinJ/Real-Time-Voice-Cloning@890f3a03187195b9829db2079b75c2ba2ab0405c,
`supported-markers` in spotDL/spotify-downloader@4aab5fdc5ad949abbb9974d8ad14c66675192c31, and
`directory` sources in LizardByte/Sunshine@9d2409f71b60f1812f482e6dd807dc52e2f72fe7.

Local-index scenarios (`off10`–`off15`, `off17`, `off18`, uv 0.10.11, 2026-10-20): each was locked against a throwaway
PEP 503 index of synthetic pure-python wheels built for the scenario, then the index host was rewritten
to `https://index.example` in the manifest, both locks and the recorded `http/` pages (`<pkg>-listing-html`
plus the `.metadata` sidecars). The same scenarios re-locked with uv 0.13.4 differ only in the
`revision` line. `off10-dead-marker-cascade` and `off12-inactive-extra-constraint` pin which of a
dependent's requirements bound a cascade target; `off11-extras-removal-keeps-package` the drop of an
optional-dependencies group whose activating declaration was removed; `off13-dev-group-constraint` a
dependency-group bound on a cascade target; `off18-removed-extra-unbinds-cascade` an extra whose
activating declaration the same edit removes; `off17-python-upper-bound-marker` an edge marker that
bounds Python from above and does not fork. `off14-upgrade-drops-extra` and
`off15-python-lower-bound-marker` are fail-loud scenarios and carry no `uv.lock.after` (BEHAVIOR.md §10).
`off19`–`off22` (uv 0.13.4, 2026-10-21, `revision = 5`) were built the same way and pin how
`[package.optional-dependencies]` groups follow activated extras (BEHAVIOR.md §8):
`off19-extra-group-lockstep-upgrade` an upgrade through an activated extra whose group depends back on
the package, `off20-transitive-add-with-extras` a transitive requirement with extras,
`off21-upgrade-activates-extra` an upgrade that activates an extra on an already-locked package,
`off22-or-extras-marker` a requirement gated on a disjunction of extras, `off23-extra-dnf-markers`
parenthesized extra clauses next to a disjunction no edge activates, and `off24-extra-or-platform-marker`
a marker mixing an extra with a platform clause, which the engine refuses (its `uv.lock.after` is real
uv's output, kept for the record), `off25-add-with-extras` a direct add with extras,
`off26-declared-extras-added` and `off27-declared-extras-removed` a declaration gaining and losing its
extras, and `off28-add-pinned-index` an add pinned to a second index (served under `http-alt/`).
`off29-transitive-constraint` and `off30-two-constraints` add `[tool.uv] constraint-dependencies`
(the UpgradeTransitiveDependencyVersion edit); their `uv.lock.before` was locked against an index
without the newer `urlish 2.5`, which was then published to the recorded index before the edit, so the
constraint is what moves the pin (BEHAVIOR.md §9a). `off34-exclude-newer-package-and-build-constraints`
(pypi.org, uv 0.13.4, 2026-10-21) carries `[options.exclude-newer-package]` and `[manifest] build-constraints`
through a removal that touches neither; both are read and written verbatim. `off35-exact-minor-wheel-filter`
and `off36-exact-minor-marker-drop` (local index, uv 0.13.4) lock under `requires-python = "==3.12.*"`; the
former's index serves one synthetic wheel per tag listed in its `index.json`. `off37-parent-reachability-markers`
is the one scenario whose `uv.lock.before` was written by uv 0.10.11 on purpose, since its edge markers are the
unsimplified ones 0.13.4 rewrites (BEHAVIOR.md §4).
A `uv.lock.after.uv-<release>` file is the same edit relocked by that release from the same `uv.lock.before`
(2026-10-21): 0.13.4 restamps the revision-3 `off18` lock at revision 5 and 0.12.21 restamps the
revision-5 `off21` lock at revision 3, each otherwise byte-identical to `uv.lock.after`.

`off16-tool-uv-dev-dependencies` (pypi.org) declares its dev group through the legacy `[tool.uv]
dev-dependencies` list; uv's lock is byte-identical to the same list under `[dependency-groups]`.
`z-revision-5` was locked by uv 0.13.4 (`revision = 5`, otherwise the revision-3 shape),
`z2-dynamic-version-root` by uv 0.10.11 for a project with `dynamic = ["version"]`, whose root entry
carries no `version` key, and `z3-compatible-release-python` by uv 0.10.11 for
`requires-python = "~=3.10"`, recorded as `>=3.10, <4`.

`m4-exclude-newer-span` was recorded 2026-10-20 with uv 0.12.21 (macOS arm64, empty cache, live
pypi.org) from a manifest declaring `[tool.uv] exclude-newer = "1 week"`: since 0.11.8 a relative
value is written as the sentinel `exclude-newer = "0001-01-01T00:00:00Z"` followed by a trailing
`#` comment, plus `exclude-newer-span = "P1W"`. 0.12.21 is the last release writing `revision = 3`;
0.12.22 and later write `revision = 5` with the same `[options]` shape.

`o-old-uv/proj-0.5.0/uv.lock.engine-edited` is the one ENGINE-DEFINED expectation in the corpus
(real uv cannot produce it: it would rewrite the whole file at revision 3): the surgical
six==1.16.0 pin-down of `uv.lock.as-0.5.0` in the file's own revisionless style, derived from the
`http/six-listing-json` artifact data and reviewed by hand.

File extensions are chosen so the license-header plugin skips them; do not add headers — the
round-trip tests assert byte identity against these exact contents.
