# Maven-Parity Edge-Case Catalog (rewrite-maven custom resolution)

Purpose: acceptance criteria for replacing the custom resolution algorithm with real Maven API calls.
Every entry lists: **behavior**, **where pinned** (test and/or commit), and **parity status**
(MATCHES real Maven / INTENTIONALLY DIVERGES / AUGMENTS / UNCLEAR).

All paths relative to repo root `rewrite-maven/`. Key sources:
- `src/main/java/org/openrewrite/maven/internal/MavenPomDownloader.java` (1314 lines, "MPD")
- `src/main/java/org/openrewrite/maven/tree/ResolvedPom.java` (1336 lines, "RP")
- `src/main/java/org/openrewrite/maven/internal/VersionRequirement.java` ("VR")
- `src/main/java/org/openrewrite/maven/internal/RawPom.java`
- `src/main/java/org/openrewrite/maven/tree/{Pom,Scope,ProfileActivation,MavenRepositoryMirror}.java`
- Tests: `MavenParserTest` (5,249 lines), `MavenPomDownloaderTest` (1,784), `ResolvedPomTest` (921),
  `RawPomTest`, `MavenSettingsTest` (1,051), `MavenSecuritySettingsTest`, `MavenDependencyFailuresTest`,
  `VersionRequirementTest`, `MavenMetadataTest`, `MavenRepositoryMirrorTest`, `ResolvedDependencyTest`, `ScopeTest`.

---

## A. Parent resolution / reactor / relativePath

1. **Reactor-first parent lookup, three-tier match.** `download()` checks project POMs before any remote:
   (a) exact resolved GAV (`MPD:507-510`), (b) raw version equal or equal-after-merging-ancestry-properties
   (`MPD:513-528`), (c) relativePath-based file match (`MPD:531-554`). Pinned:
   `MavenPomDownloaderTest.canResolveDifferentVersionOfProjectPom:1159`, `shouldNotThrowExceptionForModulesInModulesWithRightProperty:1051`,
   `shouldThrowExceptionForModulesInModulesWithNoRightProperty:1105`. Commit e4a54e1272 (#2075: match must consider version).
   MATCHES (`DefaultModelBuilder#readParentLocally` is cited in the source comment `MPD:533-534, 541-545`).

2. **Explicit empty `<relativePath/>` disables local parent lookup.** `MPD.getParentWithinProject:217-219` and
   `MPD.download:530-532`. Pinned: `emptyRelativePathSkipsLocalParentLookup:1192` (moderneinc/customer-requests#1950),
   commits cb33ce3ab6 (#6875), 489b8eb2b9. MATCHES.

3. **relativePath default and directory-style resolution.** Default ".." in `download()` (`MPD:535`, POM §4.0.0),
   "../pom.xml" in `getParentWithinProject` (`MPD:221-222`); a relativePath not ending in `.xml` gets `/pom.xml`
   appended (`MPD:224-230`). Pinned: commit 9f84dc2658 (#7223), `MavenParserTest.directoryStyleRelativePathWithCiFriendlyVersion:5147`.
   Windows path-separator fix commit 03e1c211a4 (#6506). MATCHES.

4. **relativePath containing ':' skipped** (breaks `Paths.get` on Windows) `MPD:536`; commit 62866237ba (#3169).
   Pragmatic DIVERGENCE (platform workaround, no semantic effect on well-formed poms).

5. **GA(V) verification of relativePath candidate**: groupId+artifactId must match; version match relaxed when the
   requested version still contains `${`. `MPD:546-551`. MATCHES (guards against ".." coincidences like Maven).

6. **Parent version containing unresolved placeholders (`${revision}`, `${project.version}`) tried against the
   reactor with the RAW gav before property resolution.** `RP.resolveParentPom:580-594`. Pinned: commit f4a33150fd (#6860).
   MATCHES (CI-friendly versions).

7. **Version ranges allowed in `<parent><version>`.** `RP.resolveParentPom:601-607` resolves the range via metadata.
   Pinned: `MavenParserTest.parentVersionRange:397`; commit 3308a2c024 (#3023). MATCHES (MNG-2199).

8. **Parent cycles tolerated, not fatal.** Ancestry-walk breaks the recursion on a GAV repeat (`RP:479-484, 529-534, 561-566`).
   Pinned: `MavenParserTest.selfRecursiveParent:782` (#135). INTENTIONALLY DIVERGES — real Maven fails the build;
   rewrite must parse whatever exists in the wild.

9. **POM missing groupId or version → `MavenParsingException`.** `RawPom.toPom:436-443`; pinned:
   `RawPomTest.missingGroupIdThrowsParsingException:615`, `missingVersionThrowsParsingException:636`,
   `emptyGroupIdElementThrowsParsingException:656`, `whitespaceVersionElementThrowsParsingException:676` (#7480). MATCHES.

10. **groupId/version inherited from `<parent>` when blank.** `RawPom.getGroupId:405-413`, `getVersion:415-425`;
    pinned `RawPomTest.groupIdInheritedFromParentStillResolves:696`. MATCHES.

11. **Obsolete POMs (pre-4.0.0 `<pomVersion>`) refuse dependency resolution.** `Pom.obsoletePomVersion:78-83`
    ("We keep track of this field so that we can match Maven's behavior"), `RP.resolve:196-199`,
    `RawPom.toPom:462-469` (skips deps/DM/repos/plugins entirely when pomVersion set). Pinned:
    commit 7390cc1a62 "Match maven's handling of obsolete poms by refusing resolve their dependencies";
    `MavenDependencyFailuresTest.oldPomVersionNoDependencyResolution:293`. MATCHES (Maven warns "POM invalid,
    transitive dependencies will not be available" — see also entry 82).

12. **CI-friendly versions (`${revision}`/`${sha1}`/`${changelist}`)** across parent/child linking, multiple
    placeholders in one version, and post-`UpdateMavenModel` stability. Pinned: `MavenParserTest`
    `ciFriendlyVersionWithoutExplicitProperty:2109`, `ciFriendlyVersionWithParent:2127`,
    `canConnectProjectPomsWhenUsingCiFriendlyVersions:2167`, `ciFriendlyVersionsStillWorkAfterUpdateMavenModel:2289`,
    `multipleCiFriendlyVersionPlaceholders:2451` (#2049, #2373); null property `<sha1/>` treated as "" (`MPD:189-200`).
    Properties supplied externally via `MavenParser.builder().property()` emulate `.mvn/maven.config`
    (`propertyFromMavenConfig:3840`, `propertyFromMavenConfigFromParentPomCanBeUsedInChild:3862`,
    `profilesFromMavenConfig:3903`, commit 96679be8f9 #5070). MATCHES given equivalent `-D` input.

13. **`parentPomIsProjectPom` must not treat POMs under `~/.m2` as reactor members.** Commit 16133dcbf9 (#6119). MATCHES.

## B. DependencyManagement / BOM imports

14. **Child (nearer) DM entry wins over parent DM; first unique wins walking up the ancestry.**
    `RP.resolveParentDependenciesRecursively:495-507` (merge keeps first = child, `(x, y) -> y` comment "Keep first (child wins)").
    Pinned: `ResolvedPomTest.firstUniqueManagedDependencyWins:687`. MATCHES.

15. **Parent DM is "nearer" than an imported BOM's DM.** In `mergeDependencyManagement`, a `Defined` entry replaces an
    existing entry only if the existing one came from a BOM (`existing.getBomGav() == null → keep existing`, `RP:964-967`);
    BOM entries themselves use `putIfAbsent` (`RP:944-946`). Pinned: commit 50e53e4220 "Match Maven's treatment of parent
    dependency management being 'nearer' than a bom's dependency management"; `MavenParserTest.parentNearerThanBom:4029`.
    MATCHES.

16. **BOM import mechanics**: properties interpolated in BOM GAV incl. URLs (commit c6cc7bd0aa #1399); circular/self
    BOM imports guarded by ancestry check `isAlreadyResolved` (`RP:984-995`; commit ff02bf5ce0 #4284 SOE fix);
    pinned `MavenParserTest.circularImportDependency:3264` (#4093), `indirectBomImportedFromParent:1225` (#124). MATCHES
    (cycle tolerance is a divergence like entry 8).

17. **DM identity key = groupId:artifactId:classifier:type** (type defaults "jar").
    `RP.createDependencyManagementKey:907-923`; `GroupArtifactClassifierType`. Pinned: the six-test `@Nested` block
    at `MavenParserTest:4428-4908` (#5402: `simple`, `withType`, `twoDependencyManagementEntries[_dependencyWithType|_twoDependencies]`,
    `allDependencyManagementEntryVariants_allDependencyVariants`); commits b537267807 (#3864), ab281daccc,
    865fb21b69 (#5375), 652e4546c1 (#4290), 97bcf4f0bd (#3704: version resolved after type and classifier). MATCHES.

18. **Managed dependency may have exclusions and no version** (9ecba0ebfc #1406; 4527f5960a #4387 "Retain
    dependencyManagement entries with exclusions"); DM entry without version legal (f6adb61b50 #1085,
    `MavenParserTest.parseDependencyManagementWithNoVersion:475`; c8dd4d726c #1084 parse failure fix).
    NOTE commit 58a4ad42a0/38518e8e2b: "managed deps without a version merged with existing version" was tried and
    REVERTED (#6472) — replacement must NOT merge. MATCHES current Maven.

19. **Scope from DM applied to dependency without explicit scope; explicit dependency scope beats managed scope.**
    `RP.getValues(dep,depth):1293-1299`. Pinned: `MavenParserTest.inheritScopeFromDependencyManagement:888` and
    `dependencyScopeTakesPrecedenceOverDependencyManagementScope:937` (#323); commits 554ccefa35, 70a4a4eb17, 5c6f274d90. MATCHES.

20. **DM does NOT override the version of the declaring POM's own direct dependencies (depth-0 rule), and a
    dependency's own DM does not override its own direct deps either.** Depth guard `RP:1046-1049` + `RP:1312-1319`
    (managed version only consulted when `d.getVersion() == null || depth > 0`). Pinned:
    `MavenParserTest.transitiveDependencyManagement:3161` (assertions: "Dependency management cannot override the
    version of a direct dependency"; "The dependency management of dependency does not override the versions of its
    own direct dependencies"), `childDependencyDefinitionShouldTakePrecedence:3616`. MATCHES Maven 3.

21. **Project DM overrides transitive dependency versions (depth > 0).** Same code path; pinned
    `managedDependenciesInParentInfluenceTransitives:836`, `managedDependencyInTransitiveAndPom:1850` (#1422,
    commit a7d5b9001e "Correctly interpret layered managed dependencies"). MATCHES.

22. **Project DM scope override cannot "promote"/widen a transitive dependency into a classpath it wasn't already in;
    it can narrow.** `RP.getDependencyScope:1253-1281` (comment at 1277-1279). Pinned:
    `cannotWidenScopeOfTransitiveDependency:1997`, `cannotWidenScopeOfImplicitTransitiveDependency:2034`,
    `canNarrowScopeOfImplicitTransitiveDependency:2071`; commit 2a07fc4363. MATCHES (empirically tuned).

23. **DM inherited by a dependency from ITS parent does NOT propagate to that dependency's dependencies' version
    selection in our graph.** Test `dependencyManagementPropagatesToDependencies:1716` (#376) is `@Disabled`
    (commit 36b6f72029 "Disabled dependencyManagementPropagatesToDependencies"). KNOWN, ACCEPTED DIVERGENCE today —
    a real-Maven replacement would FIX this; treat the disabled test as a must-enable acceptance test.

24. **LATEST / RELEASE version keywords inside DM resolved via repository metadata.**
    `RP.mergeDependencyManagement:951-958`; pinned `latestOrReleaseVersionInDependencyManagement:4354`. MATCHES
    (deprecated Maven feature still honored).

25. **Managed exclusions merged into a dependency's exclusion list.** `RP.getValues:1301-1304`; pinned
    `parseMergeExclusions:516`; commit 71143ff843 (#1490). MATCHES.

26. **DM dedup ignores scope in identity** (an entry differing only by scope replaces/coalesces).
    Pinned `ResolvedPomTest.ignoreScopeInDependencyManagement:619`. MATCHES (scope is attribute, not identity).

27. **DM list is lazily sorted for binary search; serialization order compatibility** (`RP:143-156`, `#5928/#5952`).
    Internal perf detail — replacement must preserve `getDependencyManagement()` ordering contract for serialized LSTs.

## C. Properties / profiles / placeholders

28. **System properties override project properties** (`-D` emulation). `RP.getProperty:321-323`; pinned
    `MavenParserTest.systemPropertyTakesPrecedence:3811`; commit a34eacc7c3 (#4334: explicit definitions beat Maven's
    implicit ones). MATCHES. ⚠ Determinism hazard: resolution output depends on JVM system properties
    (see memory note "deterministic parsing") — replacement should scope this.

29. **Property with null/empty value (`<foo/>`) resolves to empty string, not missing.** `RP.getProperty:331-334`;
    pinned `ResolvedPomTest.propertyWithNullValue:371` (#6391); commits 485a736e29 (#6431), be9f41a4ed (#1450). MATCHES.

30. **Circular `${project.version}` chains resolve to sentinel `"error.circular.project.version"` instead of
    StackOverflow/abort.** `RP.getProperty:350-362`. Pinned: `ResolvedPomTest.circularProjectVersionReference:733`,
    `circularProjectVersionInDependency:780`, `dependencyWithCircularProjectVersionReference:160`;
    `MavenParserTest.circularMavenProperty:3332`; commits e629be2a6f (#5573), a03d2251fe (#5584). INTENTIONALLY
    DIVERGES (Maven errors out; rewrite parses on).

31. **General cyclic property references detected, not SOE.** Commit 5d5edfde29 (#7396); pinned
    `selfReferencingPropertyDoesNotStackOverflow:3381`, `cyclicPropertyReferenceDoesNotStackOverflow:3400`;
    self-referential resolution commit b2235eaf5d (#5586). DIVERGES (graceful vs fatal).

32. **Backslash-escaped placeholders `\${...}` are not interpolated.** Commit e402269a1f (#6817). MATCHES.

33. **Implicit property aliases**: `groupId|project.groupId|pom.groupId`, same for artifactId/version,
    `parent.*|project.parent.*`, `prerequisites.maven` variants (`RP.getProperty:335-370`). Pinned:
    `MavenParserTest.groupIdAndArtifactIdAsProperties:4910`; FindManagedDependency `${project.parent.version}` (#7162).
    MATCHES (incl. legacy `pom.` prefix).

34. **Property precedence in inheritance: nearest (child-most) definition wins; profile properties beat same-POM
    properties.** `RP.mergeProperties:889-905` (first-write-wins walking child→parent) and merge order
    profile-before-pom (`RP:463-467`). Pinned: `recursivePropertyFromParentPoms:1100` (#95),
    `parentPomProfileProperty:1652`, `projectVersionPropertyOverriddenByBuilderProperty:3513`. MATCHES.

35. **Properties interpolated into repository id/URL, re-resolved after parent properties arrive.**
    `RP.updateRepositories:842-852`, `Pom.getEffectiveRepositories:150-167`; pinned
    `repositoryWithPropertyPlaceholder:132` (#2603), `repositoryWithPropertyFromParent:184`; commits d0115d0ad1 (#1543),
    74a6d19e57 (#5223), 40a5463052 (mirrors/auth apply AFTER substitution). MATCHES.

36. **Profile activation — activeByDefault deactivated when any other profile in the same POM activates.**
    `Pom.effectiveProfiles:177-187`; `ProfileActivation.isActive:39-53` (comment admits the "any explicit activation"
    check is "overly broad"). Pinned: `MavenParserTest` nested tests `activeByDefaultWithoutPomLocalActiveProfile:1397`,
    `activeByDefaultWithPomLocalActiveProfile:1429` (#4269); commit cd95e0ea65 (#4270);
    `MavenSettingsTest.defaultActiveWhenNoOthersAreActive:166`, `defaultOnlyActiveIfNoOthersAreActive:248` (#131).
    MOSTLY MATCHES (documented approximation).

37. **Profile activation by JDK**: prefix match on `java.version` plus version-range support.
    `ProfileActivation.isActiveByJdk:59-75`; pinned `profileNoJdkActivation:1927`, `profileJdkSoftVersionActivation:1963`,
    `RawPomTest.profileActivationByJdk:34`; commits 7f05481233 (#1462), f6f7f3ba3f. MATCHES semantics but is
    HOST-DEPENDENT (activation depends on the JVM running rewrite, not the project) — determinism hazard.

38. **Profile activation by property checks OS ENVIRONMENT variables, not Maven user/system properties.**
    `ProfileActivation.isActiveByProperty:77-93` (`System.getenv`); `!name` negation string-replaced. Pinned:
    `parseNotInProfileActivation:1532` (#378), `RawPomTest.profileActivationByAbsenceOfProperty:39`.
    DIVERGES from real Maven (Maven evaluates `-D` properties; env only via `env.` prefix, which this code ignores).
    Empty `<activation/>` and empty `<value/>` tolerated: `parseEmptyActivationTag:1556`, `parseEmptyValueActivationTag:1587`
    (#1427, commit bc565197ff). Also `os`/`file` activations are NOT modeled at all (fields absent from
    `ProfileActivation`). DIVERGES (gap).

39. **settings.xml profiles/activeProfiles participate in resolution** (parse-time and recipe-time).
    Pinned: `MavenParserTest.ProfileActivations.settingsActiveProfiles:1461`; commit b818b096bf (#2333),
    0d7f100a55 (#190/#280/#131: parent poms defining profiles). MATCHES.

## D. Version ranges / snapshots / metadata-driven versions

40. **Conflict mediation: nearest-wins chain.** Direct (depth-0 project POM) requirement always wins
    (`VR.cacheResolved:244-246` "dependencies defined in the project POM always win"); else nearest soft requirement;
    hard requirements (ranges, LATEST/RELEASE) resolved to the HIGHEST available version matching
    (`VR:255-283`). Pinned: `VersionRequirementTest` (`multipleSoftRequirements:40`, `softRequirementThenHardRequirement:46`,
    `hardRequirementThenSoftRequirement:53`, `nearestRangeWins:60`, `emptyUnboundedRange:67`,
    `malformedVersionsAreSkipped:73` #7234). MATCHES Maven's nearest-wins for the pinned cases.

41. **Range parsing via ANTLR `VersionRangeParser`; unclosed range auto-closed** (jackson-databind 2.12.0-rc2's
    `[2.12.0-rc2` profile block) `VR.VersionSpec.build:89-94`; commits a7f9d10bf6, 1c71ccc813 (#1250 `exactly` bound),
    975cc0e12c (#3028 missing bounds), 39e3030dd8 (tilde ranges). MATCHES lenient Maven parsing.

42. **Same direct dependency declared twice with ranges → LAST range wins, explicitly "counter to what Maven does
    most of the time".** Comment in `MavenParserTest.invalidRange:231-232`; unsatisfiable range throws
    `MavenParsingException` with the RangeSet in the message (`invalidRange:228-252`). INTENTIONALLY DIVERGES
    (documented in-test).

43. **Whole-graph restart when a new requirement changes an already-resolved version.**
    `RP.doResolveDependencies:1083-1090` (clear listener, recurse from scratch). Approximates Maven's global mediation;
    also range re-resolution fixes bcb9eccc85 (#2175), 6f2ba162cc (#2174), `differentRangeVersionInDependency:255`,
    `differentRangeVersionInParent:314`, `rangeVersion:58`, open-range tomee check (a616efe52f #266), `guava25:425`.
    MATCHES outcomes.

44. **SNAPSHOT dated-version resolution**: newest `<snapshotVersion>` filtered by requested CLASSIFIER
    (`MPD.datedSnapshotVersion:797-810`, classifier recovered from containing POM `MPD:1273-1284`); fallback
    `SNAPSHOT → timestamp-buildNumber` substitution (`MPD:812-816`); metadata download failure → return base version
    (local-only artifacts, `MPD:790-795`, commit 1683a44207 #1385). Pinned:
    `fetchSnapshotWithCorrectClassifier:558` (rewrite-maven-plugin#862), `useSnapshotTimestampVersion:1365` (#3152),
    `datedSnapshotVersionIncludesSnapshotRepositories:513`; commits dbe656f5b6 (#3153), 88c15ea96c/7745427f08 (#2598
    null timestamps), 294a3ae89c (pinned snapshot versions via ctx, `MPD:780-787`). MATCHES.

45. **Timestamped snapshot version accepted as input and mapped back to base `-SNAPSHOT`.**
    `MPD.handleSnapshotTimestampVersion:764-776`, regex `MPD:67`; commit f70aad1ef5 (#6788). MATCHES.

46. **Release/snapshot repository policies honored** (`<releases>/<snapshots><enabled>` incl. property placeholders
    resolved via containing POM): `MPD.repositoryAcceptsVersion:1235-1247`; pinned `dontFetchSnapshotsFromReleaseRepos:406`,
    `MavenParserTest.emptyArtifactPolicy:665`; commits ab857250bd, 1be555782e. **Hardcoded special case:**
    `https://repo.spring.io/milestone` only accepts versions matching `.*(M|RC)\d+$` (`MPD:1240-1243`).
    INTENTIONALLY DIVERGES (hardcoded convenience).

47. **LATEST/RELEASE named versions resolved via metadata**; RELEASE excludes snapshots, LATEST includes
    (`MPD.resolveNamedVersion:822-843`; `VR.DynamicVersion:163-176`). MATCHES (legacy feature).

48. **Custom `Version` comparator** reimplementing Maven's `ComparableVersion` ordering (`tree/Version.java`;
    alloc optimizations #6317). UNVERIFIED total parity with `ComparableVersion` — replacement should use Maven's own.

## E. Repositories / mirrors / auth / settings

49. **Effective repository order: local → settings-profile repos → POM/parent repos → Maven Central appended last
    if no repo with id "central" present.** `MPD.distinctNormalizedRepositories:845-877`; super-POM modeled as just
    Central (`RP.SUPER_POM:60-64`). Pinned: `MavenPomDownloaderTest.repositoryOrder:105`,
    `centralIdOverridesDefaultRepository:161` (#3908, commit 9cf0f031f5: settings repos override same-id POM repos incl.
    central), `onlyAccessRequiredRepositories:237`; commit 33a50e34c3 (#4954 settings precede local reordering).
    PARTIAL MATCH: real Maven puts central (super-POM) FIRST then others; rewrite dedups by id and appends central last.
    Order differences are observable when the same GAV exists in multiple repos.

50. **Repository dedup by id with first-wins iteration; child-POM repos before parent's.** `RP.mergeRepositories:854-887`
    (skip incoming when id already present), lazy dedup on normalized id `MPD:880-916`. MATCHES approximately.

51. **HTTPS-upgrade probing**: try `https://` OPTIONS, then HEAD, then original `http://` (`MPD.normalizeRepository:1006-1040`);
    pinned `normalizeRepository(String,String)` parameterized test `:92`, `useHttpWhenHttpsFails:381`,
    `useHttpsWhenAvailable:1305`, `normalizeAcceptErrorStatuses:1326`, `normalizeRejectConnectException:372`;
    commits f534081ce4 (#2370), 1cf74474da, 9f6a3ed74e. INTENTIONALLY DIVERGES — Maven uses the URL as configured.

52. **Unreachable-endpoint negative caching per run, keyed host:port** (`MPD:967-989`, detailed comment on WHY);
    plus cache-level negative normalization results. Pinned `unreachableHostProbedOncePerRun:173`,
    `listenerRecordsFailedRepositoryAccess:260`; commits 82f7d5b73f (#8032), 1e340719a7 ("bad repo" TTL cache).
    INTENTIONALLY DIVERGES — this is Jon's founding reason #2; a replacement must preserve dead-repo skip behavior.

53. **4xx (except 408/425/429) treated as cacheable client-side misses** for POM and metadata
    (`MPD.HttpSenderResponseException.isClientSideException:1210-1215`, cache empty result `MPD:297-302,324-328,719-722`);
    commit fd27b0e4da (#3542). DIVERGES (negative caching by design).

54. **Retry policy: max 5 retries, 500ms + jitter, ONLY on socket/timeout exceptions** (`MPD.retryPolicy:59-65`);
    pinned `retryConnectException:359`, `connectTimeout:1017` (#4080); commits 8d07ab241c (stop retrying too many
    exception types), 8cb10baddd (#1491 leak). DIVERGES (Maven doesn't retry by default) — but benign.

55. **Mirror matching semantics**: `*`, `external:*`, comma lists, `!` exclusions, "named beats wildcard";
    mirrors never apply to the repo with id "local"; only first matching mirror applies (no recursion);
    applying a mirror overrides snapshot/release policy to the mirror's and resets `knownToExist`.
    `MavenRepositoryMirror.matches:139-163`, `apply:117-136`. Pinned: `MavenRepositoryMirrorTest` (`useFirstMirror:31`,
    `matchById:39`, `excludeFromWildcard:61`, `localM2RepositoryIsNeverMirrored:82`),
    `MavenSettingsTest.mirrorReplacesRepository:298` (#130), `starredMirrorWithExclusion:340`,
    `MavenPomDownloaderTest.mirrorsOverrideRepositoriesInPom:286`, `MavenParserTest.mirrorsAndAuth:985`;
    commits 28b8ae57bb (#3545 no recursion), 2563be7ee3 (#6453 local), 8f3f78be8d/9c611c206c (#1116 policies),
    d24a745d5f (knownToExist), 59e4634e60 (#3543 precompiled matching), 88d9bc90fd (#4159 mirror caching). MATCHES
    Maven mirror spec (minus `external:http:*` which is unhandled).

56. **Mirrors/credentials applied AFTER property substitution of repo URL** (`MPD.normalizeRepository:919-926`;
    commit 40a5463052). MATCHES.

57. **Server credentials, encrypted passwords (settings-security.xml), master password, `${env...}`/`${...}`
    interpolation in settings, HTTP headers per server, per-server timeouts.**
    `MavenSettings.Interpolator:268-275` + `MavenSecuritySettings`; `MPD.applyAuthenticationAndTimeoutToRequest:1150-1175`.
    Pinned: `MavenSettingsTest` (`serverCredentials:395`, `serverCredentialsWithEncryption:423`, `serverTimeouts:479`,
    `serverHttpHeaders:921`, `properties:565`, `unresolvedPlaceholdersRemainUnchanged:606`, `env:650`);
    `MavenSecuritySettingsTest` (`decryptCredentials:59`, `relocatedCredentials:103`, `handleInvalidEncryptedPassword:157`,
    `invalidMasterPasswordButValidPasswordFormat:263`); commits 16df13a793 (#4753), 1aa2e01d49 (#5606),
    d0eda87745 (#6845 unresolved credential placeholders → treat as no credentials, pinned
    `doesNotUseAuthenticationIfCredentialsCannotBeResolved:1480` #3142). MATCHES.

58. **Anonymous retry when credentials are rejected with 4xx** — for POM, metadata, and (since #8158) JAR download.
    `MPD.requestAsAuthenticatedOrAnonymous:1112-1138` (Javadoc: "Replicates Apache Maven's behavior to attempt
    anonymous download if repository credentials prove invalid"). Pinned:
    `usesAnonymousRequestIfRepositoryRejectsCredentials:1401`, `usesAuthenticationIfRepositoryHasCredentials:1436`;
    commits 6ba6c1bbf4 (#1988), 15bbed128f (#1859/#2401 regression), 23cdd71c02 (#8158). CLAIMED MATCH — verify against
    real Maven transport before relying on it.

59. **settings.xml discovery & merge: `~/.m2/settings.xml` merged with `$M2_HOME|MVN_HOME|MAVEN_HOME/conf/settings.xml`;
    concatenate unique-id elements, first-settings-first ordering, matching ids replaced.**
    `MavenSettings.readMavenSettingsFromDisk:126-143`, `merge:205+`; pinned `MavenSettingsTest` nested merge tests
    (`concatenatesElementsWithUniqueIds:754`, `mergedOrderingPutsFirstSettingsFirst:813`, `replacesElementsWithMatchingIds:861`),
    `idCollisionLastRepositoryWins:206`; commits 898f8fdd67 (#4956), 5baed33217 (#5242 M2_HOME). MATCHES.

60. **localRepository from settings.xml (path or file URI) honored; default `~/.m2/repository`;
    MAVEN_LOCAL_USER_NEUTRAL substitution for cache-key stability.** `MavenSettingsTest.LocalRepository` nested
    (`parsesLocalRepositoryPathFromSettingsXml:511`, `...Uri...:528`, `defaultsToTheMavenDefault:546`) (#1688);
    `MPD.download:629-632`; pinned `doNotRenameRepoForCustomMavenLocal:1005`; commits 828d7fd4d1 (#1923), 4edba6d4fd (#4484).
    MATCHES (user-neutral rewrite is an internal cache detail).

61. **Repos with URI containing `0.0.0.0` skipped** (`MPD:948-954`; `skipBlockedRepository:349` #3141). MATCHES INTENT
    (Maven 3.8's blocked-mirror default) but implemented as substring hack.

62. **Non-HTTP(S)/file schemes (s3://, etc.) rejected** (`MPD:962-966` "can be s3 among potentially other types for
    which there is a maven wagon implementation"). DIVERGES (gap): Maven supports custom wagons/transports.

63. **file:// repositories fully supported**: direct filesystem reads, `maven-metadata-local.xml` (`MPD:281-289`),
    non-ASCII percent-encoding normalization idempotent (`MPD.normalizeFileUri:1286-1313`; pinned `normalizeFileUri:783`
    parameterized, `downloadMetadataFromFileRepoWithNonAsciiPath:707`; commits bf79129128 #6960, 4a6b10636a #7027,
    457fcc12fc #6993). MATCHES.

64. **Local-repo artifact validity: POM without a downloaded (non-empty) JAR is unusable for jar-packaging deps;
    JAR without POM is usable (synthesized POM).** `MPD.download:600-649` (jar-existence checks),
    `MPD:735-752` (synthesize `RawPom` from GAV when only JAR found), `jarExistsForPomUri:1083-1109` (HEAD probe,
    #5675). Pinned: `skipsLocalInvalidArtifactsMissingJar:905`, `skipsLocalInvalidArtifactsEmptyJar:936`,
    `dontAllowPomDownloadFailureWithoutJar:969`, `allowPomDownloadFailureWithJar:983`,
    `pomNotFoundWithNoJarShouldThrow:1521`, `pomNotFoundWithJarFoundShouldNotThrow:1547` (#4687),
    `ResolvedPomTest.PomDownloadFailure` nested (`singleRepositoryContainingJar:465`, `twoRepositoriesSecondContainingJar:490`);
    commit 2da2a3873e (#7685: only fall back on 404, pinned `doesNotThrowONMissingModuleWhenNot404:1694`). MATCHES
    (Maven warns "POM missing, no dependency information available" and proceeds).

65. **Gradle Module Metadata augmentation**: for POMs marked `published-with-gradle-metadata`, fetch the `.module`
    file and inject `platform`/`enforcedPlatform` deps as DM imports. `MPD:660-691`
    ("It isn't strictly correct to do this from a maven standpoint, but helps with emulating Gradle dependency
    resolution"); commit 66585e1a61 (#6488: platforms must NOT become requested deps). INTENTIONALLY DIVERGES —
    load-bearing for rewrite-gradle; a Maven-API replacement must keep this hook.

66. **Metadata derivation when maven-metadata.xml is absent**: HTML index scraping (Nexus-style) and file-dir listing;
    only when no explicit version requested; disabled per-repo after non-404 4xx; version dirs must contain a real
    `.pom` (guards against `.lastUpdated`-only dirs). `MPD.deriveMetadata:355-458`, `hasPomFile:1259-1262`.
    Pinned: `deriveMetaDataFromFileRepository:686`, `deriveMetaDataFromHtmlBasedRepository:791`,
    `deriveMetaDataFromHtmlWithTitleAttributes:804` (#6739), commit 623ee5ed9f (#6217 no `../` links),
    0cee243991 (#6745), a5e1847acf (#2243). AUGMENTS (Maven has no equivalent).

67. **Metadata merged across ALL repositories** (union of versions, newest snapshot, max lastUpdated,
    `Semver.max` of latest/release), not first-hit. `MPD.mergeMetadata:460-490`; pinned `mergeMetadata:840`;
    commit a5ca62fb2d (#2265). MATCHES effective behavior of consulting all repos (Maven consults per-repo metadata
    during range resolution similarly, but merge tie-breaks like Semver.max for `latest` are custom).

68. **Proxies: parsed into the settings model (`parseProxies:1017`, #7092) but NOT applied to HTTP requests** —
    no usage of `Proxies` in MavenPomDownloader. DIVERGES (gap) from real Maven.

69. **HTTP timeouts configurable per repo/server** (#3951, #4302 single timeout). Custom `HttpSender` from
    ExecutionContext (eb2a398b2e #2033) — replacement must keep the HttpSender abstraction.

## F. Scopes / exclusions / optional / graph shape

70. **Scope transitivity table** per Maven's dependency-mechanism table (`Scope.transitiveOf:44-95`;
    pinned `ScopeTest.isInClasspathOf:25`). Scope names case-insensitive; unknown scope → `Invalid` and the
    dependency doesn't crash parsing (`continueOnInvalidScope:758` #199; `handlesPropertiesInDependencyScope:722` #198,
    property placeholders inside `<scope>`). MATCHES.

71. **Duplicate direct dependency (same GA): LAST declaration wins — version AND scope.**
    `RP.doResolveDependencies:1027-1035` (`rootDependencies.put` overwrite, comment "For direct dependencies that are
    duplicated, last declaration wins" + TODO about Gradle strategy). Pinned parameterized tests
    `lastListedDependencyIsUsed:4165`, `lastListedDependencyIsUsedForScope:4203`,
    `lastListedDependencyIsUsedForTransitiveScope:4248`. MATCHES Maven.

72. **Transitive conflicts at equal depth: FIRST declaration wins.** `RP:1211-1218` (`putIfAbsent`, comment).
    Pinned `firstDeclarationWinsForEqualDistanceOfTransitiveDependencies:4293`. MATCHES.

73. **Nearest-depth wins across depths** via BFS + `VersionRequirement` (entries 40/43). Pinned broadly
    (`diamondProblem:5028`, `guava25:425`, `jaxbRuntime:4125`). MATCHES.

74. **Exclusions propagate down the entire transitive chain** (`RP:1179-1181`; commits 9ecb580bb8, 46d40b0f47,
    942dc0620e). **Glob matching** on group/artifact incl. `*` (`RP:1182-1183`, `matchesGlob`); pinned
    `wildcardExclusion:3998`, commit 00cca918ab. **Effective exclusions attributed to the DECLARING dependency**
    (`RP:1184-1200`, `includedByMap`; commit 8f08ce3ba3 #8117; `ResolvedDependencyTest.exclusionHidesTransitiveMatch:92`,
    `exclusionWithGlobPattern:102`). Behavior MATCHES Maven for `*`; arbitrary globs are a SUPERSET (Maven only
    supports literal ids and `*`).

75. **Optional dependencies excluded from transitive resolution** (`RP:1204-1207`); pinned `optionalDependencies:2493`.
    MATCHES.

76. **Dependency `<type>` filter: only jar, ejb, pom, zip, bom, tgz enter the graph** (`RP:1058`); `pom`-type deps
    resolved transitively (7127f8508e #1583); tgz included (a979e4be32 #5609,
    `unresolvableTgzDependencyShouldNotFailBuild`, `siblingDependencyWithTgzPackagingAndSnapshot` ResolvedPomTest:846);
    missing POM for other types skipped "like Maven does" (`RP:1221-1228`). PARTIAL MATCH (Maven's artifact handlers
    are extensible; e.g. `test-jar`, `war`, `maven-plugin` types not classpath-resolved here).

77. **BOM/import-scope entries never appear as runtime dependencies** (#6487 `bomsShouldNotAppearInRuntimeDependencies:4992`).
    MATCHES.

78. **Classifier significant in graph identity and metadata lookups** (`RP.contains:1243-1251`, dedup key `RP:1212-1217`);
    transitive GAV/classifier/type resolved with the CONTAINING POM's properties (`RP:1163-1177`, commit 0ad7b82356 #6464,
    pinned `emptyClassifierPropertyInIntermediatePom:1655`). MATCHES.

79. **Scope cannot be widened by the root project for transitives; scope narrows per containing-POM's classpath**
    (entries 19/22). Also parent's test-scope deps not leaked into child compile classpath (b5a8207e45 #813,
    07b641435d #914). MATCHES.

80. **Direct dependency without version (and no DM source) → hard error naming coordinates** (`RP:1051-1057`
    "No version provided for direct dependency"); pinned d1e901bf41 (#5405 with type). MATCHES.

81. **Unresolved `${...}` in final GAV → MavenDownloadingException, not URISyntaxException** (`RP:1098-1102`,
    `MPD:561-564`; commit 83bd3887c1 #7313). MATCHES intent (fail with context).

82. **Invalid transitive POMs degrade gracefully exactly like Maven's "[WARNING] The POM ... is invalid, transitive
    dependencies will not be available".** Giant behavioral comment + tests `invalidTransitives:3953`,
    `invalidDirect:3975` (MavenParserTest:3925-3951 documents the exact `mvn dependency:tree` output being matched);
    `MavenDependencyFailuresTest.unresolvableTransitiveDependencyDueToInvalidPom:126,317`. MATCHES (explicitly
    verified against mvn output).

83. **Self-recursive dependency tolerated** (`selfRecursiveDependency:806` #135, in-test comment: "Maven itself would
    respond to this pom with a fatal error. So long as we don't produce an AST with cycles it's OK").
    INTENTIONALLY DIVERGES.

84. **Deterministic scope resolution order** (commit 378e6acd6e #6238). Internal contract for stable LSTs.

## G. Metadata / caching semantics (cross-cutting)

85. **Pluggable `MavenPomCache`** with positive AND negative entries for POMs, metadata, and normalized repositories
    (`MPD:270, 327, 340, 587, 637, 696, 721, 960, 991, 999`); RocksDB impl invalidated via `Pom.getModelVersion()`
    bump (commit 849595d70f #7887). REQUIREMENT: replacement must keep this seam (founding reason #3).

86. **Resolved-dependency-POM memoization** during graph walk (`RP:1106-1124` `getResolvedDependencyPom`).
    Perf seam; commit 0f9a438b25 "Don't solve the same parent/depManagement subtree problems repeatedly".

87. **Non-dated SNAPSHOT artifacts always re-checked in `LocalMavenArtifactCache`** (dfa1e7a162 #6623) — artifact side,
    mirrors Maven's updatePolicy=always for snapshots (approximate; there is no updatePolicy/interval model at all —
    DIVERGES: Maven's `daily`/`interval:X` update policies are unimplemented).

88. **Default packaging "jar"** (`RP.getPackaging:306-308`, commit 8311e3ce02); packaging can be a property
    (97ad1837cb #888); "bundle" packaging treated as jar (ddde0aeaa7 #4219). MATCHES.

89. **Metadata `lastUpdated` parsed as ZonedDateTime; null-safe snapshot/version lists** (bf6af04ddf #4511,
    56986647f0 #4285, ce1e53d80d; pinned `MavenMetadataTest.deserializeMetadata:31`, `deserializeSnapshotMetadata:58`,
    `deserializeMetadataWithEmptyVersions:101`). Parser resilience.

## H. Error handling / reporting

90. **Aggregated `MavenDownloadingExceptions` with per-root GAV attribution; resolution continues past individual
    failures** (`RP:1221-1228`, `setRoot`); pinned `MavenDependencyFailuresTest` (all 8 tests:
    `unresolvableParent:46`, `unresolvableMavenMetadata:85`, `unresolvableDependency:224`, `unreachableRepository:265`,
    `oldPomVersionNoDependencyResolution:293`, `unresolvableTransitiveDependencyDueToInvalidPom:126,317`,
    `unresolvableTgzDependencyShouldNotFailBuild:349`). DIVERGES BY DESIGN: rewrite must produce an LST with warning
    markers where `mvn` would abort; the *model contents* should still match Maven where resolution succeeds.

91. **Repository responses recorded per repo in the exception** (`MPD:571, 750-751` `setRepositoryResponses`);
    "previous failure" phrasing for negative-cache hits (`MPD:330, 731`). Observability contract used by
    `MavenMetadataFailures` data table (#2660/#4279).

92. **Failing POM identified in parse/deserialization errors** (#5558 `clearlyIdentifyWhichPomFailedToParse:1579`;
    `RawPomTest.pomAtOriginOfDeserializationExceptionIsPartOfExceptionMessage:198`).

93. **`ResolutionEventListener` event stream** (downloads, errors, parents, DM, BOM imports, repository access
    failures) — `MPD` & `RP` call sites throughout (`MPD:255, 265, 346, 503, 579, 638, 644, 697, 714, 744, 748, 943-994`;
    `RP:525-527, 556-559, 897-899, 938-940, 960-962, 1087-1089, 1138-1140`). Pinned `listenerRecordsRepository:202`,
    `listenerRecordsFailedRepositoryAccess:260`. API contract to preserve.

94. **`skipDependencyResolution` parser option** (`MavenParserTest.skipDependencyResolution:107`) and
    `resolveDirectDependencies` fast path (`RP:1007-1015`). Performance contracts.

## Notable issue references tied to resolution bugs (for archaeology)
#93 #95 #124 #130 #131 #135 #190 #198 #199 #261 #266 #280 #323 #376 #378 #813 #888 #914 #1084 #1085 #1116 #1250
#1385 #1399 #1406 #1422 #1427 #1443 #1450 #1454 #1462 #1487 #1490 #1543 #1583 #1624 #1688 #1801 #1859 #1923 #1988
#2033 #2049 #2075 #2151 #2159 #2174 #2175 #2243 #2253 #2265 #2333 #2334 #2373 #2401 #2598 #2603 #3023 #3028 #3141
#3142 #3152 #3153 #3169 #3542 #3543 #3545 #3678 #3704 #3727 #3801 #3908 #3951 #4080 #4093 #4159 #4269 #4270 #4284
#4285 #4302 #4319 #4334 #4387 #4484 #4511 #4512 #4687 #4753 #4888 #4954 #4956 #5070 #5223 #5242 #5375 #5402 #5405
#5450 #5491 #5529 #5573 #5584 #5586 #5606 #5609 #5668 #5675 #5899 #5928 #6119 #6217 #6238 #6391 #6431 #6453 #6464
#6472(reverted) #6477 #6487 #6488 #6506 #6546 #6574 #6596 #6605 #6623 #6739 #6745 #6788 #6817 #6845 #6860 #6875
#6902 #6908 #6955 #6960 #6993 #7027 #7092 #7162 #7223 #7234 #7305 #7313 #7351 #7396 #7480 #7633 #7685 #7887 #7905
#8032 #8117 #8158; plus rewrite-maven-plugin#543, #862 and moderneinc/customer-requests#1155, #1950.

---

## The 15 highest-value acceptance tests for a replacement

1. **`MavenParserTest.parentNearerThanBom`** — parent DM beats imported-BOM DM (commit 50e53e4220). The single most
   subtle DM-precedence rule; regressions here silently change managed versions.
2. **`MavenParserTest.transitiveDependencyManagement`** — DM never overrides a POM's own direct dependency versions
   (depth-0 rule), at both root and dependency level.
3. **`MavenParserTest.dependencyScopeTakesPrecedenceOverDependencyManagementScope` +
   `inheritScopeFromDependencyManagement`** (#323) — scope inheritance duality.
4. **`MavenParserTest.cannotWidenScopeOfTransitiveDependency` + `canNarrowScopeOfImplicitTransitiveDependency`** —
   scope-widening prohibition (commit 2a07fc4363).
5. **`MavenParserTest.lastListedDependencyIsUsed*` (3 parameterized tests) +
   `firstDeclarationWinsForEqualDistanceOfTransitiveDependencies`** — duplicate-declaration mediation:
   last-wins direct, first-wins transitive.
6. **`VersionRequirementTest` (whole class) + `MavenParserTest.rangeVersion`/`differentRangeVersionInParent`** —
   nearest-wins chains, soft-vs-hard requirements, range resolution to highest matching, malformed versions skipped.
7. **`MavenParserTest.exclusionsAffectTransitiveDependencies` + `wildcardExclusion` +
   `ResolvedDependencyTest.exclusionHidesTransitiveMatch`** — exclusion propagation + glob semantics +
   effective-exclusion attribution (#8117).
8. **`MavenParserTest.canConnectProjectPomsWhenUsingCiFriendlyVersions` + `multipleCiFriendlyVersionPlaceholders` +
   `propertyFromMavenConfigFromParentPomCanBeUsedInChild`** — CI-friendly versions across reactor with external properties.
9. **`MavenPomDownloaderTest.emptyRelativePathSkipsLocalParentLookup` +
   `shouldNotThrowExceptionForModulesInModulesWithRightProperty` + `canResolveDifferentVersionOfProjectPom`** —
   reactor/relativePath parent-lookup tiers (§4.0.0 semantics).
10. **`MavenPomDownloaderTest.mirrorsOverrideRepositoriesInPom` + `MavenSettingsTest.starredMirrorWithExclusion` +
    `MavenRepositoryMirrorTest.localM2RepositoryIsNeverMirrored`** — full mirror-spec matrix incl. policy override.
11. **`MavenPomDownloaderTest.usesAnonymousRequestIfRepositoryRejectsCredentials` +
    `usesAuthenticationIfRepositoryHasCredentials` + `doesNotUseAuthenticationIfCredentialsCannotBeResolved`** —
    auth application, anonymous fallback, unresolved-placeholder credentials.
12. **`MavenPomDownloaderTest.dontFetchSnapshotsFromReleaseRepos` + `fetchSnapshotWithCorrectClassifier` +
    `useSnapshotTimestampVersion`** — snapshot repo policy + classifier-aware dated-snapshot pinning.
13. **`MavenPomDownloaderTest.pomNotFoundWithJarFoundShouldNotThrow` + `dontAllowPomDownloadFailureWithoutJar` +
    `ResolvedPomTest.PomDownloadFailure.twoRepositoriesSecondContainingJar`** — jar-without-pom synthesis and
    pom-without-jar rejection (local repo hygiene).
14. **`MavenParserTest.selfRecursiveParent` + `selfRecursiveDependency` + `circularImportDependency` +
    `ResolvedPomTest.circularProjectVersionReference`** — the cycle-tolerance suite (every one is an intentional
    divergence where real Maven aborts; the replacement MUST NOT regress to fatal errors).
15. **`MavenDependencyFailuresTest` (whole class) + `MavenParserTest.invalidTransitives`/`invalidDirect`** —
    graceful degradation contract: warn-and-continue with exact Maven-equivalent dependency sets for invalid POMs
    (documented against real `mvn dependency:tree` output at MavenParserTest:3925-3951).

**Plus two meta-criteria** that aren't single tests:
- `MavenParserTest.dependencyManagementPropagatesToDependencies` (currently `@Disabled`, #376) should be RE-ENABLED
  by a real-Maven replacement — it is a known deficit of the custom algorithm.
- Repository-order observability (`repositoryOrder`, `centralIdOverridesDefaultRepository`, `onlyAccessRequiredRepositories`)
  and dead-repo skip (`unreachableHostProbedOncePerRun`) must be preserved even though real Maven has no equivalent —
  they are founding performance requirements, not parity requirements.
