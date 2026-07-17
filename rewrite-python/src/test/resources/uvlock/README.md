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

`o-old-uv/proj-0.5.0/uv.lock.engine-edited` is the one ENGINE-DEFINED expectation in the corpus
(real uv cannot produce it: it would rewrite the whole file at revision 3): the surgical
six==1.16.0 pin-down of `uv.lock.as-0.5.0` in the file's own revisionless style, derived from the
`http/six-listing-json` artifact data and reviewed by hand.

File extensions are chosen so the license-header plugin skips them; do not add headers — the
round-trip tests assert byte identity against these exact contents.
