# 11. Native npm lock file regeneration

Date: 2026-07-29

## Status

Proposed

Applies the approach of [9. Native Python lock file regeneration](0009-native-python-lock-regeneration.md)
to the Node ecosystem, npm first.

## Context

When a dependency recipe edits a `package.json`, the sibling lock file must be updated to
match. Regeneration previously ran the package manager (`npm install --package-lock-only
--ignore-scripts --legacy-peer-deps`) in a temp directory. At fleet scale this fails for
environmental reasons unrelated to the edit — the package manager missing from the
worker, registry credentials not reaching the sandbox — and the run finishes with the
manifest changed and the lock stale, surfacing only truncated subprocess stderr
(customer-requests#2893).

Node is materially easier to regenerate natively than Python was:

- **One registry request per moving package.** The full packument carries each version's
  dependencies, `engines`, `license`, `funding`, `bin` and the `dist.integrity` hash the
  lock needs — npm itself resolves with `fullMetadata: true`. Python's three-tier
  metadata ladder has no analogue here, and no tarball is ever downloaded.
- **No backtracking.** npm permits coexisting versions, so resolution is a greedy tree
  walk; the only search-like behavior is placement (below).
- **The lock records the requested state.** The root `""` entry mirrors the manifest's
  dependency blocks, so the edit set falls out of a diff with no external oracle.

And harder in one way: `package-lock.json` records a **physical `node_modules` tree**.
Whether a package lands hoisted at `node_modules/x` or nested under a dependent is a
placement decision (arborist's `placeDep`), and `dev`/`optional`/`devOptional`/`peer`
flags are whole-graph reachability colors.

npm's emission is fully deterministic and reimplementable: the lock is
`json-stringify-nice(data, swKeyOrder, indent) + '\n'` with `\n` mapped to the source
file's EOL — every object's keys sorted scalars-before-objects, `swKeyOrder` first, then
`localeCompare('en')`.

## Decision

Replace the npm shell-out with a **native Java engine** behind the existing
`LockFileRegeneration` seam, preserving ADR 0009's contract: minimal update, untouched
entries byte-for-byte, fail loud with a structured failure rather than guess, and no
dual mechanism (the npm shell-out is deleted; the other package managers keep theirs
until they go native).

The engine (`internal/npmlock/NpmLockEngine`) handles the provable subset:

- a **version move** whose closure stays satisfied by the existing tree, checked in both
  directions: the new version's requirements resolve satisfied from its location
  (nested copies shadowing, `overrides` substituted), and every dependent that resolved
  the old copy still accepts the new version;
- **removals**, with an orphan sweep and flag recoloring via arborist's own
  `calc-dep-flags` fixed-point algorithm — applied only where the edit changed an
  entry's color, so pre-existing drift is never rewritten as a side effect;
- **top-level additions** that fit without displacing anything;
- **range edits** the current pin already satisfies (zero network);
- version selection matching npm-pick-manifest: the `latest` dist-tag wins when it
  satisfies the range, else the highest satisfying version.

Everything else fails loud with `RESOLUTION_REQUIRED` or `UNSUPPORTED_ENTRY_TYPE`:
cascading placement, peer conflicts, workspace and `link:` locks, git/file/alias/tarball
specs, bundled dependencies, lockfileVersion &lt; 3, and **override edits that move a
pin** — the recorded `override` fixture shows npm re-places overridden copies (nesting
them under dependents), which is placement, not substitution. A lock that fails the
parse→emit round-trip (hand-edited, or written by npm &lt; 9) is refused rather than
normalized wholesale.

Supporting layers, mirroring ADR 0009's stack:

- `internal/semver`: node-semver versions and ranges ported from npm/node-semver and
  driven by its own fixture corpus.
- `internal/registry`: packument client on the run's `HttpSender`; `.npmrc` resolution
  (scoped registries, the npm-registry-fetch auth-key walk, `${VAR}` expansion failing
  only when used); host overrides via `JavaScriptExecutionContextView`
  (the `MavenExecutionContextView` pattern).
- `internal/npmlock/NpmLockWriter`: the `json-stringify-nice` emission ported exactly,
  including an `Intl`-collator-equivalent key comparator pinned by Node-generated
  vectors. Round-trip byte identity over every npm-written fixture is the founding
  invariant.

Failures carry a `Reason`/package/registry/detail structure surfaced three ways: the
existing manifest warning, a warning **on the unchanged lock file itself**, and a
`NodeLockRegenerationFailures` data table for fleet-scale aggregation. On any failure
the manifest edit still applies and the old lock is untouched, deferring to CI's
`npm ci` — the defer-to-CI composition from ADR 0010.

Two deliberate divergences from a local npm run, both environment-decoupling:
npm-pick-manifest deprioritizes versions whose `engines` exclude the *locally running*
node — the engine ignores engines during selection so results don't depend on the
worker's node version; and lockfileVersion 1/2 locks fail loud rather than being
rewritten wholesale to v3 as a modern npm would.

### Testing

Scenario fixtures recorded from real npm 11.17.0 (`src/test/resources/npmlock/`,
regenerable via `record.sh`): before/after manifests, before/after locks, and the full
packuments for every moving package, captured together. Offline tests replay them
through a routed `HttpSender` and assert the engine's output **byte-identical to what
npm produced for the same edit**, plus that nothing escaped to the network. node-semver
conformance runs its transposed fixture corpus; the writer's collation is pinned by
Node-sorted vectors; recipe-level tests run the full scan/edit/regenerate/overlay flow
pure-JVM.

## Consequences

- npm lock regeneration works wherever the JVM runs: no node, no npm, no PATH or
  credential coupling. Registry access uses the run's `HttpSender` with
  host-configurable credentials, the same operational posture as rewrite-maven.
- Coverage is the provable subset; the residual (cascades, placement, workspaces,
  Yarn/pnpm/bun) still shells out or defers to CI, now with structured, aggregatable
  failure data that will size the next phase (ADR 0010's sizing-before-building
  sequence).
- Yarn Berry can never be fully native: its lock `checksum` hashes the zip archive Yarn
  re-packs from the tarball, not the registry artifact. Any Berry strategy is a separate
  decision.
- We own a semver port, a registry client, and an emitter that track npm. The tracked
  surfaces are small and slow-moving, and the fixture corpus pins them per npm version —
  the same maintenance posture as rewrite-python's engines.

## Addendum: behavior changes surfaced in review

- The deleted shell-out ran npm with `--legacy-peer-deps`, tolerating peer conflicts
  (lagging third-party libraries during major framework bumps). The native engine
  enforces peers strictly and defers those edits to CI — a coverage regression the
  failures table will quantify.
- npm's environment-variable configuration (`npm_config_registry`, `NPM_CONFIG_*`,
  `npm_config_//host/:_authToken`) is not consulted: registry discovery reads only the
  captured `.npmrc` files (with `${VAR}` expansion) and
  `JavaScriptExecutionContextView`. Hosts injecting registries via environment
  variables must configure the view instead.
