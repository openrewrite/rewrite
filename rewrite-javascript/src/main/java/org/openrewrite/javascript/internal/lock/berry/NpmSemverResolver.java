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
package org.openrewrite.javascript.internal.lock.berry;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static org.openrewrite.javascript.internal.lock.berry.NpmHttpUtils.PROTOCOL;

/**
 * Port of @yarnpkg/plugin-npm sources/NpmSemverResolver.ts (Yarn 4.10.3), with each other release's change beside it
 * ({@link YarnRelease}).
 */
final class NpmSemverResolver implements Resolver {

    private static final Ident NODE_GYP_IDENT = StructUtils.makeIdent(null, "node-gyp");

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        if (!descriptor.range.startsWith(PROTOCOL)) {
            return false;
        }
        return YarnpkgSemverUtils.validRange(descriptor.range.substring(PROTOCOL.length())) != null;
    }

    @Override
    public boolean supportsLocator(Locator locator, ResolveOptions opts) {
        if (!locator.reference.startsWith(PROTOCOL)) {
            return false;
        }
        return YarnpkgSemverUtils.valid(StructUtils.parseRange(locator.reference).getSelector());
    }

    @Override
    public boolean shouldPersistResolution(Locator locator, ResolveOptions opts) {
        return true;
    }

    @Override
    public Descriptor bindDescriptor(Descriptor descriptor, Locator fromLocator, ResolveOptions opts) {
        return descriptor;
    }

    @Override
    public Map<String, Descriptor> getResolutionDependencies(Descriptor descriptor, ResolveOptions opts) {
        return Collections.emptyMap();
    }

    @Override
    public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies,
                                       ResolveOptions opts) {
        YarnpkgSemverUtils.Range range = YarnpkgSemverUtils.validRange(descriptor.range.substring(PROTOCOL.length()));
        if (range == null) {
            throw new JsError(JsError.ERROR, "Expected a valid range, got " +
                                             descriptor.range.substring(PROTOCOL.length()));
        }
        Project project = opts.getProject();
        YarnRelease release = project.configuration.getRelease();
        Map<String, @Nullable Object> registryData = NpmHttpUtils.getPackageMetadata(descriptor, project,
                opts.getRegistry());
        Object versions = registryData.get("versions");
        List<String> candidates = new ArrayList<>();
        if (release.approvesCandidatesAfterTheRange()) {
            List<String> semverCandidates = selectMatchingVersions(range, Js.keys(versions), () -> release);
            for (String version : semverCandidates) {
                if (NpmHttpUtils.isPackageApproved(project, descriptor, version, registryData.get("time"))) {
                    candidates.add(version);
                }
            }
            if (!semverCandidates.isEmpty() && candidates.isEmpty()) {
                throw new ReportError(ReportError.REMOTE_NOT_FOUND, "All versions satisfying \"" +
                                                                    descriptor.range.substring(PROTOCOL.length()) +
                                                                    "\" are quarantined");
            }
        } else {
            for (Map.Entry<String, @Nullable Object> version : Js.entries(versions)) {
                try {
                    if (YarnpkgSemverUtils.valid(version.getKey()) && range.test(version.getKey()) &&
                        (!release.approvesPackagesByAge() || NpmHttpUtils.isPackageApproved(project, descriptor,
                                version.getKey(), registryData.get("time")))) {
                        candidates.add(version.getKey());
                    }
                } catch (JsError ignored) {
                    // Yarn's candidate filter skips a version whose test throws
                }
            }
        }
        List<String> noDeprecatedCandidates = new ArrayList<>();
        for (String version : candidates) {
            if (!Js.truthy(Js.get(Js.get(versions, version), "deprecated"))) {
                noDeprecatedCandidates.add(version);
            }
        }
        List<String> finalCandidates = noDeprecatedCandidates.isEmpty() ? candidates : noDeprecatedCandidates;
        finalCandidates.sort((a, b) -> -YarnpkgSemverUtils.compare(a, b));
        List<Locator> out = new ArrayList<>();
        for (String version : finalCandidates) {
            Locator versionLocator = StructUtils.makeLocator(descriptor, PROTOCOL + version);
            Object archiveUrl = Js.read(Js.read(Js.get(versions, version), "dist"), "tarball");
            if (NpmHttpUtils.isConventionalTarballUrl(versionLocator, archiveUrl, project.configuration)) {
                out.add(versionLocator);
            } else {
                out.add(StructUtils.bindLocator(versionLocator,
                        Collections.singletonMap("__archiveUrl", Js.string(archiveUrl))));
            }
        }
        return out;
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        YarnpkgSemverUtils.Range range = YarnpkgSemverUtils.validRange(descriptor.range.substring(PROTOCOL.length()));
        if (range == null) {
            throw new JsError(JsError.ERROR, "Expected a valid range, got " +
                                             descriptor.range.substring(PROTOCOL.length()));
        }
        List<Locator> results = new ArrayList<>();
        List<String> versions = new ArrayList<>();
        for (Locator locator : locators) {
            if (!locator.identHash.equals(descriptor.identHash)) {
                continue;
            }
            StructUtils.Range parsedRange = StructUtils.tryParseRange(locator.reference,
                    new StructUtils.RangeOptions(false, PROTOCOL, false, false));
            if (parsedRange == null) {
                continue;
            }
            if (!YarnpkgSemverUtils.valid(parsedRange.getSelector())) {
                throw new JsError(JsError.TYPE_ERROR, "Invalid Version: " + parsedRange.getSelector());
            }
            results.add(locator);
            versions.add(parsedRange.getSelector());
        }
        // through selectMatchingVersions as getCandidates from 4.18.0, the range alone before
        Set<String> matching = new HashSet<>(selectMatchingVersions(range, versions,
                () -> opts.getProject().configuration.getRelease()));
        for (int i = results.size() - 1; i >= 0; i--) {
            if (!matching.contains(versions.get(i))) {
                results.remove(i);
                versions.remove(i);
            }
        }
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            order.add(i);
        }
        order.sort((a, b) -> -YarnpkgSemverUtils.compare(versions.get(a), versions.get(b)));
        List<Locator> sorted = new ArrayList<>();
        for (int index : order) {
            sorted.add(results.get(index));
        }
        return new Satisfying(sorted, true);
    }

    /**
     * {@code selectMatchingVersions} (Yarn 4.18.0): the versions the range matches, or where a {@code *} range matches
     * none, the prereleases; before 4.18.0 the range alone.
     */
    private static List<String> selectMatchingVersions(YarnpkgSemverUtils.Range range, List<String> versions,
                                                       Supplier<YarnRelease> release) {
        List<String> matched = new ArrayList<>();
        for (String version : versions) {
            if (YarnpkgSemverUtils.valid(version) && range.test(version)) {
                matched.add(version);
            }
        }
        if (!matched.isEmpty() || !"*".equals(range.getRaw().trim()) || !release.get().resolvesStarToPrereleases()) {
            return matched;
        }
        for (String version : versions) {
            if (YarnpkgSemverUtils.valid(version) &&
                NodeSemver.satisfies(version, "*", NodeSemver.Options.INCLUDE_PRERELEASE)) {
                matched.add(version);
            }
        }
        return matched;
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        String selector = StructUtils.parseRange(locator.reference).getSelector();
        String version = YarnpkgSemverUtils.clean(selector);
        if (version == null) {
            throw new ReportError(ReportError.RESOLVER_NOT_FOUND, "The npm semver resolver got selected, but the " +
                                                                  "version isn't semver");
        }
        Project project = opts.getProject();
        Map<String, @Nullable Object> registryData = NpmHttpUtils.getPackageMetadata(locator, project,
                opts.getRegistry());
        if (!registryData.containsKey("versions") || registryData.get("versions") == Syml.UNDEFINED) {
            throw new ReportError(ReportError.REMOTE_INVALID, "Registry returned invalid data for - missing " +
                                                              "\"versions\" field");
        }
        Object versionData = Js.get(registryData.get("versions"), version);
        if (versionData == Syml.UNDEFINED) {
            throw new ReportError(ReportError.REMOTE_NOT_FOUND, "Registry failed to return reference \"" + version +
                                                                "\"");
        }
        return fromManifest(locator, version, versionData, project.configuration);
    }

    /**
     * The package a registry version document makes, the node-gyp rule of the release applied.
     */
    static Package fromManifest(Locator locator, String version, @Nullable Object versionData,
                                Configuration configuration) {
        Manifest manifest = new Manifest();
        manifest.load(versionData, false);
        YarnRelease release = configuration.getRelease();
        if (!manifest.dependencies.containsKey(NODE_GYP_IDENT.identHash) &&
            !manifest.peerDependencies.containsKey(NODE_GYP_IDENT.identHash)) {
            for (String value : manifest.scripts.values()) {
                if (release.getNodeGypMatch().matcher(value).find()) {
                    manifest.dependencies.put(NODE_GYP_IDENT.identHash,
                            StructUtils.makeDescriptor(NODE_GYP_IDENT, release.getNodeGypRange()));
                    break;
                }
            }
        }
        return new Package(locator, version, "node", Package.HARD, manifest.getConditions(),
                configuration.normalizeDependencyMap(manifest.dependencies), manifest.peerDependencies,
                manifest.dependenciesMeta, manifest.peerDependenciesMeta, manifest.bin);
    }
}
