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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Port of @yarnpkg/core sources/MultiResolver.ts (Yarn 4.10.3).
 */
final class MultiResolver implements Resolver {

    private final List<Resolver> resolvers;

    MultiResolver(Resolver... resolvers) {
        this.resolvers = new ArrayList<>(Arrays.asList(resolvers));
    }

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        return tryResolverByDescriptor(descriptor, opts) != null;
    }

    @Override
    public boolean supportsLocator(Locator locator, ResolveOptions opts) {
        return tryResolverByLocator(locator, opts) != null;
    }

    @Override
    public boolean shouldPersistResolution(Locator locator, ResolveOptions opts) {
        return getResolverByLocator(locator, opts).shouldPersistResolution(locator, opts);
    }

    @Override
    public Descriptor bindDescriptor(Descriptor descriptor, Locator fromLocator, ResolveOptions opts) {
        return getResolverByDescriptor(descriptor, opts).bindDescriptor(descriptor, fromLocator, opts);
    }

    @Override
    public Map<String, Descriptor> getResolutionDependencies(Descriptor descriptor, ResolveOptions opts) {
        Resolver resolver = getResolverByDescriptor(descriptor, opts);
        ResolutionChain chain = opts.getChain();
        if (chain != null && !(resolver instanceof MultiResolver)) {
            chain.enter(descriptor, resolver);
        }
        return resolver.getResolutionDependencies(descriptor, opts);
    }

    @Override
    public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies, ResolveOptions opts) {
        return getResolverByDescriptor(descriptor, opts).getCandidates(descriptor, dependencies, opts);
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies, List<Locator> locators,
                                    ResolveOptions opts) {
        return getResolverByDescriptor(descriptor, opts).getSatisfying(descriptor, dependencies, locators, opts);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        return getResolverByLocator(locator, opts).resolve(locator, opts);
    }

    private @Nullable Resolver tryResolverByDescriptor(Descriptor descriptor, ResolveOptions opts) {
        for (Resolver resolver : resolvers) {
            if (resolver.supportsDescriptor(descriptor, opts)) {
                return resolver;
            }
        }
        return null;
    }

    private Resolver getResolverByDescriptor(Descriptor descriptor, ResolveOptions opts) {
        Resolver resolver = tryResolverByDescriptor(descriptor, opts);
        if (resolver == null) {
            throw new JsError(JsError.ERROR, StructUtils.stringifyDescriptor(descriptor) +
                                            " isn't supported by any available resolver");
        }
        return resolver;
    }

    private @Nullable Resolver tryResolverByLocator(Locator locator, ResolveOptions opts) {
        for (Resolver resolver : resolvers) {
            if (resolver.supportsLocator(locator, opts)) {
                return resolver;
            }
        }
        return null;
    }

    private Resolver getResolverByLocator(Locator locator, ResolveOptions opts) {
        Resolver resolver = tryResolverByLocator(locator, opts);
        if (resolver == null) {
            throw new JsError(JsError.ERROR, StructUtils.stringifyLocator(locator) +
                                            " isn't supported by any available resolver");
        }
        return resolver;
    }
}
