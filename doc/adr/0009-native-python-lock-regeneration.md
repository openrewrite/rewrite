# 9. Native Python lock file regeneration

Date: 2026-07-16

## Status

Proposed

## Context

The Python dependency recipes (`AddDependency`, `RemoveDependency`, `ChangeDependency`,
`UpgradeDependencyVersion`, `UpgradeTransitiveDependencyVersion`) and downstream consumers
such as `UpgradePythonVersion` edit a manifest (`Pipfile`, `pyproject.toml`) and then
regenerate the corresponding lock file (`Pipfile.lock`, `uv.lock`). Today regeneration
shells out to the real package manager:
[`LockFileRegeneration`](../../rewrite-python/src/main/java/org/openrewrite/python/internal/LockFileRegeneration.java)
writes the edited manifest (plus the old lock) into an empty temporary directory and runs
`pipenv lock` / `uv lock` there, via
[`PackageManagerExecutor`](../../rewrite-python/src/main/java/org/openrewrite/python/internal/PackageManagerExecutor.java).

At fleet scale this fails for reasons that have nothing to do with the edit being made:

1. **The package manager is not installed** on the machine running recipes. Recipe
   execution environments frequently have a JVM but no Python toolchain at all.
2. **Index credentials and configuration are not inherited.** `pipenv lock` in a scratch
   directory does not see the repository-relative or user-level pip configuration the
   project depends on; in environments where the public index is unreachable and packages
   resolve from a private mirror, resolution finds nothing
   (`Could not find a version that satisfies the requirement requests>=2.32.6 (from versions: none)`).
3. **Interpreter selection is environmental.** Whatever Python happens to be newest on
   `PATH` drives resolution; locking under an interpreter ahead of wheel availability
   fails even when every declared dependency is healthy.
4. **A full re-resolve makes the whole graph the failure surface.** `pipenv lock`
   re-resolves and re-hashes *every* package. A single wheel-less legacy pin anywhere in
   the lock — a package no recipe is touching — blocks regeneration of the entire file.
5. **Failures are opaque.** The subprocess stderr is reduced to a warning marker; there is
   no structured way to see which package or index caused the failure across a fleet.

Observed in practice, roughly half of manifest edits in a large private-index
organization failed to regenerate their lock, and in Python-less execution environments
the failure rate is 100%.

No existing tool solves this without executing Python. Renovate runs `pipenv lock` in a
sidecar container; Dependabot shells out to pipenv and spawns a Python helper just to
compute the lock's `_meta.hash`; Snyk does not open fix PRs for Pipenv projects. However,
non-Python implementations of Python resolution are proven: uv (Rust) resolves PyPI with
PubGrub and writes its own lock formats natively, and Google's deps.dev ships a Go
translation of pip's `resolvelib` resolver. The gap is Pipfile.lock-format-specific, not
fundamental.

### Verified pipenv behavior this design relies on

The following was verified against pipenv source (`v2026.6.2` main branch and `v2024.0.1`
for the legacy variant), not documentation:

- **`_meta.hash.sha256`** is the SHA-256 of
  `json.dumps(data, sort_keys=True, separators=(",", ":")).encode("utf-8")` where
  `data = {"_meta": {"sources": <[[source]] list>, "requires": <[requires] table>},
  "default": <[packages]>, "develop": <[dev-packages]>, <custom categories>...}`
  (`Project.calculate_pipfile_hash`, `pipenv/project.py`). Since pipenv ~2026.4
  (commit `44cfd54db2`) package-name keys are PEP 503-canonicalized before hashing;
  all earlier releases hash names exactly as written (vendored
  `plette.Pipfile.get_hash`). The two variants agree whenever names are already
  canonical, which is the common case.
- **When the Pipfile has no `[[source]]` section**, the hash input still contains a
  sources list: pipenv injects a default source derived from the *locking machine's* pip
  configuration (pip.conf, `PIP_INDEX_URL`), falling back to
  `{name: "pypi", url: "https://pypi.org/simple", verify_ssl: true}`.
- **Lock emission** is `json.JSONEncoder(indent=4, separators=(",", ": "),
  sort_keys=True)` with default `ensure_ascii=True`, UTF-8, plus exactly one trailing
  `"\n"`; newline style is preserved from a pre-existing lock file
  (`_LockFileEncoder` / `write_lockfile`, `pipenv/project.py`).
- **`_meta` is rebuilt at every lock**: `{"hash": ..., "pipfile-spec": 6, "sources":
  <populated [[source]] list, env vars UNexpanded>, "requires": <[requires]>}`.
  Credential placeholders like `${TOKEN}` therefore never leak into the lock.
- **`pipenv verify` checks exactly one thing**: stored `_meta.hash.sha256` equals a
  freshly computed Pipfile hash. **`pipenv install --deploy`** checks the same hash plus
  `requires.python_version`, then installs with pip hash-checking against the locked
  `sha256:` digests.
- **`pipenv lock` has no minimal-update behavior**: it re-resolves the entire graph to
  the latest constraint-satisfying versions on every run (`--keep-outdated` and
  `--selective-upgrade` were removed in 2023.7.9). The old lock is consulted only to
  back-fill a `==*` resolution and to preserve entries whose markers evaluate false on
  the locking machine. Consequently *no* mechanism — including running real pipenv —
  produces a lock that a later `pipenv lock` reproduces once the index has moved.
- **Per-entry shape** (`format_requirement_for_lockfile`, `pipenv/utils/locking.py`):
  `version` is the exact specifier string (`"==2.31.0"`); `hashes` is the sorted set of
  `"sha256:<hex>"` over **all** artifacts of the version (every wheel + sdist, so
  installs on other platforms can pick a different wheel); `markers` is a normalized
  single-quoted PEP 508 string; `extras` is sorted; `index` appears only on entries
  declared top-level in the Pipfile.
- **Hash collection** (`Resolver.collect_hashes`): for pypi.org, the PyPI JSON API
  digests; for any other index, the PEP 503 simple page's `#sha256=` fragments,
  downloading and hashing any file whose link lacks one.
- **Resolution environment**: `python_version`/`python_full_version` markers come from
  the interpreter selected via `[requires].python_version`; `sys_platform`/
  `platform_machine`/`os_name` come from the locking machine. Marker strings are
  *preserved* in the lock for install-time evaluation, not stripped.
- **`[[source]]` env expansion**: all fields except `url` via `os.path.expandvars`; the
  `url` field expands `${VAR}` tokens and percent-encodes only the userinfo portion;
  unset variables stay literal.

### What index servers expose without executing Python

- **Artifact hashes**: PEP 503 HTML `#sha256=` fragments and/or PEP 691 JSON `hashes`
  dicts. Universally available.
- **Dependency metadata (`Requires-Dist`)**: PEP 658/714 `.metadata` sidecar files —
  full core METADATA fetchable as a small HTTP GET. PyPI has served these for wheels
  since May 2023. Enterprise support varies: Sonatype Nexus ≥ 3.93.0 automatic;
  JFrog Artifactory opt-in (PEP 691 via an admin checkbox, PEP 658 via a system
  property) — **default Artifactory installations serve PEP 503 HTML only**; AWS
  CodeArtifact and Azure Artifacts have no PEP 658.
- **Universal metadata fallback**: HTTP Range reads of a wheel's end-of-central-directory
  + central directory + `METADATA` entry (the pip `fast-deps` / uv / poetry lazy-wheel
  technique) — a few KB of ranged reads plus `java.util.zip.Inflater`. Range requests are
  honored by PyPI's file host and Artifactory; a full-download fallback remains necessary
  for registries that ignore Range.
- **`Requires-Python` per file**: `data-requires-python` on PEP 503 anchors /
  `requires-python` in PEP 691 — available even without PEP 658.
- **The PyPI JSON API is PyPI-only** (not proxied by Artifactory or CodeArtifact) and its
  `requires_dist` is release-level and officially unreliable; it is an optimization for
  pypi.org, never a dependency of the design.
- **The hard boundary**: a release that is **sdist-only** has no `.metadata` sidecar and
  no wheel to range-read. Its `PKG-INFO` is trustworthy only when `Metadata-Version >=
  2.2` (PEP 643) and `Requires-Dist` is not declared `Dynamic`. Beyond that, dependency
  metadata exists only by executing a build — even uv invokes `setup.py` (via PEP 517)
  for such packages. All of the top ~360 most-downloaded PyPI packages ship wheels;
  sdist-only packages are a small residual concentrated in legacy pins.

### Existing machinery

- `Pipfile` and `Pipfile.lock` are already parsed natively
  ([`PipfileParser`](../../rewrite-python/src/main/java/org/openrewrite/python/PipfileParser.java),
  [`PipfileLockParser`](../../rewrite-python/src/main/java/org/openrewrite/python/internal/PipfileLockParser.java));
  only lock *production* shells out. The same split exists for `uv.lock`.
- Regeneration is centralized behind three seams all consumers already use:
  `LockFileRegeneration.forPackageManager(PackageManager)`,
  `PyProjectHelper.regenerateLockContent(SourceFile, String)`, and
  `PyProjectHelper.applyResolvedDependencies(SourceFile, String)`.
- rewrite-maven is the in-repo precedent for fully native resolution: an `HttpSender`
  taken from the `ExecutionContext`, host-supplied repository credentials via
  `MavenExecutionContextView`, and a downloader/cache layer — no Maven executable
  involved.
- `PythonResolutionResult.SourceIndex` (name/url/defaultIndex) already models index
  configuration on the marker but nothing populates it yet.
- The only PEP 508 handling in the repo is a regex splitter in
  `PythonDependencyParser`; there is no PEP 440 version comparison anywhere
  (rewrite-core's `Semver` is node-semver/Maven-oriented and cannot represent epochs,
  `~=`, `===`, or post/dev-release ordering).

## Decision

Replace the `pipenv lock` shell-out with a **native Java lock regeneration engine** with
**minimal-update semantics**, behind the existing `LockFileRegeneration` /
`PyProjectHelper` seams. No Python is executed. `uv` regeneration is unchanged initially
and migrates onto the same engine in a later phase (the layers below the lock writer are
package-manager-agnostic).

### Accuracy contract

The engine guarantees, for every lock it emits:

1. **`pipenv verify` passes** — `_meta.hash.sha256` is recomputed with pipenv's exact
   algorithm over the edited Pipfile.
2. **`pipenv install --deploy` / `pipenv sync` succeed** — every entry carries genuine
   artifact digests obtained from the project's configured index, and the pinned set is
   internally consistent (every entry's requirements are satisfied by the pinned
   versions, under the lock environment).
3. **Minimal change** — only the edited package(s) and pins their new requirements force
   are modified; every other entry is preserved byte-for-byte. This matches
   `pipenv upgrade <pkg>` semantics (and uv/Poetry `update` semantics), not
   `pipenv lock`'s re-resolve-the-world semantics.
4. **Fail loud, never guess** — if any moving package's metadata or hashes cannot be
   obtained (unreachable index, sdist-only release without static metadata, unsupported
   entry type), the engine emits **no lock at all** and returns a structured,
   per-package failure. It never emits a lock it cannot stand behind.

Byte-equivalence with a *future* `pipenv lock` run is explicitly a non-goal: pipenv
re-resolves everything to latest on every invocation, so equivalence over time is
unattainable by pipenv's own design, for any mechanism.

### Architecture

Seven components, layered bottom-up. Everything except the format layer is shared with a
future uv backend.

```
                 ┌───────────────────────────────────────────┐
 recipes ──────► │ LockFileRegeneration.forPackageManager    │  (existing seam)
                 └───────────────┬───────────────────────────┘
                                 ▼
                 ┌───────────────────────────────────────────┐
                 │ 7. PipenvLockEngine                       │  orchestration:
                 │    surgical patch │ delta resolve         │  Phase A / Phase B
                 └──┬──────────┬──────────┬─────────────┬────┘
                    ▼          ▼          ▼             ▼
              ┌─────────┐ ┌─────────┐ ┌──────────┐ ┌─────────┐
              │6. Format│ │5. Resol-│ │4. PEP    │ │3. Meta- │
              │  layer  │ │  ver    │ │ 440/508  │ │  data   │
              │ hash +  │ │ (B only)│ │ versions,│ │  ladder │
              │ writer  │ │         │ │ markers  │ │         │
              └─────────┘ └─────────┘ └──────────┘ └────┬────┘
                                                        ▼
                                            ┌──────────────────┐
                                            │ 2. Index client  │
                                            └────────┬─────────┘
                                                     ▼
                                            ┌──────────────────┐
                                            │ 1. Config /      │
                                            │    credentials   │
                                            └──────────────────┘
```

#### 1. Index configuration and credentials

A `PythonExecutionContextView` (modeled on `MavenExecutionContextView`) carrying the
resolved list of package indexes: name, URL, `verify_ssl`, credentials. Population order,
mirroring pipenv:

1. `[[source]]` entries from the Pipfile, with pipenv's env-expansion rules
   (`os.path.expandvars` semantics for non-URL fields; `${VAR}` expansion +
   userinfo-only percent-encoding for URLs; unset variables left literal → treated as
   a configuration failure if the URL is used).
2. When the Pipfile has no `[[source]]`: the **existing lock's `_meta.sources`** (see
   hash-consistency rule below), then `PIP_INDEX_URL`/`PIP_EXTRA_INDEX_URL`, then
   pip.conf (global → user → site, `PIP_CONFIG_FILE` last), then the pypi.org default.
3. Credentials: URL-embedded (post-expansion) → host-matched `PythonIndexCredentials`
   supplied on the view → `~/.netrc` by hostname. Host credentials merge into
   discovered indexes by hostname (the Maven settings-credentials pattern);
   `setPackageIndexes` remains a separate full-override channel. Hosts embedding
   OpenRewrite configure the view directly, exactly as they configure
   `MavenExecutionContextView` today.

HTTP goes through the `HttpSender` from `HttpSenderExecutionContextView` — the repo-wide
convention — so hosts control transport, proxies, and TLS.

#### 2. Index client

One client speaking the Simple Repository API with content negotiation
(`Accept: application/vnd.pypi.simple.v1+json, text/html;q=0.1`):

- PEP 691 JSON when offered: `files[].hashes.sha256`, `requires-python`,
  `core-metadata` (with `dist-info-metadata` legacy-key fallback per PEP 714).
- PEP 503 HTML otherwise: anchor parsing for `#sha256=` fragments,
  `data-requires-python`, `data-core-metadata`/`data-dist-info-metadata`.
- PyPI JSON API used opportunistically **only** when the source URL is pypi.org, for
  cheaper hash collection (matching pipenv's own special-casing).

Responses are cached per run keyed by (index, package), following the
`MavenPomCache` pattern.

#### 3. Metadata ladder

For each package version whose dependency metadata is needed (moving packages only):

1. PEP 658/714 sidecar: `GET {file_url}.metadata` — a few KB.
2. Lazy wheel: HTTP Range read of the wheel's end-of-central-directory record → central
   directory → deflate-decompress the `*.dist-info/METADATA` entry.
3. Full wheel download (registries that ignore Range).
4. Sdist `PKG-INFO`: acceptable **only** when `Metadata-Version >= 2.2` and
   `Requires-Dist` is not listed in `Dynamic` (PEP 643).
5. Otherwise: structured failure `DYNAMIC_SDIST_METADATA` for that package.

`METADATA`/`PKG-INFO` parsing is RFC 822-style header parsing of exactly the fields
needed: `Requires-Dist`, `Requires-Python`, `Provides-Extra`, `Metadata-Version`,
`Dynamic`.

#### 4. Versions and requirements (PEP 440 / PEP 508)

- **PEP 440**: adopt `org.cthing:versionparser` (Maven Central, Apache-2.0), which
  implements PyPA versions and specifier sets (`PypaVersion`, `PypaSpecifierSet`).
  Wrapped behind a thin internal interface so it can be replaced by an in-repo port if
  its conformance proves insufficient — validated against pypa/packaging's version and
  specifier test corpus either way.
- **PEP 508**: a small recursive-descent parser and marker evaluator (the PEP publishes
  a complete PEG grammar), producing a structural requirement: name, extras, specifier
  set, URL, marker AST. Conformance via the pypa/packaging marker/requirement test
  corpus, the same approach `pep508_rs` (uv) used.
- **Lock environment**: a value type holding the marker variables resolution is
  evaluated against. `python_version`/`python_full_version` derive from
  `[requires].python_version` (else preserved from the existing lock's
  `_meta.requires`, else unconstrained); platform variables default to `linux`/`x86_64`
  values but, matching pipenv, entries whose markers evaluate false in the lock
  environment are **preserved from the old lock**, never dropped — so platform defaults
  cannot destroy cross-platform entries.

#### 5. Resolver (Phase B)

A port of `resolvelib`'s backtracking core (small and provider-abstracted; deps.dev's Go
translation is precedent) with a provider that encodes pip + minimal-update behavior:

- **Candidate ordering** replicates pip's `CandidateEvaluator._sort_key`:
  `(has_allowed_hash, not_yanked, binary_preference, version, wheel_tag_priority,
  build_tag)` — a strict total order, so results are deterministic for a fixed index
  state.
- **Preference seeding** replicates Poetry's `get_locked`/`use_latest`: every package
  pinned in the existing lock resolves to its locked version unless (a) it is the
  explicit upgrade target, or (b) the locked version no longer satisfies the incoming
  constraints — only then is the index consulted for that package.
- **Requirement collection** honors extras and folds markers down the parent chain the
  way pipenv records them. Note (verified against current pipenv main): when a candidate
  has a `Requires-Python` bound, pipenv *overwrites* the requirement's own metadata
  marker with the synthesized `python_version` marker rather than AND-ing them — the
  provider reproduces this to keep emitted `markers` strings equivalent.

#### 6. Format layer

- **`PipfileLockHash`**: builds the canonical hash input from the Pipfile LST
  (`Toml.Document`) — sources with env placeholders intact, requires table, all
  dependency categories — serializes with a compact, code-point-sorted,
  ASCII-escaping JSON emitter, and computes the SHA-256. Emits the **legacy (plette)
  variant** — package names as written — because that is what every deployed pipenv
  before 2026.4 verifies against, and the two variants coincide when names are already
  canonical. Revisit the default once ≥ 2026.4 is the installed base.
- **Hash-consistency rule**: when the Pipfile has no `[[source]]`, the hash input's
  sources come from the existing lock's `_meta.sources` (what the machine that produced
  the lock actually used), not from a hardcoded pypi default — otherwise our hash would
  disagree with `pipenv verify` on any machine whose pip.conf feeds the default source.
- **`PipfileLockWriter`**: pipenv-byte-compatible JSON emission — 4-space indent,
  `", "`-free item separators with `": "` key separators, keys sorted by UTF-16 code
  unit, non-ASCII `\uXXXX`-escaped, single trailing newline, newline style inherited
  from the existing lock. Round-trip property: parsing an existing pipenv-written lock
  and re-emitting it unchanged is byte-identical (this is a standing test).
- **Entry emission** matches `format_requirement_for_lockfile`: `version` as the exact
  `==` specifier, `hashes` as the sorted `sha256:` set over **all** artifacts of the
  version, `markers` normalized to single-quoted form, `extras` sorted, `index` only on
  entries declared top-level in the Pipfile, `_meta` fully rebuilt with env placeholders
  unexpanded.

#### 7. Engine orchestration

`PipenvLockEngine` implements `LockFileRegeneration`'s contract for
`PackageManager.Pipenv` and picks the cheapest sufficient strategy:

**Surgical patch (Phase A).** Applicable when the edit is expressible as
"category-level add/remove/retarget of named packages" (which all five dependency
recipes and the `requires` bump are):

1. Diff old vs. new Pipfile (both are LSTs) → the **edit set**.
2. For a pure `[requires]` bump (no dependency edits): verify every pinned version's
   `Requires-Python` (from the simple API, no PEP 658 needed) admits the new Python;
   recompute `_meta.requires` + `_meta.hash`; done. Any violated pin → fall through to
   delta resolution (Phase B) or structured failure `PIN_EXCLUDED_BY_PYTHON` pre-B.
3. For a version change of package P to version V: fetch V's metadata via the ladder.
   If every requirement of V is already satisfied by existing pins (and V's
   `Requires-Python` admits the lock environment), the closure is **provably
   unchanged**: rewrite P's entry (version, hashes, markers), recompute `_meta.hash`,
   preserve everything else byte-for-byte.
4. For a removal: drop the entry; orphaned transitive-only entries are additionally
   dropped when no remaining entry (or top-level declaration) requires them —
   computable from the metadata of remaining movers plus the recorded graph; when the
   old graph is not reconstructible without extra fetches, leave orphans in place
   (a superset lock installs correctly and matches pipenv's own tolerance) and record
   `ORPHANS_RETAINED` in the result.
5. For an addition, or when step 3's proof fails: Phase B.

**Delta resolution (Phase B).** Run the seeded resolver over the affected categories.
Emit entries for exactly the packages whose resolved version differs from the old lock,
preserve the rest, recompute `_meta.hash`.

**Pass-through rule**: VCS, `path`/`file`, and editable entries are preserved verbatim
when not in the moving set. When an edit targets one of them, structured failure
`UNSUPPORTED_ENTRY_TYPE` (they require repository/filesystem access to re-pin).

### Failure model

`LockFileRegeneration.Result` grows a structured failure:

```java
@Value
class Failure {
    Reason reason;        // INDEX_UNREACHABLE, AUTH_FAILED, PACKAGE_NOT_FOUND,
                          // DYNAMIC_SDIST_METADATA, PIN_EXCLUDED_BY_PYTHON,
                          // UNSUPPORTED_ENTRY_TYPE, RESOLUTION_REQUIRED,
                          // RESOLUTION_CONFLICT, HASH_UNAVAILABLE,
                          // MALFORMED_MANIFEST, MALFORMED_LOCK
    @Nullable String packageName;
    @Nullable String indexUrl;
    String detail;
}
```

Recipes surface failures two ways: the existing `Markup.warn` on the manifest (behavior
preserved for compatibility), plus a new `PythonLockRegenerationFailures` data table
(source path, package, reason, detail) so fleet-scale runs can aggregate *why* locks
did not regenerate — directly addressing the observability gap of the current
mechanism. On any failure the old lock is left untouched.

### Integration and compatibility

- The five in-repo recipes and external callers of
  `PyProjectHelper.regenerateLockContent` / `editAndRegenerate` are **unchanged**: the
  dispatch inside `LockFileRegeneration.forPackageManager` returns a native engine for
  both package managers. The shell-out paths are deleted, not kept as fallbacks — two
  engines producing different locks for the same input is exactly the disagreement this
  design exists to eliminate. (`PackageManagerExecutor` survives only for
  `DependencyWorkspace`, which serves type attribution and is out of scope here.)
- `PipfileParser`'s parse-time regeneration fallback (regenerating a missing lock to
  populate resolved dependencies) switches to the same engine.
- `PipfileParser` starts populating `PythonResolutionResult.sourceIndexes` from
  `[[source]]`, giving the engine (and any other consumer) index configuration without
  re-walking the LST.

### Phasing

| Phase | Scope | Acceptance criteria |
|---|---|---|
| **A** | Format layer, config/credentials, index client, metadata ladder, PEP 440/508, surgical patch | Round-trip byte-identity on a corpus of real pipenv-written locks; hash corpus matches recorded pipenv/plette outputs (both variants); all five dependency recipes + `requires` bumps regenerate locks for closure-unchanged edits with `pipenv verify`-green results; structured failures for the rest |
| **B** | resolvelib-core port, pip-ordered provider, lock-seeded preferences | Additions and closure-changing upgrades resolve; resolver conformance against recorded pip resolutions on fixture indexes; deterministic output for fixed index state |
| **C** | uv backend on the same layers: revision-aware byte-exact `uv.lock` reader/writer, uv index discovery (`[[tool.uv.index]]`/`uv.toml`/`UV_*` env, flat and repo-relative indexes), graph-aware `UvLockEngine` (exact offline removals via the recorded dependency graph; requires-python upward bumps as pure wheel-list filters; conservative fork gate), replacing the `uv lock` shell-out | Engine output byte-identical to real `uv lock` for pin-bump, removal, and requires-python-bump goldens; `uv lock --check` passes on engine output and a real `uv lock` over it is a byte-identical no-op; repo-relative `[[tool.uv.index]]` sources resolve against the project directory |

Estimated volume: Phase A ≈ 2–3k main-source lines, Phase B ≈ 1.5–2.5k, plus ported
conformance corpora in tests. For calibration, resolvelib's core is under a thousand
lines of Python and deps.dev's complete Go PyPI resolver is a few thousand lines; the
manifest/lock parsing and recipe plumbing this design plugs into already exist.

### Testing strategy

- **Byte-level golden tests**: fixture Pipfiles + locks *recorded from real pipenv runs*
  (multiple pipenv versions), asserted byte-identical through parse → re-emit and
  through hash computation. No pipenv at test time.
- **Conformance corpora**: pypa/packaging's version, specifier, and marker test data
  ported as parameterized tests over the PEP 440 wrapper and PEP 508 parser.
- **Index client tests**: an in-process HTTP stub serving PEP 503 HTML, PEP 691 JSON,
  PEP 658 sidecars, and Range responses (including a Range-ignoring mode), with and
  without auth.
- **Engine tests**: recipe-level `RewriteTest`s asserting the emitted lock verbatim,
  including recomputed `_meta.hash` values, for each Phase A path and each structured
  failure.
- **Resolver tests (B)**: fixture index snapshots with recorded `pip`/`pipenv`
  resolutions as expected outputs.

## Consequences

- Lock regeneration works wherever the JVM runs: no Python toolchain, no pipenv, no
  interpreter-version coupling. The failure surface shrinks from "the whole graph, plus
  the environment" to "a moving package whose only distribution is a dynamic-metadata
  sdist, or an unreachable index" — and those become structured, aggregatable data
  instead of opaque subprocess stderr.
- Legacy wheel-less pins stop poisoning regeneration entirely: untouched entries are
  preserved verbatim and need no network at all.
- Semantics change from `pipenv lock` (re-resolve world) to `pipenv upgrade`
  (minimal update). For fleet-scale automated changes this is the more reviewable
  behavior — diffs contain only what the recipe caused — but it is a behavior change:
  runs that previously also picked up unrelated newer versions no longer do.
- We own a resolver and format emitter that track pipenv. The tracked surface is small
  and slow-moving (the hash algorithm changed once in eight years; the emission format
  never has), and the golden-fixture suite pins it per pipenv version, but it is a real
  maintenance commitment — the same one rewrite-maven made, at smaller scale.
- Recipes performing lock regeneration now require network access to the project's
  package index over the run's `HttpSender`, with host-configurable credentials — the
  same operational posture as rewrite-maven's dependency recipes.
- The PEP 440/508/index-client/resolver layers are Python-ecosystem infrastructure
  usable beyond lock regeneration (version comparison in recipes, dependency insight
  recipes, `requirements.txt` tooling) and carry no pipenv specifics.
