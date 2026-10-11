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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/lockfile.fs src/lockfileFormatConverters.ts (pnpm 11.2.2); a value of a type
 * pnpm does not write is cannot-know.
 */
final class LockfileFormatConverters {

    private static final Pattern LOCAL_TARBALL_RE = Pattern.compile("\\.(?:tgz|tar\\.gz|tar)\\z",
            Pattern.CASE_INSENSITIVE);

    private static final List<String> SNAPSHOT_KEYS = Arrays.asList("dependencies", "optionalDependencies",
            "transitivePeerDependencies", "optional", "id");

    private static final List<String> PACKAGE_KEYS = Arrays.asList("bundledDependencies", "cpu",
            "deprecated", "engines", "hasBin", "libc", "name", "os", "peerDependencies", "peerDependenciesMeta",
            "resolution", "version");

    private LockfileFormatConverters() {
    }

    /**
     * A JavaScript {@code TypeError} pnpm's code raises over a malformed lock; the reader reports it as the
     * lock pnpm calls broken.
     */
    static final class TypeError extends RuntimeException {
        TypeError(String message) {
            super(message);
        }
    }

    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> convertToLockfileFile(LockfileObject lockfile) {
        Map<String, @Nullable Object> packages = new LinkedHashMap<>();
        Map<String, @Nullable Object> snapshots = new LinkedHashMap<>();
        if (lockfile.getPackages() != null) {
            for (Map.Entry<String, PackageSnapshot> entry : lockfile.getPackages().entrySet()) {
                String depPath = entry.getKey();
                Map<String, @Nullable Object> pkg = toFileEntry(entry.getValue());
                snapshots.put(depPath, pick(SNAPSHOT_KEYS, pkg));
                String pkgId = DepPath.removeSuffix(depPath);
                if (!Js.truthy(packages.get(pkgId))) {
                    packages.put(pkgId, pick(PACKAGE_KEYS, pkg));
                }
            }
        }
        Map<String, @Nullable Object> newLockfile = new LinkedHashMap<>(lockfile.getRest());
        newLockfile.put("snapshots", snapshots);
        newLockfile.put("packages", packages);
        newLockfile.put("lockfileVersion", LockfileObject.LOCKFILE_VERSION);
        Map<String, @Nullable Object> importers = new LinkedHashMap<>();
        for (Map.Entry<String, ProjectSnapshot> importer : lockfile.getImporters().entrySet()) {
            importers.put(importer.getKey(), convertProjectSnapshotToInlineSpecifiersFormat(importer.getValue()));
        }
        newLockfile.put("importers", importers);
        Object settings = newLockfile.get("settings");
        if (settings instanceof Map) {
            Map<String, @Nullable Object> s = (Map<String, @Nullable Object>) settings;
            if (Js.strictEquals(s.get("peersSuffixMaxLength"), 1000.0)) {
                Map<String, @Nullable Object> omitted = new LinkedHashMap<>(s);
                omitted.remove("peersSuffixMaxLength");
                newLockfile.put("settings", omitted);
                s = omitted;
            }
            if (Boolean.FALSE.equals(s.get("injectWorkspacePackages"))) {
                s.remove("injectWorkspacePackages");
            }
        }
        return normalizeLockfile(newLockfile);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> normalizeLockfile(Map<String, @Nullable Object> lockfile) {
        Map<String, @Nullable Object> importers = new LinkedHashMap<>();
        Object fileImporters = lockfile.get("importers");
        for (String importerId : Js.keys((Map<String, @Nullable Object>) fileImporters)) {
            Map<String, @Nullable Object> importer =
                    (Map<String, @Nullable Object>) ((Map<String, @Nullable Object>) fileImporters).get(importerId);
            Map<String, @Nullable Object> normalized = new LinkedHashMap<>();
            Object dependenciesMeta = importer.get("dependenciesMeta");
            if (dependenciesMeta != null && !isEmpty(dependenciesMeta)) {
                normalized.put("dependenciesMeta", dependenciesMeta);
            }
            for (String depType : ProjectSnapshot.DEPENDENCIES_FIELDS) {
                Object deps = importer.get(depType);
                if (!isEmpty(deps == null ? new LinkedHashMap<>() : deps)) {
                    normalized.put(depType, deps);
                }
            }
            if (Js.truthy(importer.get("publishDirectory"))) {
                normalized.put("publishDirectory", importer.get("publishDirectory"));
            }
            importers.put(importerId, normalized);
        }
        Map<String, @Nullable Object> lockfileToSave = new LinkedHashMap<>(lockfile);
        lockfileToSave.put("importers", importers);
        Object packages = lockfileToSave.get("packages");
        if (packages == null || isEmpty(packages)) {
            lockfileToSave.remove("packages");
        }
        Object snapshots = lockfileToSave.get("snapshots");
        if (snapshots == null || isEmpty(snapshots)) {
            lockfileToSave.remove("snapshots");
        }
        if (Js.truthy(lockfileToSave.get("time"))) {
            lockfileToSave.put("time", pruneTimeInLockfile(lockfileToSave.get("time"), importers));
        }
        for (String key : new String[]{"catalogs", "overrides", "patchedDependencies"}) {
            Object value = lockfileToSave.get(key);
            if (value != null && value != Js.UNDEFINED && isEmpty(value)) {
                lockfileToSave.remove(key);
            }
        }
        if (!Js.truthy(undefinedToNull(lockfileToSave.get("packageExtensionsChecksum")))) {
            lockfileToSave.remove("packageExtensionsChecksum");
        }
        Object ignored = lockfileToSave.get("ignoredOptionalDependencies");
        if (!Js.truthy(length(ignored))) {
            lockfileToSave.remove("ignoredOptionalDependencies");
        }
        if (!Js.truthy(undefinedToNull(lockfileToSave.get("pnpmfileChecksum")))) {
            lockfileToSave.remove("pnpmfileChecksum");
        }
        return lockfileToSave;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> pruneTimeInLockfile(@Nullable Object time,
                                                                     Map<String, @Nullable Object> importers) {
        Set<String> rootDepPaths = new HashSet<>();
        for (Object importer : importers.values()) {
            for (String depType : ProjectSnapshot.DEPENDENCIES_FIELDS) {
                Object deps = ((Map<String, @Nullable Object>) importer).get(depType);
                if (!(deps instanceof Map)) {
                    continue;
                }
                for (Map.Entry<String, @Nullable Object> dep : ((Map<String, @Nullable Object>) deps).entrySet()) {
                    Object version = ((Map<String, @Nullable Object>) dep.getValue()).get("version");
                    if (!(version instanceof String)) {
                        throw new TypeError("Cannot read properties of undefined (reading 'indexOf')");
                    }
                    String ref = (String) version;
                    int suffixStart = ref.indexOf('(');
                    String refWithoutPeerDepGraphHash = suffixStart == -1 ? ref : ref.substring(0, suffixStart);
                    String depPath = DepPath.refToRelative(refWithoutPeerDepGraphHash, dep.getKey());
                    if (depPath == null) {
                        continue;
                    }
                    rootDepPaths.add(depPath);
                }
            }
        }
        Map<String, @Nullable Object> pruned = new LinkedHashMap<>();
        if (time instanceof Map) {
            for (String depPath : Js.keys((Map<String, @Nullable Object>) time)) {
                if (rootDepPaths.contains(depPath)) {
                    pruned.put(depPath, ((Map<String, @Nullable Object>) time).get(depPath));
                }
            }
        }
        return pruned;
    }

    /**
     * {@code convertToLockfileObject} of the value js-yaml read.
     */
    @SuppressWarnings("unchecked")
    static LockfileObject convertToLockfileObject(@Nullable Object file) {
        if (file == null || file == Js.UNDEFINED) {
            throw new TypeError("Cannot destructure property 'importers' of 'lockfile' as it is " +
                                (file == null ? "null" : "undefined") + ".");
        }
        LockfileObject lockfile = new LockfileObject();
        if (!(file instanceof Map)) {
            // a primitive or an array destructures to no importers and no lockfileVersion
            return lockfile;
        }
        Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) file;
        Map<String, PackageSnapshot> packages = new LinkedHashMap<>();
        Object snapshots = map.get("snapshots");
        Object filePackages = map.get("packages");
        if (snapshots != null && snapshots != Js.UNDEFINED) {
            for (Map.Entry<String, @Nullable Object> entry : entries(snapshots, "snapshots")) {
                String depPath = entry.getKey();
                Object pkg = entry.getValue();
                if (pkg == null) {
                    throw new TypeError("Cannot convert undefined or null to object");
                }
                String pkgId = DepPath.removeSuffix(depPath);
                Object info = filePackages instanceof Map ?
                        ((Map<String, @Nullable Object>) filePackages).get(pkgId) : null;
                PackageSnapshot snapshot = new PackageSnapshot();
                assign(snapshot, objectAt(pkg, "snapshots." + depPath), "snapshots." + depPath);
                if (info != null) {
                    assign(snapshot, objectAt(info, "packages." + pkgId), "packages." + pkgId);
                }
                if (snapshot.getResolution() == null) {
                    String ref = DepPath.parse(depPath).getNonSemverVersion();
                    if (ref != null && ref.startsWith("file:") && !LOCAL_TARBALL_RE.matcher(ref).find()) {
                        Map<String, @Nullable Object> resolution = new LinkedHashMap<>();
                        resolution.put("directory", ref.substring("file:".length()));
                        resolution.put("type", "directory");
                        snapshot.setResolution(resolution);
                    }
                }
                packages.put(depPath, snapshot);
                enrichGitHostedFlag(snapshot.getResolution());
            }
        }
        for (String key : Js.keys(map)) {
            if (!"snapshots".equals(key) && !"importers".equals(key)) {
                lockfile.set(key, map.get(key));
            }
        }
        Object patchedDependencies = migratePatchedDependencies(map.get("patchedDependencies"));
        if (patchedDependencies == Js.UNDEFINED) {
            lockfile.getRest().remove("patchedDependencies");
        } else {
            lockfile.set("patchedDependencies", patchedDependencies);
        }
        lockfile.getRest().remove("packages");
        lockfile.setPackages(packages);
        Object importers = map.get("importers");
        if (importers != null && importers != Js.UNDEFINED) {
            for (Map.Entry<String, @Nullable Object> importer : entries(importers, "importers")) {
                lockfile.getImporters().put(importer.getKey(), revertProjectSnapshot(importer.getValue(),
                        "importers." + importer.getKey()));
            }
        }
        return lockfile;
    }

    @SuppressWarnings("unchecked")
    private static void enrichGitHostedFlag(@Nullable Map<String, @Nullable Object> resolution) {
        if (resolution == null) {
            return;
        }
        if (resolution.containsKey("type") && resolution.get("type") != Js.UNDEFINED) {
            return;
        }
        if (resolution.get("gitHosted") != null) {
            return;
        }
        Object tarball = resolution.get("tarball");
        if (tarball != null) {
            if (!(tarball instanceof String)) {
                throw new TypeError("url.startsWith is not a function");
            }
            if (isGitHostedTarballUrl((String) tarball)) {
                resolution.put("gitHosted", true);
            }
        }
    }

    private static boolean isGitHostedTarballUrl(String url) {
        return (url.startsWith("https://codeload.github.com/") || url.startsWith("https://bitbucket.org/") ||
                url.startsWith("https://gitlab.com/")) && url.contains("tar.gz");
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object migratePatchedDependencies(@Nullable Object patchedDependencies) {
        if (!Js.truthy(undefinedToNull(patchedDependencies))) {
            return Js.UNDEFINED;
        }
        Map<String, @Nullable Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Object> entry : entries(patchedDependencies, "patchedDependencies")) {
            Object value = entry.getValue();
            if (value instanceof String) {
                result.put(entry.getKey(), value);
            } else if (value == null) {
                throw new TypeError("Cannot read properties of null (reading 'hash')");
            } else {
                Object hash = value instanceof Map ? ((Map<String, @Nullable Object>) value).get("hash") : null;
                result.put(entry.getKey(), hash == null ? Js.UNDEFINED : hash);
            }
        }
        return result;
    }

    private static Map<String, @Nullable Object> convertProjectSnapshotToInlineSpecifiersFormat(
            ProjectSnapshot projectSnapshot) {
        Map<String, @Nullable Object> rest = new LinkedHashMap<>();
        if (projectSnapshot.getDependenciesMeta() != null) {
            rest.put("dependenciesMeta", projectSnapshot.getDependenciesMeta());
        }
        if (projectSnapshot.getPublishDirectory() != null) {
            rest.put("publishDirectory", projectSnapshot.getPublishDirectory());
        }
        Map<String, @Nullable String> specifiers = projectSnapshot.getSpecifiers();
        rest.put("dependencies", inline(projectSnapshot.getDependencies(), specifiers));
        rest.put("optionalDependencies", inline(projectSnapshot.getOptionalDependencies(), specifiers));
        rest.put("devDependencies", inline(projectSnapshot.getDevDependencies(), specifiers));
        return rest;
    }

    private static Map<String, @Nullable Object> inline(@Nullable Map<String, @Nullable String> resolvedDependencies,
                                                        Map<String, @Nullable String> specifiers) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        if (resolvedDependencies == null) {
            return out;
        }
        for (String depName : Js.keys(resolvedDependencies)) {
            Map<String, @Nullable Object> ref = new LinkedHashMap<>();
            ref.put("specifier", specifiers.containsKey(depName) ? specifiers.get(depName) : Js.UNDEFINED);
            String version = resolvedDependencies.get(depName);
            ref.put("version", version == null ? Js.UNDEFINED : version);
            out.put(depName, ref);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static ProjectSnapshot revertProjectSnapshot(@Nullable Object from, String path) {
        if (from == null) {
            throw new TypeError("Cannot read properties of null (reading 'dependencies')");
        }
        Map<String, @Nullable Object> map = objectAt(from, path);
        ProjectSnapshot snapshot = new ProjectSnapshot();
        Map<String, @Nullable String> specifiers = snapshot.getSpecifiers();
        snapshot.setDependencies(moveSpecifiers(map.get("dependencies"), specifiers, path + ".dependencies"));
        snapshot.setDevDependencies(moveSpecifiers(map.get("devDependencies"), specifiers, path + ".devDependencies"));
        snapshot.setOptionalDependencies(moveSpecifiers(map.get("optionalDependencies"), specifiers,
                path + ".optionalDependencies"));
        if (map.containsKey("dependenciesMeta")) {
            snapshot.setDependenciesMeta(nullToAbsent(map.get("dependenciesMeta"), path + ".dependenciesMeta"));
        }
        if (map.containsKey("publishDirectory")) {
            snapshot.setPublishDirectory(nullToAbsent(map.get("publishDirectory"), path + ".publishDirectory"));
        }
        return snapshot;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, @Nullable String> moveSpecifiers(@Nullable Object from,
                                                                          Map<String, @Nullable String> specifiers,
                                                                          String path) {
        if (from == null || from == Js.UNDEFINED) {
            return null;
        }
        Map<String, @Nullable String> resolvedDependencies = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Object> entry : entries(from, path)) {
            String depName = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                throw new TypeError("Cannot destructure property 'specifier' of 'object null' as it is null.");
            }
            Map<String, @Nullable Object> ref = objectAt(value, path + "." + depName);
            String specifier = string(ref.get("specifier"), path + "." + depName + ".specifier");
            String version = string(ref.get("version"), path + "." + depName + ".version");
            String existingValue = specifiers.get(depName);
            if (existingValue != null && !existingValue.equals(specifier)) {
                throw new TypeError("Project snapshot lists the same dependency more than once with conflicting " +
                                    "versions: " + depName);
            }
            specifiers.put(depName, specifier);
            resolvedDependencies.put(depName, version);
        }
        return resolvedDependencies;
    }

    @SuppressWarnings("unchecked")
    private static void assign(PackageSnapshot snapshot, Map<String, @Nullable Object> source, String path) {
        for (String key : Js.keys(source)) {
            Object value = source.get(key);
            String at = path + "." + key;
            switch (key) {
                case "id":
                    snapshot.setId(string(value, at));
                    break;
                case "name":
                    snapshot.setName(string(value, at));
                    break;
                case "version":
                    snapshot.setVersion(string(value, at));
                    break;
                case "resolution":
                    snapshot.setResolution(objectAt(nullToAbsent(value, at), at));
                    break;
                case "engines":
                    snapshot.setEngines(nullToAbsent(value, at));
                    break;
                case "cpu":
                    snapshot.setCpu(nullToAbsent(value, at));
                    break;
                case "os":
                    snapshot.setOs(nullToAbsent(value, at));
                    break;
                case "libc":
                    snapshot.setLibc(nullToAbsent(value, at));
                    break;
                case "deprecated":
                    snapshot.setDeprecated(nullToAbsent(value, at));
                    break;
                case "hasBin":
                    snapshot.setHasBin(nullToAbsent(value, at));
                    break;
                case "bundledDependencies":
                    snapshot.setBundledDependencies(nullToAbsent(value, at));
                    break;
                case "peerDependencies":
                    snapshot.setPeerDependencies(nullToAbsent(value, at));
                    break;
                case "peerDependenciesMeta":
                    snapshot.setPeerDependenciesMeta(nullToAbsent(value, at));
                    break;
                case "dependencies":
                    snapshot.setDependencies(stringMap(value, at));
                    break;
                case "optionalDependencies":
                    snapshot.setOptionalDependencies(stringMap(value, at));
                    break;
                case "transitivePeerDependencies":
                    snapshot.setTransitivePeerDependencies(stringList(value, at));
                    break;
                case "optional":
                    snapshot.setOptional(nullToAbsent(value, at));
                    break;
                default:
                    // keys pnpm 11 neither reads in a lock-only install nor writes (dev, requiresBuild, prepare, ...)
                    break;
            }
        }
    }

    private static Map<String, @Nullable Object> toFileEntry(PackageSnapshot pkg) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        putIfPresent(out, "id", pkg.getId());
        putIfPresent(out, "name", pkg.getName());
        putIfPresent(out, "version", pkg.getVersion());
        putIfPresent(out, "resolution", pkg.getResolution());
        putIfPresent(out, "engines", pkg.getEngines());
        putIfPresent(out, "cpu", pkg.getCpu());
        putIfPresent(out, "os", pkg.getOs());
        putIfPresent(out, "libc", pkg.getLibc());
        putIfPresent(out, "deprecated", pkg.getDeprecated());
        putIfPresent(out, "hasBin", pkg.getHasBin());
        putIfPresent(out, "bundledDependencies", pkg.getBundledDependencies());
        putIfPresent(out, "peerDependencies", pkg.getPeerDependencies());
        putIfPresent(out, "peerDependenciesMeta", pkg.getPeerDependenciesMeta());
        if (pkg.getDependencies() != null) {
            out.put("dependencies", stringValues(pkg.getDependencies()));
        }
        if (pkg.getOptionalDependencies() != null) {
            out.put("optionalDependencies", stringValues(pkg.getOptionalDependencies()));
        }
        if (pkg.getTransitivePeerDependencies() != null) {
            out.put("transitivePeerDependencies", new ArrayList<Object>(pkg.getTransitivePeerDependencies()));
        }
        putIfPresent(out, "optional", pkg.getOptional());
        return out;
    }

    private static Map<String, @Nullable Object> stringValues(Map<String, @Nullable String> refs) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable String> ref : refs.entrySet()) {
            out.put(ref.getKey(), ref.getValue() == null ? Js.UNDEFINED : ref.getValue());
        }
        return out;
    }

    private static void putIfPresent(Map<String, @Nullable Object> out, String key, @Nullable Object value) {
        if (value != null) {
            out.put(key, value);
        }
    }

    // ramda's pick: the names the object has, in the order of the list
    private static Map<String, @Nullable Object> pick(List<String> names, Map<String, @Nullable Object> obj) {
        Map<String, @Nullable Object> result = new LinkedHashMap<>();
        for (String name : names) {
            if (obj.containsKey(name)) {
                result.put(name, obj.get(name));
            }
        }
        return result;
    }

    /**
     * Ramda's {@code isEmpty}: an empty object, array or string.
     */
    @SuppressWarnings("unchecked")
    static boolean isEmpty(@Nullable Object value) {
        if (value instanceof Map) {
            return ((Map<String, Object>) value).isEmpty();
        }
        if (value instanceof List) {
            return ((List<Object>) value).isEmpty();
        }
        if (value instanceof String) {
            return ((String) value).isEmpty();
        }
        return false;
    }

    private static @Nullable Object length(@Nullable Object value) {
        if (value instanceof List) {
            return (double) ((List<?>) value).size();
        }
        if (value instanceof String) {
            return (double) ((String) value).length();
        }
        return null;
    }

    private static @Nullable Object undefinedToNull(@Nullable Object value) {
        return value == Js.UNDEFINED ? null : value;
    }

    private static List<Map.Entry<String, @Nullable Object>> entries(Object value, String path) {
        return Js.entries(objectAt(value, path));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> objectAt(Object value, String path) {
        if (value instanceof Map) {
            return (Map<String, @Nullable Object>) value;
        }
        throw new CannotKnow("what pnpm makes of " + path + " (" + describe(value) +
                             ", where pnpm writes an object)");
    }

    private static @Nullable String string(@Nullable Object value, String path) {
        if (value == null || value == Js.UNDEFINED) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        throw new CannotKnow("what pnpm makes of " + path + " (" + describe(value) + ", where pnpm writes a string)");
    }

    private static @Nullable Map<String, @Nullable String> stringMap(@Nullable Object value, String path) {
        if (value == null) {
            throw new CannotKnow("what pnpm makes of " + path + " (null, which pnpm does not write)");
        }
        Map<String, @Nullable Object> map = objectAt(value, path);
        Map<String, @Nullable String> out = new LinkedHashMap<>();
        for (String key : Js.keys(map)) {
            Object ref = map.get(key);
            if (ref == null) {
                throw new CannotKnow("what pnpm makes of " + path + "." + key + " (null, which pnpm does not write)");
            }
            out.put(key, string(ref, path + "." + key));
        }
        return out;
    }

    private static List<String> stringList(@Nullable Object value, String path) {
        if (!(value instanceof List)) {
            throw new CannotKnow("what pnpm makes of " + path + " (" + describe(value) +
                                 ", where pnpm writes an array)");
        }
        List<String> out = new ArrayList<>();
        for (Object item : (List<?>) value) {
            if (!(item instanceof String)) {
                throw new CannotKnow("what pnpm makes of " + path + " (" + describe(item) +
                                     " in it, where pnpm writes a string)");
            }
            out.add((String) item);
        }
        return out;
    }

    private static @Nullable Object nullToAbsent(@Nullable Object value, String path) {
        if (value == null) {
            throw new CannotKnow("what pnpm makes of " + path + " (null, which pnpm does not write)");
        }
        return value;
    }

    private static String describe(@Nullable Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof List) {
            return "an array";
        }
        if (value instanceof Map) {
            return "an object";
        }
        return "a " + Js.typeOf(value);
    }
}
