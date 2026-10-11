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
package org.openrewrite.javascript.internal.lock.pnpm;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.UriComponent;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of @pnpm/lockfile.verification (pnpm 11.2.2), with @pnpm/lockfile.utils' refIsLocalDirectory
 * and refIsLocalTarball and version-selector-type 3.0.0.
 */
final class Verification {

    private Verification() {
    }

    /**
     * One project of the workspace: where it is (relative to the lock's directory), its importer id and its
     * manifest as parsed.
     */
    @Value
    static class Project {
        String rootDir;

        String id;

        Map<String, @Nullable Object> manifest;
    }

    @Value
    static class Satisfies {
        boolean satisfies;

        @Nullable
        String detailedReason;
    }

    @SuppressWarnings("unchecked")
    static boolean allCatalogsAreUpToDate(Map<String, @Nullable Map<String, @Nullable Object>> catalogsConfig,
                                          @Nullable Object snapshot) {
        if (!(snapshot instanceof Map)) {
            return true;
        }
        for (Map.Entry<String, @Nullable Object> catalog : ((Map<String, @Nullable Object>) snapshot).entrySet()) {
            if (!(catalog.getValue() instanceof Map)) {
                continue;
            }
            Map<String, @Nullable Object> entries = (Map<String, @Nullable Object>) catalog.getValue();
            for (Map.Entry<String, @Nullable Object> entry : entries.entrySet()) {
                Object specifier = entry.getValue() instanceof Map ?
                        ((Map<String, @Nullable Object>) entry.getValue()).get("specifier") : null;
                Map<String, @Nullable Object> config = catalogsConfig.get(catalog.getKey());
                Object configured = config == null ? null : config.get(entry.getKey());
                if (!Js.strictEquals(specifier, configured)) {
                    return false;
                }
            }
        }
        return true;
    }

    static boolean allProjectsAreUpToDate(List<Project> projects,
                                          Map<String, @Nullable Map<String, @Nullable Object>> catalogs,
                                          boolean autoInstallPeers, boolean excludeLinksFromLockfile,
                                          boolean linkWorkspacePackages, LockfileObject wantedLockfile,
                                          @Nullable Map<String, Map<String, Project>> workspacePackages,
                                          Map<String, String> files, String lockfileDir) {
        if (!allCatalogsAreUpToDate(catalogs, wantedLockfile.getCatalogs())) {
            return false;
        }
        Map<String, Map<String, @Nullable Object>> manifestsByDir = new LinkedHashMap<>();
        if (workspacePackages != null) {
            for (Map<String, Project> versions : workspacePackages.values()) {
                for (Project project : versions.values()) {
                    manifestsByDir.put(project.getRootDir(), project.getManifest());
                }
            }
        }
        for (Project project : projects) {
            ProjectSnapshot importer = wantedLockfile.getImporters().get(project.getId());
            if (importer == null) {
                for (String depType : ProjectSnapshot.DEPENDENCIES_FIELDS) {
                    Object deps = project.getManifest().get(depType);
                    if (deps != null && !LockfileFormatConverters.isEmpty(deps)) {
                        return false;
                    }
                }
                continue;
            }
            if (!satisfiesPackageManifest(autoInstallPeers, excludeLinksFromLockfile, importer, project.getManifest())
                    .isSatisfies() ||
                !localTarballDepsAreUpToDate(wantedLockfile.getPackages(), importer) ||
                !linkedPackagesAreUpToDate(linkWorkspacePackages, manifestsByDir, workspacePackages,
                        wantedLockfile.getPackages(), project, importer, files, lockfileDir)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    static Satisfies satisfiesPackageManifest(boolean autoInstallPeers, boolean excludeLinksFromLockfile,
                                              @Nullable ProjectSnapshot importer, Map<String, @Nullable Object> pkg) {
        if (importer == null) {
            return new Satisfies(false, "no importer");
        }
        Map<String, @Nullable Object> existingDeps = new LinkedHashMap<>();
        existingDeps.putAll(field(pkg, "devDependencies"));
        existingDeps.putAll(field(pkg, "dependencies"));
        existingDeps.putAll(field(pkg, "optionalDependencies"));
        if (autoInstallPeers) {
            Map<String, @Nullable Object> dependencies = new LinkedHashMap<>();
            for (Map.Entry<String, @Nullable Object> peer : field(pkg, "peerDependencies").entrySet()) {
                if (!existingDeps.containsKey(peer.getKey())) {
                    dependencies.put(peer.getKey(), peer.getValue());
                }
            }
            dependencies.putAll(field(pkg, "dependencies"));
            pkg = new LinkedHashMap<>(pkg);
            pkg.put("dependencies", dependencies);
            Map<String, @Nullable Object> withPeers = new LinkedHashMap<>(field(pkg, "peerDependencies"));
            withPeers.putAll(existingDeps);
            existingDeps = withPeers;
        }
        Map<String, @Nullable Object> specs = new LinkedHashMap<>(importer.getSpecifiers());
        if (excludeLinksFromLockfile) {
            existingDeps = pickNonLinkedDeps(existingDeps);
            specs = pickNonLinkedDeps(specs);
        }
        Diff specsDiff = diffFlatRecords(specs, existingDeps);
        if (!specsDiff.isEqual()) {
            return new Satisfies(false, "specifiers in the lockfile don't match specifiers in package.json:\n" +
                                        specsDiff.display());
        }
        Object publishConfig = pkg.get("publishConfig");
        Object directory = publishConfig instanceof Map ?
                ((Map<String, @Nullable Object>) publishConfig).get("directory") : null;
        if (!Js.strictEquals(importer.getPublishDirectory(), directory)) {
            return new Satisfies(false, "\"publishDirectory\" in the lockfile (" +
                                        orUndefined(importer.getPublishDirectory()) +
                                        ") doesn't match \"publishConfig.directory\" in package.json (" +
                                        orUndefined(directory) + ")");
        }
        Object dependenciesMeta = pkg.get("dependenciesMeta");
        if (!Js.equals(dependenciesMeta == null ? new LinkedHashMap<>() : dependenciesMeta,
                importer.getDependenciesMeta() == null ? new LinkedHashMap<>() : importer.getDependenciesMeta())) {
            return new Satisfies(false, "importer dependencies meta (" + Js.jsonStringify(
                    importer.getDependenciesMeta() == null ? Js.UNDEFINED : importer.getDependenciesMeta()) +
                    ") doesn't match package manifest dependencies meta (" +
                    Js.jsonStringify(dependenciesMeta == null ? Js.UNDEFINED : dependenciesMeta) + ")");
        }
        for (String depField : ProjectSnapshot.DEPENDENCIES_FIELDS) {
            Map<String, @Nullable String> importerDeps = importer.field(depField) == null ? new LinkedHashMap<>() :
                    importer.field(depField);
            Map<String, @Nullable Object> pkgDeps = field(pkg, depField);
            if (excludeLinksFromLockfile) {
                pkgDeps = pickNonLinkedDeps(pkgDeps);
            }
            List<String> pkgDepNames = new ArrayList<>();
            for (String depName : Js.keys(pkgDeps)) {
                if ("devDependencies".equals(depField) &&
                    (Js.truthy(field(pkg, "optionalDependencies").get(depName)) ||
                     Js.truthy(field(pkg, "dependencies").get(depName)))) {
                    continue;
                }
                if ("dependencies".equals(depField) && Js.truthy(field(pkg, "optionalDependencies").get(depName))) {
                    continue;
                }
                pkgDepNames.add(depName);
            }
            if (pkgDepNames.size() != importerDeps.size() && pkgDepNames.size() != countOfNonLinkedDeps(importerDeps)) {
                return new Satisfies(false, "\"" + depField + "\" in the lockfile (" + Js.jsonStringify(importerDeps) +
                                            ") doesn't match the same field in package.json (" +
                                            Js.jsonStringify(pkgDeps) + ")");
            }
            for (String depName : pkgDepNames) {
                String specifier = importer.getSpecifiers().get(depName);
                if (!Js.truthy(importerDeps.get(depName)) || !Js.strictEquals(specifier, pkgDeps.get(depName))) {
                    return new Satisfies(false, "importer " + depField + "." + depName + " specifier " +
                                                orUndefined(specifier) + " don't match package manifest specifier (" +
                                                Js.string(pkgDeps.get(depName)) + ")");
                }
                if (specifier == null || NodeSemver.validRange(specifier, NodeSemver.Options.DEFAULT) == null) {
                    continue;
                }
                String version = DepPath.removeSuffix(importerDeps.get(depName));
                if (NodeSemver.valid(version, NodeSemver.Options.DEFAULT) != null &&
                    !NodeSemver.satisfies(version, specifier, NodeSemver.Options.DEFAULT)) {
                    return new Satisfies(false, "The importer resolution is broken at dependency \"" + depName +
                                                "\": version \"" + version + "\" doesn't satisfy range \"" +
                                                specifier + "\"");
                }
            }
        }
        return new Satisfies(true, null);
    }

    private static String orUndefined(@Nullable Object value) {
        return value == null ? "undefined" : Js.string(value);
    }

    private static int countOfNonLinkedDeps(Map<String, @Nullable String> lockfileDeps) {
        int count = 0;
        for (String ref : lockfileDeps.values()) {
            if (ref == null || !ref.contains("link:") && !ref.contains("file:")) {
                count++;
            }
        }
        return count;
    }

    private static Map<String, @Nullable Object> pickNonLinkedDeps(Map<String, @Nullable ?> deps) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        for (String key : Js.keys(deps)) {
            Object spec = deps.get(key);
            if (!(spec instanceof String && ((String) spec).startsWith("link:"))) {
                out.put(key, spec);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> field(Map<String, @Nullable Object> pkg, String field) {
        Object value = pkg.get(field);
        if (value instanceof Map) {
            return (Map<String, @Nullable Object>) value;
        }
        return new LinkedHashMap<>();
    }

    /**
     * {@code diffFlatRecords}: what {@code right} adds, removes and changes from {@code left}.
     */
    @Value
    static class Diff {
        List<String[]> added;
        List<String[]> removed;
        List<String[]> modified;

        boolean isEqual() {
            return added.isEmpty() && removed.isEmpty() && modified.isEmpty();
        }

        String display() {
            StringBuilder result = new StringBuilder();
            if (!added.isEmpty()) {
                result.append("* ").append(added.size()).append(" dependencies were added: ");
                List<String> items = new ArrayList<>();
                for (String[] a : added) {
                    items.add(a[0] + "@" + a[1]);
                }
                result.append(String.join(", ", items)).append('\n');
            }
            if (!removed.isEmpty()) {
                result.append("* ").append(removed.size()).append(" dependencies were removed: ");
                List<String> items = new ArrayList<>();
                for (String[] r : removed) {
                    items.add(r[0] + "@" + r[1]);
                }
                result.append(String.join(", ", items)).append('\n');
            }
            if (!modified.isEmpty()) {
                result.append("* ").append(modified.size()).append(" dependencies are mismatched:\n");
                for (String[] m : modified) {
                    result.append("  - ").append(m[0]).append(" (lockfile: ").append(m[1]).append(", manifest: ")
                            .append(m[2]).append(")\n");
                }
            }
            return result.toString();
        }
    }

    static Diff diffFlatRecords(Map<String, @Nullable ?> left, Map<String, @Nullable ?> right) {
        Diff result = new Diff(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        for (String key : Js.keys(left)) {
            Object value = left.get(key);
            if (!right.containsKey(key)) {
                result.getRemoved().add(new String[]{key, Js.string(value)});
            } else if (!Js.strictEquals(value, right.get(key))) {
                result.getModified().add(new String[]{key, Js.string(value), Js.string(right.get(key))});
            }
        }
        for (String key : Js.keys(right)) {
            if (!left.containsKey(key)) {
                result.getAdded().add(new String[]{key, Js.string(right.get(key))});
            }
        }
        return result;
    }

    /**
     * {@code localTarballDepsAreUpToDate}: a local tarball's integrity is the hash of its bytes, which the engine is
     * not given.
     */
    static boolean localTarballDepsAreUpToDate(@Nullable Map<String, PackageSnapshot> lockfilePackages,
                                               ProjectSnapshot snapshot) {
        for (String depField : ProjectSnapshot.DEPENDENCIES_FIELDS) {
            Map<String, @Nullable String> lockfileDeps = snapshot.field(depField);
            if (lockfileDeps == null) {
                continue;
            }
            for (Map.Entry<String, @Nullable String> dep : lockfileDeps.entrySet()) {
                String ref = dep.getValue();
                if (ref == null || !ref.startsWith("file:")) {
                    continue;
                }
                String depPath = DepPath.refToRelative(ref, dep.getKey());
                if (depPath == null) {
                    continue;
                }
                String tarballRefWithoutPeersSuffix = DepPath.parse(depPath).getNonSemverVersion();
                if (tarballRefWithoutPeersSuffix == null || !refIsLocalTarball(tarballRefWithoutPeersSuffix)) {
                    continue;
                }
                if (lockfilePackages == null || lockfilePackages.get(depPath) == null) {
                    return false;
                }
                throw new CannotKnow("the integrity of the local tarball " + tarballRefWithoutPeersSuffix +
                                     " (pnpm hashes the file on every install)");
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean linkedPackagesAreUpToDate(boolean linkWorkspacePackages,
                                                     Map<String, Map<String, @Nullable Object>> manifestsByDir,
                                                     @Nullable Map<String, Map<String, Project>> workspacePackages,
                                                     @Nullable Map<String, PackageSnapshot> lockfilePackages,
                                                     Project project, ProjectSnapshot snapshot,
                                                     Map<String, String> files, String lockfileDir) {
        for (String depField : ProjectSnapshot.DEPENDENCIES_FIELDS) {
            Map<String, @Nullable String> lockfileDeps = snapshot.field(depField);
            Object manifestDeps = project.getManifest().get(depField);
            if (lockfileDeps == null || !(manifestDeps instanceof Map)) {
                continue;
            }
            for (String depName : Js.keys(lockfileDeps)) {
                Object currentSpecValue = ((Map<String, @Nullable Object>) manifestDeps).get(depName);
                if (!Js.truthy(currentSpecValue)) {
                    continue;
                }
                String currentSpec = Js.string(currentSpecValue);
                String lockfileRef = lockfileDeps.get(depName);
                if (lockfileRef == null) {
                    throw new LockfileFormatConverters.TypeError(
                            "Cannot read properties of undefined (reading 'startsWith')");
                }
                String specifier = snapshot.getSpecifiers().get(depName);
                if (specifier != null && refIsLocalDirectory(specifier)) {
                    if (lockfileRef.startsWith("link:")) {
                        continue;
                    }
                    String depPath = DepPath.refToRelative(lockfileRef, depName);
                    if (depPath == null || !isLocalFileDepUpdated(lockfilePackages == null ? null :
                            lockfilePackages.get(depPath), files, lockfileDir)) {
                        return false;
                    }
                    continue;
                }
                boolean isLinked = lockfileRef.startsWith("link:");
                if (isLinked && (currentSpec.startsWith("link:") || currentSpec.startsWith("file:") ||
                                 currentSpec.startsWith("workspace:."))) {
                    continue;
                }
                if (isLinked && "tag".equals(versionSelectorType(currentSpec))) {
                    continue;
                }
                String linkedDir;
                if (isLinked) {
                    linkedDir = RootRelativePath.join(project.getRootDir(), lockfileRef.substring(5));
                } else {
                    Map<String, Project> versions = workspacePackages == null ? null : workspacePackages.get(depName);
                    Project linked = versions == null ? null : versions.get(lockfileRef);
                    linkedDir = linked == null ? null : linked.getRootDir();
                }
                if (linkedDir == null) {
                    continue;
                }
                if (!linkWorkspacePackages && !currentSpec.startsWith("workspace:")) {
                    continue;
                }
                Map<String, @Nullable Object> linkedPkg = manifestsByDir.get(linkedDir);
                if (linkedPkg == null) {
                    linkedPkg = readPackageJson(files, linkedDir);
                }
                String availableRange = getVersionRange(currentSpec);
                boolean localPackageSatisfiesRange = "*".equals(availableRange) || "^".equals(availableRange) ||
                                                     "~".equals(availableRange) ||
                                                     linkedPkg != null && NodeSemver.satisfies(
                                                             Js.string(linkedPkg.get("version")), availableRange,
                                                             NodeSemver.Options.LOOSE);
                if (isLinked != localPackageSatisfiesRange) {
                    return false;
                }
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private static boolean isLocalFileDepUpdated(@Nullable PackageSnapshot pkgSnapshot, Map<String, String> files,
                                                 String lockfileDir) {
        if (pkgSnapshot == null) {
            return false;
        }
        Object directory = pkgSnapshot.getResolution() == null ? null : pkgSnapshot.getResolution().get("directory");
        Map<String, @Nullable Object> manifest = readPackageJson(files,
                RootRelativePath.join(lockfileDir, Js.string(directory)));
        if (manifest == null) {
            return false;
        }
        for (String depField : new String[]{"optionalDependencies", "dependencies", "peerDependencies"}) {
            Map<String, @Nullable Object> manifestDeps = field(manifest, depField);
            Object lockfileDepsValue = "optionalDependencies".equals(depField) ? pkgSnapshot.getOptionalDependencies() :
                    "dependencies".equals(depField) ? pkgSnapshot.getDependencies() : pkgSnapshot.getPeerDependencies();
            Map<String, @Nullable Object> lockfileDeps = lockfileDepsValue instanceof Map ?
                    (Map<String, @Nullable Object>) lockfileDepsValue : new LinkedHashMap<>();
            for (String depName : lockfileDeps.keySet()) {
                if (!Js.truthy(manifestDeps.get(depName))) {
                    return false;
                }
            }
            for (String depName : Js.keys(manifestDeps)) {
                if (!Js.truthy(lockfileDeps.get(depName))) {
                    return false;
                }
                String currentSpec = Js.string(manifestDeps.get(depName));
                if (currentSpec.startsWith("file:") || currentSpec.startsWith("link:") ||
                    currentSpec.startsWith("workspace:")) {
                    continue;
                }
                if (!NodeSemver.satisfies(Js.string(lockfileDeps.get(depName)), getVersionRange(currentSpec),
                        NodeSemver.Options.LOOSE)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static @Nullable Map<String, @Nullable Object> readPackageJson(Map<String, String> files, String dir) {
        if (RootRelativePath.climbsAboveRoot(dir)) {
            throw new CannotKnow("the package.json of " + dir + " (outside the project)");
        }
        String text = files.get(dir.isEmpty() ? "package.json" : dir + "/package.json");
        return text == null ? null : Manifests.parse(text, dir.isEmpty() ? "package.json" : dir + "/package.json");
    }

    private static String getVersionRange(String spec) {
        if (spec.startsWith("workspace:")) {
            return spec.substring(10);
        }
        if (spec.startsWith("npm:")) {
            spec = spec.substring(4);
            int index = spec.indexOf('@', 1);
            if (index == -1) {
                return "*";
            }
            String range = spec.substring(index + 1);
            return range.isEmpty() ? "*" : range;
        }
        return spec;
    }

    /**
     * version-selector-type 3.0.0 (loose): {@code version}, {@code range}, {@code tag}, or null.
     */
    static @Nullable String versionSelectorType(String selector) {
        if (NodeSemver.valid(selector, NodeSemver.Options.LOOSE) != null) {
            return "version";
        }
        if (NodeSemver.validRange(selector, NodeSemver.Options.LOOSE) != null) {
            return "range";
        }
        if (UriComponent.encodeURIComponent(selector).equals(selector)) {
            return "tag";
        }
        return null;
    }

    /**
     * @pnpm/lockfile.utils {@code refIsLocalTarball}.
     */
    static boolean refIsLocalTarball(String ref) {
        return ref.startsWith("file:") && (ref.endsWith(".tgz") || ref.endsWith(".tar.gz") || ref.endsWith(".tar"));
    }

    /**
     * @pnpm/lockfile.utils {@code refIsLocalDirectory}.
     */
    static boolean refIsLocalDirectory(String ref) {
        return ref.startsWith("file:") && !refIsLocalTarball(ref);
    }
}
