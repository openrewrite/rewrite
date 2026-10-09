# 13. Lock regeneration follows each package manager's own resolver

Date: 2026-10-07

## Status

Accepted. Supersedes [ADR 0011](0011-native-node-lock-regeneration.md). Amended 2026-10-09: every package manager
regenerates through the port of its own resolver, and the shared engine of ADR 0011 is removed.

## Context

ADR 0011 regenerates a Node lock natively (no package manager runs) with one engine for five package managers: a flat
`name -> versions` resolver behind boolean flags, a per-dependency proof that an edit leaves the installed tree
unchanged, and a byte-preserving patcher per lock format. With no model of the installed tree, placement is re-derived
from the lock text, peers are not edges, and every shape the proof cannot express is a refusal: 383 on the npm path
alone, a peer that resolves to several versions (openrewrite/rewrite#8932) among them. Recording real npm also showed
the proof's premise wrong: an incremental install warns and writes a lock where the engine predicted a failure.

## Decision

Lock regeneration follows each package manager's own resolver. The modules that package manager runs are ported into
its own package with their names, control flow and iteration order; the lock is loaded once into that package
manager's own model and written whole, as it writes it. `NativeLockEngine` builds the package manager's inputs from
the request and calls its port. What ports share is Node's behavior (V8's `Date` and `RegExp`, WHATWG URL, path, ini,
`localeCompare`) and the host's Node version and platform, never resolution.

- npm 11.17.0, `@npmcli/arborist`, in `internal.lock.npm`: the tree (`Node`, `Edge`, `Inventory`, `Link`), `Shrinkwrap`,
  `VirtualTreeLoader`, `IdealTreeBuilder` with `PlaceDep`, `CanPlaceDep` and `PeerEntrySets`, `DepFlags`, `OverrideSet`,
  `PickManifest`, `NpmOptions`, `NpmLockRegenerator`. Peers are edges; flags are one function of the tree.
- Bun 1.3.10 in `internal.lock.bun`: `Lockfile`, the bun.lock reader and writer, the hoisted `Tree`, `PackageManager`
  (enqueueing, prefer-existing versions, peers last), `BunLockRegenerator`.
- Yarn classic 1.22.22 in `internal.lock.yarnclassic`: the lock as patterns to entries, `PackageResolver` and
  `PackageRequest` on a model of Node's event loop, `YarnClassicOptions`, `YarnClassicLockRegenerator`.
- Yarn Berry 4.10.3 in `internal.lock.berry`: `Project.resolveEverything` with the virtual pass, the resolver and
  fetcher chain, checksums (`Cache`, `TgzUtils`, `ZipArchive`), each release's settings and builtin data as resources,
  `YarnBerryLockRegenerator`.
- pnpm 11.2.2 in `internal.lock.pnpm`: the lock keyed by peer suffix, `PnpmOptions` with pnpm-workspace.yaml,
  `ResolveDependencyTree`, `ResolveDependencies` and `ResolvePeers` on a model of Node's event loop, `UpdateLockfile`
  with the pruner, `PnpmLockRegenerator`.

Three rules decide the result:

1. Byte-exact or a failure: never a guess, never a silently unchanged lock.
2. A failure is for what the engine cannot know (tarball contents, a git, file or remote manifest, the filesystem, the
   environment, an outcome that follows network timing) or what the package manager itself rejects. Every failure
   says which. An unported part of a package manager is a work item, not a failure.
3. The written lock is a fixpoint: the regenerator installs again over it, and a changed lock or a rejection there is
   a failure, because npm sometimes warns, writes, and rejects that lock on the next install (`PEER_CONFLICT`).

The oracle is the package manager itself: what it does is settled by running it, or by its own test expectations,
never by reading. A scenario is a directory: `pkg-before`, `before`, `pkg`, `after`, the package manager's config files,
the registry responses under `http/`, the provenance logs and an `expect` line. The regenerator replays it offline and
must reproduce `after` byte for byte, or the recorded failure. Small scenarios are committed under
`src/test/resources/lock/<pm>/`; the rest is a corpus replayed when `REWRITE_LOCK_SCENARIOS` names it. Tests follow the
components: one class per ported module from the package manager's own tests, a round trip of every lock, one
parameterized scenario test per package manager. Adding a scenario is adding a directory, and a test expectation
changes only when a recording says so.

## Consequences

- Each package manager's regeneration is byte-exact wherever it succeeds and fails only for the kinds above; the
  refusal of a shape the old proof could not express exists on no route.
- The ports reproduce incremental behavior, quirks included (npm never re-hoists on a removal, pnpm keeps a ranged
  dependent on its locked version, Bun re-resolves every edge of a changed workspace member), so a fresh install of the
  same manifest may differ.
- The shared engine of ADR 0011 is gone: `GraphBuilder`, `RegistryAdapter`, `ResolutionGraph`, `ResolvedNode`,
  `LockEditSet`, `LockPatcher`, `LockJson`, `LockYaml`, `YarnLock`, `EngineFailure` and every per-format diff and
  patcher. The regeneration request no longer carries the pre-edit manifest its per-dependency proof read.
- The maintenance commitment is tracking five resolvers: each port names its source version, and a new release is a
  diff against that version, replayed against the corpus.
