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

import org.jspecify.annotations.Nullable;

/**
 * Port of bun's src/install/dependency.zig (bun 1.3.10), with dependency.rs (1.4.x) beside it: an edge by the name it
 * is installed under, with its spec and behavior; its resolution is at the same index of the lockfile's resolution
 * buffer.
 */
final class Dependency {

    long nameHash;
    String name;
    DependencyVersion version;
    Behavior behavior;

    Dependency(long nameHash, String name, DependencyVersion version, Behavior behavior) {
        this.nameHash = nameHash;
        this.name = name;
        this.version = version;
        this.behavior = behavior;
    }

    Dependency(String name, DependencyVersion version, Behavior behavior) {
        this(Wyhash11.stringHash(name), name, version, behavior);
    }

    Dependency copy() {
        return new Dependency(nameHash, name, version, behavior);
    }

    /**
     * Sorting order for dependencies: behavior ({@link Behavior#cmp}), then name.
     */
    static int compare(Dependency lhs, Dependency rhs) {
        return compare(lhs, rhs, BunRelease.PORTED);
    }

    /**
     * {@code isLessThan} in the release's order of behaviors ({@link Behavior#cmp(Behavior, BunRelease)}), then by
     * name.
     */
    static int compare(Dependency lhs, Dependency rhs, BunRelease release) {
        int behavior = lhs.behavior.cmp(rhs.behavior, release);
        if (behavior != 0) {
            return behavior;
        }
        return Strings.order(lhs.name, rhs.name);
    }

    /**
     * The clone parses the spec again with the tag it had: a dependency that was linked to a workspace parses its
     * literal as a workspace spec. From 1.4.1 a workspace dependency keeps its path
     * ({@link BunRelease#keepsAWorkspaceDependencysPathWhenCloned()}).
     */
    Dependency cloneDependency(BunRelease release) {
        if (version.tag == DependencyVersion.Tag.WORKSPACE && release.keepsAWorkspaceDependencysPathWhenCloned()) {
            return new Dependency(nameHash, name, version.asWorkspace(version.string()), behavior);
        }
        DependencyVersion parsed = DependencyVersion.parseWithTag(name, Wyhash11.stringHash(name), version.literal,
                version.tag, version.literal, release);
        return new Dependency(nameHash, name, parsed == null ? DependencyVersion.ZEROED : parsed, behavior);
    }

    String realname() {
        switch (version.tag) {
            case DIST_TAG:
                return version.distTag().name;
            case GIT:
            case GITHUB:
                return version.repository().packageName;
            case NPM:
                return version.npm().name;
            case TARBALL:
                return version.tarball().packageName;
            default:
                return name;
        }
    }

    boolean eql(Dependency b, BunRelease release) {
        return nameHash == b.nameHash && name.length() == b.name.length() && version.eql(b.version, release);
    }

    static boolean isSCPLikePath(String dependency) {
        if (dependency.length() < 3) {
            return false;
        }
        int atIndex = -1;
        for (int i = 0; i < dependency.length(); i++) {
            char c = dependency.charAt(i);
            if (c == '@') {
                if (atIndex < 0) {
                    atIndex = i;
                }
            } else if (c == ':') {
                if (dependency.startsWith("://", i)) {
                    return false;
                }
                return i > (atIndex >= 0 ? atIndex + 1 : 0);
            } else if (c == '/') {
                return atIndex >= 0 && i > atIndex + 1;
            }
        }
        return false;
    }

    /**
     * {@code isGitHubRepoPath}, which tells a GitHub shorthand until 1.3.2: {@code user/repo} or
     * {@code user/repo#ref} of letters, digits and {@code -}, with {@code .} and {@code _} after the first slash and
     * one {@code #} after it, ending in neither.
     */
    static boolean isGitHubRepoPath(String dependency) {
        if (dependency.length() < 3) {
            return false;
        }
        int hashIndex = 0;
        int firstSlashIndex = 0;
        for (int i = 0; i < dependency.length(); i++) {
            char c = dependency.charAt(i);
            switch (c) {
                case '/':
                    if (i == 0) {
                        return false;
                    }
                    if (firstSlashIndex == 0) {
                        firstSlashIndex = i;
                    }
                    break;
                case '#':
                    if (i == 0 || hashIndex > 0 || firstSlashIndex == 0) {
                        return false;
                    }
                    hashIndex = i;
                    break;
                case '.':
                case '_':
                    if (firstSlashIndex == 0) {
                        return false;
                    }
                    break;
                default:
                    if (!(c == '-' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9')) {
                        return false;
                    }
                    break;
            }
        }
        return hashIndex != dependency.length() - 1 && firstSlashIndex > 0 &&
               firstSlashIndex != dependency.length() - 1;
    }

    static boolean isGitHubTarballPath(String dependency, BunRelease release) {
        if (isTarball(dependency, release)) {
            return true;
        }
        String[] parts = dependency.split("/", -1);
        return parts.length >= 3 && "tarball".equals(parts[2]);
    }

    static boolean isTarball(String dependency) {
        return isTarball(dependency, BunRelease.PORTED);
    }

    /**
     * {@code isTarball}: {@code .tgz} or {@code .tar.gz} as written; from 1.4.0 {@code .tar} too, in any case
     * ({@link BunRelease#tellsTarballsByTheirSuffixInAnyCase()}).
     */
    static boolean isTarball(String dependency, BunRelease release) {
        if (release.tellsTarballsByTheirSuffixInAnyCase()) {
            return hasSuffixIgnoringAsciiCase(dependency, ".tgz") ||
                   hasSuffixIgnoringAsciiCase(dependency, ".tar.gz") ||
                   hasSuffixIgnoringAsciiCase(dependency, ".tar");
        }
        return dependency.endsWith(".tgz") || dependency.endsWith(".tar.gz");
    }

    // has_suffix_ignore_ascii_case, the suffix in lower case
    private static boolean hasSuffixIgnoringAsciiCase(String s, String suffix) {
        int offset = s.length() - suffix.length();
        if (offset < 0) {
            return false;
        }
        for (int i = 0; i < suffix.length(); i++) {
            char c = s.charAt(offset + i);
            char lower = c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c;
            if (lower != suffix.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@code is_safe_install_folder_name} (src/install/dependency.rs, from 1.4.0): a name that becomes a directory under
     * {@code node_modules/} stays inside it, each {@code /} component neither empty nor {@code .} or {@code ..}, and
     * without {@code \}, {@code :} or NUL.
     */
    static boolean isSafeInstallFolderName(String name) {
        if (name.isEmpty()) {
            return false;
        }
        for (String component : name.split("/", -1)) {
            if (component.isEmpty() || ".".equals(component) || "..".equals(component)) {
                return false;
            }
            for (int i = 0; i < component.length(); i++) {
                char c = component.charAt(i);
                if (c == '\\' || c == ':' || c == '\0') {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean isRemoteTarball(String dependency) {
        return dependency.startsWith("https://") || dependency.startsWith("http://");
    }

    /**
     * {@code foo@1.1.1} is {@code foo}, {@code 1.1.1}; {@code @foo/bar@1.1.1} is {@code @foo/bar}, {@code 1.1.1};
     * {@code foo} is {@code foo} and no version.
     */
    static String[] splitNameAndMaybeVersion(String str) {
        int atIndex = str.indexOf('@');
        if (atIndex >= 0) {
            if (atIndex != 0) {
                String version = atIndex + 1 < str.length() ? str.substring(atIndex + 1) : null;
                return new String[]{str.substring(0, atIndex), version};
            }
            int second = str.indexOf('@', 1);
            if (second < 0) {
                return new String[]{str, null};
            }
            return new String[]{str.substring(0, second), second + 1 < str.length() ? str.substring(second + 1) : null};
        }
        return new String[]{str, null};
    }

    static String @Nullable [] splitNameAndVersion(String str) {
        String[] split = splitNameAndMaybeVersion(str);
        return split[1] == null ? null : split;
    }

    @Override
    public String toString() {
        return name + "@" + version.literal;
    }
}
