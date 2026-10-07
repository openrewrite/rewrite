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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of @yarnpkg/core sources/WorkspaceResolver.ts (Yarn 4.10.3).
 */
final class WorkspaceResolver implements Resolver {

    static final String PROTOCOL = "workspace:";

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        if (descriptor.range.startsWith(PROTOCOL)) {
            return true;
        }
        return opts.getProject().tryWorkspaceByDescriptor(descriptor) != null;
    }

    @Override
    public boolean supportsLocator(Locator locator, ResolveOptions opts) {
        return locator.reference.startsWith(PROTOCOL);
    }

    @Override
    public boolean shouldPersistResolution(Locator locator, ResolveOptions opts) {
        return false;
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
        Workspace workspace = opts.getProject().getWorkspaceByDescriptor(descriptor);
        return Collections.singletonList(workspace.getAnchoredLocator());
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        Locator locator = getCandidates(descriptor, dependencies, opts).get(0);
        List<Locator> out = new ArrayList<>();
        for (Locator candidate : locators) {
            if (candidate.locatorHash.equals(locator.locatorHash)) {
                out.add(candidate);
            }
        }
        return new Satisfying(out, false);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        return resolve(locator, opts.getProject());
    }

    /**
     * The workspace's package: dependencies and devDependencies in one map (devDependencies win), normalized.
     */
    static Package resolve(Locator locator, Project project) {
        Workspace workspace = project.getWorkspaceByCwd(locator.reference.substring(PROTOCOL.length()));
        Manifest manifest = workspace.getManifest();
        Map<String, Descriptor> dependencies = new LinkedHashMap<>(manifest.dependencies);
        dependencies.putAll(manifest.devDependencies);
        return new Package(locator, manifest.version != null && !manifest.version.isEmpty() ? manifest.version :
                "0.0.0", "unknown", Package.SOFT, null,
                project.configuration.normalizeDependencyMap(dependencies),
                new LinkedHashMap<>(manifest.peerDependencies), manifest.dependenciesMeta,
                manifest.peerDependenciesMeta, manifest.bin);
    }
}
