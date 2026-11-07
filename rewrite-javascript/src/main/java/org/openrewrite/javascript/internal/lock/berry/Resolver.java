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

import org.openrewrite.javascript.internal.lock.RegistrySource;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Port of @yarnpkg/core sources/Resolver.ts (Yarn 4.10.3).
 */
interface Resolver {

    /**
     * {@code ResolveOptions}: the project, the resolver chain at the top, and the registry the npm resolvers ask.
     */
    @Value
    @AllArgsConstructor
    class ResolveOptions {
        Project project;
        Resolver resolver;
        RegistrySource registry;

        /**
         * The resolution step's chain of resolution dependencies, checked as each one is taken.
         */
        @Nullable
        ResolutionChain chain;

        ResolveOptions(Project project, Resolver resolver, RegistrySource registry) {
            this(project, resolver, registry, null);
        }
    }

    /**
     * {@code getSatisfying}'s answer: the locators, and whether they are in preference order.
     */
    @Value
    class Satisfying {
        List<Locator> locators;
        boolean sorted;
    }

    boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts);

    boolean supportsLocator(Locator locator, ResolveOptions opts);

    boolean shouldPersistResolution(Locator locator, ResolveOptions opts);

    Descriptor bindDescriptor(Descriptor descriptor, Locator fromLocator, ResolveOptions opts);

    Map<String, Descriptor> getResolutionDependencies(Descriptor descriptor, ResolveOptions opts);

    List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies, ResolveOptions opts);

    Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies, List<Locator> locators,
                             ResolveOptions opts);

    Package resolve(Locator locator, ResolveOptions opts);
}
