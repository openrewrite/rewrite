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

import static org.openrewrite.javascript.internal.lock.berry.NpmHttpUtils.PROTOCOL;

/**
 * Port of @yarnpkg/plugin-npm sources/NpmRemapResolver.ts (Yarn 4.10.3).
 */
final class NpmRemapResolver implements Resolver {

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        if (!descriptor.range.startsWith(PROTOCOL)) {
            return false;
        }
        return StructUtils.tryParseDescriptor(descriptor.range.substring(PROTOCOL.length()), true) != null;
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

    private static Descriptor nextDescriptor(Descriptor descriptor, ResolveOptions opts) {
        return opts.getProject().configuration.normalizeDependency(
                StructUtils.parseDescriptor(descriptor.range.substring(PROTOCOL.length()), true));
    }

    @Override
    public Map<String, Descriptor> getResolutionDependencies(Descriptor descriptor, ResolveOptions opts) {
        return opts.getResolver().getResolutionDependencies(nextDescriptor(descriptor, opts), opts);
    }

    @Override
    public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies,
                                       ResolveOptions opts) {
        return opts.getResolver().getCandidates(nextDescriptor(descriptor, opts), dependencies, opts);
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        return opts.getResolver().getSatisfying(nextDescriptor(descriptor, opts), dependencies, locators, opts);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        throw new JsError(JsError.ERROR, "Unreachable");
    }
}
