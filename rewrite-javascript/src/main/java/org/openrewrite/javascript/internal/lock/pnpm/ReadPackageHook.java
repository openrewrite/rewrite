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
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.V8Sort;
import org.openrewrite.javascript.semver.NodeSemver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;

/**
 * Port of @pnpm/hooks.read-package-hook (pnpm 11.2.2), with @pnpm/deps.peer-range.
 */
final class ReadPackageHook {

    private static final String COMPAT_DB = "compat-db.json.gz";
    private static @Nullable Map<String, @Nullable Object> compatPackageExtensions;

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
        List<Hook> hooks = new ArrayList<>();
        if (!ignoreCompatibilityDb) {
            hooks.add(createPackageExtender(compatPackageExtensions()));
        }
        if (packageExtensions instanceof Map && !((Map<?, ?>) packageExtensions).isEmpty()) {
            @SuppressWarnings("unchecked")
            Map<String, @Nullable Object> extensions = (Map<String, @Nullable Object>) packageExtensions;
            hooks.add(createPackageExtender(extensions));
        }
        if (!overrides.isEmpty()) {
            hooks.add(createVersionsOverrider(overrides, lockfileDir));
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

    static ReadPackageHook packageExtender(Map<String, @Nullable Object> packageExtensions) {
        return new ReadPackageHook(Collections.singletonList(createPackageExtender(packageExtensions)));
    }

    static ReadPackageHook versionsOverrider(List<ParseOverrides.VersionOverride> overrides, String rootDir) {
        return new ReadPackageHook(Collections.singletonList(createVersionsOverrider(overrides, rootDir)));
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
            Object entries = Manifests.parseValue(readResource(), COMPAT_DB);
            for (Object entry : (List<Object>) entries) {
                List<Object> pair = (List<Object>) entry;
                extensions.put((String) pair.get(0), pair.get(1));
            }
            compatPackageExtensions = extensions;
        }
        return compatPackageExtensions;
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
    private static Hook createPackageExtender(Map<String, @Nullable Object> packageExtensions) {
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
                if (range != null &&
                    !NodeSemver.satisfies(manifest.get("version"), range, NodeSemver.Options.DEFAULT)) {
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
    private static Hook createVersionsOverrider(List<ParseOverrides.VersionOverride> overrides, String rootDir) {
        List<VersionOverride> versionOverrides = new ArrayList<>();
        List<VersionOverride> genericVersionOverrides = new ArrayList<>();
        for (ParseOverrides.VersionOverride override : overrides) {
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
                     NodeSemver.satisfies(manifest.get("version"), parentPkg.getBareSpecifier(),
                             NodeSemver.Options.DEFAULT))) {
                    versionOverridesWithParent.add(override);
                }
            }
            Object dependencies = manifest.get("dependencies");
            Object optionalDependencies = manifest.get("optionalDependencies");
            Object devDependencies = manifest.get("devDependencies");
            Object peerDependencies = manifest.get("peerDependencies");
            for (Object deps : new Object[]{dependencies, optionalDependencies, devDependencies}) {
                if (Js.truthy(deps)) {
                    overrideDeps(versionOverridesWithParent, genericVersionOverrides, dir,
                            (Map<String, @Nullable Object>) deps, null);
                }
            }
            if (Js.truthy(peerDependencies)) {
                if (!Js.truthy(manifest.get("dependencies"))) {
                    manifest.put("dependencies", new LinkedHashMap<>());
                }
                overrideDeps(versionOverridesWithParent, genericVersionOverrides, dir,
                        (Map<String, @Nullable Object>) manifest.get("dependencies"),
                        (Map<String, @Nullable Object>) peerDependencies);
            }
            return manifest;
        };
    }

    private static void overrideDeps(List<VersionOverride> versionOverrides,
                                     List<VersionOverride> genericVersionOverrides,
                                     @Nullable String dir, Map<String, @Nullable Object> deps,
                                     @Nullable Map<String, @Nullable Object> peerDeps) {
        Map<String, @Nullable Object> source = peerDeps != null ? peerDeps : deps;
        for (String name : new ArrayList<>(Js.keys(source))) {
            String bareSpecifier = Js.string(source.get(name));
            VersionOverride versionOverride = pickMostSpecificVersionOverride(
                    matching(versionOverrides, name, bareSpecifier));
            if (versionOverride == null) {
                versionOverride = pickMostSpecificVersionOverride(
                        matching(genericVersionOverrides, name, bareSpecifier));
            }
            if (versionOverride == null) {
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
            if (peerDeps == null || !isValidPeerRange(newBareSpecifier)) {
                deps.put(targetName, newBareSpecifier);
            } else {
                peerDeps.put(targetName, newBareSpecifier);
            }
        }
    }

    private static List<VersionOverride> matching(List<VersionOverride> overrides, String name, String bareSpecifier) {
        List<VersionOverride> out = new ArrayList<>();
        for (VersionOverride override : overrides) {
            ParseOverrides.PackageSelector target = override.override.getTargetPkg();
            if (target.getName().equals(name) && isIntersectingRange(target.getBareSpecifier(), bareSpecifier)) {
                out.add(override);
            }
        }
        return out;
    }

    private static String resolveLocalOverride(VersionOverride target, @Nullable String pkgDir) {
        return target.specifiedViaRelativePath && pkgDir != null ?
                RootRelativePath.relative(pkgDir, target.targetPath) : String.valueOf(target.targetPath);
    }

    private static @Nullable VersionOverride pickMostSpecificVersionOverride(List<VersionOverride> versionOverrides) {
        V8Sort.sort(versionOverrides, (a, b) -> isIntersectingRange(
                orEmpty(b.override.getTargetPkg().getBareSpecifier()),
                orEmpty(a.override.getTargetPkg().getBareSpecifier())) ? -1 : 1);
        return versionOverrides.isEmpty() ? null : versionOverrides.get(0);
    }

    private static String orEmpty(@Nullable String s) {
        return s == null ? "" : s;
    }

    static boolean isIntersectingRange(@Nullable String range1, String range2) {
        return range1 == null || range1.isEmpty() || range2.equals(range1) ||
               NodeSemver.validRange(range2, NodeSemver.Options.DEFAULT) != null &&
               NodeSemver.validRange(range1, NodeSemver.Options.DEFAULT) != null &&
               NodeSemver.intersects(range2, range1, NodeSemver.Options.DEFAULT);
    }

    /**
     * @pnpm/deps.peer-range {@code isValidPeerRange}.
     */
    static boolean isValidPeerRange(String version) {
        return NodeSemver.validRange(version, NodeSemver.Options.DEFAULT) != null || version.contains("workspace:") ||
               version.contains("catalog:");
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
