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
 * Port of bun's src/install/dependency.zig (bun 1.3.10): an edge by the name it is installed under, with its spec
 * and behavior; its resolution is at the same index of the lockfile's resolution buffer.
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
        int behavior = lhs.behavior.cmp(rhs.behavior);
        if (behavior != 0) {
            return behavior;
        }
        return Strings.order(lhs.name, rhs.name);
    }

    /**
     * The clone parses the spec again with the tag it had: a dependency that was linked to a workspace parses its
     * literal as a workspace spec.
     */
    Dependency cloneDependency() {
        DependencyVersion parsed = DependencyVersion.parseWithTag(name, Wyhash11.stringHash(name), version.literal,
                version.tag, version.literal);
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

    boolean eql(Dependency b) {
        return nameHash == b.nameHash && name.length() == b.name.length() && version.eql(b.version);
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

    static boolean isGitHubTarballPath(String dependency) {
        if (isTarball(dependency)) {
            return true;
        }
        String[] parts = dependency.split("/", -1);
        return parts.length >= 3 && "tarball".equals(parts[2]);
    }

    static boolean isTarball(String dependency) {
        return dependency.endsWith(".tgz") || dependency.endsWith(".tar.gz");
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
