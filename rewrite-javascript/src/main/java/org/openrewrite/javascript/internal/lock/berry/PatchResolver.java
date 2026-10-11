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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Port of @yarnpkg/plugin-patch sources/PatchResolver.ts (Yarn 4.10.3).
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
        if (shipsBuiltin(spec, opts)) {
            for (String patchPath : spec.getPatchPaths()) {
                if (!PatchUtils.isShippedBuiltin(patchPath, opts.getProject().configuration.getRelease())) {
                    throw new CannotKnow("the hash of the patches " + String.join(" & ", spec.getPatchPaths()) +
                                         " of " + StructUtils.stringifyDescriptor(sourceDescriptor) + " (it covers " +
                                         "the text of a builtin patch the Yarn release ships)");
                }
            }
            if (!opts.getProject().lockfileLocators.containsKey(descriptor.descriptorHash)) {
                throw new CannotKnow("the builtin compat patch of " +
                                     StructUtils.stringifyDescriptor(sourceDescriptor) + " (its hash= and checksum " +
                                     "need the patch text the Yarn release ships)");
            }
        }
        return Collections.singletonMap(SOURCE_DESCRIPTOR, sourceDescriptor);
    }

    // the release's own patch text is not data the engine holds: an unchanged one keeps the locator it had
    private static boolean shipsBuiltin(PatchUtils.Spec<?> spec, ResolveOptions opts) {
        for (String patchPath : spec.getPatchPaths()) {
            if (PatchUtils.isShippedBuiltin(patchPath, opts.getProject().configuration.getRelease())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies,
                                       ResolveOptions opts) {
        PatchUtils.Spec<Descriptor> parsed = PatchUtils.parseDescriptor(descriptor);
        if (!shipsBuiltin(parsed, opts)) {
            List<@Nullable String> patchFiles = PatchUtils.loadPatchFiles(parsed.getParentLocator(),
                    parsed.getPatchPaths(), opts.getProject());
            Package sourcePackage = dependencies.get(SOURCE_DESCRIPTOR);
            if (sourcePackage == null) {
                throw new JsError(JsError.ERROR, "Assertion failed: The dependency should have been resolved");
            }
            String patchHash = PatchUtils.makePatchHash(patchFiles, sourcePackage.version);
            return Collections.singletonList(PatchUtils.makeLocator(descriptor, parsed.getParentLocator(),
                    sourcePackage, parsed.getPatchPaths(), patchHash));
        }
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
