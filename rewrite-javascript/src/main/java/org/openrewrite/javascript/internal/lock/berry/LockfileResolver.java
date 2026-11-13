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

import org.openrewrite.javascript.internal.lock.JsError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Port of @yarnpkg/core sources/LockfileResolver.ts (Yarn 4.10.3).
 */
final class LockfileResolver implements Resolver {

    private final Resolver resolver;

    LockfileResolver(Resolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        Project project = opts.getProject();
        if (project.storedResolutions.containsKey(descriptor.descriptorHash)) {
            return true;
        }
        return project.originalPackages.containsKey(
                StructUtils.convertDescriptorToLocator(descriptor).locatorHash);
    }

    @Override
    public boolean supportsLocator(Locator locator, ResolveOptions opts) {
        Project project = opts.getProject();
        return project.originalPackages.containsKey(locator.locatorHash) && !project.lockfileNeedsRefresh;
    }

    @Override
    public boolean shouldPersistResolution(Locator locator, ResolveOptions opts) {
        throw new JsError(JsError.ERROR, "The shouldPersistResolution method shouldn't be called on the lockfile " +
                                         "resolver, which would always answer yes");
    }

    @Override
    public Descriptor bindDescriptor(Descriptor descriptor, Locator fromLocator, ResolveOptions opts) {
        return descriptor;
    }

    @Override
    public Map<String, Descriptor> getResolutionDependencies(Descriptor descriptor, ResolveOptions opts) {
        return resolver.getResolutionDependencies(descriptor, opts);
    }

    @Override
    public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies,
                                       ResolveOptions opts) {
        Project project = opts.getProject();
        String resolution = project.storedResolutions.get(descriptor.descriptorHash);
        if (resolution != null) {
            Package resolvedPkg = project.originalPackages.get(resolution);
            if (resolvedPkg != null) {
                return Collections.singletonList(resolvedPkg);
            }
        }
        Package originalPkg = project.originalPackages.get(
                StructUtils.convertDescriptorToLocator(descriptor).locatorHash);
        if (originalPkg != null) {
            return Collections.singletonList(originalPkg);
        }
        throw new JsError(JsError.ERROR, "Resolution expected from the lockfile data");
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
        Package pkg = opts.getProject().originalPackages.get(locator.locatorHash);
        if (pkg == null) {
            throw new JsError(JsError.ERROR, "The lockfile resolver isn't meant to resolve packages - they should " +
                                             "already have been stored into a cache");
        }
        return pkg;
    }
}
