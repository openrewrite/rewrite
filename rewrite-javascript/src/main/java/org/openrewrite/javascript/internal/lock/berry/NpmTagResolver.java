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
        throw new IllegalStateException("Unreachable");
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
        Object distTags = registryData.get("dist-tags");
        if (distTags == Syml.UNDEFINED) {
            throw new ReportError(ReportError.REMOTE_INVALID, "Registry returned invalid data - missing " +
                                                              "\"dist-tags\" field");
        }
        Object version = Js.get(distTags, tag);
        if (version == Syml.UNDEFINED) {
            throw new ReportError(ReportError.REMOTE_NOT_FOUND, "Registry failed to return tag \"" + tag + "\"");
        }
        if ("latest".equals(tag)) {
            NpmHttpUtils.isPackageApproved(project.configuration.getOptions());
        }
        Locator versionLocator = StructUtils.makeLocator(descriptor, PROTOCOL + Js.string(version));
        Object archiveUrl = Js.read(Js.read(Js.read(registryData.get("versions"), Js.string(version)), "dist"),
                "tarball");
        if (NpmHttpUtils.isConventionalTarballUrl(versionLocator, archiveUrl, project.configuration.getOptions())) {
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
            if (parsedRange == null || !SemverUtils.valid(parsedRange.getSelector())) {
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
        throw new IllegalStateException("Unreachable");
    }
}
