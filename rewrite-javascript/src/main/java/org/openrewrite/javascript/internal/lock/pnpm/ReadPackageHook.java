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
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.V8Sort;
import org.openrewrite.javascript.semver.NodeSemver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

/**
 * Port of @pnpm/hooks.read-package-hook (pnpm 11.2.2), with @pnpm/deps.peer-range.
 */
final class ReadPackageHook {

    private static final String COMPAT_DB = "compat-db.json.gz";
    private static @Nullable Map<String, @Nullable Object> compatPackageExtensions;
    private static @Nullable List<Map.Entry<String, @Nullable Object>> compatPackageExtensionEntries;

    private final List<Hook> hooks;

    private interface Hook {
        Map<String, @Nullable Object> apply(Map<String, @Nullable Object> manifest, @Nullable String dir);
    }

    private ReadPackageHook(List<Hook> hooks) {
        this.hooks = hooks;
    }

    /**
     * {@code createReadPackageHook}, null when no hook applies; {@code lockfileDir} is where relative override
     * targets start.
     */
    static @Nullable ReadPackageHook createReadPackageHook(boolean ignoreCompatibilityDb, String lockfileDir,
                                                           List<ParseOverrides.VersionOverride> overrides,
                                                           @Nullable Object ignoredOptionalDependencies,
                                                           @Nullable Object packageExtensions) {
        return createReadPackageHook(ignoreCompatibilityDb, lockfileDir, overrides, ignoredOptionalDependencies,
                packageExtensions, PnpmRelease.PORTED);
    }

    /**
     * {@link #createReadPackageHook(boolean, String, List, Object, Object)} as {@code release} makes it.
     */
    static @Nullable ReadPackageHook createReadPackageHook(boolean ignoreCompatibilityDb, String lockfileDir,
                                                           List<ParseOverrides.VersionOverride> overrides,
                                                           @Nullable Object ignoredOptionalDependencies,
                                                           @Nullable Object packageExtensions,
                                                           PnpmRelease release) {
        List<Hook> hooks = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Map<String, @Nullable Object> userExtensions = packageExtensions instanceof Map &&
                                                       !((Map<?, ?>) packageExtensions).isEmpty() ?
                (Map<String, @Nullable Object>) packageExtensions : null;
        List<Hook> extenders = new ArrayList<>();
        if (!ignoreCompatibilityDb) {
            extenders.add(createPackageExtender(compatPackageExtensions(), release));
        }
        if (userExtensions != null) {
            extenders.add(createPackageExtender(userExtensions, release));
        }
        if (!extenders.isEmpty()) {
            hooks.add(mergedPackageExtender(extenders, ignoreCompatibilityDb, userExtensions, release));
        }
        if (!overrides.isEmpty()) {
            hooks.add(createVersionsOverrider(overrides, lockfileDir, release));
        }
        if (ignoredOptionalDependencies instanceof List && !((List<?>) ignoredOptionalDependencies).isEmpty()) {
            List<String> toBeRemoved = new ArrayList<>();
            for (Object pattern : (List<?>) ignoredOptionalDependencies) {
                toBeRemoved.add(Js.string(pattern));
            }
            hooks.add(createOptionalDependenciesRemover(toBeRemoved));
        }
        return hooks.isEmpty() ? null : new ReadPackageHook(hooks);
    }

    private static final String[] PACKAGE_EXTENSION_FIELDS = {"dependencies", "optionalDependencies",
            "peerDependencies", "peerDependenciesMeta"};

    /**
     * The package extensions' hook: 11.2.2's compatibility database extender (its last entry of a selector) and the
     * project's after it; from 11.7.0 one extender over getEffectivePackageExtensions, which merges the entries of a
     * selector, the first one's keys winning (PnpmRelease#mergesThePackageExtensionsOfASelector). The two differ
     * only for a package a selector named more than once matches, the release asked where they extend it otherwise.
     */
    @SuppressWarnings("unchecked")
    private static Hook mergedPackageExtender(List<Hook> extenders, boolean ignoreCompatibilityDb,
                                              @Nullable Map<String, @Nullable Object> userExtensions,
                                              PnpmRelease release) {
        List<Map.Entry<String, @Nullable Object>> entries = new ArrayList<>();
        if (!ignoreCompatibilityDb) {
            entries.addAll(compatPackageExtensionEntries());
        }
        if (userExtensions != null) {
            for (String selector : Js.keys(userExtensions)) {
                entries.add(new AbstractMap.SimpleEntry<>(selector, userExtensions.get(selector)));
            }
        }
        Set<String> seen = new HashSet<>();
        Set<@Nullable String> mergedNames = new HashSet<>();
        for (Map.Entry<String, @Nullable Object> entry : entries) {
            if (!seen.add(entry.getKey())) {
                mergedNames.add(ParseOverrides.parseWantedDependency(entry.getKey()).getAlias());
            }
        }
        Hook legacy = (manifest, dir) -> {
            Map<String, @Nullable Object> extended = manifest;
            for (Hook extender : extenders) {
                extended = extender.apply(extended, dir);
            }
            return extended;
        };
        if (mergedNames.isEmpty()) {
            return legacy;
        }
        Map<String, @Nullable Object> effective = new LinkedHashMap<>();
        RuntimeException failure = null;
        try {
            for (Map.Entry<String, @Nullable Object> entry : entries) {
                effective.put(entry.getKey(), mergePackageExtension(
                        (Map<String, @Nullable Object>) effective.get(entry.getKey()), entry.getValue()));
            }
        } catch (JsError e) {
            failure = e;
        }
        RuntimeException mergeFailure = failure;
        Hook merged = mergeFailure == null ? createPackageExtender(effective, release) : null;
        return (manifest, dir) -> {
            Object name = manifest.get("name");
            if (!mergedNames.contains(name instanceof String ? (String) name : null)) {
                return legacy.apply(manifest, dir);
            }
            if (mergeFailure != null) {
                if (release.mergesThePackageExtensionsOfASelector()) {
                    throw mergeFailure;
                }
                return legacy.apply(manifest, dir);
            }
            Map<String, @Nullable Object> viaLegacy = legacy.apply(deepCopy(manifest), dir);
            Map<String, @Nullable Object> viaMerged = merged.apply(deepCopy(manifest), dir);
            return Js.jsonStringify(viaLegacy).equals(Js.jsonStringify(viaMerged)) ||
                   release.mergesThePackageExtensionsOfASelector() ? viaMerged : viaLegacy;
        };
    }

    // createReadPackageHook.ts' mergePackageExtension of 11.7.0: the earlier entry's keys win
    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> mergePackageExtension(@Nullable Map<String, @Nullable Object> previous,
                                                                      @Nullable Object next) {
        if (previous == null) {
            return clonePackageExtension(next);
        }
        Map<String, @Nullable Object> merged = clonePackageExtension(previous);
        for (String field : PACKAGE_EXTENSION_FIELDS) {
            Object nextField = property(next, field);
            if (nextField == null || nextField == Js.UNDEFINED) {
                continue;
            }
            Map<String, @Nullable Object> values = new LinkedHashMap<>();
            spread(values, nextField);
            spread(values, merged.get(field));
            merged.put(field, values);
        }
        return merged;
    }

    private static Map<String, @Nullable Object> clonePackageExtension(@Nullable Object packageExtension) {
        Map<String, @Nullable Object> cloned = new LinkedHashMap<>();
        for (String field : PACKAGE_EXTENSION_FIELDS) {
            Object value = property(packageExtension, field);
            if (value == null || value == Js.UNDEFINED) {
                continue;
            }
            Map<String, @Nullable Object> values = new LinkedHashMap<>();
            spread(values, value);
            cloned.put(field, values);
        }
        return cloned;
    }

    // packageExtension[field]: undefined of anything but null and undefined, which throw
    @SuppressWarnings("unchecked")
    private static @Nullable Object property(@Nullable Object packageExtension, String field) {
        if (packageExtension == null || packageExtension == Js.UNDEFINED) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of " +
                                                  (packageExtension == null ? "null" : "undefined") +
                                                  " (reading '" + field + "')");
        }
        return packageExtension instanceof Map ? ((Map<String, @Nullable Object>) packageExtension).get(field) : null;
    }

    static ReadPackageHook packageExtender(Map<String, @Nullable Object> packageExtensions) {
        return new ReadPackageHook(Collections.singletonList(createPackageExtender(packageExtensions,
                PnpmRelease.PORTED)));
    }

    static ReadPackageHook versionsOverrider(List<ParseOverrides.VersionOverride> overrides, String rootDir) {
        return new ReadPackageHook(Collections.singletonList(createVersionsOverrider(overrides, rootDir,
                PnpmRelease.PORTED)));
    }

    static ReadPackageHook optionalDependenciesRemover(List<String> toBeRemoved) {
        return new ReadPackageHook(Collections.singletonList(createOptionalDependenciesRemover(toBeRemoved)));
    }

    /**
     * The hooks over a copy of the manifest ({@code clone(manifest)}), each over the one before's result.
     */
    Map<String, @Nullable Object> apply(Map<String, @Nullable Object> manifest, @Nullable String dir) {
        Map<String, @Nullable Object> pkg = deepCopy(manifest);
        for (Hook hook : hooks) {
            pkg = hook.apply(pkg, dir);
        }
        return pkg;
    }

    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> deepCopy(Map<String, @Nullable Object> manifest) {
        return (Map<String, @Nullable Object>) copy(manifest);
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Object copy(@Nullable Object value) {
        if (value instanceof Map) {
            Map<String, @Nullable Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, @Nullable Object> entry : ((Map<String, @Nullable Object>) value).entrySet()) {
                out.put(entry.getKey(), copy(entry.getValue()));
            }
            return out;
        }
        if (value instanceof List) {
            List<@Nullable Object> out = new ArrayList<>();
            for (Object item : (List<?>) value) {
                out.add(copy(item));
            }
            return out;
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static synchronized Map<String, @Nullable Object> compatPackageExtensions() {
        if (compatPackageExtensions == null) {
            Map<String, @Nullable Object> extensions = new LinkedHashMap<>();
            for (Map.Entry<String, @Nullable Object> entry : compatPackageExtensionEntries()) {
                extensions.put(entry.getKey(), entry.getValue());
            }
            compatPackageExtensions = extensions;
        }
        return compatPackageExtensions;
    }

    /**
     * The compatibility database's entries as @yarnpkg/extensions lists them, a selector more than once among them.
     */
    @SuppressWarnings("unchecked")
    static synchronized List<Map.Entry<String, @Nullable Object>> compatPackageExtensionEntries() {
        if (compatPackageExtensionEntries == null) {
            List<Map.Entry<String, @Nullable Object>> list = new ArrayList<>();
            Object entries = Manifests.parseValue(readResource(), COMPAT_DB);
            for (Object entry : (List<Object>) entries) {
                List<Object> pair = (List<Object>) entry;
                list.add(new AbstractMap.SimpleImmutableEntry<>((String) pair.get(0), pair.get(1)));
            }
            compatPackageExtensionEntries = Collections.unmodifiableList(list);
        }
        return compatPackageExtensionEntries;
    }

    private static String readResource() {
        try (InputStream raw = ReadPackageHook.class.getResourceAsStream(COMPAT_DB)) {
            if (raw == null) {
                throw new IllegalStateException(COMPAT_DB + " is missing from the classpath");
            }
            try (InputStream in = new GZIPInputStream(raw)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    out.write(buf, 0, n);
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Value
    private static class PackageExtension {
        @Nullable
        Object extension;

        @Nullable
        String range;
    }

    // createPackageExtender: extensions by package name, each with its range
    @SuppressWarnings("unchecked")
    private static Hook createPackageExtender(Map<String, @Nullable Object> packageExtensions, PnpmRelease release) {
        Map<@Nullable String, List<PackageExtension>> extensionsByPkgName = new LinkedHashMap<>();
        for (String selector : Js.keys(packageExtensions)) {
            Object packageExtension = packageExtensions.get(selector);
            ParseOverrides.ParsedWantedDependency wanted = ParseOverrides.parseWantedDependency(selector);
            extensionsByPkgName.computeIfAbsent(wanted.getAlias(), a -> new ArrayList<>())
                    .add(new PackageExtension(packageExtension, wanted.getBareSpecifier()));
        }
        return (manifest, dir) -> {
            Object name = manifest.get("name");
            List<PackageExtension> extensions = extensionsByPkgName.get(name instanceof String ? (String) name :
                    null);
            if (extensions == null) {
                return manifest;
            }
            for (PackageExtension extension : extensions) {
                String range = extension.getRange();
                if (range != null && !release.underSemver(semver -> NodeSemver.satisfies(manifest.get("version"),
                        range, NodeSemver.Options.DEFAULT.release(semver)))) {
                    continue;
                }
                if (!(extension.getExtension() instanceof Map)) {
                    continue;
                }
                Map<String, @Nullable Object> packageExtension =
                        (Map<String, @Nullable Object>) extension.getExtension();
                for (String field : new String[]{"dependencies", "optionalDependencies", "peerDependencies",
                        "peerDependenciesMeta"}) {
                    Object fromExtension = packageExtension.get(field);
                    if (!Js.truthy(fromExtension)) {
                        continue;
                    }
                    Map<String, @Nullable Object> merged = new LinkedHashMap<>();
                    spread(merged, fromExtension);
                    spread(merged, manifest.get(field));
                    manifest.put(field, Js.ordered(merged));
                }
            }
            return manifest;
        };
    }

    @SuppressWarnings("unchecked")
    private static void spread(Map<String, @Nullable Object> into, @Nullable Object value) {
        if (value instanceof Map) {
            for (String key : Js.keys((Map<String, @Nullable Object>) value)) {
                into.put(key, ((Map<String, @Nullable Object>) value).get(key));
            }
        } else if (value instanceof String) {
            for (int i = 0; i < ((String) value).length(); i++) {
                into.put(String.valueOf(i), String.valueOf(((String) value).charAt(i)));
            }
        } else if (value instanceof List) {
            for (int i = 0; i < ((List<?>) value).size(); i++) {
                into.put(String.valueOf(i), ((List<?>) value).get(i));
            }
        }
    }

    private static final class VersionOverride {
        final ParseOverrides.VersionOverride override;
        final @Nullable String protocol;
        final @Nullable String targetPath;
        final boolean specifiedViaRelativePath;

        VersionOverride(ParseOverrides.VersionOverride override, String rootDir) {
            this.override = override;
            String spec = override.getNewBareSpecifier();
            if (spec.startsWith("file:")) {
                protocol = "file:";
            } else if (spec.startsWith("link:")) {
                protocol = "link:";
            } else {
                protocol = null;
            }
            if (protocol != null) {
                String pkgPath = spec.substring(protocol.length());
                specifiedViaRelativePath = !pkgPath.startsWith("/");
                targetPath = specifiedViaRelativePath ? RootRelativePath.join(rootDir, pkgPath) : pkgPath;
            } else {
                specifiedViaRelativePath = false;
                targetPath = null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Hook createVersionsOverrider(List<ParseOverrides.VersionOverride> overrides, String rootDir,
                                                PnpmRelease release) {
        List<VersionOverride> versionOverrides = new ArrayList<>();
        List<VersionOverride> genericVersionOverrides = new ArrayList<>();
        // 11.13.0's convergence overrides, by package name; only a release from 11.13.0 marks any
        // (PnpmRelease#convergesAnEmptyRangeOverride)
        Map<String, String> convergeVersions = new LinkedHashMap<>();
        for (ParseOverrides.VersionOverride override : overrides) {
            if (override.isConverge()) {
                convergeVersions.put(override.getTargetPkg().getName(), override.getNewBareSpecifier());
                continue;
            }
            VersionOverride withTarget = new VersionOverride(override, rootDir);
            if (override.getParentPkg() != null) {
                versionOverrides.add(withTarget);
            } else {
                genericVersionOverrides.add(withTarget);
            }
        }
        return (manifest, dir) -> {
            List<VersionOverride> versionOverridesWithParent = new ArrayList<>();
            for (VersionOverride override : versionOverrides) {
                ParseOverrides.PackageSelector parentPkg = override.override.getParentPkg();
                if (parentPkg.getName().equals(manifest.get("name")) &&
                    (!Js.truthy(parentPkg.getBareSpecifier()) ||
                     release.underSemver(semver -> NodeSemver.satisfies(manifest.get("version"),
                             parentPkg.getBareSpecifier(), NodeSemver.Options.DEFAULT.release(semver))))) {
                    versionOverridesWithParent.add(override);
                }
            }
            Object dependencies = manifest.get("dependencies");
            Object optionalDependencies = manifest.get("optionalDependencies");
            Object devDependencies = manifest.get("devDependencies");
            Object peerDependencies = manifest.get("peerDependencies");
            for (Object deps : new Object[]{dependencies, optionalDependencies, devDependencies}) {
                if (Js.truthy(deps)) {
                    overrideDeps(versionOverridesWithParent, genericVersionOverrides, convergeVersions, dir,
                            (Map<String, @Nullable Object>) deps, null, release);
                }
            }
            if (Js.truthy(peerDependencies)) {
                if (!Js.truthy(manifest.get("dependencies"))) {
                    manifest.put("dependencies", new LinkedHashMap<>());
                }
                overrideDeps(versionOverridesWithParent, genericVersionOverrides, convergeVersions, dir,
                        (Map<String, @Nullable Object>) manifest.get("dependencies"),
                        (Map<String, @Nullable Object>) peerDependencies, release);
            }
            return manifest;
        };
    }

    private static void overrideDeps(List<VersionOverride> versionOverrides,
                                     List<VersionOverride> genericVersionOverrides,
                                     Map<String, String> convergeVersions,
                                     @Nullable String dir, Map<String, @Nullable Object> deps,
                                     @Nullable Map<String, @Nullable Object> peerDeps, PnpmRelease release) {
        Map<String, @Nullable Object> source = peerDeps != null ? peerDeps : deps;
        for (String name : new ArrayList<>(Js.keys(source))) {
            String bareSpecifier = Js.string(source.get(name));
            VersionOverride versionOverride = pickMostSpecificVersionOverride(
                    matching(versionOverrides, name, bareSpecifier, release), release);
            if (versionOverride == null) {
                versionOverride = pickMostSpecificVersionOverride(
                        matching(genericVersionOverrides, name, bareSpecifier, release), release);
            }
            if (versionOverride == null) {
                convergeDep(convergeVersions, deps, peerDeps, name, source.get(name), release);
                continue;
            }
            String targetName = versionOverride.override.getTargetPkg().getName();
            if ("-".equals(versionOverride.override.getNewBareSpecifier())) {
                if (peerDeps != null) {
                    peerDeps.remove(targetName);
                } else {
                    deps.remove(targetName);
                }
                continue;
            }
            String newBareSpecifier = versionOverride.protocol != null ?
                    versionOverride.protocol + resolveLocalOverride(versionOverride, dir) :
                    versionOverride.override.getNewBareSpecifier();
            if (peerDeps == null || !isValidPeerRange(newBareSpecifier, release)) {
                deps.put(targetName, newBareSpecifier);
            } else {
                peerDeps.put(targetName, newBareSpecifier);
            }
        }
    }

    // convergeDep (11.13.0): where no other override matched, a convergence override's exact version replaces a
    // declared range (loose) it satisfies, a peer's in the peers; the ranges it records serve only a warning
    private static void convergeDep(Map<String, String> convergeVersions, Map<String, @Nullable Object> deps,
                                    @Nullable Map<String, @Nullable Object> peerDeps, String name,
                                    @Nullable Object bareSpecifier, PnpmRelease release) {
        String convergeVersion = convergeVersions.get(name);
        if (convergeVersion == null || !(bareSpecifier instanceof String) ||
            release.underSemver(semver -> NodeSemver.validRange((String) bareSpecifier,
                    NodeSemver.Options.LOOSE.release(semver))) == null) {
            return;
        }
        if (!release.underSemver(semver -> NodeSemver.satisfies(convergeVersion, (String) bareSpecifier,
                NodeSemver.Options.LOOSE.release(semver)))) {
            return;
        }
        (peerDeps == null ? deps : peerDeps).put(name, convergeVersion);
    }

    private static List<VersionOverride> matching(List<VersionOverride> overrides, String name, String bareSpecifier,
                                                  PnpmRelease release) {
        List<VersionOverride> out = new ArrayList<>();
        for (VersionOverride override : overrides) {
            ParseOverrides.PackageSelector target = override.override.getTargetPkg();
            if (target.getName().equals(name) &&
                isIntersectingRange(target.getBareSpecifier(), bareSpecifier, release)) {
                out.add(override);
            }
        }
        return out;
    }

    private static String resolveLocalOverride(VersionOverride target, @Nullable String pkgDir) {
        return target.specifiedViaRelativePath && pkgDir != null ?
                RootRelativePath.relative(pkgDir, target.targetPath) : String.valueOf(target.targetPath);
    }

    private static @Nullable VersionOverride pickMostSpecificVersionOverride(List<VersionOverride> versionOverrides,
                                                                            PnpmRelease release) {
        V8Sort.sort(versionOverrides, (a, b) -> isIntersectingRange(
                orEmpty(b.override.getTargetPkg().getBareSpecifier()),
                orEmpty(a.override.getTargetPkg().getBareSpecifier()), release) ? -1 : 1);
        return versionOverrides.isEmpty() ? null : versionOverrides.get(0);
    }

    private static String orEmpty(@Nullable String s) {
        return s == null ? "" : s;
    }

    // semver's range functions under the node-semver the release bundles
    static boolean isIntersectingRange(@Nullable String range1, String range2, PnpmRelease release) {
        return range1 == null || range1.isEmpty() || range2.equals(range1) ||
               release.underSemver(semver -> NodeSemver.validRange(range2,
                       NodeSemver.Options.DEFAULT.release(semver)) != null &&
                                             NodeSemver.validRange(range1,
                                                     NodeSemver.Options.DEFAULT.release(semver)) != null &&
                                             NodeSemver.intersects(range2, range1,
                                                     NodeSemver.Options.DEFAULT.release(semver)));
    }

    /**
     * @pnpm/deps.peer-range {@code isValidPeerRange}.
     */
    static boolean isValidPeerRange(String version, PnpmRelease release) {
        return release.underSemver(semver -> NodeSemver.validRange(version,
                NodeSemver.Options.DEFAULT.release(semver))) != null || version.contains("workspace:") ||
               version.contains("catalog:");
    }

    /**
     * @pnpm/deps.peer-range {@code isAcceptablePeerSpec} (11.14.0): a peer range, or a dependency specifier.
     */
    static boolean isAcceptablePeerSpec(String version, PnpmRelease release) {
        return isValidPeerRange(version, release) || version.contains(":");
    }

    /**
     * @pnpm/deps.peer-range {@code getPeerVersionRange} (11.14.0): the range a peer spec gives, each of its
     * {@code ||} parts read apart: a peer range less its {@code workspace:} prefix, the range after a specifier's
     * protocol ({@code <registry>:^1}) or after the last {@code @} of what follows it ({@code npm:x@^1}), else any.
     */
    static String getPeerVersionRange(String version, PnpmRelease release) {
        if (version.contains("||")) {
            List<String> parts = new ArrayList<>();
            for (String part : version.split("\\|\\|", -1)) {
                parts.add(getPeerVersionRange(Js.trim(part), release));
            }
            return String.join(" || ", parts);
        }
        if (isValidPeerRange(version, release)) {
            return version.startsWith("workspace:") ? version.substring("workspace:".length()) : version;
        }
        int colon = version.indexOf(':');
        if (colon > 0) {
            String body = version.substring(colon + 1);
            if (release.underSemver(semver -> NodeSemver.validRange(body,
                    NodeSemver.Options.DEFAULT.release(semver))) != null) {
                return body;
            }
            int at = body.lastIndexOf('@');
            if (at > 0 && release.underSemver(semver -> NodeSemver.validRange(body.substring(at + 1),
                    NodeSemver.Options.DEFAULT.release(semver))) != null) {
                return body.substring(at + 1);
            }
        }
        return "*";
    }

    @SuppressWarnings("unchecked")
    private static Hook createOptionalDependenciesRemover(List<String> toBeRemoved) {
        if (toBeRemoved.isEmpty()) {
            return (manifest, dir) -> manifest;
        }
        Predicate<String> shouldBeRemoved = Matcher.createMatcher(toBeRemoved);
        return (manifest, dir) -> {
            Object optionalDependencies = manifest.get("optionalDependencies");
            if (!(optionalDependencies instanceof Map)) {
                return manifest;
            }
            Map<String, @Nullable Object> optional = (Map<String, @Nullable Object>) optionalDependencies;
            for (String optionalDependency : new ArrayList<>(Js.keys(optional))) {
                if (shouldBeRemoved.test(optionalDependency)) {
                    optional.remove(optionalDependency);
                    Object dependencies = manifest.get("dependencies");
                    if (dependencies instanceof Map) {
                        ((Map<String, @Nullable Object>) dependencies).remove(optionalDependency);
                    }
                }
            }
            return manifest;
        };
    }
}
