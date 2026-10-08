# 13. Lock regeneration follows each package manager's own resolver

Date: 2026-10-07

## Status

Accepted. Supersedes the npm, Bun and Yarn classic parts of [ADR 0011](0011-native-node-lock-regeneration.md).

## Context

ADR 0011 regenerates a Node lock natively (no package manager runs) with one engine for five package
managers: a flat `name -> versions` resolver (then `NpmGraphBuilder`) behind boolean flags, a per-dependency
proof that an edit leaves the installed tree unchanged, and a byte-preserving patcher per lock format.
The engine has no model of the installed tree, so placement is re-derived from the lock text wherever it
is needed, peers are not edges, and every shape the proof cannot express is a refusal. The npm path held
383 such refusal sites, many reading "not yet"; a peer that resolves to several versions
(openrewrite/rewrite#8932) was one of them, and each fix added another guard. Recording real npm also
showed the proof's premise wrong: an incremental install warns and writes a lock where the engine
predicted a failure, and a fresh install of the same manifest fails.

## Decision

Lock regeneration follows each package manager's own resolver: the module that package manager runs is
ported, in its own package, and the lock is written the way that package manager writes it. Each package
manager gets its own port against its own resolver; the shared engine of ADR 0011 remains for pnpm only
until its port exists, and carries no npm, Bun, Yarn classic or Yarn Berry code.

For Bun the resolver is Bun 1.3.10's package manager, ported from its Zig sources into
`org.openrewrite.javascript.internal.lock.bun`: the lockfile model (`Lockfile`, `Package`, `Dependency`,
`Resolution`), the bun.lock reader and writer, the hoisted `Tree`, the root's diff (`PackageDiff`), the
resolver (`PackageManager`: enqueueing, prefer-existing versions, peers last) and `BunLockRegenerator`
(`install_with_manager.zig` for `bun install --lockfile-only`). Bun's result can depend on the order
manifests arrive from the network; where it does, the engine says it cannot know.

For Yarn classic the resolver is yarn 1.22.22's, ported into `org.openrewrite.javascript.internal.lock.yarnclassic`:
the lock as a map from pattern to entry (`Lockfile`, `LockfileParser`, `LockfileStringifier`), the resolver
(`PackageResolver`, `PackageRequest`, `PackageReference`, `NpmResolver` and the exotic and workspace resolvers,
`ResolutionMap` with minimatch 3.0.4), `PackageCompatibility`, the config (`YarnClassicOptions`) and
`YarnClassicLockRegenerator` (`install.js` without node_modules). yarn's find runs on a model of Node's event loop:
which of two patterns of one version is resolved first decides the entry they share, and where that follows
the order the registry answers in, the engine says it cannot know.

For Yarn Berry the resolver is Yarn 4.10.3's, ported into `org.openrewrite.javascript.internal.lock.berry`: the
lock as stored descriptors, resolutions and packages (`Project`, `Syml`, `Manifest`, `structUtils`), the resolver
chain (`LockfileResolver`, `LegacyMigrationResolver`, `WorkspaceResolver` and each plugin's resolver and fetcher),
`Project.resolveEverything` with the virtual pass that decides which packages are optional, the fetch step's
checksums (`Cache`, `TgzUtils`, `ZipArchive`), the configuration (`BerryOptions`, with each release's settings,
builtin extensions and patches as data) and `YarnBerryLockRegenerator` (`yarn install --mode=update-lockfile`).
Peers never change the lock; a builtin compat patch added or changed, a fresh checksum under a non-zero
`compressionLevel`, and git, `file:` and remote tarball contents are what the engine cannot know.

For npm the resolver is `@npmcli/arborist` (npm 11.17.0), ported module for module into
`org.openrewrite.javascript.internal.lock.npm`, same names, same control flow and iteration order:
`Node`, `Edge`, `Inventory`, `Link` (the tree), `DepValid`, `Spec` (npm-package-arg), `Shrinkwrap` (the
lock as data, with `YarnLock`), `JsonStringifyNice`, `VirtualTreeLoader` (lock plus edited manifests to
a tree), `IdealTreeBuilder` with `PlaceDep`, `CanPlaceDep`, `PeerEntrySets`, `DeepestNestingTarget` and
`GatherDepSet`, `DepFlags`, `OverrideSet`, `PickManifest` (npm-pick-manifest), `NpmInstallChecks`,
`Pacote` (the registry manifest), `NpmOptions` (npm config, read from the project's `.npmrc` through
`ini`), `MapWorkspaces`, and `NpmLockRegenerator` (`arborist/index.js` for a lock-only install). One
tree is loaded from the lock, rebuilt for the edited manifests and written whole; there is no patching
of the old text. Peers are edges, placement is `PlaceDep`, flags are one function of the tree, overrides
are rules on edges. What a lock-only install never runs (reify, scripts, bins, audit) is omitted, not
stubbed.

Three rules decide the result:

1. Byte-exact or a failure: never a guess, never a silently unchanged lock.
2. A failure is for what the engine cannot know (tarball contents, a git, file or directory manifest,
   the filesystem, the environment) or what npm itself rejects (`ERESOLVE`, `ETARGET`, `EOVERRIDE`,
   `E404`, ...). Every failure says which. An unported part of arborist is a work item, not a failure.
3. The written lock is a fixpoint: the regenerator loads it and builds again; a changed lock or an
   `ERESOLVE` there is a failure (`PEER_CONFLICT`, naming both sides), because npm sometimes warns,
   writes, and rejects that lock on the next install.

The oracle is npm itself: what npm does is settled by running it, or by arborist's own test
expectations, never by reading. A scenario is a directory: `pkg-before`, `before` (npm's lock for it),
`pkg`, `after` (npm's lock for the edit over `before`), `npmrc`, the registry responses under `http/`,
and the provenance logs; the engine replays it offline and must reproduce `after` byte for byte or the
recorded failure. The scenarios recorded from npm 11.17.0 (some five hundred) pin the shapes of #8932,
every refusal of the old engine, every override shape, arborist's own build-ideal-tree tests turned into
edits, real projects under recipe-shaped edits, the questions the ports raised, and a small form of each
behavior. The small forms are committed under `src/test/resources/lock/npm/` (about two hundred, a few
hundred kilobytes at most each); the rest is a corpus the suite replays when `REWRITE_LOCK_SCENARIOS`
names it. Tests follow the components: one class per ported module, from arborist's own tests; a round
trip of every lock we have through load, flags and write; one parameterized scenario test. Adding a
scenario is adding a directory, never Java. A test expectation changes only when a recording from npm
says so.

## Consequences

- npm regeneration is byte-exact wherever npm succeeds and fails only for the kinds above; the
  `RESOLUTION_REQUIRED` refusal for an unproven shape no longer exists on the npm route.
- The port reproduces npm's incremental behavior, quirks included: a removal never re-hoists, a lowered
  override nests under the dependent, a fresh install of the same manifest may differ.
- The maintenance commitment is tracking arborist: the port names its source version, and a new npm
  release is a diff against that version, replayed against the corpus.
- pnpm still carries the ADR 0011 engine and its refusals; it is a separate port of its own resolver, in
  its own package, under the same rules and the same kind of oracle.
