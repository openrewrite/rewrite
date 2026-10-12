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
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.berry.NpmHttpUtils.PROTOCOL;

/**
 * Port of @yarnpkg/plugin-npm sources/NpmTagResolver.ts (Yarn 4.10.3).
 */
final class NpmTagResolver implements Resolver {

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        if (!descriptor.range.startsWith(PROTOCOL)) {
            return false;
        }
        return Configuration.TAG_REGEXP.matcher(descriptor.range.substring(PROTOCOL.length())).find();
    }

    @Override
    public boolean supportsLocator(Locator locator, ResolveOptions opts) {
        return false;
    }

    @Override
    public boolean shouldPersistResolution(Locator locator, ResolveOptions opts) {
        throw new JsError(JsError.ERROR, "Unreachable");
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
        String tag = descriptor.range.substring(PROTOCOL.length());
        Project project = opts.getProject();
        Map<String, @Nullable Object> registryData = NpmHttpUtils.getPackageMetadata(descriptor, project,
                opts.getRegistry());
        if (!registryData.containsKey("dist-tags")) {
            throw new ReportError(ReportError.REMOTE_INVALID, "Registry returned invalid data - missing " +
                                                              "\"dist-tags\" field");
        }
        Object distTags = registryData.get("dist-tags");
        if (distTags == null || distTags == Syml.UNDEFINED) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot convert undefined or null to object");
        }
        Object version = Js.get(distTags, tag);
        if (version == Syml.UNDEFINED) {
            throw new ReportError(ReportError.REMOTE_NOT_FOUND, "Registry failed to return tag \"" + tag + "\"");
        }
        Object times = registryData.get("time");
        if ("latest".equals(tag) && project.configuration.getRelease().approvesPackagesByAge() &&
            !NpmHttpUtils.isPackageApproved(project, descriptor, version, times)) {
            boolean skipsPrereleases = project.configuration.getRelease().skipsPrereleasesBelowLatest();
            List<String> versions = new ArrayList<>();
            for (Map.Entry<String, @Nullable Object> entry : Js.entries(registryData.get("versions"))) {
                versions.add(entry.getKey());
            }
            if (!(version instanceof String)) {
                // 4.10.3 asks version.includes first; before, semver.lt the first candidate it compares
                if (skipsPrereleases || !versions.isEmpty()) {
                    throw new JsError(JsError.TYPE_ERROR, skipsPrereleases ? "version.includes is not a function" :
                            "Invalid version. Must be a string. Got type \"" + Js.typeOf(version) + "\".");
                }
                version = "";
            }
            String latest = (String) version;
            boolean toleratePrereleases = !skipsPrereleases || latest.contains("-");
            String nextVersion = null;
            try {
                NodeSemver.rsort(versions, NodeSemver.Options.DEFAULT);
                for (String candidateVersion : versions) {
                    if (NodeSemver.compare(candidateVersion, latest, NodeSemver.Options.DEFAULT) < 0 &&
                        (toleratePrereleases || !candidateVersion.contains("-")) &&
                        NpmHttpUtils.isPackageApproved(project, descriptor, candidateVersion, times)) {
                        nextVersion = candidateVersion;
                        break;
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new JsError(JsError.TYPE_ERROR, e.getMessage());
            }
            if (nextVersion == null) {
                throw new ReportError(ReportError.REMOTE_NOT_FOUND, "The version for tag \"" + tag + "\" is " +
                                                                    "quarantined, and no lower version is available");
            }
            version = nextVersion;
        }
        Locator versionLocator = StructUtils.makeLocator(descriptor, PROTOCOL + Js.string(version));
        Object archiveUrl = Js.read(Js.read(Js.read(registryData.get("versions"), Js.string(version)), "dist"),
                "tarball");
        if (NpmHttpUtils.isConventionalTarballUrl(versionLocator, archiveUrl, project.configuration)) {
            return Collections.singletonList(versionLocator);
        }
        return Collections.singletonList(StructUtils.bindLocator(versionLocator,
                Collections.singletonMap("__archiveUrl", Js.string(archiveUrl))));
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        List<Locator> filtered = new ArrayList<>();
        for (Locator locator : locators) {
            if (!locator.identHash.equals(descriptor.identHash)) {
                continue;
            }
            StructUtils.Range parsedRange = StructUtils.tryParseRange(locator.reference,
                    new StructUtils.RangeOptions(false, PROTOCOL, false, false));
            if (parsedRange == null || !YarnpkgSemverUtils.valid(parsedRange.getSelector())) {
                continue;
            }
            if (parsedRange.getParams() != null && Js.truthy(parsedRange.getParams().get("__archiveUrl"))) {
                String newRange = StructUtils.makeRange(PROTOCOL, null, parsedRange.getSelector(), null);
                Locator resolvedLocator = opts.getResolver().getCandidates(
                        StructUtils.makeDescriptor(descriptor, newRange), dependencies, opts).get(0);
                if (!locator.reference.equals(resolvedLocator.reference)) {
                    continue;
                }
            }
            filtered.add(locator);
        }
        return new Satisfying(filtered, false);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        throw new JsError(JsError.ERROR, "Unreachable");
    }
}
