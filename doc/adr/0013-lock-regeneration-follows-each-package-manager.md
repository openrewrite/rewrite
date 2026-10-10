# 13. Lock regeneration follows each package manager's own resolver

Date: 2026-10-07

## Status

Accepted. Supersedes [ADR 0011](0011-native-node-lock-regeneration.md). Amended 2026-10-09: every package manager
regenerates through the port of its own resolver, and the shared engine of ADR 0011 is removed. Amended 2026-10-10:
what `internal.lock` holds, and how the recipes write overrides and follow catalogs for each package manager.

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
the request, seen once from the lock's directory (`LockDirectory`), and calls its port. What ports share lives in
`internal.lock` and is Node's behavior, never resolution: JavaScript's value semantics (`EcmaScript`, which each port's
`Js` extends), Node's event loop and promises (`EventLoop`), V8's `Date`, `RegExp` and sort, WHATWG URL,
path, ini, `localeCompare`, the host's Node version and platform (`Host`) and environment (`HostEnvironment`).

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
   carries its kind (`LockFileRegeneration.Kind`: cannot know, the package manager rejects or fails, discards or
   deletes the lock, a second install), and the errors every package manager shares (a missing package, no matching
   version, an engine or platform it excludes) are reported under one reason (`SharedError`). An unported part of a
   package manager is a work item, not a failure.
3. The written lock is a fixpoint: the regenerator installs again over it (`LockOnlyInstall.verify`), and a changed
   lock or a rejection there is a failure, because npm sometimes warns, writes, and rejects that lock on the next
   install (`PEER_CONFLICT`).

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
  `LockEditSet`, `LockPatcher`, `LockJson`, `LockYaml`, `YarnLock`, `EngineFailure`, every per-format diff and
  patcher, and the registry client's typed packument and manifest. The regeneration request no longer carries the
  pre-edit manifest its per-dependency proof read.
- The maintenance commitment is tracking five resolvers: each port names its source version, and a new release is a
  diff against that version, replayed against the corpus.

## Amendment 2026-10-10: the shared package, overrides and catalogs

`internal.lock` holds what more than one port needs, and nothing that resolves:

- the dispatch (`NativeLockEngine`), the request seen from the lock's directory (`LockDirectory`), and per package
  manager an `*Inputs` mapper from it to that port's own `Inputs` value, which selects the files the package manager
  reads and declares their names, so the recipes' scan captures only those, and a registry source over the host's
  client;
- the failure model: `CannotKnow`, `Rejected`, `SharedError`, and `LockOnlyInstall`, which words a failure with the
  package manager's command, gives it one of four kinds (cannot know, the package manager rejects, it fails, the
  second install rejects or rewrites the lock) and runs the second install;
- Node's and V8's behavior: `EcmaScript`, `EventLoop`, `V8Date`, `V8RegExp`, `V8Sort` (V8's TimSort at any length),
  `WhatwgUrl`, `UriComponent`, `PosixPath`, `RootRelativePath`, `Ini`, `LocaleCompare`, `Host`, `HostEnvironment`;
- one port of some upstream libraries that several package managers bundle, the release an option: `Spec`
  (npm-package-arg), `GitHost` (hosted-git-info), `Picomatch` (2.3.1 and 4.0.4), `JsYaml` (3.13.1 and 3.14.1),
  `YarnpkgSemverUtils`. Others are still one port per package manager: minimatch (npm's 10.2.5, Yarn classic's
  3.0.4), brace-expansion, ssri, ini (Yarn classic's 1.3.5 beside `Ini`, ini 6.0.0), braces and fill-range (Yarn
  classic's, Berry's), glob-parent and is-glob, pnpm's own npm-package-arg and hosted-git-info inside its projects
  graph, and pnpm's js-yaml fork;
- the registry sources ask the registry each port's own config names for the package, and give the request the
  host's credentials by its URL for npm and pnpm (the longest registry above it holding credentials, as npm's
  `getAuth` and pnpm's `getAuthHeaderByURI` pick them), by the package's scope for Yarn and Bun.

`Spec` is also the one classifier of a manifest's dependency value for the recipes: npa's kinds, and the catalog,
workspace, link, portal, patch, jsr and exec protocols npa rejects, each its own kind. A recipe leaves a value that is
no version range unchanged and records it in `NodeDependencyProtocolsSkipped`.

Overrides are written where the project's package manager reads them, at the workspace root only, and the lock follows:

- npm: `overrides`; a package already overridden by an object gets its `"."`. Bun: `overrides`, else `resolutions`
  when the root has only that; a dependency path is declined, since Bun 1.3.10 ignores nested overrides.
- Yarn classic: `resolutions` with `**/<path>/x`, since `parent/x` matches from the root only; the resolution yarn
  reads as the same glob (`x` is `**/x`) is edited in place, since yarn applies the first that matches. Yarn Berry:
  `resolutions` with one parent, by name or exact version.
- pnpm, by the release the project names (`devEngines.packageManager`, `packageManager`, 11.2.2 when none, the rule
  `PnpmOptions.releaseMajor`; a range open between majors is declined, the registry picking the release): 11 reads
  pnpm-workspace.yaml only; 10 reads package.json's `pnpm.overrides` and `resolutions` instead of the workspace's when
  they hold any; 9 reads package.json only. The recipe writes where that release's own writer would, editing
  pnpm-workspace.yaml in place or creating the key or file as pnpm places it; a release other than 11.2.2 makes the
  regeneration cannot-know.
- An override the package manager cannot read, or one that would win over a declaration through a value that is no
  version range (a catalog or workspace reference), is declined with a `NodeDependencyEditsDeclined` row and writes
  nothing; an override scoped to a parent that pnpm or Berry applies before a global one is named in such a row.

Catalogs are read where each package manager reads them (pnpm's pnpm-workspace.yaml, Bun's root `workspaces` with its
top-level fallback, Yarn 4.10's `.yarnrc.yml`), as `NodeCatalogs`, whose entries come from the OpenRewrite YAML or JSON
tree and whose pnpm settings come from pnpm's own reader. A recipe that changes a `catalog:` dependency edits the entry
in place, quoting kept, and the regeneration reads the edited file. ChangeDependency renames the key and gives the
catalog the new name's entry; RemoveDependency does what `pnpm remove` does under `cleanupUnusedCatalogs`, the cleanup
counting the projects the workspace's `packages` match; AddDependency saves what `pnpm add` saves (`lock.pnpm.
InstallSome`: the default catalog's match, `catalogMode`, `saveCatalogName`), the version for pnpm 9, and the member's
dependencies sorted, as pnpm writes them. pnpm-workspace.yaml is edited by two editors, a line editor for an override
and the YAML tree for a catalog entry; both place a new key through one port of pnpm's workspace manifest writer, the
catalogs at pnpm 11's rules.

The five dependency recipes are one engine, `NodeDependencyEdits`: each adds its edits of manifests and
package-manager files to the cycle in the order its `generate` runs (`PackageJsonHelper.FileEditor`), decides from the
documents as the recipes before it leave them, and regenerates from those, so the lock written last holds every
recipe's edit in any visit order. An override is the root's decision, an edit of package.json or of
pnpm-workspace.yaml. Another recipe's edit is seen only at its file's visit: a lock written before that visit from an
earlier revision is warned with one row, and a decision that visit overturns after files were written by it is
withdrawn, warned and rowed. No marker is read as state: declines are derived from the documents each cycle, a marker
is a content-equal warning, and rows are deduplicated within a cycle.
