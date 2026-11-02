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

import java.util.List;
import java.util.Map;

/**
 * Port of @yarnpkg/core sources/VirtualResolver.ts (Yarn 4.10.3).
 */
final class VirtualResolver implements Resolver {

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        return StructUtils.isVirtualDescriptor(descriptor);
    }

    @Override
    public boolean supportsLocator(Locator locator, ResolveOptions opts) {
        return StructUtils.isVirtualLocator(locator);
    }

    @Override
    public boolean shouldPersistResolution(Locator locator, ResolveOptions opts) {
        return false;
    }

    @Override
    public Descriptor bindDescriptor(Descriptor descriptor, Locator fromLocator, ResolveOptions opts) {
        throw unsupported("bindDescriptor");
    }

    @Override
    public Map<String, Descriptor> getResolutionDependencies(Descriptor descriptor, ResolveOptions opts) {
        throw unsupported("getResolutionDependencies");
    }

    @Override
    public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies,
                                       ResolveOptions opts) {
        throw unsupported("getCandidates");
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        throw unsupported("getSatisfying");
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        throw new JsError(JsError.ERROR, "Assertion failed: calling \"resolve\" on a virtual locator is unsupported");
    }

    private static JsError unsupported(String method) {
        return new JsError(JsError.ERROR, "Assertion failed: calling \"" + method + "\" on a virtual descriptor is " +
                                          "unsupported");
    }
}
