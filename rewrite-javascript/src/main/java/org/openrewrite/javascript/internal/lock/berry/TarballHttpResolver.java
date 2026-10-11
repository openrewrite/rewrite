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

import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/plugin-http sources/TarballHttpResolver.ts and urlUtils.ts (Yarn 4.10.3).
 */
final class TarballHttpResolver implements Resolver {

    private static final Pattern TGZ_PATHNAME = Pattern.compile("(\\.tar\\.gz|\\.tgz|/[^.]+)$");

    static boolean isTgzUrl(String url) {
        WhatwgUrl parsed = WhatwgUrl.tryParse(url);
        if (parsed == null) {
            return false;
        }
        if (!"http:".equals(parsed.getProtocol()) && !"https:".equals(parsed.getProtocol())) {
            return false;
        }
        return TGZ_PATHNAME.matcher(parsed.getPathname()).find();
    }

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        return isTgzUrl(descriptor.range);
    }

    @Override
    public boolean supportsLocator(Locator locator, ResolveOptions opts) {
        return isTgzUrl(locator.reference);
    }

    @Override
    public boolean shouldPersistResolution(Locator locator, ResolveOptions opts) {
        return true;
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
        return Collections.singletonList(StructUtils.convertDescriptorToLocator(descriptor));
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        return LinkResolver.only(getCandidates(descriptor, dependencies, opts).get(0), locators);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        throw new CannotKnow("the contents of the remote tarball " + StructUtils.stringifyLocator(locator) +
                             " (Yarn reads its package.json from the archive)");
    }
}
