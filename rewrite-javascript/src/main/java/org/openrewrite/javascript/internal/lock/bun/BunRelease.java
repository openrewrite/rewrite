/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.javascript.internal.lock.bun;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Bun release an install is reproduced as. The port follows Bun 1.3.10; what another release does differently is
 * asked here, one question per upstream change, named for what Bun does and answered by the release that changed it
 * (tag {@code bun-v<release>} of oven-sh/bun). The ported modules read as 1.3.10's source with each other release's
 * branch beside it.
 * <p>
 * Every question is answered through {@link #atLeast}, and nothing else reads the version.
 */
public final class BunRelease implements Comparable<BunRelease> {

    /**
     * The release the port follows, and the one an install runs as when nothing names another.
     */
    public static final BunRelease PORTED = new BunRelease(1, 3, 10);

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)");

    private final int major;
    private final int minor;
    private final int patch;

    private BunRelease(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * The release of a version such as {@code 1.3.9}; a prerelease or a range names none.
     */
    public static BunRelease of(String version) {
        Matcher m = VERSION.matcher(version);
        if (!m.matches()) {
            throw new IllegalArgumentException("not a Bun release: " + version);
        }
        return new BunRelease(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)));
    }

    /**
     * Whether this is the given release or a later one: the one way a ported module asks about its release.
     */
    public boolean atLeast(int major, int minor, int patch) {
        return compareTo(new BunRelease(major, minor, patch)) >= 0;
    }

    // src/semver: Version.zig, SemverQuery.zig, SemverRange.zig (src/install/semver.zig before 1.2.3)

    /**
     * A version whose {@code x}, {@code X} or {@code *} directly follows its patch number ({@code 1.2.3x}) is that
     * version, the wildcard skipped ({@code Version.parse}), from 1.2.2; before, the parser reaches code it takes for
     * unreachable ({@code else => unreachable}), which its release build does not check.
     */
    boolean skipsAWildcardAfterThePatchNumber() {
        return atLeast(1, 2, 2);
    }

    /**
     * A version's major, minor and patch are u64s ({@code VersionType(u64)}), from 1.2.23: read from up to 20 digits,
     * 0 past the u64 maximum. Before, u32s: more than 10 digits is a missing number, a value past 4294967295 is 0, the
     * ceilings of a range and a saturated bound are 4294967295, and a numeric prerelease identifier past it compares
     * as text ({@code Tag.orderPre}'s {@code parseUnsigned(u32)}).
     */
    boolean readsSixtyFourBitVersionNumbers() {
        return atLeast(1, 2, 23);
    }

    // src/install/lockfile/bun.lock.zig

    /**
     * {@code Stringifier} writes, and the text lockfile's parser reads, the integrity of a git, github, local or
     * remote tarball package after its other parts, from 1.3.10; before, neither, so that an integrity a lock holds
     * there is left behind.
     */
    boolean carriesTarballIntegrity() {
        return atLeast(1, 3, 10);
    }

    /**
     * The text lockfile's parser reads a package entry of the root without a name ({@code "@root:"}), which 1.3.0
     * writes for a {@code file:} dependency on the root; before, that is an invalid package resolution, and bun
     * ignores the lock.
     */
    boolean readsAnUnnamedRootEntry() {
        return atLeast(1, 3, 0);
    }

    /**
     * The text lockfile's parser expands an npm package's {@code ""} registry to the registry of the package's scope
     * ({@code scopeForPackageName}), from 1.3.7; before, to the default {@code https://registry.npmjs.org/}.
     */
    boolean expandsEmptyRegistryToScope() {
        return atLeast(1, 3, 7);
    }

    /**
     * bun.lock holds {@code "configVersion"} (src/ConfigVersion.zig), from 1.3.2: the lock's own when it has one, 0
     * for a lock read without it, 1 for a new lock; before, it is neither written nor read.
     */
    boolean writesConfigVersion() {
        return atLeast(1, 3, 2);
    }

    /**
     * bun.lock's overrides and patched dependencies are written JSON-escaped ({@code fmtJson}), from 1.2.3; before,
     * as they are ({@code "{s}": "{s}"}), so that a {@code "} or {@code \} in a name, range or patch path is written
     * bare.
     */
    boolean escapesOverridesAndPatchesInBunLock() {
        return atLeast(1, 2, 3);
    }

    /**
     * The text lockfile's parser reads each package entry as a package of its own ({@code appendPackageNoDedupe}),
     * in 1.2.3 alone (bun#16952); before and after, an entry whose name and resolution equal an earlier entry's is
     * that package ({@code appendPackageDedupe}), so that its dependencies are bound once per key it stands at.
     */
    boolean readsEveryPackageEntryAsItsOwnPackage() {
        return atLeast(1, 2, 3) && !atLeast(1, 2, 4);
    }

    /**
     * A package's dependency is bundled where bun.lock marks the key {@code <package's key>/<name>} {@code "bundled":
     * true}, read from each entry's third element in a pass of its own ({@code bundled_pkgs}), from 1.2.4; before,
     * every dependency the parser binds to an entry takes that entry's own {@code "bundled"} ({@code mapDepToPkg}),
     * read from its info object, where anything but a boolean is {@code Expected a boolean}.
     */
    boolean bundlesADependencyByItsOwnKey() {
        return atLeast(1, 2, 4);
    }

    /**
     * The text lockfile's parser binds a {@code workspace:} entry under a key no member took (an alias of a member) to
     * the member its resolution names and goes on, from 1.2.5; before, it falls through to {@code Unknown workspace:
     * '<path>'}, and bun ignores the lock.
     */
    boolean readsAWorkspaceEntryUnderAnotherKey() {
        return atLeast(1, 2, 5);
    }

    /**
     * bun.lock lists every override, from 1.2.8; before, only those a dependency of the hoisted tree is named for,
     * under the dependency's name, in the slot order of a hash map keyed by the name's hash ({@code found_overrides}).
     */
    boolean writesEveryOverride() {
        return atLeast(1, 2, 8);
    }

    /**
     * bun.lock's overrides are sorted by name before they are written ({@code overrides.sort}), from 1.2.14; 1.2.8 to
     * 1.2.13 write them in the order the lockfile holds them.
     */
    boolean sortsOverridesInBunLock() {
        return atLeast(1, 2, 14);
    }

    // src/install/bin.zig

    /**
     * A workspace's {@code bin} object in bun.lock has an entry a line ({@code Bin.toJson(.multi_line)}, bun#17041),
     * from 1.2.3; before, a named file's entry ends without a comma, and a map's entries follow its first line with a
     * {@code ,} and the indentation between them, the last followed by the indentation of the close.
     */
    boolean writesAWorkspaceBinAnEntryALine() {
        return atLeast(1, 2, 3);
    }

    // src/js_printer.zig writePreQuotedString, src/string_immutable.zig indexOfNeedsEscapeForJavaScriptString

    /**
     * The printer quoting a string writes what follows a character it prints as it is, up to the next byte its escape
     * search flags; from 1.2.11 that search is src/bun.js/bindings/highway_strings.cpp's, whose vectors flag a tab
     * but whose scalar loop over the last {@code len % N} bytes does not, N being the lane count of the CPU's widest
     * vector (16 on NEON and SSE, 32 on AVX2, 64 on AVX-512), so that a tab there is written as it is, until 1.3.0
     * (bun#23298). Before 1.2.11 the search flags every tab.
     */
    boolean leavesATabInTheEscapeSearchsTail() {
        return atLeast(1, 2, 11) && !atLeast(1, 3, 0);
    }

    // src/install/migration.zig detectAndLoadOtherLockfile

    /**
     * An install without bun.lock migrates a pnpm-lock.yaml where neither a package-lock.json nor a yarn.lock comes
     * first, rewriting package.json (src/install/pnpm.zig), from 1.2.23; before, it ignores a pnpm-lock.yaml.
     */
    boolean migratesAPnpmLock() {
        return atLeast(1, 2, 23);
    }

    // src/install/integrity.zig Integrity.parse, which reads bun.lock's and a packument's integrities

    /**
     * An integrity's hash is decoded from past its algorithm's {@code -}, and one with nothing past it is unknown
     * ({@code Tag.parse}'s offset), from 1.2.20 (bun#21220); before, from its eighth character whatever the
     * algorithm, two characters into a sha1's hash, which then does not decode, so that a sha1 integrity is unknown
     * (a packument falls back to its {@code shasum}, a lock's is written {@code ""}), and {@code sha256-} alone is a
     * sha256 of zeros.
     */
    boolean decodesAnIntegrityPastItsAlgorithm() {
        return atLeast(1, 2, 20);
    }

    /**
     * A hash that decodes to more bytes than its algorithm's digest is unknown ({@code calcSizeForSlice}), from 1.2.21;
     * before, it is decoded into 64 bytes whatever the algorithm and kept, cut to the digest where it is written, and
     * one of more than 64 bytes is decoded past that buffer, which the release build does not check.
     */
    boolean refusesAnIntegrityLongerThanItsDigest() {
        return atLeast(1, 2, 21);
    }

    // src/install/default-trusted-dependencies.txt, which the root's diff compares a lock's trustedDependencies against
    // where package.json lists none

    /**
     * The default trusted dependencies list {@code opencode-ai}, from 1.2.16.
     */
    boolean trustsOpencodeAiByDefault() {
        return atLeast(1, 2, 16);
    }

    /**
     * The default trusted dependencies list {@code yarn} until 1.2.19, which drops it.
     */
    boolean trustsYarnByDefault() {
        return !atLeast(1, 2, 19);
    }

    /**
     * The default trusted dependencies list {@code @anthropic-ai/claude-code}, from 1.3.12.
     */
    boolean trustsClaudeCodeByDefault() {
        return atLeast(1, 3, 12);
    }

    // src/install/lockfile.zig Package.Diff.generate, src/install/install.zig enqueueDependencyWithMainAndSuccessFn

    /**
     * The root's diff sorts both sides' overrides by name, in place, before it compares them pair by pair
     * ({@code OverrideMap.sort}), from 1.2.8; before, it compares them in the orders they were read in, so that the
     * same overrides in another order are a change, and the dependencies they name are resolved again.
     */
    boolean sortsOverridesBeforeComparingThem() {
        return atLeast(1, 2, 8);
    }

    /**
     * No override applies to the root's dependency on a member ({@code isWorkspaceOnly}), from 1.2.3; before, an
     * override named as the member replaces that dependency's version.
     */
    boolean keepsOverridesOffWorkspaceEdges() {
        return atLeast(1, 2, 3);
    }

    // src/install/lockfile/bun.lock.rs

    /**
     * bun.lock's versions 2 and 3, from 1.4.0 (bun#31539, bun#31556, bun#31602, bun#38333): a lock keeps the version
     * it was read with, 0 written as 1; a new one is 2, or 1 where a package it writes would not read back at 2, and
     * a lock whose overrides hold scoped rules is 3 unless it is held at 1 ({@code Stringifier::version_to_write}). A
     * lock of version 2 or more rejects an npm package whose tarball is outside the registries without a supported
     * integrity, and a git package whose tag is no single safe path component. Before, every lock is written at 1,
     * and a version above 1 is unknown.
     */
    boolean keepsTheLockfileVersionItRead() {
        return atLeast(1, 4, 0);
    }

    /**
     * An npm package's tarball is the default registry's, written {@code ""}, where the registry without its trailing
     * slash ends the url or is followed by {@code /} ({@code url_is_under_registry}), from 1.4.0 (bun#31129); before,
     * wherever the url starts with it.
     */
    boolean matchesTheDefaultRegistryAtAPathBoundary() {
        return atLeast(1, 4, 0);
    }

    /**
     * A GitHub package whose tag is no single safe path component ({@code repository::is_safe_resolved_tag}) is an
     * invalid package at every lockfile version, from 1.4.0; before, any tag is read.
     */
    boolean rejectsAnUnsafeGitHubTag() {
        return atLeast(1, 4, 0);
    }

    /**
     * A lockfile version the release does not read is {@code Unsupported lockfile version <n>. This lockfile was likely
     * created by a newer version of Bun. (This is Bun v<release>, which supports lockfile versions up to 3.)}, from
     * 1.4.0 (bun#32465); before, {@code Unknown lockfile version}.
     */
    boolean explainsAnUnsupportedLockfileVersion() {
        return atLeast(1, 4, 0);
    }

    // src/install/error.rs

    /**
     * A bun.lock that parses as JSON but not as a lock fails to load as {@code InvalidLockfile}, whatever the parser's
     * own error, from 1.4.0 ({@code From<bun_lock::ParseError> for Error}); before, as the parser's error, such as
     * {@code InvalidPackageInfo}.
     */
    boolean reportsEveryLockfileErrorAsInvalidLockfile() {
        return atLeast(1, 4, 0);
    }

    // src/install: dependency.zig, lockfile/Package.zig, bun.lock.zig, Tree.zig, PackageManagerEnqueue.zig

    /**
     * A package's dependencies are told apart by their group, from 1.2.19 ("fix resolving duplicate dependencies"):
     * sorted workspace, dev, optional, prod, then peer first; the root's diff matches a dependency of the same
     * behavior, and diffs a member again where the root's workspaces list it; only the workspaces group carries the
     * workspace bit, which the tests read alone; and of a package's same-named dependencies in the lock the first
     * group's resolution stands. Before, each of prod, dev, optional, peer and workspace sorts last; the diff matches
     * by name unless only one of the two is a workspace alone, and diffs a member again where the dependency is a
     * workspace spec; a member's dependencies, and one the lock resolves to a member, carry the workspace bit too, so
     * that the tests ask for a workspace alone; and the root's members follow its other dependencies in the lock.
     */
    boolean tellsDependencyGroupsApart() {
        return atLeast(1, 2, 19);
    }

    // src/install/dependency.zig Tag.infer and parseWithTag

    /**
     * A git or GitHub spec is told and read as npm's hosted-git-info does ({@code hosted_git_info.isGitHubShorthand},
     * {@code HostedGitInfo.fromUrl}), from 1.3.2 (bun#24138). Before, a GitHub shorthand is a path of letters, digits
     * and {@code -} with a slash, {@code .} and {@code _} after it, and at most one {@code #} after it
     * ({@code isGitHubRepoPath}); a URL is git when its host is github, gitlab or bitbucket ({@code Repository.Hosts}),
     * else a tarball, and any {@code git://} or {@code git+http} spec is git; and a GitHub spec's owner and repo are
     * split at its last slash before any {@code #}, a URL's repo without {@code .git}.
     */
    boolean readsGitSpecsAsHostedGitInfoDoes() {
        return atLeast(1, 3, 2);
    }

    // src/install: dependency.zig Tag.infer, lockfile/Package.zig parseWithJSON, CatalogMap.zig, bun.lock.zig

    /**
     * Catalogs, from 1.2.14: a {@code catalog:} spec names an entry of the root's workspaces {@code catalog} or
     * {@code catalogs}, which bun.lock holds; before, there are none, and {@code catalog:} reads as a git spec.
     */
    boolean parsesCatalogSpecs() {
        return atLeast(1, 2, 14);
    }

    /**
     * A {@code workspaces} object without {@code packages} declares no workspace (it may hold the catalogs alone), and
     * one whose {@code packages} is no array is {@code "workspaces.packages" expects an array of strings}, from 1.2.14;
     * before, any object without a {@code packages} array fails as {@code Workspaces expects an array of strings}, as
     * does a value neither array nor object (from 1.2.14 {@code "workspaces" expects an array of strings}).
     */
    boolean acceptsAWorkspacesObjectWithoutPackages() {
        return atLeast(1, 2, 14);
    }

    /**
     * The root's own {@code catalog} and {@code catalogs} are its catalogs when it has workspaces whose object holds
     * neither, from 1.2.19; before, only the workspaces object's are.
     */
    boolean readsTopLevelCatalogs() {
        return atLeast(1, 2, 19);
    }

    // src/ini.zig

    /**
     * .npmrc expands {@code ${VAR}} inside a quoted value too, and {@code ${VAR?}} to nothing where the variable is
     * unset, from 1.3.5; before, a quoted value is taken as it is, and {@code VAR?} is a name.
     */
    boolean expandsQuotedNpmrcValues() {
        return atLeast(1, 3, 5);
    }

    /**
     * Each .npmrc bun reads loses its leading byte order mark ({@code loadNpmrcConfig}'s
     * {@code toSource(.{ .convert_bom = true })}), from 1.2.13; before, the mark starts the first line's key, so that
     * its setting is not read.
     */
    boolean stripsTheByteOrderMarkOfNpmrc() {
        return atLeast(1, 2, 13);
    }

    /**
     * .npmrc's {@code link-workspace-packages} sets whether an npm range links to the workspace of its name
     * ({@code loadNpmrc}), from 1.2.19; before, it is not read.
     */
    boolean readsLinkWorkspacePackagesFromNpmrc() {
        return atLeast(1, 2, 19);
    }

    // src/toml/toml_parser.zig

    /**
     * bunfig.toml's parser has the lexer take {@code [[} as one token again before it consumes an array's or an inline
     * table's close ({@code parseValue}), from 1.2.16; before, the token after the close is lexed as the elements left
     * the lexer, which after an inline table's assignment or in an array of none reads a {@code [[table]]} header on
     * the next line as two {@code [}s, so that bunfig fails to load.
     */
    boolean lexesADoubleBracketAfterAValue() {
        return atLeast(1, 2, 16);
    }

    // src/bunfig.zig and src/install/PackageManager/PackageManagerOptions.zig

    /**
     * bunfig.toml's {@code [install] linkWorkspacePackages} sets whether an npm range links to the workspace of its
     * name ({@code link_workspace_packages}), from 1.2.16; before, there is no such setting, and such a range always
     * links.
     */
    boolean readsLinkWorkspacePackages() {
        return atLeast(1, 2, 16);
    }

    /**
     * bunfig.toml's {@code minimumReleaseAge} and {@code minimumReleaseAgeExcludes} hold back versions published
     * within that age (the full packument's {@code time}), from 1.3.0; before, neither is a setting.
     */
    boolean readsMinimumReleaseAge() {
        return atLeast(1, 3, 0);
    }

    // src/install/PackageManager/install_with_manager.zig installWithManager

    /**
     * The install processes the peer queue once before it waits for the peers, and waits only while a request is
     * pending, from 1.3.1; before, it waits for the peers whenever there are any, processing them in the wait.
     */
    boolean processesPeersBeforeTheWait() {
        return atLeast(1, 3, 1);
    }

    /**
     * The wait for the peers never ends once a pass of it leaves peers queued and no request pending: nothing wakes
     * the event loop again (usockets' kqueue or epoll wait without a timeout). From 1.2.21, whose
     * {@code --lockfile-only} no longer prefetches the tarball of each package it adds (PackageManagerOptions.zig
     * {@code prefetch_resolved_tarballs}), whose request kept the wait going; until 1.3.10, whose event loop counts
     * the wakeup a request's end made (bun-usockets {@code pending_wakeups}) and gives such a pass one more. 1.3.10 to
     * 1.4.0 are taken to end the wait there, as the port of 1.3.10 does: on kqueue they wait after a second such pass,
     * and on epoll as 1.3.9 does, which the port does not reproduce; from 1.4.1 (bun#40470) the wait processes the
     * peers again until none is queued before it sleeps, and ends everywhere.
     */
    boolean waitsForeverOnPeersQueuedWithNothingPending() {
        return atLeast(1, 2, 21) && !atLeast(1, 3, 10);
    }

    /**
     * bunfig.toml's {@code [install.security] scanner} names a package whose code bun runs over the resolution before
     * it saves the lock, for {@code bun install} as for {@code add}, {@code update} and {@code remove}
     * ({@code runAndWait}), from 1.2.22; its advisories may end the install, which is not the engine's to know. 1.2.21
     * runs it for {@code bun add} alone, and before, bunfig has no such setting.
     */
    boolean runsTheSecurityScannerOnInstall() {
        return atLeast(1, 2, 22);
    }

    /**
     * Before it waits for the peers, the install processes the peer queue until it is empty, so that the peers of a
     * package a peer added with nothing to fetch are resolved too, from 1.3.11; before, it processes the queue once,
     * and those peers stay unresolved when no request is pending.
     */
    boolean drainsPeersOfResolvedPeers() {
        return atLeast(1, 3, 11);
    }

    // src/install/lockfile/Package/WorkspaceMap.zig processNamesArray

    /**
     * A directory a workspaces glob matches is no workspace when a later pattern negates it ({@code !packages/a}),
     * from 1.3.0; before, a negation excludes nothing.
     */
    boolean excludesWorkspacesALaterPatternNegates() {
        return atLeast(1, 3, 0);
    }

    /**
     * The glob walker takes a component of two characters starting with {@code .} ({@code .*}, {@code .x}) for
     * itself, from 1.3.4 (src/glob/GlobWalker.zig {@code makeComponent}, bun#24939); before, for {@code ..}, so that
     * the walk climbs where the pattern does not.
     */
    boolean walksDotComponentsAsWritten() {
        return atLeast(1, 3, 4);
    }

    // src/glob: GlobWalker.zig matchPatternSlow, match.zig (from 1.2.3), ascii.zig (before 1.2.3)

    /**
     * A workspaces glob component with syntax beyond {@code *}, {@code **} and {@code *.ext} is matched by
     * src/glob/match.zig, which tries each brace branch with the rest of the pattern, from 1.2.3 (bun#16824); before,
     * by Devon Govett's glob-match (src/glob/ascii.zig, over code points in GlobWalker.zig's {@code matchImpl}), which
     * keeps the longest branch that matched and never tries a shorter one again ({@code {a,ab}b} does not match
     * {@code ab}), matches no {@code ,} or {@code }} outside a group, and takes a pattern it cannot read for no match
     * under {@code !} too.
     */
    boolean triesEveryBraceBranch() {
        return atLeast(1, 2, 3);
    }

    /**
     * Past the branch that matched, the matcher skips to the {@code }} that closes the branch's own group
     * ({@code skipBranch}), from 1.2.4; 1.2.3 stops at the first {@code }}, a nested group's too, and goes on
     * matching after it ({@code {a,{b,c}x}} matches {@code ax}, not {@code a}).
     */
    boolean skipsABraceBranchToItsOwnClose() {
        return atLeast(1, 2, 4);
    }

    // src/install/patch_install.zig PatchTask.calcHash

    /**
     * A patch file that is not there is {@code Couldn't find patch file: '<path>'}, from 1.3.0; before,
     * {@code could not find patch file <path>}.
     */
    boolean quotesAMissingPatchFile() {
        return atLeast(1, 3, 0);
    }

    /**
     * An empty patch file is {@code patchfile <path> is empty, please restore or delete it.}, from 1.2.5; before,
     * {@code plese}.
     */
    boolean spellsPleaseForAnEmptyPatchFile() {
        return atLeast(1, 2, 5);
    }

    // src/install/npm.zig PackageManifest.parse and src/install/lockfile/Package.zig parseWithJSON

    /**
     * A name {@code peerDependenciesMeta} marks optional that {@code peerDependencies} does not list is an optional
     * peer of {@code "*"}, as pnpm and Yarn take it, from 1.3.14. With it a packument's dependency group is read only
     * when it holds a dependency (the peers also for such a name), and the peers no longer reset the bundled names the
     * other groups found; before, every group that is an object is read, and the last resets them.
     */
    boolean impliesOptionalPeersOfPeerDependenciesMeta() {
        return atLeast(1, 3, 14);
    }

    // 1.4.x: src/install/lockfile/OverrideMap.rs and override_selector.rs

    /**
     * The overrides hold, beside the plain rules by name, rules scoped to the parent that declares the edge and to the
     * range it declares, from 1.4.0 (bun#38333). A package.json key is a selector: pnpm's {@code parent>child}, Yarn's
     * {@code parent/child} (one parent, {@code **} dropped), {@code name@range}; one starting {@code //} is a comment;
     * an {@code overrides} object holds its key's children and, as {@code "."}, the key's own rule; a {@code $name}
     * value is the spec the root declares for the name, else the one every workspace declares, as the key's rule; a
     * value {@code -} is refused, and an {@code overrides} that is no object only warned of. An edge takes the scoped
     * rule of its name whose parent, the parent's range and its own declared range match best, else the plain one;
     * bun.lock writes the scoped rules grouped by parent at version 3 ({@link #keepsTheLockfileVersionItRead}) and
     * reads such groups at every version; the diff and the edges resolved again count them. Before, every key is a
     * plain rule as written, but that {@code resolutions} drops a leading {@code **}{@code /} and skips a key holding a
     * slash past its scope's; an object counts for its {@code "."} alone; a {@code $name} value is the root's
     * dependency itself, written under that dependency's name; and an {@code overrides} that is no object fails the
     * install.
     */
    boolean readsScopedOverrides() {
        return atLeast(1, 4, 0);
    }

    /**
     * A {@code $name} override value is the spec the root declares for the name as written, the root's workspaces
     * entries not counting, read as the key's own spec, from 1.4.1 (bun#41302); 1.4.0 counts a workspaces entry and
     * reads the spec as the kind the root's dependency is, so that a range linked to a workspace reads as its path.
     */
    boolean readsAReferencedSpecAsWritten() {
        return atLeast(1, 4, 1);
    }

    /**
     * When the catalogs change, the install resolves again the edges whose name has a catalog-valued override,
     * plain or scoped, as well as the {@code catalog:} edges, from 1.4.0 (bun#38333); before, the {@code catalog:}
     * edges alone.
     */
    boolean reresolvesCatalogValuedOverrides() {
        return atLeast(1, 4, 0);
    }

    // 1.4.x: src/parsers (json.rs, toml.rs), src/dotenv, src/glob, src/js_printer

    /**
     * package.json and bun.lock are read by a JSON parser that indexes the document's structure first
     * (src/parsers/json_index.rs) and then descends over the index (json_stage2.rs), from 1.4.0 (bun#33032): what
     * follows the root is only looked at for a {@code /} that starts no comment or a {@code /*} without its end; an
     * object or array open at the end of the file expects its close; a key position expects a string and a missing
     * colon is {@code Expected ":"}; a token's text in a message is its whole run; and every error logged fails the
     * parse, as the parser's own ParserError or SyntaxError. Before, the JavaScript lexer reads it a token at a time,
     * one token past the root too; a missing colon is {@code Expected " ="} (js_lexer_tables.zig {@code TColon}); a
     * token's text is the lexer's; and an error {@code expect} only logs lets the parse go on.
     */
    boolean indexesJsonBeforeParsingIt() {
        return atLeast(1, 4, 0);
    }

    /**
     * bunfig.toml is read by a strict TOML 1.1 parser (src/parsers/toml.rs), every error fatal, from 1.4.0
     * (bun#32953): a bare word value is {@code Strings must be quoted}; a table is defined once, a dotted key extends
     * only its own block, and a key once; bare keys are letters, digits, {@code -} and {@code _}, and {@code =}
     * alone separates a key from its value; a line holds one statement; a comment takes no control character and
     * ends at its line's end; escapes are TOML's, a newline after a multi-line string's opening is trimmed, and
     * numbers and dates are TOML's. Before, the TOML lexer (src/interchange/toml) reads a bare word as a string, merges
     * a table reopened, takes {@code :} for {@code =} and a word's {@code @ $ :} as its own, swallows the line after
     * a {@code #} that ends one, swaps {@code \f} and {@code \t}, and only logs what {@code expect} does not find.
     */
    boolean parsesTomlStrictly() {
        return atLeast(1, 4, 0);
    }

    /**
     * A quoted string in bun.lock ({@code format_json_string_utf8} with {@code quote}) is escaped by
     * src/bun_core/fmt.rs {@code encode_json_string_chars}, from 1.4.0 (bun#30412) until 1.4.1 (bun#40718): only
     * {@code "}, {@code \}, the short escapes and the other control characters, those as <code>&#92;u00xx</code> in lowercase
     * hex, so that U+2028, U+2029, U+FEFF and a lone surrogate are written as they are. Before and after, by the
     * printer ({@code writePreQuotedString}), as an unquoted one is.
     */
    boolean escapesQuotedJsonStringsMinimally() {
        return atLeast(1, 4, 0) && !atLeast(1, 4, 1);
    }

    /**
     * The printer quoting a string for JSON writes BEL and VT as <code>&#92;u0007</code> and <code>&#92;u000B</code>, from 1.4.0
     * (bun#36746); before, as {@code \x07} and {@code \v}.
     */
    boolean escapesBelAndVtAsUnicodeInJson() {
        return atLeast(1, 4, 0);
    }

    /**
     * bun.lock's npm registry url and git or GitHub resolution are written JSON-escaped
     * ({@code format_json_string_utf8} without quotes), from 1.4.0 (bun#31417); before, as they are.
     */
    boolean escapesTheRegistryAndRepositoryInBunLock() {
        return atLeast(1, 4, 0);
    }

    /**
     * A glob is matched from past its leading {@code !}s (src/glob/matcher.rs {@code glob_start}), so that a
     * {@code **} right after them is a globstar, from 1.4.0 (bun#33759); before, from its start, so that it is two
     * {@code *}s that stop at a slash and {@code !**}{@code /test} negates no path below the top.
     */
    boolean readsAGlobstarAfterANegation() {
        return atLeast(1, 4, 0);
    }

    /**
     * Whether a workspaces entry is a glob counts the backslashes right before each {@code *}, opening brace,
     * {@code [} or {@code ?} (src/glob/lib.rs {@code detect_glob_syntax}), from 1.4.0 (bun#37052); before, those at the
     * token's offset in what is left of the pattern counted from its start, so that a token after an escaped one
     * of its kind reads as unescaped ({@code pkgs/\*x\*} is a glob).
     */
    boolean countsTheBackslashesBeforeEachGlobToken() {
        return atLeast(1, 4, 0);
    }

    /**
     * The workspaces' glob walker goes into a hidden directory where the pattern's component for it itself starts
     * with {@code .} ({@code .github/actions/*}), from 1.4.0 (bun#32853); {@code *}, {@code **} and the other
     * wildcards still pass hidden entries over. Before, it passes over every hidden entry.
     */
    boolean matchesHiddenEntriesThePatternNames() {
        return atLeast(1, 4, 0);
    }

    /**
     * An .npmrc value that is empty once its single quotes are stripped ({@code ''}) is the empty string, from 1.4.0
     * (bun#32712); before, it is parsed as JSON's empty document, an empty object, which no string setting takes.
     */
    boolean readsAnEmptyQuotedNpmrcValueAsAString() {
        return atLeast(1, 4, 0);
    }

    /**
     * A quoted .env value ends at its first unescaped closing quote and the rest of its line is dropped, from 1.4.0
     * (bun#34003); before, a closing quote ends it only where whitespace and then a newline, a {@code #} or the end
     * of the file follow, else the scan goes on to the next one, past the end of the line.
     */
    boolean endsAQuotedEnvValueAtItsFirstClosingQuote() {
        return atLeast(1, 4, 0);
    }

    /**
     * The whitespace a .env key and value are trimmed of is ASCII's alone, from 1.4.0 (bun#34001); before, the byte
     * 0xA0 too, the last of a UTF-8 no-break space's two.
     */
    boolean keepsNoBreakSpacesInEnvValues() {
        return atLeast(1, 4, 0);
    }

    /**
     * A .env file's leading UTF-8 byte order mark is dropped, from 1.4.0 (bun#34002); before, it is the first line's,
     * whose key then does not read, so that the line is skipped.
     */
    boolean stripsTheByteOrderMarkOfEnvFiles() {
        return atLeast(1, 4, 0);
    }

    /**
     * A .env value's {@code $NAME}, {@code ${NAME}} and {@code ${NAME:-default}} are expanded left to right, a
     * default in turn, {@code \$} is {@code $}, and a {@code $} that starts neither is itself (env_loader.rs
     * {@code expand_into}), from 1.4.0 (bun#36199); before, right to left, a {@code $} before any other character
     * expands the empty name, and a default ends at its first closing brace or backslash, so that bun crashes on
     * {@code ${A:-$B}}.
     */
    boolean expandsEnvValuesLeftToRight() {
        return atLeast(1, 4, 0);
    }

    // 1.4.x: src/install (dependency.rs, integrity.rs, repository.rs, npm.rs, resolution.rs) and src/semver

    /**
     * A spec or resolution is a tarball when it ends in {@code .tgz}, {@code .tar.gz} or {@code .tar}, in any case
     * ({@code dependency::is_tarball}), from 1.4.0 (bun#38333); before, when it ends in {@code .tgz} or
     * {@code .tar.gz} as written, so that {@code ./x.tar} is a folder and {@code user/repo.tar} a GitHub repo.
     */
    boolean tellsTarballsByTheirSuffixInAnyCase() {
        return atLeast(1, 4, 0);
    }

    /**
     * The text lockfile's parser rejects a package whose name is no safe {@code node_modules} folder name
     * ({@code dependency::is_safe_install_folder_name}: a {@code /} component that is empty, {@code .} or {@code ..},
     * or that holds {@code \}, {@code :} or NUL) as {@code Invalid package name}, at every lockfile version, from 1.4.0
     * (bun#31339); before, it reads any name.
     */
    boolean rejectsAnUnsafePackageName() {
        return atLeast(1, 4, 0);
    }

    /**
     * The hoisting skips a dependency whose name is no safe {@code node_modules} folder name, logging
     * {@code Invalid dependency name "<name>"} (Tree.rs {@code process_subtree}), from 1.4.0 (bun#31221, an empty name
     * passing from bun#31655); before, it hoists it.
     */
    boolean skipsAnUnsafeDependencyName() {
        return atLeast(1, 4, 0);
    }

    /**
     * An integrity of several hashes separated by ASCII whitespace is its strongest supported one, each read up to a
     * {@code ?} ({@code Integrity::parse}), from 1.4.0 (bun#33072); before, the whole string is one hash, which a
     * second hash or a {@code ?} makes unknown, so that an npm version falls back to its {@code shasum}.
     */
    boolean keepsTheStrongestOfSeveralHashes() {
        return atLeast(1, 4, 0);
    }

    /**
     * A packument's {@code shasum} is pairs of hexadecimal digits, anything else or an odd count of them making it
     * unknown ({@code Integrity::parse_sha_sum}), from 1.4.0 (bun#30412); before, {@code g} to {@code z} read as the
     * digits 16 to 35.
     */
    boolean readsAShasumAsHexadecimal() {
        return atLeast(1, 4, 0);
    }

    /**
     * Git and GitHub resolutions of one owner, repo and committish order by their resolved tag
     * ({@code Repository::order}), from 1.4.0 (bun#36578); before, they are equal.
     */
    boolean ordersRepositoriesByTheirResolvedTag() {
        return atLeast(1, 4, 0);
    }

    /**
     * A {@code github:} resolution of bun.lock splits at its last {@code #}, and its owner from its repo at the last
     * {@code /} before that, an owner being empty without one ({@code Repository::parse_append_github}), from 1.4.0
     * (bun#36165); before, at its last {@code /} and its last {@code #} wherever they stand, so that a repo without an
     * owner loses its first character ({@code github:repo} names {@code epo}) and a {@code /} after the {@code #}
     * slices the repo from past its end.
     */
    boolean splitsAGitHubResolutionAtTheSlashBeforeItsHash() {
        return atLeast(1, 4, 0);
    }

    /**
     * A workspace dependency cloned into another lockfile keeps its path ({@code clone_with_different_buffers}), from
     * 1.4.1 (bun#41302); before, the clone reads its literal again as a workspace spec, so that an npm range the
     * dependency was linked to a member by ({@code "a": "^1.0.0"}) becomes its path.
     */
    boolean keepsAWorkspaceDependencysPathWhenCloned() {
        return atLeast(1, 4, 1);
    }

    /**
     * A packument's {@code versions} key that is no version is skipped without an error (with {@code --verbose}, the
     * warning {@code Skipping version "<key>" of "<name>": not a valid semver version}), from 1.4.3 (bun#43035, which
     * holds bun#43115); before, the install logs {@code Failed to parse dependency <key>}.
     */
    boolean skipsAVersionKeyThatIsNoVersionSilently() {
        return atLeast(1, 4, 3);
    }

    /**
     * 1.4.0's manifest string builder takes a string of 8 bytes whose last byte is not ASCII for one it stores inline
     * ({@code len <= 8} where {@code String::can_inline} holds it out of line) and keeps an offset to where the string
     * stood in the packument, which it reads back as whatever its buffer holds there, an empty string where the
     * offset falls outside it (bun#40406 counts and copies such a string from 1.4.1). Before 1.4.0, Zig's builder did
     * likewise, and the port reads the string as written there.
     */
    boolean misplacesAnEightByteNonAsciiManifestString() {
        return atLeast(1, 4, 0) && !atLeast(1, 4, 1);
    }

    /**
     * The manifest parser's counting pass leaves out the {@code directories.bin} of a version whose {@code bin} is
     * {@code ""}, which the build pass appends, so that it panics ({@code range end index out of range}) where the
     * buffer's slack cannot hold it, from 1.4.0 until 1.4.3 (bun#43145) counts it; Zig's builder (before 1.4.0) wrote
     * past its buffer unchecked, which the port reads as the install going on.
     */
    boolean leavesAnEmptyBinsDirectoryUncounted() {
        return atLeast(1, 4, 0) && !atLeast(1, 4, 3);
    }

    /**
     * A range skips a chunk without a comparator, such as the {@code - foo} of {@code 1 || - foo}, wherever it stands
     * and keeping a pending {@code ||} (src/semver/SemverQuery.rs {@code parse}), from 1.4.0 (bun#31234); before, only
     * ahead of the first comparator, where it drops a pending {@code ||}, and after it bun reaches code it takes for
     * unreachable.
     */
    boolean skipsARangeChunkWithoutAComparator() {
        return atLeast(1, 4, 0);
    }

    /**
     * A version of whitespace, {@code v} and {@code =} alone is invalid, all of it read (src/semver/Version.rs
     * {@code parse}), from 1.4.0 (bun#33072); before, none of it is read, and one starting with a space is a valid
     * {@code 0.0.0}.
     */
    boolean rejectsAVersionOfSeparatorsAlone() {
        return atLeast(1, 4, 0);
    }

    /**
     * A version's number of more than 20 digits is its value, or 0 past the u64 maximum ({@code parse_version_number}),
     * from 1.4.0 (bun#34521); before, it is missing, as an {@code x} is.
     */
    boolean readsALongVersionNumberByItsValue() {
        return atLeast(1, 4, 0);
    }

    /**
     * A caret, tilde, x, partial or hyphen range whose upper bound would pass the u64 maximum ends at {@code <=} that
     * ceiling ({@code Comparator::lt_next_major}, {@code lt_next_minor}, {@code lt_next_patch}), from 1.4.0
     * (bun#34600); before, at {@code <} the saturated bound, which leaves {@code ^18446744073709551615} empty.
     */
    boolean boundsARangeAtTheCeilingInclusively() {
        return atLeast(1, 4, 0);
    }

    // 1.4.x: src/install (PackageManagerEnqueue.rs, lockfile.rs, Tree.rs, Package.rs, bun.lock.rs's reader)

    /**
     * {@code enqueue_dependency_list} enqueues every dependency of the list, from 1.4.0 (bun#35392); before, where the
     * lockfile's first dependency is a peer, each leading peer of the list that a later non-dev dependency of the list
     * also names is left out, its slot overwritten with the list's first dependency not yet left out.
     */
    boolean enqueuesEveryDependencyOfTheList() {
        return atLeast(1, 4, 0);
    }

    /**
     * Only a dependency that is no npm alias itself is redirected to the npm alias another package declared under its
     * name, from 1.4.0 (bun#33835); before, an alias whose target bears that name is redirected too.
     */
    boolean redirectsOnlyPlainDependenciesToAnAlias() {
        return atLeast(1, 4, 0);
    }

    /**
     * Before the peers are installed, a peer whose range is not {@code *} takes an existing package only where it is
     * the very version its range picks, and otherwise waits for the peers ({@code suppress_peer_satisfies}), from
     * 1.4.0 (bun#30412); before, it takes any package its range accepts.
     */
    boolean bindsAPeerBeforeThePeersOnlyToItsPick() {
        return atLeast(1, 4, 0);
    }

    /**
     * A range is not deduplicated to a package this install added below the range's own pick and of another major,
     * unless that package was added for a range of one exact version ({@code Lockfile::get_package_id}'s
     * order-independence guard, {@code mark_exact_pin}, {@code mark_loaded_packages}), from 1.4.0 (bun#30412); before,
     * to any package the range accepts.
     */
    boolean keepsARangeOffALowerMajorAddedThisInstall() {
        return atLeast(1, 4, 0);
    }

    /**
     * Where the root's diff changes what resolves (an added, removed or updated dependency, or changed overrides or
     * catalogs), every edge still resolving to the package a root or workspace dependency resolved to before follows
     * that dependency to its new npm package of the same name where the edge's range, after overrides and catalogs,
     * accepts it ({@code DirectDependencies::redirect_dependents}), from 1.4.0 (bun#38333); before, it keeps the old
     * package.
     */
    boolean redirectsDependentsOfAMovedDirectDependency() {
        return atLeast(1, 4, 0);
    }

    /**
     * A {@code file:} dependency of a package that is neither the root nor a workspace, whose path leaves the
     * package's directory ({@code bin_target_escapes_package_dir}: absolute, a colon in its first component, or
     * {@code ..} above its start), resolves to nothing unless an override or resolution names it ({@code Could not find
     * package.json for "file:<path>" dependency "<name>"}), from 1.4.0 (bun#31417, bun#32452); before, to a folder
     * package without dependencies.
     */
    boolean refusesAFolderPathLeavingItsPackage() {
        return atLeast(1, 4, 0);
    }

    /**
     * Such a path is also taken where a {@code file:} package the root or a workspace depends on directly declares it
     * ({@code is_trusted_folder_dependency}), from 1.4.3 (bun#33106); before, only where an override names it.
     */
    boolean trustsTheFolderPathsOfALocalPackage() {
        return atLeast(1, 4, 3);
    }

    /**
     * A {@code workspace:} dependency of a package that is neither the root nor a workspace, unless an override or
     * resolution names it, resolves only to the root's workspace of that name, else to nothing ({@code Workspace
     * dependency "<name>" not found}), from 1.4.0 (bun#37669); before, to the workspace folder its path names.
     */
    boolean resolvesAForeignWorkspaceSpecOnlyToTheRootsWorkspace() {
        return atLeast(1, 4, 0);
    }

    /**
     * The hoisting leaves out a folder package that a directory above already holds under the same name, so that a
     * cycle of folder packages ends, from 1.4.0 (bun#38333); before, a folder package is always placed below its
     * dependent, and such a cycle nests forever.
     */
    boolean skipsAFolderPackageADirectoryAboveHolds() {
        return atLeast(1, 4, 0);
    }

    /**
     * A peer resolved to a folder package is deduplicated where a directory above provides that package, instead of
     * nesting a second copy, from 1.4.1 (bun#40564); before, it is placed below its dependent as any folder package is.
     */
    boolean dedupesAPeerResolvedToAFolderPackage() {
        return atLeast(1, 4, 1);
    }

    /**
     * The text lockfile's parser leaves a peer that no package of the lock provides unresolved, as it does an optional
     * dependency ({@code may_stay_unresolved}), from 1.4.0 (bun#38851); before, that is {@code Failed to resolve peer
     * dependency}, and bun ignores the lock.
     */
    boolean readsAPeerTheLockLeavesUnresolved() {
        return atLeast(1, 4, 0);
    }

    /**
     * The text lockfile's parser binds a required peer whose range (its catalog entry's, for a {@code catalog:} peer)
     * is not {@code *} and that no override names as the resolver's peer pass does: to the first package of the name,
     * highest first, whose resolution satisfies the range, else to the highest when it is of the range's kind
     * ({@code resolve_peer_dep_version_based}), from 1.4.0 (bun#32182, bun#38333); before, by the package's path, as
     * every other edge.
     */
    boolean bindsALoadedPeerByVersion() {
        return atLeast(1, 4, 0);
    }

    /**
     * Only the root and its workspaces reference catalogs ({@code CatalogMap::strip_reference}), from 1.4.0
     * (bun#38333): a {@code catalog:} dependency of a package of bun.lock, of a registry manifest or of a folder's
     * package.json is left unresolved with its literal, and a {@code catalog:} peer there is an optional peer of
     * {@code *}; before, each resolves against the root's catalogs.
     */
    boolean stripsCatalogReferencesOutsideTheWorkspaces() {
        return atLeast(1, 4, 0);
    }

    /**
     * A {@code workspace:} spec names a workspace path only in the root's and a workspace's package.json
     * ({@code parse_dependency}), from 1.4.0 (bun#37669); before, a folder's package.json registers one too.
     */
    boolean registersWorkspaceSpecsOfTheWorkspacesOnly() {
        return atLeast(1, 4, 0);
    }

    /**
     * The text lockfile's parser looks an optional peer of a package below a bundled one up no higher than the bundled
     * package's parent ({@code find_resolution_bounded_at_bundle}), as the hoist does, from 1.4.0 (bun#37350); before,
     * up to the root.
     */
    boolean boundsTheOptionalPeersOfABundle() {
        return atLeast(1, 4, 0);
    }

    /**
     * The text lockfile's parser resolves the root's and the workspaces' dependencies of a lock with workspaces but no
     * {@code "packages"} as against an empty one, from 1.4.0 (bun#38765); before, it leaves them without resolutions.
     */
    boolean resolvesALockWithoutPackages() {
        return atLeast(1, 4, 0);
    }

    /**
     * The hoist dedupes an edge onto another edge of the same package to its name, in whichever tree it finds it
     * ({@code input_dep_range.contains}), and a dependency loop is no error, from 1.4.0 (bun#36303); before, it dedupes
     * only a dev edge onto one that is not, or the reverse, in the package's own tree, nests the edge below the other
     * it finds higher up, and fails on a dependency loop.
     */
    boolean dedupesAPackagesEdgesToOneName() {
        return atLeast(1, 4, 0);
    }

    /**
     * The hoist dedupes a peer by its catalog entry's range ({@code CatalogMap::resolve_range}), from 1.4.0
     * (bun#38333); before, no version satisfies a {@code catalog:} peer, which nests.
     */
    boolean dedupesACatalogPeerByItsRange() {
        return atLeast(1, 4, 0);
    }

    /**
     * The hoist rebinds an optional peer to the package it dedupes onto ({@code Rebind}), and hoists again while a pass
     * bound one late ({@code late_bound_optional_peer}), from 1.4.0 (bun#37426); before, an optional peer keeps its
     * binding, and one hoist is all.
     */
    boolean rebindsTheOptionalPeersItDedupes() {
        return atLeast(1, 4, 0);
    }

    /**
     * The clean keeps a package that only optional peers resolve to only where the install changes no resolution
     * ({@code keep_optional_peer_targets}: frozen, or no add, remove, update, override or catalog change), binding
     * those peers after the clone to what survived it, from 1.4.0 (bun#35681, bun#37426, bun#38333, bun#38853);
     * before, it keeps it whatever changed.
     */
    boolean dropsWhatOnlyOptionalPeersHold() {
        return atLeast(1, 4, 0);
    }

    /**
     * The root's diff keeps an npm dependency whose literal changed resolved to its locked version while that still
     * satisfies the new range (npm's sticky rule), counting it updated, from 1.4.0 (bun#30412); before, it is resolved
     * again.
     */
    boolean keepsALockedVersionTheNewRangeAccepts() {
        return atLeast(1, 4, 0);
    }

    /**
     * The root's diff keeps a workspace's resolution unless the workspace's own diff changes resolutions (an add,
     * remove, update, override or catalog change), from 1.4.0 (bun#38333); before, unless it has any diff (trusted or
     * patched dependencies too).
     */
    boolean keepsAWorkspaceWhoseDiffChangesNoResolution() {
        return atLeast(1, 4, 0);
    }

    /**
     * The root's diff fails where the root or a workspace on disk depends by {@code workspace:} on a workspace that
     * bun.lock lists but whose package.json is gone, and a frozen install does not count such a workspace removed
     * ({@code pruned_workspaces}), from 1.4.0 (bun#38333); before, the workspace is a removal like any other.
     */
    boolean failsOnAWorkspaceMissingFromDisk() {
        return atLeast(1, 4, 0);
    }

    /**
     * After it binds the edges, the text lockfile's parser makes an edge resolved to a workspace a workspace link only
     * where a reparse of its package.json would: a {@code workspace:} edge, or an npm range that links to the workspace
     * ({@code tag_workspace_links}), from 1.4.1 (bun#41302); before, every edge it binds to a workspace, but the second
     * of a root's or a workspace's edges to one name, becomes one.
     */
    boolean tagsWorkspaceLinksAsAReparseDoes() {
        return atLeast(1, 4, 1);
    }

    /**
     * An npm range of {@code *} links to the workspace of its name even when that workspace has no version, and a range
     * that links to none replaces the workspace of its name only when it has one ({@code linked_workspace_path}), from
     * 1.4.1 (bun#41302); before, only a range the workspace's version satisfies links.
     */
    boolean linksAStarRangeToAWorkspaceWithoutVersion() {
        return atLeast(1, 4, 1);
    }

    /**
     * The root's diff counts a workspace whose version changed, so that bun.lock is written again for it
     * ({@code workspace_versions_changed}), from 1.4.1 (bun#41302); before, a version change alone is no diff.
     */
    boolean diffsWorkspaceVersions() {
        return atLeast(1, 4, 1);
    }

    /**
     * Only the root's package.json is read for {@code patchedDependencies}, from 1.4.1 (bun#39816); before, a
     * workspace's and a folder's too.
     */
    boolean readsPatchedDependenciesFromTheRootOnly() {
        return atLeast(1, 4, 1);
    }

    /**
     * The root's {@code workspaces.selfContained} is an array of strings, or the package.json is invalid, from 1.4.1
     * (bun#40014); before, it is not read.
     */
    boolean readsSelfContainedWorkspaces() {
        return atLeast(1, 4, 1);
    }

    /**
     * bun.lock lists the dependencies of a group of the root and of each workspace by their names as they are when
     * written, so that a git or tarball dependency its resolution renamed moves to its name's place, from 1.4.1
     * (bun#40809); before, in the order they were parsed in.
     */
    boolean sortsWorkspaceDependenciesByName() {
        return atLeast(1, 4, 1);
    }

    /**
     * The text lockfile's parser reads a package's {@code "bundled": true} from the first object of its entry, from
     * 1.4.3 (bun#42884); before, only from its third element, so that a git, tarball or folder package is never
     * bundled.
     */
    boolean findsTheBundledMarkInAnyEntry() {
        return atLeast(1, 4, 3);
    }

    // 1.4.x: src/install/PackageManager (install_with_manager.rs, PackageManager.rs init), CatalogMap.rs, bunfig.rs

    /**
     * Two {@code catalog:} specs are the same dependency only when they name the same catalog, from 1.4.0
     * (dependency.rs {@code Version::eql}, bun#38333), so that a member whose {@code catalog:} becomes
     * {@code catalog:default} is diffed and written as package.json has it; before, any two are.
     */
    boolean comparesCatalogNames() {
        return atLeast(1, 4, 0);
    }

    /**
     * {@code catalog:} looks in {@code catalog}, then in {@code catalogs.default}, and {@code catalog:default} the other
     * way round, from 1.4.0 (CatalogMap.rs {@code locate}, bun#38333); before, each in its own only.
     */
    boolean looksUpTheDefaultCatalogUnderBothNames() {
        return atLeast(1, 4, 0);
    }

    /**
     * A name both {@code catalog} and {@code catalogs.default} hold fails the install, {@code "<name>" is defined in both
     * "catalog" and "catalogs.default"; keep one of them}, from 1.4.0 (CatalogMap.rs {@code parse_append},
     * bun#38333); before, each is read.
     */
    boolean rejectsAnEntryOfBothDefaultCatalogs() {
        return atLeast(1, 4, 0);
    }

    /**
     * A {@code catalog:} dependency that did not resolve fails as {@code <name>@<literal>: there is no catalog named
     * "<catalog>" in the root package.json}, {@code <name>@<literal> is not in the catalog} or {@code <name>@<literal>
     * is not in catalog "<catalog>"}, from 1.4.0 (PackageManagerResolution.rs {@code verify_resolutions}); before, as
     * any other dependency, {@code <name>@<literal> failed to resolve}.
     */
    boolean namesTheCatalogADependencyIsMissingFrom() {
        return atLeast(1, 4, 0);
    }

    /**
     * A frozen {@code --lockfile-only} install writes nothing, the lock being as it was, unless it migrates another
     * lockfile, from 1.4.0 (install_with_manager.rs {@code save_lockfile_only}); before, it saves the lock, which may
     * then hold the package.json's literals.
     */
    boolean writesNothingForAFrozenLockfileOnlyInstall() {
        return atLeast(1, 4, 0);
    }

    /**
     * A frozen install whose root's overrides or catalogs changed fails, noting {@code <overrides|resolutions|the
     * catalog> in package.json changed since bun.lock was saved}, from 1.4.0 (install_with_manager.rs
     * {@code frozen_changed_section}); before, only when the placements differ.
     */
    boolean failsAFrozenInstallWhoseOverridesOrCatalogsChanged() {
        return atLeast(1, 4, 0);
    }

    /**
     * The frozen install's comparison takes every placement, one of the lock before the clean bound to a package
     * appended after loading being a change, from 1.4.0 (lockfile.rs {@code eql} with {@code loaded_package_count});
     * before, both sides leave out the packages from the count before the install on.
     */
    boolean countsAPlacementBoundAfterLoadingAsAChange() {
        return atLeast(1, 4, 0);
    }

    /**
     * A frozen install keeps the workspaces bun.lock lists that the checkout lacks, and takes catalogs missing entries
     * the lock does not use, as a pruned checkout, noting what it skipped, from 1.4.0 (lockfile/pruned_workspaces.rs,
     * Package.rs {@code Diff}); before, either is a change. The engine does not reproduce it: such an install is
     * cannot-know.
     */
    boolean toleratesAPrunedCheckoutWhenFrozen() {
        return atLeast(1, 4, 0);
    }

    /**
     * What bunfig.toml's {@code [install]} sets wins over the .npmrc files, its scopes name by name, from 1.4.0
     * (PackageManager.rs {@code init}, {@code overlay_bunfig_install}: npmrc &lt; bunfig &lt; CLI); before, the .npmrc
     * files are read over bunfig's settings and win.
     */
    boolean letsBunfigOverrideNpmrc() {
        return atLeast(1, 4, 0);
    }

    /**
     * A registry's url is its WHATWG serialization, credentials split off (host lowercased, default port dropped, dot
     * segments resolved), from 1.4.0 (npm.rs {@code Scope::set_url}, bun#38183); before, the url as written. It is
     * what a {@code ""} registry in bun.lock expands to, and what the default registry's {@code ""} is told by.
     */
    boolean canonicalizesTheRegistryUrl() {
        return atLeast(1, 4, 0);
    }

    /**
     * bunfig.toml's {@code install.offline}, and {@code install.prefer = "offline"}, have the install read versions from
     * bun's manifest cache, from 1.4.1 (bunfig.rs, PackageManagerOptions.rs, bun#40010); before, neither reaches the
     * install. What that cache holds is the machine's: such an install is cannot-know.
     */
    boolean readsTheManifestCacheOffline() {
        return atLeast(1, 4, 1);
    }

    @Override
    public int compareTo(BunRelease o) {
        int c = Integer.compare(major, o.major);
        if (c == 0) {
            c = Integer.compare(minor, o.minor);
        }
        return c != 0 ? c : Integer.compare(patch, o.patch);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BunRelease && compareTo((BunRelease) o) == 0;
    }

    @Override
    public int hashCode() {
        return (major * 1000 + minor) * 1000 + patch;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
