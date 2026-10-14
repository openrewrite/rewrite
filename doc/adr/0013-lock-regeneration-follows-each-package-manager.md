# 13. Lock regeneration follows each package manager's own resolver

Date: 2026-10-07

## Status

Accepted. Supersedes [ADR 0011](0011-native-node-lock-regeneration.md). Amended 2026-10-09: every package manager
regenerates through the port of its own resolver, and the shared engine of ADR 0011 is removed. Amended 2026-10-10:
what `internal.lock` holds, and how the recipes write overrides and follow catalogs for each package manager; and
again: one port per library, the host's registries as the user's config, corepack, the regenerators by module.
Amended 2026-10-11: later releases of the ported pnpm and Yarn majors; the npm release that built the LST,
with every npm release from 7.0.4 reproduced beside the one ported; every Yarn 4.x release reproduced beside
4.10.3; Bun releases reproduced beside 1.3.10; and Yarn 1 releases reproduced beside 1.22.22. Amended 2026-10-12:
pnpm releases reproduced beside 11.2.2; and the manifests a port is given.

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
  pnpm-workspace.yaml in place or creating the key or file as pnpm places it; a release outside
  `PnpmReleases.PROVEN` makes the regeneration cannot-know.
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
writes are recorded (9.5.0 - 9.15.9, 10.32.1 - 10.34.6, and the 11.x releases of `PnpmReleases.PROVEN`); pnpm 9's
catalogs and Yarn's .yarnrc.yml
are edited in place, and a layout the port does not read (document markers, CR line ends, a tab before a comment, a
flow mapping with entries) is declined. A workspace pnpm rejects (`catalog` beside `catalogs.default`, recorded) is
declined. AddDependency sorts the dependency fields as each package manager's add saves them, and still keeps inline
arrays and objects that npm, pnpm and yarn 1 print again with `JSON.stringify`.

## Amendment 2026-10-11: the manifests a port is given

Running the recipes on real repositories (openrewrite/rewrite#9151) showed locks written from an incomplete view of
the project. The view is now the repository's, and the port decides what belongs to the project:

- The scan gives a regeneration every package.json it read under the lock's directory and under each package.json
  above it, and the package.json of each directory a `file:`, `link:` or `portal:` dependency or a bare path names,
  wherever the repository holds it. Each port maps its workspaces over them with its own port of the package
  manager's glob; the marker's `workspacePackagePaths` decide nothing. A lock's importers are the manifests whose
  package manager's walk leads to it.
- A package.json the host holds without its contents (a quark, a parse error without text) is cannot-know where the
  package manager reads it, in every port: a workspace member is never left out of a lock, and npm's lock entry stands
  in for a link target only where the host holds no package.json there. A target above the repository's root is
  cannot-know. npm writes the name of a package only where its directory is named otherwise, so the package at the
  repository's own root, whose directory name the host does not know, is read where the lock already holds the name
  the manifest still declares, and is cannot-know otherwise.
- A manifest's package manager is its marker's where a lock of that package manager lies beside it. One without such
  a lock is installed by the nearest package manager above that holds its own lock and, run in the manifest's
  directory, installs there or stops on the way. The marker says otherwise for a workspace member parsed beside its
  installed node_modules (npm), which made the recipes edit it as a root of its own; the parser now walks up for a lock
  before it falls back on node_modules.
- A package.json parsed without its marker is kept as a file the package manager reads. A recipe that would edit it
  leaves it as it is and says so in `NodeDependencyEditsDeclined`: the marker records the registries a lock is
  regenerated from.

Scenarios that need more than one directory of a repository are recorded as `lock/repository/<package manager>/`
(`tree-before/`, `tree/`, `lock-dir`, an optional `seed/` for a lock the committed files have moved on from), and
`RepositoryScenarioTest` replays them through the recipes' own scan, decisions and visitor, with markers that name the
package manager only and again with every manifest that has no lock beside it marked as npm's.

## Amendment 2026-10-11: later releases of the ported majors

- pnpm (superseded by the amendment on pnpm releases below, which replaces `Main.RUNS_AS_PORTED` with
  `PnpmReleases.PROVEN`): a release from 11.0.6 to below 12 (`Main.RUNS_AS_PORTED`) runs as the port of 11.2.2,
  for the port
  (`SwitchCliVersion`, `Main.release`) and the recipes (`PnpmRelease`) alike; 11.0.5 sorts what 11.0.6 appends, and
  another major stays cannot-know. What a later 11.x release changed in a ported module is an option of that module's
  class, a range of releases asked of the release that installs (`Main.releaseIn`): the package manager check names
  that release (`Main.checkWantedPackageManager`: the one pnpm hands the install to, else 11.2.2), and the install's
  config carries it as pnpm's own `packageManager.version`, the resolver's `pnpmVersion`. Where the inputs do not say
  which side of an option it is on (a range of which the root's lock pins one, corepack starting another release than
  pnpm run directly), both sides are computed and a difference is cannot-know. One option so far: from 11.5.1 a
  missing optional peer is hoisted at a version only the lock holds
  (`ResolveDependencies.LOCKED_OPTIONAL_PEER_CANDIDATES`, pnpm/pnpm#12075), recorded with 11.2.2 and 11.5.0 dropping
  the peer and 11.5.1, 11.15.1 and 11.28.5 keeping it. The other changes of 11.3.0 to 11.28.5 to the ported modules
  are work items, so a later release is byte-exact per recording, not per release: recordings with 11.13.0 and
  11.28.5 write the locks 11.2.2 writes for the scenarios taken with them, while seek-oss/playroom's lock after one
  version bump, recorded with every release from 11.2.2 to 11.28.5, is the port's up to 11.5.1 and another one from
  each of 11.5.2, 11.7.0, 11.15.0, 11.28.0 and 11.28.1. Their changesets name what those releases changed (read, not
  recorded apart): the peer contexts the lock records are preferred (pnpm/pnpm#12083) and kept from a node's
  siblings (pnpm/pnpm#12320), an optional peer only `peerDependenciesMeta` declares is hoisted (pnpm/pnpm#13120), a
  name a package lists as a dependency and as an optional peer stays a dependency (pnpm/pnpm#15503), and a candidate
  for an optional peer is skipped when the importer provides one of its own peers at a version it rejects
  (pnpm/pnpm#15573).
- Yarn Berry: every 4.x release the release data holds (4.0.0 to 4.18.1) is reproduced beside 4.10.3, as the
  amendment on Yarn Berry releases below has it. Yarn 2 and 3, and a release after the data's, stay cannot-know.

## Amendment 2026-10-11: the npm release that built the LST

The npm port follows npm 11.17.0 and reproduces other releases beside it. A release is not inferred from the project:
npm reads `packageManager` for nothing, and a sweep of every npm release showed that releases a project's declarations
allow write different locks for the plainest edit (every release before 10.8.0 drops `license` from a new entry).

- **The release that installs** is the one that built the LST. The JavaScript parser records it in a marker beside
  `NodeResolutionResult`, `NodeRuntime`, as `JavaVersion` records the JDK that built a Java LST: the package manager it
  found, what that package manager's `--version` answers in the project's root directory, and the Node version and
  platform the parser ran on. The npm port installs as the marker's release, and as 11.17.0 without one. The machine
  engines and platforms are checked against is the execution context's where the host gives one, else the marker's.
  npm's own `devEngines` check (`lib/base-cmd.js`, from npm 10.9.0) runs for that release and rejects as npm does.
- **One question per upstream change.** `NpmRelease` holds each difference between 11.17.0 and another release as a
  question named for what npm does, answered through `atLeast` by the release that made the change; the ported module
  branches on it where upstream changed, 11.17.0's code unchanged beside it. A library npm bundles at several releases
  is ported once with its release as an option (`NodeSemver`, `Ssri`, `PickManifest`, `PackageJson`, `MapWorkspaces`),
  and `NpmRelease` names the one each npm bundles.
- **An older generation is a class beside the ported one.** Where a release's placement or tree model differs
  throughout rather than at a few points, the older module is ported whole and chosen through one question:
  `LegacyPlacement` (7.20.2's placement) before 7.20.3, and `LegacyNode` (arborist 1.0.14's tree model) before 7.1.1.
- **What is reproduced.** `NpmReleases.PROVEN` is the range of releases for which every recorded scenario replays as
  that release wrote it. A marker naming a release outside it, or one npm never published, is cannot-know.
- **How a release is proven.** Every npm release from 7.0.0 is run over every recorded scenario against a frozen
  registry: the scenario's own `http/`, a full packument it kept abbreviated rebuilt from today's registry cut back to
  the versions, tags and deprecations the recording saw, and an abbreviated packument whose versions an older
  recording kept bare filled from today's the same way (`http/<name>@abbreviated`, which the replays serve to a
  release before 10.4.0, the ones that read the abbreviated packument alone). The release that recorded a scenario
  is its control and must reproduce it byte for byte. Each run keeps the recorder's phases, the second install over every lock it writes, and
  `cross`: the edit installed over the scenario's own `before`. A scenario's `releases` file names the releases that
  write its recorded outcome, and each `release/<first>/` another outcome with the releases that write it;
  `NpmScenarioTest` replays the first and last release of every range. An expect line `exact-or <failure>` holds a
  release to the lock byte for byte where the engine writes one, and to the failure otherwise: a recording the engine
  cannot know may be one another release, reading less, writes plainly.

## Amendment 2026-10-11: Yarn Berry releases

The Berry port follows Yarn 4.10.3 and reproduces every 4.x release beside it, as the npm port does npm's.

- **The release that installs** is the one the `NodeRuntime` marker names, else the one `yarnPath` or corepack starts;
  one outside `YarnReleases.PROVEN`, or one the release data does not hold (Yarn 2 and 3), is cannot-know.
- **One question per upstream change.** `YarnRelease` holds each difference beside the release's data (package
  extensions, builtin patches, plugins, settings) as a question named for what Yarn does, answered through `atLeast`
  alone. Settings, their defaults and types are the release's: a setting it does not define is unknown to it, and
  `npmMinimalAgeGate` is minutes until 4.11.0, a duration after, `1d` by default from 4.15.0, per scope from 4.17.0.
  The lockfile migration rules (`approvedGitRepositories` and `enableScripts` over a lock older than version 9 from
  4.14.0, `npmMinimalAgeGate: 0` older than 10 from 4.15.0) set what the install writes to `.yarnrc.yml`, which the
  regeneration returns beside the lock. A builtin compat patch's `hash=` is the release's: the data holds, per patch,
  its semver ranges and the hash each gives, and a lock holding another release's patch is cannot-know, since the
  checksum is that of the patch applied to the source's archive.
- **How a release is proven.** Every stable release from 2.1.0 was swept over every Berry scenario as npm's were. The
  export (the fixtures repository's `releases` and `release/<first>/`) holds Yarn 4.x only; Yarn 2 and 3 write
  another lockfile format, which no port reads. A release's outcome is its locks and the `.yarnrc.yml` it writes; a
  lock with a builtin compat patch entry its start lock does not hold is `exact-or cannot-know builtin compat patch
  changed`, as the scenarios recorded over such an entry say. `YarnBerryScenarioTest` replays the first and last
  release of every range, and every release with `REWRITE_LOCK_EVERY_RELEASE`.

## Amendment 2026-10-11: Bun releases

The Bun port follows Bun 1.3.10 and reproduces other releases beside it, as the npm port does npm's.

- **The release that installs** is the one the `NodeRuntime` marker names (what `bun --version` answered), else
  1.3.10. One outside `BunReleases.PROVEN`, or one that writes no bun.lock (before 1.1.39), is cannot-know.
- **One question per upstream change.** `BunRelease` holds each difference as a question named for what Bun does,
  answered through `atLeast` alone by the release whose tag (`bun-v<release>` of oven-sh/bun) made it; the ported Zig
  module branches on it beside 1.3.10's code, the release reaching it through `BunOptions` and the `Lockfile`. bun.lock
  is read and written as the release does it: `configVersion` from 1.3.2, a `""` registry expanded to the package's
  scope registry from 1.3.7, the integrity of a git, github or tarball package from 1.3.10, an unnamed root's entry
  from 1.3.0, catalogs from 1.2.14. A name `peerDependenciesMeta` alone lists is an optional peer of `"*"` from
  1.3.14; the peer queue is drained before the wait for the peers from 1.3.11, processed once before it from 1.3.1;
  a workspaces glob a later `!` pattern negates is no workspace from 1.3.0; a git or GitHub spec is told and read as
  hosted-git-info does from 1.3.2; bunfig's minimum release age is read
  from 1.3.0, a quoted .npmrc value expanded from 1.3.5, the root's own catalogs from 1.2.19; and before 1.2.19 a
  package's dependency groups are not told apart (their order, the root's diff, the workspace bit). The patch file
  errors are worded as each release words them.
- **A release that never finishes.** From 1.2.21 (`--lockfile-only` no longer prefetches tarballs) to 1.3.9, a pass
  of the wait for the peers that leaves peers queued and no request pending is never followed by another: nothing
  wakes bun's event loop. The port fails there, as npm's does where npm never finishes. Installs over a lock that
  holds the packages never reach that state; an install from a lock whose root lists nothing can.
- **How a release is proven.** Every release that writes bun.lock (1.1.39 to 1.4.3) was swept over every recorded Bun
  scenario through a frozen registry, as npm's were; the release that recorded a scenario (1.3.10) is its control. The
  export (the fixtures repository's `releases` and `release/<first>/`) holds 1.2.0 on, since 1.1.x writes
  lockfileVersion 0 and reads none of the fixtures' version 1 locks. A release's outcome is the lock of the edit over
  its own `before`, of the edit over the scenario's `before` (`cross`), and the second install over each; a lock bun
  wrote from scratch is no outcome, since a regeneration installs over a lock, so that a from-scratch install that
  never finishes (1.2.21 to 1.3.9 over two peer graphs) leaves the edits over a lock, which finish, to be replayed.
  A `git clone` bun failed is `cannot-know`: what git reaches over the network is not the engine's to know, as npm's
  export takes the filesystem npm ran on. `BunScenarioTest` replays the first and last release of every range, and
  every release with `REWRITE_LOCK_EVERY_RELEASE`; a release replay answers only for the registries that release
  asked (its outcome's `requests.log`), since 1.4.0 asks bunfig's registry where 1.3.x asked .npmrc's.
- **1.4.x, a Rust translation.** 1.4.0 translated every Zig file the port names to Rust and changed much on the way;
  each change is a question beside 1.3.10's code like any other, the Java classes still following their modules (the
  Rust file named beside the Zig one). bun.lock holds versions 0 to 3: a lock keeps the version it was read with, a
  new one is 2, held at 1 where a package would not read back at 2, and 3 where the overrides hold rules scoped to a
  parent or a range, which 1.4.0 reads, matches and writes. Among the rest: the JSON and TOML parsers are new (their
  errors and what they accept), bunfig's registry wins over .npmrc's, registry urls are canonical, catalogs compare and
  look up by name, a frozen `--lockfile-only` install writes nothing, the resolver dedupes and redirects otherwise
  (`get_package_id`'s guard, `update_transitive.rs`), the reader binds peers by version, the hoist dedupes a
  package's second edge to a name, and the clean drops what only optional peers hold. What the engine cannot see is
  cannot-know: bun's manifest cache (`install.offline`, from 1.4.1), a frozen install over a pruned checkout, and two
  1.4.0 bugs whose bytes depend on memory or response headers.
- **1.2.x.** Every consecutive pair from 1.2.0 to 1.3.0 was diffed in the files the port names and in what Bun builds
  into them (its semver, glob matcher, .npmrc and TOML parsers and bunfig reader, Zig's standard library, Highway's
  string search), each change written down before it was ported. Among the questions: a version's numbers are u32s
  before 1.2.23; workspaces globs are matched by glob-match before 1.2.3; bun.lock lists only the overrides its tree
  uses before 1.2.8 and sorts them from 1.2.14, escapes overrides and patches from 1.2.3, keeps duplicate entries apart
  in 1.2.3 alone, takes a dependency's bundled mark from the entry it binds to before 1.2.4, and reads a workspace entry
  under another key from 1.2.5; an integrity is decoded past its algorithm from 1.2.20 and refused past its digest from
  1.2.21; .npmrc loses its byte order mark from 1.2.13; `linkWorkspacePackages` is read from bunfig from 1.2.16 and
  from .npmrc from 1.2.19; bunfig's security scanner runs on install from 1.2.22. What a release build leaves
  undefined (code it takes for unreachable, a write past a buffer) or to the CPU (1.2.11 to 1.2.23 leave a tab in the
  escape search's scalar tail unescaped, the tail's length set by the vector width) is cannot-know.
- **What is reproduced.** `BunReleases.PROVEN` is 1.2.0 to 1.4.3: every scenario replays as each of them, and every
  upstream change in the files the port names between them and 1.3.10 that reaches a lock is a question. 1.1.39 to
  1.1.45 write and read bun.lock's version 0 alone, which no recording holds, so they stay cannot-know.

## Amendment 2026-10-11: Yarn classic releases

The Yarn classic port follows yarn 1.22.22 and reproduces the 1.x releases beside it, as the npm port does npm's.

- **The release that installs** is the one the `NodeRuntime` marker names; without one, 1.22.22 as before. A
  yarn-path hands the install to the file it names: a file named as a release (`yarn-<version>.js`, `.cjs`, `.mjs`)
  is the release the marker names, whose `--version` answered through the same hand-off, and only 1.22.22's own file
  without a marker; any other file is a script whose install the engine cannot know. A release outside
  `YarnClassicReleases.PROVEN`, or one the registry does not publish as yarn 1, is cannot-know.
- **One question per upstream change.** `YarnClassicRelease` holds each difference as a question named for what yarn
  does, answered through `atLeast` alone by the tag of yarnpkg/yarn that made it. The release names itself where yarn
  does: the header `yarn-enable-lockfile-versions` writes, `engines.yarn`, the `packageManager` message. An error that
  is no MessageError is worded as the release reports it: `An unexpected error occurred: "<message>".` before 1.22.20,
  its stack from 1.22.21; 1.22.20 prints none, and the engine words it as 1.22.21 does. From 1.4.0 to 1.5.1 yarn's
  bundle makes none of its own errors a MessageError, so those releases word every error as unexpected. What ends
  yarn before `main` reports (an rc file it cannot read, a yarn-path that does not load, the manifest 1.22.20 reads
  first) is printed as its stack by every release. The questions cover every change between two releases of `PROVEN`
  in the modules the port names, those no scenario reaches among them (each tested as the release did it when run),
  and the libraries yarn bundles: ini 1.3.4 and tar-fs 1.16.0 before 1.10.0, and tar-stream 1.5.4 before 1.7.0 (with
  readable-stream's `_final`, from 1.8.0), are options of their one port.
- **How a release is proven.** Every stable 1.x release was swept over every Yarn classic scenario as npm's were;
  1.22.12, which the registry publishes without its bundle, never ran and is not reproduced. The export's failure of a
  phase is the error that ended it, yarn's last `error` line (the steps go to stdout, which interleaves with stderr
  from run to run), none where 1.22.20 printed none. A recording's `cannot-know` or `malformed` line is `exact-or`
  that line for a release that writes a lock, in the export's outcomes and in the replays of the recorded outcome as
  another release; where a release writes what the recorders say the engine cannot know and the recording release
  does not (a member directory's own lock, a lock that names yarn's cache folder), the export's line is the
  recorders' `cannot-know` for it. A registry's `Not found` is yarn rejecting the edit however a release words it.
  `YarnClassicScenarioTest` replays the first and last release of every range, and every release with
  `REWRITE_LOCK_EVERY_RELEASE`. `PROVEN` is every 1.x release but 1.22.12.

## Amendment 2026-10-12: pnpm releases

The pnpm port follows pnpm 11.2.2 and reproduces the releases of `PnpmReleases.PROVEN` beside it, as the npm port does
npm's; `Main.RUNS_AS_PORTED`, which let every 11.x release run as 11.2.2, is gone.

- **The release that installs** is the one the `NodeRuntime` marker names, what `pnpm --version` answered in the
  project's root: the release on the path or the one corepack starts, which main.ts answers before any switch. It runs
  its own package manager check; a hand-over to another release is cannot-know, since the release that runs may write
  the env document before it hands over. Without a marker 11.2.2 runs as before and hands the install to the release
  the project names, which the engine follows where it reproduces it. Where the inputs leave the release at a range
  (corepack or pnpm run directly, a range the root's lock pins one of), it is every reproduced release of the range,
  and a question their answers differ on for the inputs is cannot-know; each question is asked only where its answers
  differ. A release outside `PROVEN` is cannot-know, worded as the other package managers word it.
- **One question per upstream change.** `PnpmRelease` holds each difference as a question named for what pnpm does,
  answered through `atLeast` alone by the tag `v<release>` of pnpm/pnpm that made it, the ported class branching where
  upstream changed with 11.2.2's code beside the other release's. pnpm names its own release only where it does (its
  `packageManager.version`, `engines.pnpm`). The resolver's `LOCKED_OPTIONAL_PEER_CANDIDATES` (pnpm/pnpm#12075) stays
  as #9151 wrote it, and `countsLockedVersionsAsOptionalPeerCandidates` answers the same for every reproduced release.
  A change no scenario reaches is a question all the same, its unit test what the release did when run: pnpm before
  11.5.0 writes a locked remote tarball without its integrity beside an edit (pnpm/pnpm#12001), where the port of
  11.2.2 had filled in the locked one.
- **How a release is proven.** Every stable release from 9.0.0 to 12.12.1 was swept over every pnpm scenario, and the
  export wrote what each wrote into the scenarios' `releases` files and `release/` outcomes, with the registry
  documents its replays need; pnpm's own packuments, which six scenarios that pin a release would copy (81 MB), are
  left out, every release's replay of every scenario being the same without them. `PnpmScenarioTest` replays the
  first and last release of every range, and every release with `REWRITE_LOCK_EVERY_RELEASE`. A recording a release
  outside `PROVEN` made is held to cannot-know until the engine reproduces that release. `PROVEN` widens a boundary at
  a time, once every replay of its releases passes and every pair of releases in it is reviewed, every module of the
  bundle and every library it bundles: 11.0.3 to 11.16.0 so far. 11.5.2 resolves peers a second time from the peer
  contexts the lock records, reads a locked package it does not update from its snapshot, and binds each locked
  tarball URL to the registry's; 11.7.0 resolves the children of a package several occurrences reach in the context of
  the lowest of them (pnpm/pnpm#12358); a pnpm 11 release before 11.0.6 writes pnpm-workspace.yaml as pnpm 10 does,
  which the recipes follow.
- **A bundled library is a question too.** Each release bundles its own node-semver (7.7.4 to 11.1.3, 7.8.0, 7.8.1
  from 11.3.0, 7.8.4 from 11.7.0, 7.8.5 from 11.9.0), whose range functions the port asks through
  `PnpmRelease.underSemver`, only where the node-semvers of the releases the install may be answer otherwise; yaml
  2.8.4 (11.0.7) prints a negative number's fraction digits, which the recipes' pnpm-workspace.yaml writer asks the
  release that installs. Where pnpm writes a file the regeneration does not (loose minimumReleaseAge adding immature
  picks to pnpm-workspace.yaml from 11.1.3) or reads one the host does not give (a user .npmrc the environment names,
  from 11.0.6), the regeneration is cannot-know.
