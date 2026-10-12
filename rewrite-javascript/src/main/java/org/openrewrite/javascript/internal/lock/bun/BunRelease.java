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

    // src/install/dependency.zig, lockfile/Package.zig, lockfile/bun.lock.zig, lockfile/Tree.zig, PackageManagerEnqueue.zig

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

    // src/install/PackageManager/install_with_manager.zig installWithManager

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
