# Behavioral Specification: OpenRewrite's Custom Maven Resolution Algorithm

Repo root: `/Users/jon/Projects/github/openrewrite/rewrite/.claude/worktrees/maven-resolution`
All paths below are relative to `rewrite-maven/src/main/java/org/openrewrite/maven/`.

Core files:
- `internal/RawPom.java` (671 lines) — XML deserialization + RawPom→Pom mapping
- `tree/Pom.java` (231) — cacheable "requested" model
- `tree/ResolvedPom.java` (1336) — the resolution core (`Resolver` inner class + `doResolveDependencies`)
- `internal/VersionRequirement.java` (299) — version conflict mediation / ranges / LATEST / RELEASE
- `tree/Scope.java` (202), `tree/Version.java` (420), `tree/Profile.java` (51), `tree/ProfileActivation.java` (103)
- `MavenParser.java` (235) — entry point
- `internal/MavenPomDownloader.java` — only cited here for reactor/relativePath/snapshot behavior that the resolver depends on

---

## 1. Raw parse: RawPom → Pom mapping

### 1.1 XML deserialization (what is normalized at parse time)

`RawPom.parse(InputStream, snapshotVersion)` (RawPom.java:153) uses Jackson XML with the config in `internal/MavenXmlMapper.java`:

- **Namespace-unaware** parsing (`IS_NAMESPACE_AWARE=false`, MavenXmlMapper.java:50) — tolerates POMs with undefined namespaces.
- **Entity references not replaced** (`IS_REPLACING_ENTITY_REFERENCES=false`, MavenXmlMapper.java:49).
- **Every string value is `trim()`ed** by a global `StringTrimModule` (MavenXmlMapper.java:88-100). Whitespace inside `<version> 1.0 </version>` is normalized at parse, unlike Maven which preserves and warns.
- `EMPTY_ELEMENT_AS_NULL` enabled (MavenXmlMapper.java:55); unknown elements ignored (`FAIL_ON_UNKNOWN_PROPERTIES` disabled, line 61).
- Empty `<releases/>`/`<snapshots/>` policy elements deserialize as `enabled="true"` (RawRepositories.java:67-70). Only `enabled` is read; `updatePolicy`, `checksumPolicy`, `layout` are **dropped**.
- Only these POM sections are deserialized at all (RawPom.java:53-128): parent, gav, obsolete `pomVersion`/`currentVersion`, snapshotVersion, name, description, prerequisites, packaging, dependencies, dependencyManagement, properties, build{plugins,pluginManagement}, repositories, pluginRepositories, licenses, profiles, modules, subprojects (Maven 4). Everything else (`distributionManagement`, `scm`, `reporting`, `build` beyond plugins, `organization`, etc.) is discarded.
- `RawPom.Profile` (RawPom.java:380-403) deserializes only: id, activation, properties, dependencies, dependencyManagement, repositories, pluginRepositories. **A profile's `<build>` (plugins) is not deserialized at all.**
- Plugin `<configuration>` is kept as a raw Jackson `JsonNode` (RawPom.java:319), untyped.

### 1.2 GAV inheritance at raw level

- `RawPom.getGroupId()` falls back to `parent.groupId` (RawPom.java:405-413); `getVersion()` falls back to obsolete `currentVersion` then `parent.version` (RawPom.java:415-426). Blank strings treated as absent.
- `toPom` (RawPom.java:429-472) throws `MavenParsingException` if groupId or version cannot be determined even via parent (RawPom.java:436-443).

### 1.3 What `Pom` preserves verbatim ("requested" data)

`Pom` (tree/Pom.java:51) is the cacheable, unresolved representation (model version 3, Pom.java:61-64):

- `gav` is a `ResolvedGroupArtifactVersion` whose repository = the source repo URI (or null for project POMs), datedSnapshotVersion = null at this stage (RawPom.java:449-454).
- Dependencies preserved with raw strings: version/scope/type/classifier/optional/exclusions all as-written, including property placeholders (`mapRequestedDependencies`, RawPom.java:571-611). `optional` is a **String** (may be a property reference).
- DependencyManagement entries are split at parse time into `ManagedDependency.Imported` (when `scope == "import"`, literal comparison — a scope of `${imp}` that resolves to "import" would NOT be treated as an import) vs `ManagedDependency.Defined` (RawPom.java:552-569; ManagedDependency.java:35-110). Type/classifier/exclusions kept raw on `Defined`; `Imported` keeps only GAV.
- **Profiles are stored in reverse declaration order** "to put them in precedence order left to right" (RawPom.java:495-509). Later-declared profiles therefore merge first and win first-wins merges — implements Maven's later-profile-takes-precedence rule (see Profile.java:26-29 javadoc).
- **Profile plugins bug**: `mapProfiles` populates each profile's `plugins`/`pluginManagement` from the **project-level** `build` (RawPom.java:506-507), because profile builds aren't parsed. Profile-specific `<build><plugins>` are silently ignored; project plugins are duplicated into every profile object.
- Plugins: missing groupId defaulted to `org.apache.maven.plugins` (RawPom.java:618-620, Plugin.java:37). `extensions`/`inherited` kept as Strings.
- Licenses classified into a `License.Type` enum by fuzzy name matching (`License.fromName`, License.java:28+).
- Modules + Maven-4 `subprojects` concatenated into `Pom.subprojects` (RawPom.java:659-670).
- **Obsolete POM format**: old `pomVersion` field recorded as `obsoletePomVersion`; if present, `toPom` skips mapping dependencies/depMgmt/repositories/plugins entirely (RawPom.java:463-470) and `ResolvedPom.resolve` refuses to resolve (ResolvedPom.java:196-199), "same as Maven itself would" (Pom.java:77-83).
- Repositories: id/url/releases-enabled/snapshots-enabled only; `knownToExist=false`, no credentials (RawPom.java:516-550).

### 1.4 MavenParser entry point

`MavenParser.parseInputs` (MavenParser.java:66-179):
- Parses each input to `RawPom.toPom(path, null)`, then **putAll's user-supplied parser properties directly into the Pom's properties map** (MavenParser.java:78-81) — user `-D`-style properties override same-named POM properties before resolution.
- Effective active profiles = `MavenExecutionContextView.getActiveProfiles()` + parser's `activeProfiles` (MavenParser.java:106).
- `MavenPomDownloader` gets the full map of project POMs by path for reactor resolution (MavenParser.java:101).
- Each POM resolved via `Pom.resolve(...)` → `MavenResolutionResult` marker; then `resolveDependencies` for the four scopes unless `skipDependencyResolution` (MavenParser.java:110-122).
- Download failures produce `ParseExceptionResult` markers + warnings but still yield a parsed document (MavenParser.java:124-143).
- Post-pass wires `parent`/`modules` between `MavenResolutionResult`s by GAV comparison (MavenParser.java:146-176). Note line 167 compares the parent candidate's own version to `parent.version` resolved with the **parent's** properties.
- `MavenResolutionResult.resolveDependencies` resolves exactly `RESOLVE_SCOPES = {Compile, Runtime, Test, Provided}` (MavenResolutionResult.java:184-210) into a `Map<Scope, List<ResolvedDependency>>`. Dedup of repeated download exceptions across scopes by (root GA, failedOn GAV) (MavenResolutionResult.java:190-201).

---

## 2. `ResolvedPom.resolve()` — effective-model construction

`Pom.resolve(activeProfiles, downloader, initialRepositories, ctx)` (Pom.java:201-219) seeds a `ResolvedPom` with `initialRepositories = concatAll(callerInitialRepos, getEffectiveRepositories())`, where `getEffectiveRepositories()` = origin repo + declared repos with `~` home-dir expansion and placeholder replacement from the POM's *own raw* properties (Pom.java:144-167). It then calls `ResolvedPom.resolve(ctx, downloader)` (ResolvedPom.java:195-279), which:

1. Returns `this` unchanged for obsolete-format POMs (ResolvedPom.java:196-199).
2. Builds a fresh `ResolvedPom` and runs `Resolver.resolve()` → `resolveParentsRecursively(requested)` (ResolvedPom.java:424-427).
3. Compares the new result field-by-field (version, properties, requestedDependencies, dependencyManagement, repositories, plugins, pluginManagement) and returns the **old instance** if nothing changed (ResolvedPom.java:216-278). `pluginRepositories` is *not* compared.

### 2.1 Overall pass order (ResolvedPom.java:429-456)

`resolveParentsRecursively` runs **three separate ancestry walks**, each re-downloading/walking the parent chain:

1. `mergeRepositories(initialRepositories)` first (ResolvedPom.java:433-435) — initial repos (settings/ctx + own effective repos) take precedence positions.
2. **Pass A — properties & repositories**: `resolveParentPropertiesAndRepositoriesRecursively` (ResolvedPom.java:458-489).
3. GAV placeholder fix-up: after properties merge, the requested Pom's gav (repository/groupId/artifactId/version/datedSnapshotVersion) has placeholders resolved eagerly "because any system properties used within the coordinates are transient" (ResolvedPom.java:441-452).
4. **Pass B — dependency management & dependencies**: `resolveParentDependenciesRecursively` (ResolvedPom.java:491-539).
5. **Pass C — plugins & pluginManagement**: `resolveParentPluginsRecursively` (ResolvedPom.java:541-572).

### 2.2 Property interpolation semantics

- Properties are merged raw (uninterpolated) into one map; **interpolation is lazy**, at every `getValue()` call site (ResolvedPom.java:310-315) using `PropertyPlaceholderHelper("${","}",null)`.
- Merge order per POM in pass A: **active profile properties first, then POM properties**, child before parent, all via first-wins `putIfAbsent`-style merge (ResolvedPom.java:463-467, 889-905). Net precedence: child > parent; within a POM, profile > body; among profiles, later-declared > earlier (via the parse-time reversal).
- `getProperty` (ResolvedPom.java:317-373):
  - **System properties override project properties** (`System.getProperty(property, properties.get(property))`, line 323) — models `-D` overrides but makes resolution **host-JVM-dependent**.
  - Self-referential value `${x}` for key `x` falls through to built-ins (lines 326-330).
  - A present key with null value → `""` (lines 331-334).
  - Built-ins supported: `[project.|pom.]groupId/artifactId/version`, `[project.]parent.groupId/artifactId/version`, `[project.|pom.]prerequisites.maven` (lines 335-370). `project.version` has a circular-reference guard returning `"error.circular.project.version"` (lines 350-362).
  - Final fallback: `System.getProperty(property)` (line 372).
  - **Not supported**: `project.basedir`, `project.build.*`, `settings.*`, `env.*`, `maven.*`, `project.name/packaging`, CI-friendly `${revision}`-style late binding beyond ordinary property lookup.
- `updateRepositories()` (ResolvedPom.java:842-852) re-interpolates already-merged repository id/uri after each POM's properties merge, using **only the raw properties map** (no system props/built-ins here — asymmetric with `mergeRepositories` which uses full `getValue`).

### 2.3 Profile activation semantics

- `Pom.effectiveProfiles(explicitActiveProfiles)` (Pom.java:177-187): returns all profiles active by id/JDK/property; if **none**, returns those with `activeByDefault=true`. Used by passes A and B.
- `ProfileActivation.isActive(id, activeProfiles, activation)` (ProfileActivation.java:39-53):
  - Active if id matches any explicitly-activated profile (trimmed string equality; **no `!profile` disable syntax, no `?profile` optional syntax**).
  - Or `activation.isActive()`: **JDK** match (prefix match on host `System.getProperty("java.version")`, else version-range via `VersionRequirement`, ProfileActivation.java:59-75) — host-JVM-dependent; or **property** match — but checked against **`System.getenv()`**, not Java system properties or user properties (ProfileActivation.java:77-93); `!name` negation supported via `name.replace("!","")`.
  - Or `activeByDefault && no explicit profiles at all` — the code comment admits this is "overly broad" versus Maven's rule (deactivate only when another profile *in the same POM* activates) (ProfileActivation.java:49-52).
- **Not supported at all**: `<os>`, `<file>` activation (not even deserialized), JDK activation against a configurable JDK, property activation against `-D` properties, settings.xml `<activeProfiles>` handled only insofar as callers put them into the ctx.
- **Inconsistency**: passes A and B use `effectiveProfiles` (with the activeByDefault fallback logic); pass C (plugins) uses per-profile `profile.isActive(activeProfiles)` directly (ResolvedPom.java:544-549), so activeByDefault profiles behave slightly differently for plugins.
- Profiles in **transitive dependency POMs** are evaluated with the same `activeProfiles` list (`resolvedPom = new ResolvedPom(dPom, getActiveProfiles(), ...)`, ResolvedPom.java:1109-1122) — so property/JDK/env-based activation applies inside dependency POMs too (Maven only activates dep-POM profiles by activeByDefault/JDK/OS-property present in the *repository* pom context).

### 2.4 Parent traversal & relativePath

- Parent chain is walked by repeated `downloader.download(gav, parent.relativePath, ResolvedPom.this, repositories)` (ResolvedPom.java:574-611).
- Version must be present (MavenParsingException otherwise, lines 576-578 and 597-599).
- If the parent version contains `${...}`, the raw GAV is first tried against the **reactor** (project POMs) before property resolution (ResolvedPom.java:587-594) — handles `${revision}`-style parents within the same repository.
- Parent version may be a **range**: resolved through `VersionRequirement.fromVersion(v, 0).resolve(...)` against repository metadata (ResolvedPom.java:601-607).
- **Cycle guard**: if a resolved parent GAV equals any GAV already in the ancestry list, traversal stops silently (ResolvedPom.java:479-484, 529-534, 561-566).
- relativePath handling lives in the downloader (MavenPomDownloader.java:492-554 and 211-238):
  - Exact-GAV reactor match first, then raw-version match, then property-merged version match (MavenPomDownloader.java:507-528).
  - `<relativePath/>` (explicitly empty) = "do not look locally" (lines 530-532, 217-220).
  - Missing relativePath defaults to `..` (line 535) / `../pom.xml` (line 222); non-`.xml` paths get `/pom.xml` appended (lines 227-230).
  - Local candidate must match groupId+artifactId, and version unless the wanted version has unresolved placeholders (lines 546-551).
- Each of passes A/B/C repeats the parent download (served from cache after the first pass).

### 2.5 DependencyManagement merging (pass B)

`resolveParentDependenciesRecursively` (ResolvedPom.java:491-539) maintains a `LinkedHashMap<GroupArtifactClassifierType, ResolvedManagedDependency>` (key = groupId/artifactId/classifier/type with type defaulted to "jar", lines 907-923) and walks child → parent, per POM: profile depMgmt + profile dependencies first, then body depMgmt + body dependencies (lines 514-520).

`mergeDependencyManagement` (ResolvedPom.java:925-982):

- **BOM import (`Imported`)**: GAV interpolated with current accumulated properties; skipped if the BOM GAV equals an ancestor POM's GAV (`isAlreadyResolved`, lines 984-995 — self/ancestor-import cycle guard). Otherwise the BOM is downloaded and **fully recursively resolved** (`.resolve(activeProfiles, downloader, initialRepositories, ctx)`, line 936-937) — imports within imports recurse naturally. Its entire resolved dependencyManagement is added with `putIfAbsent` (first-wins), each entry tagged with `requestedBom` + `bomGav` (lines 941-946).
- **`Defined` entries**: GAV interpolated; literal `LATEST`/`RELEASE` versions resolved via repository metadata (lines 950-958). Conflict rule (lines 964-979): `compute` — an existing entry **without** a `bomGav` always wins (first-declared direct entry wins, i.e. child > parent, earlier > later); an existing entry that came **from a BOM** is overridden by any directly-`Defined` entry, even one from a farther ancestor. This reproduces Maven's "declared depMgmt (including inherited) beats imported depMgmt".
- Scope string is interpolated then parsed (`Scope.fromName(getValue(scope))`); exclusions interpolated (lines 971-974).
- Version may remain a property reference in the stored entry; `getManagedVersion` re-interpolates on read (ResolvedPom.java:375-378).
- Managed entries are **sorted** by GACT for binary-search lookup (`getManagedDependency`, ResolvedPom.java:390-399; comparator lines 66-69; lazy sort with `dependencyManagementSorted` for serialized-model compatibility, lines 143-156).
- **Requested dependencies merging** (same pass): child-first; a parent dependency with the same groupId+artifactId as an existing one is skipped entirely — "the child takes precedence" (ResolvedPom.java:613-637). Keyed on GA only (not classifier/type).

### 2.6 Repository accumulation/inheritance

- Order of accumulation: initialRepositories (ctx/settings + requested POM's effective repos) → per-POM child-first: profile repos then body repos → parents' (ResolvedPom.java:433-438, 470-474).
- `mergeRepositories` (ResolvedPom.java:854-887) interpolates id/uri via full `getValue`, dedupes **by id only** (repos with null id are always appended), first occurrence wins ordering.
- After the first resolution, `initialRepositories` is frozen (`if (initialRepositories == null) initialRepositories = repositories`, ResolvedPom.java:437-439) and reused on re-resolution.
- **pluginRepositories are effectively dropped by resolution**: `ResolvedPom.resolve()` constructs the new instance with empty pluginRepositories and the Resolver never merges any (ResolvedPom.java:201-214); they survive only when resolve() returns the unchanged `this`. Profile pluginRepositories are never merged anywhere.

### 2.7 Plugins & pluginManagement (pass C)

`resolveParentPluginsRecursively` (ResolvedPom.java:541-572), child-first, per POM: active-profile pluginManagement + plugins first, then body pluginManagement + plugins.

- Merge key = (groupId, artifactId) (lines 639-650). Parent plugins with `inherited="false"` are skipped (line 807).
- Merging an existing (child) with incoming (parent) plugin (lines 789-800): child's version/extensions/inherited win when non-null; configurations merged; dependencies merged by GA first-wins (lines 652-673); executions merged by id.
- Configuration merge (lines 675-737) reimplements Maven's XML merge over Jackson trees: child key wins unless the parent value carries `combine.children="append"` (then lists are concatenated child-appended, honoring `combine.self="override"`). `combine.self` on non-list values is otherwise not handled.
- Execution merge (lines 739-787): goals are unioned through a **`HashSet` (order lost / nondeterministic)**; **incoming (parent) phase overrides the child's phase when different** (lines 764-769) — backwards versus Maven's child-wins; `inherited` taken from incoming.
- pluginManagement is only accumulated; **its versions/configuration are never applied onto `plugins`** within ResolvedPom — consumers must join the two lists themselves.
- Plugins are never downloaded or version-resolved (no metadata lookups for plugin versions).

---

## 3. `resolveDependencies(scope, downloader, ctx)` — transitive resolution

Entry points (ResolvedPom.java:998-1015): `resolveDependencies(scope, ...)` (fresh requirements map), an overload taking a pre-seeded `Map<GroupArtifact, VersionRequirement>`, and `resolveDirectDependencies` (no transitives). All delegate to `doResolveDependencies` (ResolvedPom.java:1017-1241).

### 3.1 Traversal order: strict BFS with restart

- Depth-0 roots: for each requested dependency (interpolated at depth 0), include if `dScope == scope || dScope.transitiveOf(scope) == scope` (lines 1028-1036). Roots are stored in a `LinkedHashMap<GroupArtifact, ...>` — **for duplicated direct GA declarations, last declaration wins** (line 1032-1034, with a TODO noting Gradle would want highest-version).
- Root `DependencyAndDependent.scope` is hardcoded `Scope.Compile` (line 1034) — the classpath filter already happened, and this makes transitive scope math behave as if the root were compile.
- Loop over depth levels; next level accumulated in a `LinkedHashMap<GroupArtifactClassifierType, ...>` with `putIfAbsent` — **"For transitive dependencies at same depth, first parent declaration wins"** (lines 1211-1218). Combined with per-level processing this is Maven's nearest-wins by depth, ties by first-encountered declaration order.
- **Restart semantics**: when a new requirement for an already-seen GA changes the resolved version, the entire resolution restarts from scratch, keeping the accumulated `requirements` map and clearing the resolution listener (lines 1083-1090). Guarantees a fixed point over version ranges.
- If requirement unchanged and GA+classifier already in the result, the node is skipped (dedup, lines 1091-1095; `contains` at 1243-1251 ignores type).

### 3.2 Per-node interpolation and dependency management application

For each node: `d = dd.getDefinedIn().getValues(dd.getDependency(), 0)` then `d = getValues(d, depth)` (lines 1044-1049) — first interpolated against the POM that declared it (depth forced to 0 so *that* POM's depMgmt cannot override its own direct dependency versions), then against the root POM with the true depth.

`getValues(Dependency, depth)` (ResolvedPom.java:1283-1326):
- Interpolates GAV, scope, classifier, type.
- Scope default: if unset, taken from managed scope, else compile.
- **Managed exclusions are concatenated onto the dependency's exclusions** (lines 1301-1304).
- Version: if `d.version == null || depth > 0`, the root POM's managed version **overrides** the requested transitive version (falling back to the requested one) (lines 1312-1319). At depth 0 an explicit version always wins over management.

### 3.3 Guards and type filter

- Depth-0 dependency without a resolvable version → `MavenDownloadingException` (lines 1051-1057).
- **Type filter**: nodes with a non-null type other than `jar|ejb|pom|zip|bom|tgz` are silently skipped (line 1058) — notably **`test-jar`, `war`, `maven-plugin`, `aar` dependencies are dropped from the tree entirely**.
- Transitive node with null version (no management, none requested) → silently skipped (line 1058).
- Unresolved `${...}` placeholders remaining in G/A/V → `MavenDownloadingException` "Could not resolve property" (lines 1098-1102).

### 3.4 Version requirements / conflict mediation (`VersionRequirement`)

`VersionRequirement` is a linked chain, newest at head, `nearer` pointing at the previously accumulated chain (VersionRequirement.java:40-81).

- `VersionSpec.build(requested, direct)` (lines 84-137): `LATEST`/`RELEASE` → DynamicVersion; anything containing `[`/`(` → ANTLR-parsed `RangeSet` (an **unclosed range like `[1.0,` is auto-closed with `]`**, matching observed Maven tolerance, lines 89-94); otherwise `DirectRequirement` at depth 0, `SoftRequirement` deeper.
- `addRequirement` is a no-op when the chain head is a `DirectRequirement` (project POM always wins) or when the spec was already seen (lines 65-81).
- `cacheResolved` (lines 237-283):
  - Any `DirectRequirement` anywhere in the chain → returned immediately (root POM version pins, line 244-246).
  - Else "nearest" soft/hard requirement = the **first-added** one (last visited walking `nearer`), i.e., shallowest in BFS order.
  - Without a hard requirement, the nearest soft version wins (no metadata lookup).
  - With a hard requirement (range/LATEST/RELEASE), the **highest** available version from repository metadata matching the *single nearest* hard requirement is chosen (lines 258-282). **Ranges are never intersected across requirers**, soft requirements are ignored once any hard requirement exists, and no error is raised for mutually incompatible ranges (null → MavenParsingException at the call site, ResolvedPom.java:1068-1069, 1078-1079).
  - `RELEASE` excludes only versions ending `-SNAPSHOT` (VersionRequirement.java:167-169).
  - Result memoized per chain instance (`selected`, lines 230-235).
- Requirements are keyed by **GroupArtifact only** (ResolvedPom.java:1062-1063) — classifier/type variants share one version requirement.
- Available versions come from `downloader.downloadMetadata(ga, null, repositories)` (VersionRequirement.java:285-290).
- Version comparison (`tree/Version.java`) is a copy of Aether's `GenericVersion` (Version.java:23-25): tokenization on `.-_` and letter/digit transitions, qualifiers alpha<beta<milestone<rc/cr<snapshot<""(ga/final/release)<sp, case-insensitive unknown qualifiers compared lexically, `min`/`max` tokens, numeric padding trimmed. This matches Maven's ComparableVersion behavior for common cases (it *is* Aether's implementation, modified only for performance).

### 3.5 Download and node construction

- POM downloaded via `downloader.download(d.getGav(), null, dd.definedIn, getRepositories())` (line 1104) — **repositories = the root ResolvedPom's accumulated repositories only**; repositories declared in transitive POMs are used for *their parents/BOMs* (during `resolveParentsRecursively`) but **not** added to the repo list used to fetch their children.
- Each dependency POM gets a partial effective model: `new ResolvedPom(dPom, activeProfiles, ..., initialRepositories, ...)` + `resolveParentsRecursively(dPom)` — properties/depMgmt/repos/plugins of its ancestry, cached per GAV in the `MavenPomCache` (`getResolvedDependencyPom`/`putResolvedDependencyPom`, lines 1106-1124). NOTE: this cache key is GAV only, so the first `activeProfiles`/initialRepositories combination to resolve a dep POM wins for the process lifetime.
- `ResolvedDependency` records: repository, resolved GAV (with datedSnapshotVersion), the *original requested* `Dependency`, licenses from the dep POM, requested type/classifier, `optional = Boolean.valueOf(requested.optional)` (a property-valued `optional` is treated as false), depth (ResolvedDependency fields, ResolvedDependency.java:40-121; construction ResolvedPom.java:1126-1136).
- Parent-child links: `includedBy.getDependencies().add(resolved)` builds the graph (lines 1143-1151); `dependencies` list on ResolvedDependency = "Direct dependencies only that survived conflict resolution and exclusion" (ResolvedDependency.java:52-58).
- Node is added to the flat result only if `dd.getScope().transitiveOf(scope) == scope` (lines 1153-1157); otherwise its subtree is abandoned.

### 3.6 Children enumeration (lines 1163-1220)

For each requested dependency `d2` of the resolved POM:
- groupId defaults to the containing POM's groupId (lines 1165-1167); G/A/V/classifier/scope/type interpolated with the **dependency POM's** properties (lines 1169-1177).
- **Exclusions**: the parent's accumulated exclusions (requested + managed, accumulated down the chain via `d2.withExclusions(concatAll(...))`) are matched with **glob semantics** (`matchesGlob`) against each child's groupId+artifactId (lines 1179-1201). Matching children are pruned, and the "effective exclusion" is attributed to the shallowest ancestor that declared it (`includedByMap` walk, lines 1188-1199; `effectiveExclusions` on ResolvedDependency).
- **Optional**: children whose interpolated `optional` is `true` (trimmed, parseBoolean) are skipped (lines 1204-1206). Depth-0 optional deps are kept (Maven behavior).
- **Scope**: `getDependencyScope(d2, resolvedPom)` (lines 1253-1281): declared scope wins; else the *dependency POM's* managed scope; else compile. Then the **root POM's managed scope overrides it, but only if that would not promote the dep into a classpath it wasn't already in** (`scopeInContainingPom.isInClasspathOf(scopeInThisProject) ? scopeInThisProject : scopeInContainingPom`, line 1280).
- Child is enqueued only if `d2Scope.isInClasspathOf(dd.getScope())` (line 1210) — i.e., only compile/runtime children propagate (see 3.7).

### 3.7 Scope transitivity table (`Scope.java`)

`transitiveOf` (Scope.java:45-97) reproduces Maven's table exactly (`this` = transitive dep's scope, argument = direct dep's scope):

| direct \ transitive | compile | provided | runtime | test |
|---|---|---|---|---|
| compile | compile | – | runtime | – |
| provided | provided | – | provided | – |
| runtime | runtime | – | runtime | – |
| test | test | – | test | – |

- `Scope.fromName(null)` → Compile; unknown → `Invalid` (Scope.java:99-119).
- **System scope**: `System.transitiveOf(anything)` = null, and `RESOLVE_SCOPES` never includes System — so **system-scoped dependencies are excluded from every resolved scope list** and `systemPath` is not even parsed. Maven puts system deps on compile/test classpaths.
- The per-scope lists are *pure*: the Compile list contains only compile-reachable deps; provided deps appear only in the Provided list (which also contains compile+runtime roots, since `Compile.transitiveOf(Provided)=Provided`). Consumers wanting Maven's "compile classpath" must union Compile+Provided(+System).

### 3.8 SNAPSHOT handling (downloader-side, driven by resolution)

- `-SNAPSHOT` versions are resolved to a dated snapshot via repository metadata; ctx-pinned snapshot versions take precedence; classifier-specific `<snapshotVersion>` entries preferred, newest by `updated` (MavenPomDownloader.java:778-820). Failure to fetch metadata falls back to the plain `-SNAPSHOT` (local-only artifacts).
- Explicit timestamped versions (`1.0-20230101.123456-1`) are normalized back to base `-SNAPSHOT` with the dated version preserved (`handleSnapshotTimestampVersion`, MavenPomDownloader.java:764-776, regex line 67).
- Resolved GAVs carry `datedSnapshotVersion` distinctly from the base version (ResolvedGroupArtifactVersion).
- `LATEST`/`RELEASE` as literal versions are resolved from metadata before download (`resolveNamedVersion`, MavenPomDownloader.java:822+).

### 3.9 Cycles & error handling

- Dependency cycles terminate via the requirements map + `contains` dedup (a revisited GA with unchanged requirement is skipped, lines 1091-1095).
- Parent cycles: ancestry GAV check (2.4).
- Download failure of a node: for type jar/ejb (or null) the exception is accumulated (`MavenDownloadingExceptions`) with the root dependency attributed; for other types it is only reported to `ctx.getOnError` and resolution continues — "Non-classpath artifacts may lack a POM; skip like Maven does" (lines 1221-1229). All accumulated exceptions thrown at the end (lines 1236-1238) — resolution of the level continues past failures.
- A POM-less JAR in a repo yields a synthesized minimal POM instead of failure (MavenPomDownloader.java:735-753).

---

## 4. Intentional simplifications / known divergences from Maven

Explicitly commented in code:
1. **Obsolete `pomVersion` POMs refuse resolution** "same as Maven" (Pom.java:77-83) — deliberate compatibility.
2. **activeByDefault deactivation is "overly broad"** (ProfileActivation.java:49-52).
3. Gradle-metadata BOM synthesis: `published-with-gradle-metadata` POMs get platform deps injected into depMgmt — "isn't strictly correct from a maven standpoint" (MavenPomDownloader.java:663-681).
4. Last-declaration-wins for duplicate direct deps with TODO about Gradle ResolutionStrategy (ResolvedPom.java:1032-1033).
5. Unclosed version ranges accepted (jackson-databind 2.12.0-rc2 example, VersionRequirement.java:89-94).
6. `.m2`-relative parents special-cased as "bizarre and generally pointless" (MavenResolutionResult.java:239-246).

Structural divergences (not necessarily commented):
7. **Profile activation by property reads environment variables, not `-D` system properties** (ProfileActivation.java:85-92); JDK activation uses the host JVM's `java.version`.
8. **`<os>`/`<file>` activation unsupported**; profile `<build><plugins>` unsupported (and project plugins leak into profile objects, RawPom.java:506-507).
9. **Version ranges are not intersected**; only the nearest hard requirement is honored; conflicting ranges pick highest-of-nearest instead of failing (VersionRequirement.java:237-283).
10. **System scope entirely unresolved**; `systemPath` unparsed.
11. **Type filter drops `test-jar`/`war`/etc. dependencies** from the tree (ResolvedPom.java:1058).
12. **Exclusions use glob matching** (`com.foo*` works); Maven supports only exact ids and `*` wildcards.
13. **Transitive POMs' `<repositories>` are not used to fetch their children** (root's repo list only, ResolvedPom.java:1104).
14. **pluginRepositories dropped/never inherited** after resolution (ResolvedPom.java:201-214).
15. **Plugin execution merge**: parent phase beats child phase; goals order nondeterministic via HashSet (ResolvedPom.java:764-777).
16. **System properties override POM properties globally** (ResolvedPom.java:321-323) — resolution output depends on the host JVM's `-D` flags.
17. Requested-dependency child/parent merge is keyed on GA only, ignoring classifier/type (ResolvedPom.java:622-634); version-requirement key likewise GA-only.
18. Distance semantics: management/exclusion decisions are made against the root POM + the immediate declaring POM only; intermediate ancestors' depMgmt is not consulted (Maven is the same for depMgmt-of-transitives by default, but Maven's `dependencyManagement` import ordering subtleties differ).
19. `dependencyManagement` per-GAV dep-POM cache ignores activeProfiles/repositories (ResolvedPom.java:1106-1124) — cross-build contamination possible within a process.
20. Optional flagged by non-literal value (e.g. `<optional>${isOptional}</optional>`) on a **direct** dep is evaluated as `false` via `Boolean.valueOf` without interpolation (ResolvedPom.java:1134); transitive optional IS interpolated (line 1204).
21. Relocation (`<distributionManagement><relocation>`) unsupported — section not parsed.

---

## 5. Trickiest behaviors a replacement must reproduce exactly

1. **The BFS + restart fixed-point loop** (ResolvedPom.java:1083-1090): version ranges anywhere in the graph can retroactively change everything; the requirements map survives restarts. Any replacement must produce the same final selections: DirectRequirement (root POM) > nearest-first-seen requirement, hard requirements resolved to highest-available-match, no range intersection.
2. **Two-stage interpolation of every node** — first against the declaring POM with depth pinned to 0, then against the root POM with real depth (ResolvedPom.java:1044-1049) — plus root-depMgmt version override for all transitives (getValues, lines 1312-1319). Recipes observe `ResolvedDependency.requested` as the *uninterpolated original*, which must be preserved.
3. **Scope override guard**: root depMgmt scope only applies when it does not "promote" a dep into a classpath it wasn't in for its containing POM (ResolvedPom.java:1277-1280); per-scope result lists are non-cumulative (compile list excludes provided/system), with roots hardcoded to Compile for transitivity math (line 1034).
4. **DepMgmt precedence lattice**: first-wins by GACT walking child→parent with profiles-before-body and reverse-declaration-order profiles; direct `Defined` entries (even in ancestors) override BOM-imported entries (even in children), tracked via `bomGav == null` (ResolvedPom.java:964-979); BOMs resolved fully recursively with ancestor-cycle suppression (lines 931-946, 984-995).
5. **Effective exclusions with glob semantics, accumulated down the chain, attributed to the shallowest declaring ancestor** (ResolvedPom.java:1179-1201) — recipes rely on `effectiveExclusions` placement.
6. **Reactor-first parent/dependency lookup** with three matching tiers (exact GAV, raw version, property-merged version) and Maven-faithful relativePath rules including empty-element opt-out (MavenPomDownloader.java:492-554); parents whose version is `${...}` tried in-reactor *before* interpolation (ResolvedPom.java:587-594).
7. **Lazy property interpolation with system-property override and env-var-based profile activation** — resolution results intentionally respond to the host environment; a replacement must decide whether to keep this (deterministic-parsing feedback says host-dependence is a liability, but recipes/tests depend on `-D` behavior).
8. **SNAPSHOT normalization invariants**: base version as the identity, `datedSnapshotVersion` carried separately, timestamped inputs folded back to `-SNAPSHOT`, ctx-pinned snapshots honored (MavenPomDownloader.java:764-820).
9. **`ResolvedPom.resolve()` identity semantics**: returns the *same instance* when re-resolution changes nothing (field-by-field comparison, ResolvedPom.java:216-278) — recipe framework relies on reference equality for change detection.
10. **Serialized-model compatibility**: lazy depMgmt sorting (`dependencyManagementSorted`, ResolvedPom.java:143-156), `@JsonIdentityInfo` reference graphs, `Pom.getModelVersion()`, and null-tolerant getters for old LSTs — the wire format of `MavenResolutionResult`/`ResolvedPom`/`ResolvedDependency` is API.
11. **Silent skips that recipes have come to expect**: unsupported types (test-jar/war), versionless transitives, non-jar POM download failures, invalid versions in metadata (`IllegalArgumentException` → continue, VersionRequirement.java:262-266).
12. **Gradle-metadata platform injection** during POM download (MavenPomDownloader.java:660-691) — affects the resolved depMgmt of any consumer of such artifacts.
