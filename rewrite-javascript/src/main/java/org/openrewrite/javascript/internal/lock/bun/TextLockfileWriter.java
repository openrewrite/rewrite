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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.bun.Package.INVALID_PACKAGE_ID;

/**
 * Port of bun's src/install/lockfile/bun.lock.zig {@code Stringifier} (bun 1.3.10) for a lockfile loaded from text,
 * as the release that installs writes it ({@link BunRelease}).
 */
final class TextLockfileWriter {

    private static final String[] GROUP_NAMES = {"dependencies", "devDependencies", "optionalDependencies",
            "peerDependencies"};
    private static final Behavior[] GROUP_BEHAVIORS = {Behavior.of(Behavior.PROD), Behavior.of(Behavior.DEV),
            Behavior.of(Behavior.OPTIONAL), Behavior.of(Behavior.PEER)};
    private static final String DEFAULT_REGISTRY = Strings.withoutTrailingSlash(BunOptions.DEFAULT_REGISTRY);

    private TextLockfileWriter() {
    }

    static String saveFromBinary(Lockfile lockfile, int configVersion, BunRelease release) {
        StringBuilder writer = new StringBuilder();
        List<Package> pkgs = lockfile.packages;
        List<Dependency> depsBuf = lockfile.dependencies;
        List<Integer> resolutionBuf = lockfile.resolutions;

        TrustedDependencies<String> foundTrustedDependencies = new TrustedDependencies<>();
        if (lockfile.trustedDependencies != null) {
            foundTrustedDependencies.ensureTotalCapacity(lockfile.trustedDependencies.size());
        }
        TrustedDependencies<String[]> foundPatchedDependencies = new TrustedDependencies<>();
        foundPatchedDependencies.ensureTotalCapacity(lockfile.patchedDependencies.size());
        // before 1.2.8 the overrides a dependency of the tree is named for, by the name's hash
        boolean writesEveryOverride = release.writesEveryOverride();
        TrustedDependencies<String[]> foundOverrides = new TrustedDependencies<>();
        if (!writesEveryOverride) {
            foundOverrides.ensureTotalCapacity(lockfile.overrides.map.size());
        }

        Tree.Iterator pkgsIter = new Tree.Iterator(lockfile);
        int[] indent = {0};
        writer.append("{\n");
        incIndent(writer, indent);
        writer.append("\"lockfileVersion\": ").append(release.keepsTheLockfileVersionItRead() ?
                versionToWrite(lockfile) : Lockfile.TEXT_LOCKFILE_VERSION_CURRENT).append(",\n");
        writeIndent(writer, indent);
        if (release.writesConfigVersion()) {
            writer.append("\"configVersion\": ").append(configVersion).append(",\n");
            writeIndent(writer, indent);
        }
        writer.append("\"workspaces\": {\n");
        incIndent(writer, indent);
        writeWorkspaceDeps(writer, indent, lockfile, 0, "", release);

        List<Integer> workspaceSortBuf = new ArrayList<>();
        for (int pkgId = 0; pkgId < pkgs.size(); pkgId++) {
            if (pkgs.get(pkgId).resolution.tag == Resolution.Tag.WORKSPACE) {
                workspaceSortBuf.add(pkgId);
            }
        }
        workspaceSortBuf.sort((l, r) -> Strings.order(pkgs.get(l).resolution.value, pkgs.get(r).resolution.value));
        for (int workspacePkgId : workspaceSortBuf) {
            writer.append('\n');
            writeIndent(writer, indent);
            writeWorkspaceDeps(writer, indent, lockfile, workspacePkgId, pkgs.get(workspacePkgId).resolution.value,
                    release);
        }
        writer.append('\n');
        decIndent(writer, indent);
        writer.append("},\n");

        List<Tree.Next> treeSortBuf = new ArrayList<>();
        Tree.Next node;
        while ((node = pkgsIter.next()) != null) {
            treeSortBuf.add(node);
            for (int depId : node.dependencies) {
                int pkgId = resolutionBuf.get(depId);
                if (pkgId == INVALID_PACKAGE_ID) {
                    continue;
                }
                Package pkg = pkgs.get(pkgId);
                Dependency dep = depsBuf.get(depId);
                if (!lockfile.patchedDependencies.isEmpty()) {
                    String nameAndVersion = pkg.name + "@";
                    if (pkg.resolution.tag == Resolution.Tag.WORKSPACE) {
                        SemverVersion workspaceVersion = lockfile.workspaceVersions.get(pkg.nameHash);
                        if (workspaceVersion != null) {
                            nameAndVersion += workspaceVersion;
                        }
                    } else {
                        nameAndVersion += pkg.resolution;
                    }
                    long nameAndVersionHash = Wyhash11.stringHash(nameAndVersion);
                    String patch = lockfile.patchedDependencies.get(nameAndVersionHash);
                    if (patch != null) {
                        foundPatchedDependencies.put(nameAndVersionHash, new String[]{nameAndVersion, patch});
                    }
                }
                if (lockfile.trustedDependencies != null && lockfile.trustedDependencies.contains((int) dep.nameHash)) {
                    foundTrustedDependencies.put(dep.nameHash, dep.name);
                }
                if (!writesEveryOverride && !lockfile.overrides.map.isEmpty()) {
                    Dependency override = lockfile.overrides.map.get(dep.nameHash);
                    if (override != null) {
                        foundOverrides.put(dep.nameHash, new String[]{dep.name, override.version.literal});
                    }
                }
            }
        }
        pkgsIter.reset();

        treeSortBuf.sort((l, r) -> l.depth != r.depth ? Integer.compare(l.depth, r.depth) :
                Strings.order(l.relativePath, r.relativePath));

        if (foundTrustedDependencies.count() > 0) {
            writeIndent(writer, indent);
            writer.append("\"trustedDependencies\": [\n");
            indent[0]++;
            for (String depName : foundTrustedDependencies.values()) {
                writeIndent(writer, indent);
                writer.append('"').append(depName).append("\",\n");
            }
            decIndent(writer, indent);
            writer.append("],\n");
        }

        if (foundPatchedDependencies.count() > 0) {
            writeIndent(writer, indent);
            writer.append("\"patchedDependencies\": {\n");
            indent[0]++;
            for (String[] value : foundPatchedDependencies.values()) {
                writeIndent(writer, indent);
                appendOverrideOrPatchString(writer, value[0], release);
                writer.append(": ");
                appendOverrideOrPatchString(writer, value[1], release);
                writer.append(",\n");
            }
            decIndent(writer, indent);
            writer.append("},\n");
        }

        if (!writesEveryOverride) {
            if (foundOverrides.count() > 0) {
                writeIndent(writer, indent);
                writer.append("\"overrides\": {\n");
                indent[0]++;
                for (String[] value : foundOverrides.values()) {
                    writeOverrideRule(writer, indent[0], value[0], "", value[1], release);
                }
                decIndent(writer, indent);
                writer.append("},\n");
            }
        } else if (!lockfile.overrides.isEmpty()) {
            if (release.sortsOverridesInBunLock()) {
                lockfile.overrides.sort();
            }
            writeIndent(writer, indent);
            writer.append("\"overrides\": {\n");
            indent[0]++;
            if (!lockfile.overrides.hasScoped()) {
                for (Dependency overrideDep : lockfile.overrides.map.values()) {
                    writeOverrideRule(writer, indent[0], overrideDep.name, "", overrideDep.version.literal, release);
                }
            } else {
                // from 1.4.0
                writeOverrideRules(writer, indent[0], lockfile.overrides, release);
            }
            decIndent(writer, indent);
            writer.append("},\n");
        }

        if (lockfile.catalogs.hasAny()) {
            lockfile.catalogs.sort();
        }
        if (!lockfile.catalogs.defaultMap.isEmpty()) {
            writeIndent(writer, indent);
            writer.append("\"catalog\": {\n");
            indent[0]++;
            for (Dependency catalogDep : lockfile.catalogs.defaultMap.values()) {
                writeIndent(writer, indent);
                Strings.appendJsonString(writer, catalogDep.name, true, release);
                writer.append(": ");
                Strings.appendJsonString(writer, catalogDep.version.literal, true, release);
                writer.append(",\n");
            }
            decIndent(writer, indent);
            writer.append("},\n");
        }
        if (!lockfile.catalogs.groups.isEmpty()) {
            writeIndent(writer, indent);
            writer.append("\"catalogs\": {\n");
            indent[0]++;
            for (Map.Entry<String, LinkedHashMap<String, Dependency>> entry : lockfile.catalogs.groups.entrySet()) {
                writeIndent(writer, indent);
                Strings.appendJsonString(writer, entry.getKey(), true, release);
                writer.append(": {\n");
                indent[0]++;
                for (Dependency catalogDep : entry.getValue().values()) {
                    writeIndent(writer, indent);
                    Strings.appendJsonString(writer, catalogDep.name, true, release);
                    writer.append(": ");
                    Strings.appendJsonString(writer, catalogDep.version.literal, true, release);
                    writer.append(",\n");
                }
                decIndent(writer, indent);
                writer.append("},\n");
            }
            decIndent(writer, indent);
            writer.append("},\n");
        }

        writeIndent(writer, indent);
        writer.append("\"packages\": {");
        boolean first = true;
        for (Tree.Next item : treeSortBuf) {
            List<Integer> treeDepsSortBuf = new ArrayList<>(item.dependencies);
            treeDepsSortBuf.sort((l, r) -> Strings.order(depsBuf.get(l).name, depsBuf.get(r).name));
            for (int depId : treeDepsSortBuf) {
                int pkgId = resolutionBuf.get(depId);
                if (pkgId == INVALID_PACKAGE_ID) {
                    continue;
                }
                Package pkg = pkgs.get(pkgId);
                Resolution res = pkg.resolution;
                switch (res.tag) {
                    case ROOT:
                    case NPM:
                    case FOLDER:
                    case LOCAL_TARBALL:
                    case GITHUB:
                    case GIT:
                    case SYMLINK:
                    case WORKSPACE:
                    case REMOTE_TARBALL:
                        break;
                    default:
                        continue;
                }
                if (first) {
                    first = false;
                    writer.append('\n');
                    incIndent(writer, indent);
                } else {
                    writer.append(",\n\n");
                    writeIndent(writer, indent);
                }
                writer.append('"');
                Strings.appendJsonString(writer, item.relativePath, false, release);
                if (item.depth != 0) {
                    writer.append('/');
                }
                Dependency dep = depsBuf.get(depId);
                Strings.appendJsonString(writer, dep.name, false, release);
                writer.append("\": ");

                List<Integer> pkgDepsSortBuf = new ArrayList<>(pkg.dependenciesLen);
                for (int pkgDepId = pkg.dependenciesOff; pkgDepId < pkg.dependenciesEnd(); pkgDepId++) {
                    pkgDepsSortBuf.add(pkgDepId);
                }
                pkgDepsSortBuf.sort((l, r) -> Strings.order(depsBuf.get(l).name, depsBuf.get(r).name));

                switch (res.tag) {
                    case ROOT:
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false, release);
                        writer.append("@root:\", ");
                        writer.append('{');
                        if (pkg.bin.tag != Bin.Tag.NONE) {
                            writer.append(pkg.bin.tag == Bin.Tag.DIR ? " \"binDir\": " : " \"bin\": ");
                            pkg.bin.toJsonSingleLine(writer, release);
                            writer.append(" }]");
                        } else {
                            writer.append("}]");
                        }
                        break;
                    case FOLDER:
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false, release);
                        writer.append("@file:");
                        Strings.appendJsonString(writer, res.value, false, release);
                        writer.append("\", ");
                        writePackageInfoObject(writer, dep.behavior, depsBuf, pkgDepsSortBuf, pkg, release);
                        writer.append(']');
                        break;
                    case LOCAL_TARBALL:
                    case REMOTE_TARBALL:
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false, release);
                        writer.append('@');
                        Strings.appendJsonString(writer, res.value, false, release);
                        writer.append("\", ");
                        writePackageInfoObject(writer, dep.behavior, depsBuf, pkgDepsSortBuf, pkg, release);
                        if (release.carriesTarballIntegrity() && pkg.meta.integrity.tag.isSupported()) {
                            writer.append(", \"").append(pkg.meta.integrity).append("\"]");
                        } else {
                            writer.append(']');
                        }
                        break;
                    case SYMLINK:
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false, release);
                        writer.append("@link:");
                        Strings.appendJsonString(writer, res.value, false, release);
                        writer.append("\", ");
                        writePackageInfoObject(writer, dep.behavior, depsBuf, pkgDepsSortBuf, pkg, release);
                        writer.append(']');
                        break;
                    case NPM:
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false, release);
                        writer.append('@').append(res.version).append("\", ");
                        boolean underDefault = release.matchesTheDefaultRegistryAtAPathBoundary() ?
                                TextLockfileReader.urlIsUnderRegistry(res.url, BunOptions.DEFAULT_REGISTRY) :
                                res.url.startsWith(DEFAULT_REGISTRY);
                        writer.append('"');
                        if (release.escapesTheRegistryAndRepositoryInBunLock()) {
                            Strings.appendJsonString(writer, underDefault ? "" : res.url, false, release);
                        } else {
                            writer.append(underDefault ? "" : res.url);
                        }
                        writer.append("\", ");
                        writePackageInfoObject(writer, dep.behavior, depsBuf, pkgDepsSortBuf, pkg, release);
                        writer.append(", \"").append(pkg.meta.integrity).append("\"]");
                        break;
                    case WORKSPACE:
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false, release);
                        writer.append("@workspace:");
                        Strings.appendJsonString(writer, res.value, false, release);
                        writer.append("\"]");
                        break;
                    default: {
                        Repository repo = res.repository;
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false, release);
                        String formatted = repo.format(res.tag == Resolution.Tag.GIT ? "git+" : "github:");
                        writer.append('@');
                        if (release.escapesTheRegistryAndRepositoryInBunLock()) {
                            Strings.appendJsonString(writer, formatted, false, release);
                        } else {
                            writer.append(formatted);
                        }
                        writer.append("\", ");
                        writePackageInfoObject(writer, dep.behavior, depsBuf, pkgDepsSortBuf, pkg, release);
                        writer.append(", ");
                        Strings.appendJsonString(writer, repo.resolved, true, release);
                        if (release.carriesTarballIntegrity() && pkg.meta.integrity.tag.isSupported()) {
                            writer.append(", \"").append(pkg.meta.integrity).append("\"]");
                        } else {
                            writer.append(']');
                        }
                        break;
                    }
                }
            }
        }
        if (!first) {
            writer.append(",\n");
            decIndent(writer, indent);
        }
        writer.append("}\n");
        decIndent(writer, indent);
        writer.append("}\n");
        return writer.toString();
    }

    private static void writePackageInfoObject(StringBuilder writer, Behavior depBehavior, List<Dependency> depsBuf,
                                               List<Integer> pkgDepIds, Package pkg, BunRelease release) {
        List<String> optionalPeersBuf = new ArrayList<>();
        writer.append('{');
        boolean any = false;
        for (int g = 0; g < GROUP_NAMES.length; g++) {
            Behavior groupBehavior = GROUP_BEHAVIORS[g];
            boolean first = true;
            for (int depId : pkgDepIds) {
                Dependency dep = depsBuf.get(depId);
                if (!dep.behavior.includes(groupBehavior)) {
                    continue;
                }
                if (dep.behavior.isOptionalPeer()) {
                    if (groupBehavior.isOptional()) {
                        continue;
                    }
                    optionalPeersBuf.add(dep.name);
                }
                if (first) {
                    if (any) {
                        writer.append(',');
                    }
                    writer.append(" \"").append(GROUP_NAMES[g]).append("\": { ");
                    first = false;
                    any = true;
                } else {
                    writer.append(", ");
                }
                Strings.appendJsonString(writer, dep.name, true, release);
                writer.append(": ");
                Strings.appendJsonString(writer, dep.version.literal, true, release);
            }
            if (!first) {
                writer.append(" }");
            }
        }

        if (!optionalPeersBuf.isEmpty()) {
            writer.append(", \"optionalPeers\": [");
            for (int i = 0; i < optionalPeersBuf.size(); i++) {
                if (i != 0) {
                    writer.append(' ');
                }
                Strings.appendJsonString(writer, optionalPeersBuf.get(i), true, release);
                if (i != optionalPeersBuf.size() - 1) {
                    writer.append(',');
                }
            }
            writer.append(']');
        }

        if (depBehavior.isBundled()) {
            if (any) {
                writer.append(',');
            } else {
                any = true;
            }
            writer.append(" \"bundled\": true");
        }

        if (pkg.meta.os != Negatable.OPERATING_SYSTEM.allValue) {
            if (any) {
                writer.append(',');
            } else {
                any = true;
            }
            writer.append(" \"os\": ");
            Negatable.toJson(Negatable.OPERATING_SYSTEM, pkg.meta.os, writer);
        }

        if (pkg.meta.arch != Negatable.ARCHITECTURE.allValue) {
            if (any) {
                writer.append(',');
            } else {
                any = true;
            }
            writer.append(" \"cpu\": ");
            Negatable.toJson(Negatable.ARCHITECTURE, pkg.meta.arch, writer);
        }

        if (pkg.bin.tag != Bin.Tag.NONE) {
            if (any) {
                writer.append(',');
            } else {
                any = true;
            }
            writer.append(pkg.bin.tag == Bin.Tag.DIR ? " \"binDir\": " : " \"bin\": ");
            pkg.bin.toJsonSingleLine(writer, release);
        }

        writer.append(any ? " }" : "}");
    }

    private static void writeWorkspaceDeps(StringBuilder writer, int[] indent, Lockfile lockfile, int pkgId,
                                           String res, BunRelease release) {
        List<String> optionalPeersBuf = new ArrayList<>();
        boolean any = false;
        Package pkg = lockfile.packages.get(pkgId);
        if (pkgId == 0) {
            writer.append("\"\": {");
            String rootName = pkg.name;
            if (!rootName.isEmpty()) {
                writer.append('\n');
                incIndent(writer, indent);
                writer.append("\"name\": ");
                Strings.appendJsonString(writer, rootName, true, release);
                any = true;
            }
        } else {
            Strings.appendJsonString(writer, res, true, release);
            writer.append(": {");
            writer.append('\n');
            incIndent(writer, indent);
            writer.append("\"name\": ");
            Strings.appendJsonString(writer, pkg.name, true, release);
            SemverVersion version = lockfile.workspaceVersions.get(pkg.nameHash);
            if (version != null) {
                writer.append(",\n");
                writeIndent(writer, indent);
                writer.append("\"version\": \"").append(version).append('"');
            }
            if (pkg.bin.tag != Bin.Tag.NONE) {
                writer.append(",\n");
                writeIndent(writer, indent);
                writer.append(pkg.bin.tag == Bin.Tag.DIR ? "\"binDir\": " : "\"bin\": ");
                pkg.bin.toJsonMultiLine(writer, indent, release);
            }
            any = true;
        }

        List<Dependency> deps = new ArrayList<>(lockfile.dependenciesOf(pkg));
        if (lockfile.release.sortsWorkspaceDependenciesByName()) {
            // a git or tarball dependency without an alias sorts under its literal when parsed, and its resolution
            // renames it without sorting it again
            deps.sort((l, r) -> Strings.order(l.name, r.name));
        }
        for (int g = 0; g < GROUP_NAMES.length; g++) {
            Behavior groupBehavior = GROUP_BEHAVIORS[g];
            boolean first = true;
            for (Dependency dep : deps) {
                if (!dep.behavior.includes(groupBehavior)) {
                    continue;
                }
                if (dep.behavior.isOptionalPeer()) {
                    if (groupBehavior.isOptional()) {
                        continue;
                    }
                    optionalPeersBuf.add(dep.name);
                }
                if (first) {
                    if (any) {
                        writer.append(',');
                    }
                    writer.append('\n');
                    if (any) {
                        writeIndent(writer, indent);
                    } else {
                        incIndent(writer, indent);
                    }
                    writer.append('"').append(GROUP_NAMES[g]).append("\": {\n");
                    incIndent(writer, indent);
                    any = true;
                    first = false;
                } else {
                    writer.append(",\n");
                    writeIndent(writer, indent);
                }
                Strings.appendJsonString(writer, dep.name, true, release);
                writer.append(": ");
                Strings.appendJsonString(writer, dep.version.literal, true, release);
            }
            if (!first) {
                writer.append(",\n");
                decIndent(writer, indent);
                writer.append('}');
            }
        }
        if (!optionalPeersBuf.isEmpty()) {
            writer.append(",\n");
            writeIndent(writer, indent);
            writer.append("\"optionalPeers\": [\n");
            indent[0]++;
            for (String optionalPeer : optionalPeersBuf) {
                writeIndent(writer, indent);
                Strings.appendJsonString(writer, optionalPeer, true, release);
                writer.append(",\n");
            }
            decIndent(writer, indent);
            writer.append(']');
        }
        if (any) {
            writer.append(",\n");
            decIndent(writer, indent);
        }
        writer.append("},");
    }

    // override_selector_key: name when range is empty, else name@range
    private static String overrideSelectorKey(String name, String range) {
        return range.isEmpty() ? name : name + "@" + range;
    }

    private static void writeOverrideRule(StringBuilder writer, int indent, String name, String range, String value,
                                          BunRelease release) {
        writeIndent(writer, new int[]{indent});
        appendOverrideOrPatchString(writer, overrideSelectorKey(name, range), release);
        writer.append(": ");
        appendOverrideOrPatchString(writer, value, release);
        writer.append(",\n");
    }

    // a key or value of the overrides or the patched dependencies: before 1.2.3 quoted as it is
    private static void appendOverrideOrPatchString(StringBuilder writer, String s, BunRelease release) {
        if (release.escapesOverridesAndPatchesInBunLock()) {
            Strings.appendJsonString(writer, s, true, release);
        } else {
            writer.append('"').append(s).append('"');
        }
    }

    // a group of write_override_rules: a parent's, or a ranged rule's without a parent, both keyed name[@range]
    private static final class GroupKey {
        final long nameHash;
        final String name;
        final String literal;

        GroupKey(long nameHash, String name, String literal) {
            this.nameHash = nameHash;
            this.name = name;
            this.literal = literal;
        }

        int order(GroupKey rhs) {
            int c = Strings.order(name, rhs.name);
            return c != 0 ? c : Strings.order(literal, rhs.literal);
        }
    }

    /**
     * {@code write_override_rules} (from 1.4.0): the sorted plain rules, rules without a parent and parents' groups
     * merged by key; scoped rules live in selector-keyed objects, and a plain rule folds into its name's unranged
     * group as {@code "."}.
     */
    private static void writeOverrideRules(StringBuilder writer, int indent, OverrideMap overrides,
                                           BunRelease release) {
        List<Dependency> flat = new ArrayList<>(overrides.map.values());
        List<OverrideMap.ScopedOverride> scoped = overrides.scoped;
        int soloEnd = 0;
        while (soloEnd < scoped.size() && scoped.get(soloEnd).parent == null) {
            soloEnd++;
        }
        int f = 0;
        int s = 0;
        int g = soloEnd;
        while (f < flat.size() || s < soloEnd || g < scoped.size()) {
            GroupKey solo = s < soloEnd ? new GroupKey(scoped.get(s).dep.nameHash, scoped.get(s).dep.name,
                    scoped.get(s).targetRange.literal) : null;
            Dependency parent = g < scoped.size() ? scoped.get(g).parent : null;
            GroupKey parented = parent == null ? null : new GroupKey(parent.nameHash, parent.name,
                    parent.version.literal);
            GroupKey group = solo != null && parented != null ? (parented.order(solo) < 0 ? parented : solo) :
                    solo != null ? solo : parented;

            if (f < flat.size()) {
                Dependency flatDep = flat.get(f);
                boolean flatFirst;
                if (group == null) {
                    flatFirst = true;
                } else {
                    int c = Strings.order(flatDep.name, group.name);
                    flatFirst = c < 0 || c == 0 && !group.literal.isEmpty();
                }
                if (flatFirst) {
                    writeOverrideRule(writer, indent, flatDep.name, "", flatDep.version.literal, release);
                    f++;
                    continue;
                }
            }
            if (group == null) {
                break;
            }

            writeIndent(writer, new int[]{indent});
            Strings.appendJsonString(writer, overrideSelectorKey(group.name, group.literal), true, release);
            writer.append(": {\n");

            String dot = null;
            if (group.literal.isEmpty()) {
                if (f < flat.size() && flat.get(f).nameHash == group.nameHash) {
                    dot = flat.get(f).version.literal;
                    f++;
                }
            } else if (s < soloEnd && scoped.get(s).dep.nameHash == group.nameHash &&
                       scoped.get(s).targetRange.literal.equals(group.literal)) {
                dot = scoped.get(s).dep.version.literal;
                s++;
            }
            if (dot != null) {
                writeIndent(writer, new int[]{indent + 1});
                writer.append("\".\": ");
                Strings.appendJsonString(writer, dot, true, release);
                writer.append(",\n");
            }

            while (g < scoped.size()) {
                OverrideMap.ScopedOverride rule = scoped.get(g);
                if (rule.parent == null || rule.parent.nameHash != group.nameHash ||
                    !rule.parent.version.literal.equals(group.literal)) {
                    break;
                }
                writeOverrideRule(writer, indent + 1, rule.dep.name, rule.targetRange.literal,
                        rule.dep.version.literal, release);
                g++;
            }

            writeIndent(writer, new int[]{indent});
            writer.append("},\n");
        }
    }

    /**
     * {@code Stringifier::version_to_write} (from 1.4.0): a lock read keeps its version, 0 as 1, unless its overrides
     * hold scoped rules; otherwise, among the packages written, an npm package without a supported integrity whose
     * tarball is outside the default registry, or a git package whose tag is unsafe, holds it at 1; else it is 3 with
     * scoped rules and 2 without.
     */
    private static int versionToWrite(Lockfile lockfile) {
        int loaded = lockfile.textLockfileVersion;
        boolean hasScoped = lockfile.overrides.hasScoped();
        if (!hasScoped && loaded < Lockfile.TEXT_LOCKFILE_VERSION_SCOPED_OVERRIDES) {
            return Math.max(loaded, Lockfile.TEXT_LOCKFILE_VERSION_CURRENT);
        }
        Tree.Iterator iter = new Tree.Iterator(lockfile);
        Tree.Next node;
        while ((node = iter.next()) != null) {
            for (int depId : node.dependencies) {
                int pkgId = lockfile.resolutions.get(depId);
                if (pkgId == INVALID_PACKAGE_ID) {
                    continue;
                }
                Package pkg = lockfile.packages.get(pkgId);
                Resolution res = pkg.resolution;
                if (res.tag == Resolution.Tag.NPM) {
                    if (!pkg.meta.integrity.tag.isSupported() &&
                        !TextLockfileReader.urlIsUnderRegistry(res.url, BunOptions.DEFAULT_REGISTRY)) {
                        return Lockfile.TEXT_LOCKFILE_VERSION_CURRENT;
                    }
                } else if (res.tag == Resolution.Tag.GIT && !Repository.isSafeResolvedTag(res.repository.resolved)) {
                    return Lockfile.TEXT_LOCKFILE_VERSION_CURRENT;
                }
            }
        }
        return hasScoped ? Lockfile.TEXT_LOCKFILE_VERSION_SCOPED_OVERRIDES : Lockfile.TEXT_LOCKFILE_VERSION_STRICT;
    }

    static void writeIndent(StringBuilder writer, int[] indent) {
        for (int i = 0; i < indent[0]; i++) {
            writer.append("  ");
        }
    }

    private static void incIndent(StringBuilder writer, int[] indent) {
        indent[0]++;
        writeIndent(writer, indent);
    }

    private static void decIndent(StringBuilder writer, int[] indent) {
        indent[0]--;
        writeIndent(writer, indent);
    }
}
