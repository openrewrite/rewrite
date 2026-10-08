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

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Port of @yarnpkg/plugin-patch sources/PatchResolver.ts (Yarn 4.10.3). A locator's {@code hash=} is made from the
 * patch files' text, which for a builtin patch ships inside the release: a builtin patch descriptor the lockfile
 * resolved, over the same source, keeps the locator it had; any other patch is something the engine cannot know.
 */
final class PatchResolver implements Resolver {

    private static final String SOURCE_DESCRIPTOR = "sourceDescriptor";

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        return PatchUtils.isPatchDescriptor(descriptor);
    }

    @Override
    public boolean supportsLocator(Locator locator, ResolveOptions opts) {
        return PatchUtils.isPatchLocator(locator);
    }

    @Override
    public boolean shouldPersistResolution(Locator locator, ResolveOptions opts) {
        return false;
    }

    @Override
    public Descriptor bindDescriptor(Descriptor descriptor, Locator fromLocator, ResolveOptions opts) {
        for (String patchPath : PatchUtils.parseDescriptor(descriptor).getPatchPaths()) {
            if (PatchUtils.isParentRequired(patchPath)) {
                return StructUtils.bindDescriptor(descriptor,
                        Collections.singletonMap("locator", StructUtils.stringifyLocator(fromLocator)));
            }
        }
        return descriptor;
    }

    @Override
    public Map<String, Descriptor> getResolutionDependencies(Descriptor descriptor, ResolveOptions opts) {
        PatchUtils.Spec<Descriptor> spec = PatchUtils.parseDescriptor(descriptor);
        Descriptor sourceDescriptor = opts.getProject().configuration.normalizeDependency(spec.getSourceItem());
        for (String patchPath : spec.getPatchPaths()) {
            if (PatchUtils.builtinName(patchPath) == null) {
                throw new CannotKnow("the hash of the patch " + patchPath + " of " +
                                     StructUtils.stringifyDescriptor(sourceDescriptor) + " (Yarn hashes the patch " +
                                     "file on every install)");
            }
        }
        if (!opts.getProject().lockfileLocators.containsKey(descriptor.descriptorHash)) {
            throw new CannotKnow("the builtin compat patch of " + StructUtils.stringifyDescriptor(sourceDescriptor) +
                                 " (its hash= and checksum need the patch text the Yarn release ships)");
        }
        return Collections.singletonMap(SOURCE_DESCRIPTOR, sourceDescriptor);
    }

    @Override
    public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies,
                                       ResolveOptions opts) {
        Package sourcePackage = dependencies.get(SOURCE_DESCRIPTOR);
        if (sourcePackage == null) {
            throw new JsError(JsError.ERROR, "Assertion failed: The dependency should have been resolved");
        }
        Locator locked = opts.getProject().lockfileLocators.get(descriptor.descriptorHash);
        PatchUtils.Spec<Locator> spec = PatchUtils.parseLocator(locked);
        if (!spec.getSourceItem().locatorHash.equals(sourcePackage.locatorHash) ||
            !String.valueOf(sourcePackage.version).equals(spec.getSourceVersion())) {
            throw new CannotKnow("the builtin compat patch of " + StructUtils.stringifyLocator(sourcePackage) +
                                 ", locked over " + StructUtils.stringifyLocator(spec.getSourceItem()) +
                                 " (its hash= and checksum need the patch text the Yarn release ships)");
        }
        return Collections.singletonList(locked);
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        return LinkResolver.only(getCandidates(descriptor, dependencies, opts).get(0), locators);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        Locator sourceLocator = PatchUtils.parseLocator(locator).getSourceItem();
        Package sourcePkg = opts.getResolver().resolve(sourceLocator, opts);
        return StructUtils.renamePackage(sourcePkg, locator);
    }
}
