# poetry.lock fixture corpus

Locks generated 2026-07-17 with Poetry 2.4.1 (lock-version 2.1) on macOS x86_64 against live
pypi.org. Each scenario directory carries the `pyproject.toml` the lock was derived from; the
contents are byte-exact reference outputs, so the round-trip and content-hash tests assert
identity against them.

Scenarios:
- `a-minimal` — one leaf dependency (six), legacy `[tool.poetry]` layout.
- `b-graph` — requests' transitive graph, exercising `[package.dependencies]` and `[package.extras]`.
- `c-modern` — PEP 621 `[project]` + `[tool.poetry]` (Poetry ≥ 2.0) layout.
- `d-extras` — `requests[socks]`: an inline-table dependency constraint with `optional`/`markers`.
- `e-groups` — a `[tool.poetry.group.dev.dependencies]` group; package-level `groups`.
- `f-markers` — a package-level environment `markers` string.
- `g-git` — a git dependency: `[package.source]` plus `develop`.
- `i-upgrade` — the legacy-layout six 1.16.0 → 1.17.0 upgrade golden pair (`.before`/`.after`).
- `j-modern-upgrade` — the PEP 621-layout six 1.16.0 → 1.17.0 upgrade pair; drives the recipe test.

`http/` holds recorded pypi.org simple-API (PEP 691 JSON) and PEP 658 metadata responses so
`PoetryLockEngineTest` and `UpgradeDependencyVersionPoetryLockTest` replay the golden upgrade
offline. `PoetryLockEngineLiveTest` (disabled) re-records them against pypi.org.

File extensions are chosen so the license-header plugin skips them; do not add headers — the
tests assert byte identity against these exact contents.
