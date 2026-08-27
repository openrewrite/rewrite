# 11. Native Node.js lock file regeneration

Date: 2026-07-28

## Status

Accepted.

## Context

The JavaScript dependency recipes (`AddDependency`, `RemoveDependency`, `ChangeDependency`,
`UpgradeDependencyVersion`, `UpgradeTransitiveDependencyVersion`) edit a `package.json` and then
regenerate the matching lock file (`package-lock.json`, `pnpm-lock.yaml`, `yarn.lock`, `bun.lock`).
Until now regeneration shelled out to the real package manager:
[`LockFileRegeneration`](../../rewrite-javascript/src/main/java/org/openrewrite/javascript/internal/LockFileRegeneration.java)
wrote the edited `package.json` (plus the old lock and `.npmrc`) into a temporary directory and ran
`npm`/`yarn`/`pnpm`/`bun install` there via
[`PackageManagerExecutor`](../../rewrite-javascript/src/main/java/org/openrewrite/javascript/internal/PackageManagerExecutor.java).

At fleet scale this fails for reasons unrelated to the edit: the package manager is not installed on
the runner, registry credentials and configuration are not inherited into the scratch directory, or a
full re-install makes the whole graph the failure surface. When it fails the `package.json` is left
changed while the lock is left stale, so the two files go out of sync (customer-requests #2893). This
is the Node analogue of the Python problem addressed by ADR 0009 / 0010.

### Verified npm behavior this design relies on

- **The registry serves everything needed over HTTP.** The abbreviated packument
  (`GET {registry}/{name}` with `Accept: application/vnd.npm.install-v1+json`) yields the version set
  and dist-tags; the single-version manifest (`GET {registry}/{name}/{version}`) yields
  `dependencies`, `optionalDependencies`, `peerDependencies`, `peerDependenciesMeta`, `os`, `cpu`,
  `libc`, `engines`, `bin`, `bundleDependencies`, `hasInstallScript`, `license`, and
  `dist.{tarball, shasum, integrity}`. There is no tarball archaeology (no METADATA range reads, no
  PEP-658-style sidecar variance, no dynamic-metadata boundary). **The abbreviated packument omits
  `license`**, so a byte-exact lock entry requires the two-step fetch: abbreviated for version
  selection, full single-version manifest for the entry.
- **Minimal update matches the real tool.** Unlike `pipenv lock`, `npm install` / `yarn` / `pnpm`
  with an existing lock preserve the tree and change only what the edit forces, keeping the
  already-locked version when it still satisfies the new range. Byte-equivalence with a subsequent
  real install is therefore attainable, not merely `verify`-green.
- **Integrity is registry-derivable for four of five formats.** npm/pnpm/bun record
  `dist.integrity`; yarn-classic records `dist.shasum` in its `resolved` suffix. **Yarn Berry's
  `checksum: <cacheKey>/<hash>` is yarn's own normalized-zip hash, not registry integrity**, and is
  not packument-derivable.
- **The resolution model is a hoisted tree, not a flat environment.** A package may appear at several
  versions across nested `node_modules`; hoisting and dedup decide placement. Dependency-set equality
  is *not* graph-layout equality: a version bump can reshape the tree via peer changes, `os`/`cpu`/
  `engine`/`bundled`/`hasInstallScript` changes, being a peer provider, or a dedup reshuffle.

### Existing machinery

- Each lock format is already *read* natively — `LockFileParser` plus the `*LockAdapter` classes
  reduce every format to an npm-v3 shape to populate the `NodeResolutionResult` marker. These readers
  are lossy (pnpm peer suffixes stripped, yarn paths synthesized) and are reused only for the marker
  overlay, never for the byte-exact patch or the closure proof.
- Regeneration is centralized behind `LockFileRegeneration.forPackageManager` and
  `PackageJsonHelper.editAndRegenerate` / `regenerateLockContent`.
- rewrite-javascript already depends on rewrite-json and rewrite-yaml; their LSTs round-trip
  `package-lock.json`, `bun.lock`, and `pnpm-lock.yaml` byte-for-byte, so those formats are edited in
  place. `yarn.lock` (classic) is not valid YAML and is edited as targeted text.
- rewrite-core's `org.openrewrite.semver` was node-inspired but is Maven-flavored (no top-level `||`,
  Maven qualifier-ladder prereleases, a permitted 4th component) and cannot be reused for npm range
  matching directly.

## Decision

Replace the package-manager shell-out with a **native Java lock regeneration engine** behind the
existing seams. **No package manager is executed, and there is no shell-out fallback** — a fallback
would run in the same credential-losing scratch sandbox that causes the customer's failure, so it
offers no reliable upside and reintroduces a second engine that could disagree.

### Accuracy contract

For every lock it emits the engine guarantees: genuine registry-sourced integrity on every moving
entry; a **minimal change** (only the edited package and what its new requirements force; every other
byte preserved); and **fail loud, never guess** — when it cannot prove the result it emits no lock and
records a structured, per-package failure. Byte-equivalence with a subsequent real install is the
target, since the package managers themselves do minimal updates.

Regeneration is one idea applied at two scopes: **resolve the new dependency set, then edit the lock
file in place**. The per-dependency scope diffs the two manifests, proves each changed dependency's
closure effect (the strict layout whitelist below), and hands `PackageEdit`s to the format's
`LockPatcher`. When that proof cannot be made (`RESOLUTION_REQUIRED`), the engine resolves the
**whole closure** instead — seeded by the existing lock, so a locked version that still satisfies its
range is kept exactly as a real incremental install would keep it — and diffs the resolved graph
against the existing lock into the same `PackageEdit`s for the same patcher. There is no whole-file
serialization: untouched entries keep their bytes because the lossless trees preserve everything the
edits do not name, and only what actually changed is ever verified or rewritten.

The per-dependency whitelist: a version change of a package that is not fork/peer-duplicated in the
lock, whose new version's `dependencies` are unchanged and already satisfied, with no change to
`peerDependencies` / `peerDependenciesMeta` / `optionalDependencies` / `os` / `cpu` / `libc` /
`bundleDependencies` / `hasInstallScript`. Non-layout metadata (`engines`, `license`, `deprecated`,
entry flags) is patched in place rather than failing loud. Removals of leaf/orphan entries are
supported. Everything else routes to the whole-closure scope, which itself fails loud on any
difference its format's patcher cannot express byte-exact.

### Architecture

A shared, package-manager-agnostic orchestrator plus a per-format patcher:

- **Registry configuration and credentials.** A `NodeExecutionContextView` (modeled on
  `MavenExecutionContextView`) carries host-injectable registries and credentials; discovery reads the
  marker's `npmrcConfigs` plus `.npmrc` / `.yarnrc.yml` / pnpm config (default `registry`, per-scope
  `@scope:registry`, `//host/:_authToken` / `_auth` / `username`+`_password`, `always-auth`,
  env-var expansion, netrc fallback). HTTP goes through the run's `HttpSender`. `cafile`/custom-CA and
  proxy that the default sender cannot honor fail loud rather than silently mis-handshake.
- **Registry client** (`internal.registry`) — the two-step fetch above, with a per-run cache; only
  moving packages are fetched (their dependencies are already in the lock), so there is no N+1.
- **node-semver** — exact npm/node-semver semantics through the existing `org.openrewrite.semver`
  family: `Semver.validate`/`satisfies`/`maxSatisfying`/`compare` take an `Ecosystem.NODE` argument
  that selects npm's range grammar and SemVer 2.0.0 precedence over `TildeRange`/`CaretRange`/`XRange`/
  `HyphenRange`, leaving the Maven paths untouched. Gated by node-semver's own conformance fixtures
  **and** the existing Maven/Gradle semver suites staying green.
- **`NativeLockEngine`** — diffs the pre-edit and post-edit `package.json` to the recipe's edit set,
  runs the closure-safe proof over the raw lock (not the lossy adapters), resolves versions
  minimal-update (keep the locked version if it still satisfies, else max-satisfying), honors
  overrides/resolutions, builds a `LockEditSet`, and dispatches to the format's `LockPatcher`.
- **Whole-closure resolution** — `NpmGraphBuilder` resolves the closure from the importer manifest
  (regular/dev/optional scopes, satisfied peers, npm 7+ peer auto-install, `npm:` aliases),
  preferring a locked version that still satisfies its range over the registry maximum;
  `ResolutionGraph` / `ResolvedNode` are the plain value model. A per-format graph-to-lock diff
  (`NpmLockDiff`, `PnpmLockDiff`, `BunLockDiff`, `YarnClassicLockDiff`, `YarnBerryLockDiff`) matches
  the graph to the lock's own keys — hoisted `node_modules` slots, `name@version` content addresses,
  flat selector blocks, or merged descriptors — and expresses the difference as `PackageEdit`s.
- **`LockPatcher`** per format — `NpmLockPatcher` (v2 + v3, workspaces, via rewrite-json),
  `PnpmLockPatcher` (v9 + v6, workspaces, via rewrite-yaml; v5.4 fails loud), `YarnClassicLockPatcher`
  (targeted text patch with merged-header split), `BunLockPatcher` (JSONC via rewrite-json),
  `YarnBerryLockPatcher` (via rewrite-yaml; each moving entry's `checksum` reproduced from its
  tarball by `BerryZipChecksum`, untouched entries keeping theirs).

### Failure model

`LockFileRegeneration.Result` grows a structured `Failure { Reason, packageName, detail }` (existing
fields kept for back-compat). Reasons: `REGISTRY_UNREACHABLE`, `AUTH_FAILED`, `PACKAGE_NOT_FOUND`,
`VERSION_NOT_FOUND`, `CHECKSUM_UNAVAILABLE`, `RESOLUTION_REQUIRED`, `UNSUPPORTED_LOCKFILE_VERSION`,
`UNSUPPORTED_ENTRY_TYPE`, `MALFORMED_LOCK`, `MALFORMED_MANIFEST`. Recipes surface failures via the
existing `Markup.warn` plus a new `NodeLockRegenerationFailures` data table (source path, package,
reason, detail) for fleet aggregation. On failure the old lock is untouched.

The shell-out path is deleted; `PackageManagerExecutor` survives only for `DependencyWorkspace`
(type attribution), which is out of scope.

## Phasing

| Phase | Scope | Status |
|---|---|---|
| **A** | Registry client + discovery/creds, isolated node-semver, `NativeLockEngine` with the strict layout whitelist, and byte-exact patchers for npm (v2/v3), pnpm (v9/v6), yarn-classic, and bun, all with full workspace support; failure model + data table; recipe wiring; PM-free E2E tests against goldens recorded from real installs. | Implemented |
| **A-follow** | Yarn Berry checksum reproduction (tarball fetch + yarn's normalized-zip hash). | Implemented |
| **B** | Whole-closure regeneration for adds and closure-changing upgrades. | Implemented as lock-seeded resolution plus per-format graph-to-lock diffs feeding the same patchers; two-phase incremental fixtures (a real before lock, then the real incremental after) pin the bytes |

## Testing strategy

Byte-level goldens recorded from real `npm`/`pnpm`/`yarn`/`bun` output, asserted through the native
patch with no package manager at test time; a node-semver conformance corpus alongside the unchanged
Maven/Gradle suites; an in-process stub `HttpSender` serving recorded packument/manifest responses for
the engine and recipe tests. The previous PM-driven recipe tests are retained as a package-manager-
gated parity cross-check that keeps the goldens honest.

## Consequences

- Lock regeneration works wherever the JVM runs — no Node toolchain, no interpreter coupling, no
  credential loss in a scratch sandbox. The failure surface shrinks from "the whole graph plus the
  environment" to "a moving package whose registry is unreachable, or an edit outside the safe subset,"
  and those become structured, aggregatable data instead of opaque subprocess stderr.
- Semantics change from re-install to minimal update: diffs contain only what the recipe caused, which
  is more reviewable for fleet-scale change, but runs that previously also picked up unrelated newer
  versions no longer do.
- We own a node-semver implementation and five format patchers that track the package managers. The
  tracked surface is small and slow-moving, pinned per format by real-output golden fixtures, but it is
  a real maintenance commitment — the same one rewrite-maven made, at Node's scale.
- The node-semver, registry-client, and `.npmrc` layers are Node-ecosystem infrastructure usable
  beyond lock regeneration (version comparison, dependency insight, registry-aware recipes).
