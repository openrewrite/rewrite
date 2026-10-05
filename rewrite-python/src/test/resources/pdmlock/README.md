# pdm.lock fixture corpus

Locks generated 2026-07-17 with PDM 2.28.0 (lock_version 4.5.0, `inherit_metadata` strategy) on
macOS x86_64 against live pypi.org. Each scenario directory carries the `pyproject.toml` the lock
was derived from; the contents are byte-exact reference outputs, so the round-trip and content-hash
tests assert identity against them.

Scenarios:
- `a-minimal` — one leaf dependency (six).
- `b-graph` — requests' transitive graph, exercising the multiline `dependencies` array.
- `c-extras` — `requests[socks]`: the extras-variant `[[package]]` with an `extras` key.
- `d-groups` — a `[tool.pdm.dev-dependencies]` group; per-package `groups` and multi-group metadata.
- `e-markers` — a package-level environment `marker` string.
- `f-git` — a git dependency: top-level `git`/`ref`/`revision` keys.
- `g-upgrade` — the six 1.16.0 → 1.17.0 upgrade golden pair (`.before`/`.after`); drives the engine
  and recipe tests.

`http/` holds recorded pypi.org simple-API (PEP 691 JSON) and PEP 658 metadata responses so
`PdmLockEngineTest` and `UpgradeDependencyVersionPdmLockTest` replay the golden upgrade offline.

File extensions are chosen so the license-header plugin skips them; do not add headers — the tests
assert byte identity against these exact contents.
