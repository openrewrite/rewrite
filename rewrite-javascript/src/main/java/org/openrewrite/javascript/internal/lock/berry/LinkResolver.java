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
 * Port of @yarnpkg/plugin-link sources/LinkResolver.ts (Yarn 4.10.3).
 */
final class LinkResolver implements Resolver {

    static final String PROTOCOL = "link:";

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        return descriptor.range.startsWith(PROTOCOL);
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
        return StructUtils.bindDescriptor(descriptor,
                Collections.singletonMap("locator", StructUtils.stringifyLocator(fromLocator)));
    }

    @Override
    public Map<String, Descriptor> getResolutionDependencies(Descriptor descriptor, ResolveOptions opts) {
        return Collections.emptyMap();
    }

    @Override
    public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies,
                                       ResolveOptions opts) {
        String path = descriptor.range.substring(PROTOCOL.length());
        return Collections.singletonList(StructUtils.makeLocator(descriptor, PROTOCOL + path));
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        return only(getCandidates(descriptor, dependencies, opts).get(0), locators);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        return new Package(locator, "0.0.0", opts.getProject().configuration
                .getString("defaultLanguageName"), Package.SOFT, null, new LinkedHashMap<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    /**
     * {@code getSatisfying} of a resolver whose descriptor has one candidate: the locators that are it.
     */
    static Satisfying only(Locator locator, List<Locator> locators) {
        List<Locator> out = new ArrayList<>();
        for (Locator candidate : locators) {
            if (candidate.locatorHash.equals(locator.locatorHash)) {
                out.add(candidate);
            }
        }
        return new Satisfying(out, false);
    }
}
