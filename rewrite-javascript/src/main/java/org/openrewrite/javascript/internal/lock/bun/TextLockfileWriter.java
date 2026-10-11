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

/** Port of bun's src/install/lockfile/bun.lock.zig {@code Stringifier} (bun 1.3.10) for a lockfile loaded from text. */
final class TextLockfileWriter {

    private static final String[] GROUP_NAMES = {"dependencies", "devDependencies", "optionalDependencies",
            "peerDependencies"};
    private static final Behavior[] GROUP_BEHAVIORS = {Behavior.of(Behavior.PROD), Behavior.of(Behavior.DEV),
            Behavior.of(Behavior.OPTIONAL), Behavior.of(Behavior.PEER)};
    private static final String DEFAULT_REGISTRY = Strings.withoutTrailingSlash(BunOptions.DEFAULT_REGISTRY);

    private TextLockfileWriter() {
    }

    static String saveFromBinary(Lockfile lockfile, int configVersion) {
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

        Tree.Iterator pkgsIter = new Tree.Iterator(lockfile);
        int[] indent = {0};
        writer.append("{\n");
        incIndent(writer, indent);
        writer.append("\"lockfileVersion\": ").append(Lockfile.TEXT_LOCKFILE_VERSION_CURRENT).append(",\n");
        writeIndent(writer, indent);
        writer.append("\"configVersion\": ").append(configVersion).append(",\n");
        writeIndent(writer, indent);
        writer.append("\"workspaces\": {\n");
        incIndent(writer, indent);
        writeWorkspaceDeps(writer, indent, lockfile, 0, "");

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
            writeWorkspaceDeps(writer, indent, lockfile, workspacePkgId, pkgs.get(workspacePkgId).resolution.value);
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
                Strings.appendJsonString(writer, value[0], true);
                writer.append(": ");
                Strings.appendJsonString(writer, value[1], true);
                writer.append(",\n");
            }
            decIndent(writer, indent);
            writer.append("},\n");
        }

        if (!lockfile.overrides.map.isEmpty()) {
            lockfile.overrides.sort();
            writeIndent(writer, indent);
            writer.append("\"overrides\": {\n");
            indent[0]++;
            for (Dependency overrideDep : lockfile.overrides.map.values()) {
                writeIndent(writer, indent);
                Strings.appendJsonString(writer, overrideDep.name, true);
                writer.append(": ");
                Strings.appendJsonString(writer, overrideDep.version.literal, true);
                writer.append(",\n");
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
                Strings.appendJsonString(writer, catalogDep.name, true);
                writer.append(": ");
                Strings.appendJsonString(writer, catalogDep.version.literal, true);
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
                Strings.appendJsonString(writer, entry.getKey(), true);
                writer.append(": {\n");
                indent[0]++;
                for (Dependency catalogDep : entry.getValue().values()) {
                    writeIndent(writer, indent);
                    Strings.appendJsonString(writer, catalogDep.name, true);
                    writer.append(": ");
                    Strings.appendJsonString(writer, catalogDep.version.literal, true);
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
                Strings.appendJsonString(writer, item.relativePath, false);
                if (item.depth != 0) {
                    writer.append('/');
                }
                Dependency dep = depsBuf.get(depId);
                Strings.appendJsonString(writer, dep.name, false);
                writer.append("\": ");

                List<Integer> pkgDepsSortBuf = new ArrayList<>(pkg.dependenciesLen);
                for (int pkgDepId = pkg.dependenciesOff; pkgDepId < pkg.dependenciesEnd(); pkgDepId++) {
                    pkgDepsSortBuf.add(pkgDepId);
                }
                pkgDepsSortBuf.sort((l, r) -> Strings.order(depsBuf.get(l).name, depsBuf.get(r).name));

                switch (res.tag) {
                    case ROOT:
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false);
                        writer.append("@root:\", ");
                        writer.append('{');
                        if (pkg.bin.tag != Bin.Tag.NONE) {
                            writer.append(pkg.bin.tag == Bin.Tag.DIR ? " \"binDir\": " : " \"bin\": ");
                            pkg.bin.toJsonSingleLine(writer);
                            writer.append(" }]");
                        } else {
                            writer.append("}]");
                        }
                        break;
                    case FOLDER:
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false);
                        writer.append("@file:");
                        Strings.appendJsonString(writer, res.value, false);
                        writer.append("\", ");
                        writePackageInfoObject(writer, dep.behavior, depsBuf, pkgDepsSortBuf, pkg);
                        writer.append(']');
                        break;
                    case LOCAL_TARBALL:
                    case REMOTE_TARBALL:
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false);
                        writer.append('@');
                        Strings.appendJsonString(writer, res.value, false);
                        writer.append("\", ");
                        writePackageInfoObject(writer, dep.behavior, depsBuf, pkgDepsSortBuf, pkg);
                        if (pkg.meta.integrity.tag.isSupported()) {
                            writer.append(", \"").append(pkg.meta.integrity).append("\"]");
                        } else {
                            writer.append(']');
                        }
                        break;
                    case SYMLINK:
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false);
                        writer.append("@link:");
                        Strings.appendJsonString(writer, res.value, false);
                        writer.append("\", ");
                        writePackageInfoObject(writer, dep.behavior, depsBuf, pkgDepsSortBuf, pkg);
                        writer.append(']');
                        break;
                    case NPM:
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false);
                        writer.append('@').append(res.version).append("\", ");
                        writer.append('"').append(res.url.startsWith(DEFAULT_REGISTRY) ? "" : res.url).append("\", ");
                        writePackageInfoObject(writer, dep.behavior, depsBuf, pkgDepsSortBuf, pkg);
                        writer.append(", \"").append(pkg.meta.integrity).append("\"]");
                        break;
                    case WORKSPACE:
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false);
                        writer.append("@workspace:");
                        Strings.appendJsonString(writer, res.value, false);
                        writer.append("\"]");
                        break;
                    default: {
                        Repository repo = res.repository;
                        writer.append("[\"");
                        Strings.appendJsonString(writer, pkg.name, false);
                        writer.append('@').append(repo.format(res.tag == Resolution.Tag.GIT ? "git+" : "github:"))
                                .append("\", ");
                        writePackageInfoObject(writer, dep.behavior, depsBuf, pkgDepsSortBuf, pkg);
                        writer.append(", ");
                        Strings.appendJsonString(writer, repo.resolved, true);
                        if (pkg.meta.integrity.tag.isSupported()) {
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
                                               List<Integer> pkgDepIds, Package pkg) {
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
                Strings.appendJsonString(writer, dep.name, true);
                writer.append(": ");
                Strings.appendJsonString(writer, dep.version.literal, true);
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
                Strings.appendJsonString(writer, optionalPeersBuf.get(i), true);
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
            pkg.bin.toJsonSingleLine(writer);
        }

        writer.append(any ? " }" : "}");
    }

    private static void writeWorkspaceDeps(StringBuilder writer, int[] indent, Lockfile lockfile, int pkgId,
                                           String res) {
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
                Strings.appendJsonString(writer, rootName, true);
                any = true;
            }
        } else {
            Strings.appendJsonString(writer, res, true);
            writer.append(": {");
            writer.append('\n');
            incIndent(writer, indent);
            writer.append("\"name\": ");
            Strings.appendJsonString(writer, pkg.name, true);
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
                pkg.bin.toJsonMultiLine(writer, indent);
            }
            any = true;
        }

        for (int g = 0; g < GROUP_NAMES.length; g++) {
            Behavior groupBehavior = GROUP_BEHAVIORS[g];
            boolean first = true;
            for (Dependency dep : lockfile.dependenciesOf(pkg)) {
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
                Strings.appendJsonString(writer, dep.name, true);
                writer.append(": ");
                Strings.appendJsonString(writer, dep.version.literal, true);
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
                Strings.appendJsonString(writer, optionalPeer, true);
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
