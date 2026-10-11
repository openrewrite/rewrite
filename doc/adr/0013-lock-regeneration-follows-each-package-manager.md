# 13. Lock regeneration follows each package manager's own resolver

Date: 2026-10-07

## Status

Accepted. Supersedes [ADR 0011](0011-native-node-lock-regeneration.md). Amended 2026-10-09: every package manager
regenerates through the port of its own resolver, and the shared engine of ADR 0011 is removed. Amended 2026-10-10:
what `internal.lock` holds, and how the recipes write overrides and follow catalogs for each package manager; and
again: one port per library, the host's registries as the user's config, corepack, the regenerators by module.
Amended 2026-10-11: later releases of the ported pnpm and Yarn majors.

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
  `PackageRequest` on a model of Node's event loop, `Config` (config.js), `Rc`, `Cli`, `Install`,
  `YarnClassicLockRegenerator`.
- Yarn Berry 4.10.3 in `internal.lock.berry`: `Project.resolveEverything` with the virtual pass, the resolver and
  fetcher chain, checksums (`Cache`, `TgzUtils`, `ZipArchive`), each release's settings and builtin data as resources,
  `YarnBerryLockRegenerator`.
- pnpm 11.2.2 in `internal.lock.pnpm`: the lock keyed by peer suffix, `PnpmOptions` with pnpm-workspace.yaml,
  `ResolveDependencyTree`, `ResolveDependencies` and `ResolvePeers` on a model of Node's event loop, `UpdateLockfile`
  with the pruner, `Main`, `InstallDeps`, `Recursive`, `Install`, `ExtendInstallOptions`, and `PnpmLockRegenerator`
  for main.ts's install entry.

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
must reproduce `after` byte for byte, or the recorded failure. Small scenarios live in
openrewrite/rewrite-javascript-lock-fixtures, which the build puts on the test classpath; the rest is a corpus replayed
when `REWRITE_LOCK_SCENARIOS` names it. Tests follow the
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

- the dispatch (`NativeLockEngine`), the request seen from the lock's directory (`LockDirectory`), the host's side of
  it (`HostInputs`: machine, time, environment, registries with their credentials), and per package manager a `Port`,
  its `*Inputs`, which maps both to that port's own `Inputs` value, selects the files the package manager reads and
  declares their names, so the recipes' scan captures only those, and holds the lock's name and the port's
  `ManifestRules`;
- the failure model: `CannotKnow` (with the one rule for a file the host holds without its contents), `Rejected`,
  `SharedError`, `JsError` (an error JavaScript or Node raises inside a package manager, by its name), and
  `LockOnlyInstall`, which words a failure with the package manager's command, gives it one of four kinds (cannot
  know, the package manager rejects, it fails, the second install rejects or rewrites the lock), checks that a walk
  installs at the lock and runs the second install; a `Result` carries the files the install writes besides the lock
  (npm's yarn.lock);
- Node's and V8's behavior: `EcmaScript`, `EventLoop`, `V8Date`, `V8RegExp`, `V8Sort` (V8's TimSort at any length),
  `WhatwgUrl`, `UriComponent`, `PosixPath`, `RootRelativePath`, `Ini`, `LocaleCompare`, `Host`, `HostEnvironment`;
- one port of each upstream library that several package managers bundle, the release an option (the amendment
  below lists them);
- the registry sources ask the registry each port's own config names for the package, and give the request the
  host's credentials by its URL for npm and pnpm (the longest registry above it holding credentials, as npm's
  `getAuth` and pnpm's `getAuthHeaderByURI` pick them); Bun's by the registry's host, as bun gives a user .npmrc
  credential to every registry there; Yarn's as each Yarn's own request computes them from its config.

`ManifestRules`, implemented in each port's package, is what the recipes ask about the files they edit: where and
under which key the package manager reads an override, whether it applies it and what it applies first, its catalog
file and the catalogs its own reader reads there, a catalog reference, pnpm's release and settings, and how its add
saves a manifest. The recipes hold no rule of a package manager of their own.

`Spec` is also the one classifier of a manifest's dependency value for the recipes: npa's kinds, and the catalog,
workspace, link, portal, patch, jsr and exec protocols npa rejects, each its own kind. A recipe leaves a value that is
no version range unchanged and records it in `NodeDependencyProtocolsSkipped`.

Overrides are written where the project's package manager reads them, at the workspace root only, and the lock follows:

- npm: `overrides`; a package already overridden by an object gets its `"."`. Bun: `overrides`, else `resolutions`
  when the root has only that; a dependency path is declined, since Bun 1.3.10 ignores nested overrides.
- Yarn classic: `resolutions` with `**/<path>/x`, since `parent/x` matches from the root only; the resolution yarn
  reads as the same glob (`x` is `**/x`) is edited in place, since yarn applies the first that matches. Yarn Berry:
  `resolutions` with one parent, by name or exact version.
- pnpm, by the release pnpm runs (`PnpmRelease`: the one the root names and pnpm switches to, 11.2.2 otherwise; a
  range open between majors, or a release whose writes are not recorded, is declined): 11 reads
  pnpm-workspace.yaml only; 10 reads package.json's `pnpm.overrides` and `resolutions` instead of the workspace's when
  they hold any; 9 reads package.json only. The recipe writes where that release's own writer would, editing
  pnpm-workspace.yaml in place or creating the key or file as pnpm places it; a release outside 11.0.6 - 11 makes
  the regeneration cannot-know.
- An override the package manager cannot read (its own parser's answer: pnpm's `parsePkgAndParentSelector`, Berry's
  `parseResolution`), or one that would win over a declaration through a value that is no version range (a catalog or
  workspace reference), is declined with a `NodeDependencyEditsDeclined` row and writes nothing; an override scoped to
  a parent that pnpm, Berry or Yarn classic's `find` applies before a global one is named in such a row.

Catalogs are read where each package manager reads them (pnpm's pnpm-workspace.yaml, Bun's root `workspaces` with its
top-level fallback, Yarn 4.10's `.yarnrc.yml`), as `NodeCatalogs`, whose entries come from the OpenRewrite YAML or JSON
tree and whose pnpm settings come from pnpm's own reader. A recipe that changes a `catalog:` dependency edits the entry
in place, quoting kept, and the regeneration reads the edited file. ChangeDependency renames the key and gives the
catalog the new name's entry; RemoveDependency does what `pnpm remove` does under `cleanupUnusedCatalogs`, the cleanup
counting the projects the workspace's `packages` match; AddDependency saves what `pnpm add` saves (`lock.pnpm.
ParseWantedDependencies`, `Install.installSome`'s `catalogMode` check, `ResolveDependencyTree`'s catalog lookup for
`saveCatalogName`), the version for pnpm 9, and the member's dependencies sorted, as pnpm writes them; a release range
open between majors is declined. pnpm's cleanup is the port's (`WorkspaceManifestWriter.cleanupUnusedCatalogs`, the
recipes removing from the tree what it removed). pnpm-workspace.yaml has one editor, the YAML tree, placing a new key
by the port of pnpm's workspace manifest writer for the release the root names, for an override as for a catalog
entry. A catalog reference is read by each port's own parse, and the package manager a manifest names by corepack's
`loadSpec` (below).

The five dependency recipes are one engine, `NodeDependencyEdits`: each adds its edits of manifests and
package-manager files to the cycle in the order its `generate` runs (`PackageJsonHelper.FileEditor`), decides from the
documents as the recipes before it leave them, and regenerates from those, so the lock written last holds every
recipe's edit in any visit order. An override is the root's decision, an edit of package.json or of
pnpm-workspace.yaml. Another recipe's edit is seen only at its file's visit: a lock written before that visit from an
earlier revision is warned with one row, and a decision that visit overturns after files were written by it is
withdrawn, warned and rowed. No marker is read as state: declines are derived from the documents each cycle, a marker
is a content-equal warning, and rows are deduplicated within a cycle.

## Amendment 2026-10-10: one port per library, the host's registries, corepack

One port per upstream library, in `internal.lock`, stands for every release a package manager bundles, the release an
option, and every upstream test case of every release runs against it (`lock/generate/upstream-calls.js` records each
call with its answer, `UpstreamCalls` replays them): `Minimatch` (3.0.4, 10.2.5 with its extglobs), `BraceExpansion`,
`Braces` and `FillRange`, `GlobParent`, `IsGlob`, `IsExtglob`, `Ssri`, `Ini` (1.3.5, 6.0.0), `StripBom`,
`ValidateNpmPackageName`, `Spec` (npm-package-arg 13.0.2 and pnpm's fork 2.0.0), `GitHost` (hosted-git-info 4.1.0
and 9.0.3), `JsYaml` (3.13.1, 3.14.1 and pnpm's @zkochan/js-yaml 0.0.11) with `JsYamlDumper`, `Picomatch`, and
`YamlDocument` (yaml 2.9.0's parseDocument and toString, with pnpm's patchDocument). A JavaScript built-in is ported
once too: `EcmaScript` (JSON.parse and JSON.stringify, parseInt, Number, Object.entries, the string functions),
`V8RegExp` (V8's u-flag grammar and messages; without the u flag Java still reads a surrogate pair as one code point),
`V8Date` (toISOString, Date.UTC, TimeClip), `UriComponent`, `PosixPath` for Node's `path.posix`.

The host's registry configuration is each package manager's user-level configuration, under the project's own files
and by its precedence. `HostInputs` renders it, over the marker's npmrc scopes that package manager reads (npm and Yarn
classic the global and user ones, pnpm and Bun the user's only, as recorded), never the marker's project npmrc, which
is the repository root's file, as the user .npmrc text each port reads through its own npmrc reader, or Berry's home
.yarnrc.yml. One rule says whether the host names a default registry: the view's list as given, else a `registry` key
in those npmrc files, so a host naming npmjs names it (yarn 1.22.22 then asks npmjs over a project .yarnrc, recorded):

- npm and pnpm take it as the user .npmrc, under the project's .npmrc; npm no longer falls back to npmjs where the
  project names no registry, and asks the host's;
- Bun takes the registries the host names as the user .npmrc, read after bunfig.toml and before the project's .npmrc,
  so a host default of npmjs is still a named default;
- Yarn classic takes them, with the host's credentials for the registries a project .npmrc names, as the home .npmrc,
  each credential under always-auth; Yarn Berry as the home .yarnrc.yml (`npmRegistryServer`, `npmScopes`,
  `npmRegistries` holding the credentials), which Yarn reads under every project rc file.

What the package manager writes with that configuration (resolved URLs, `tarball:` entries) is what the port writes;
each rule was recorded with a user config naming a second registry. Both Yarn ports send the Authorization header
their own request computes, through one source; npm's, pnpm's and Bun's sources still take the request's credentials
from `discover` over every scope.

corepack 0.35.0's specUtils is `SpecUtils`: `parseSpec`, `parsePackageJSON` (`packageManager` checked against
`devEngines.packageManager`, a mismatch corepack's error, the latter alone as a spec), `loadSpec` walking up through
the package.json files the host holds with the `.corepack.env` beside the one it settles on, `findProjectSpec` and the
release `installVersion` starts. A spec corepack rejects is the package manager rejecting the edit, with corepack's
message. Three questions ask it:

- Berry's release is the one corepack starts (`yarn@1` is corepack's error, not Yarn Berry);
- the package manager a lockless root names, for the recipes, is `loadSpec`'s (bun, which corepack does not run, by
  its name), else the one whose workspace files lie above it; a spec corepack rejects declines the edit with its
  message;
- pnpm's release, for the port and the recipes alike, is the one pnpm switches to or fails without (`pmOnFail`
  download or error), else 11.2.2 (`Main.release`). Where corepack would start another release or refuse the spec
  (devEngines beside packageManager, `onFail` or `pmOnFail` ignore or warn, a range or a value that is no string,
  recorded), the host does not say whether pnpm runs under corepack: the regeneration is cannot-know and the recipes
  decline. With the environment known, pnpm's own reader and `COREPACK_ROOT` decide, as in pnpm's tests.

A regenerator ports its package manager's entry module only. pnpm's main.ts install entry is `PnpmLockRegenerator`;
main.ts is `Main`, installDeps.ts `InstallDeps`, recursive.ts `Recursive` (with `bail: false`), install/index.ts
`Install`, extendInstallOptions.ts `ExtendInstallOptions`. Yarn classic's config.js is `Config`, set while `init` runs
and read only afterwards, beside `Rc`, `Cli` and `Install`; Berry's Configuration.ts is `Configuration`.

pnpm-workspace.yaml is printed as pnpm 10 and 11 print it, through `YamlDocument` and `WorkspaceManifestWriter`
(pnpm 10's sortKeysByPriority, pnpm 11's reorderRecursive and propagateBlankLinesToNewPairs), for the releases whose
writes are recorded (9.5.0 - 9.15.9, 10.32.1 - 10.34.6, 11.0.6 - 11); pnpm 9's catalogs and Yarn's .yarnrc.yml
are edited in place, and a layout the port does not read (document markers, CR line ends, a tab before a comment, a
flow mapping with entries) is declined. A workspace pnpm rejects (`catalog` beside `catalogs.default`, recorded) is
declined. AddDependency sorts the dependency fields as each package manager's add saves them, and still keeps inline
arrays and objects that npm, pnpm and yarn 1 print again with `JSON.stringify`.

## Amendment 2026-10-11: later releases of the ported majors

- pnpm: a release from 11.0.6 to below 12 (`Main.RUNS_AS_PORTED`) runs as 11.2.2, for the port (`SwitchCliVersion`,
  `Main.release`) and the recipes (`PnpmRelease`) alike. Recordings with 11.13.0 and 11.28.5 write the locks 11.2.2
  writes; 11.0.5 sorts what 11.0.6 appends. Another major stays cannot-know.
- Yarn Berry: every 4.x release the release data holds (4.0.0 to 4.18.1) runs as 4.10.3, with its own data and
  lockfile version (8, 9 from 4.14.0, 10 from 4.15.0: `YarnRelease.getLockfileVersion`). Recordings with 4.11.0 to
  4.18.1 write the entries 4.10.3 writes, but for one: a `npmPreapprovedPackages` entry that makes micromatch throw
  under the age gate fails 4.18.1's install, where 4.10.3 skips that version. Yarn 2 and 3, and a release after the
  data's, stay cannot-know.
