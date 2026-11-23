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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/plugin-file sources/TarballFileResolver.ts (Yarn 4.10.3).
 */
final class TarballFileResolver implements Resolver {

    private static final Pattern TARBALL_REGEXP = Pattern.compile("^[^?]*\\.(?:tar\\.gz|tgz)(?:::.*)?$");

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        if (!TARBALL_REGEXP.matcher(descriptor.range).find()) {
            return false;
        }
        return descriptor.range.startsWith(FileResolver.PROTOCOL) ||
               FileResolver.FILE_REGEXP.matcher(descriptor.range).find();
    }

    @Override
    public boolean supportsLocator(Locator locator, ResolveOptions opts) {
        return TARBALL_REGEXP.matcher(locator.reference).find() && locator.reference.startsWith(FileResolver.PROTOCOL);
    }

    @Override
    public boolean shouldPersistResolution(Locator locator, ResolveOptions opts) {
        return false;
    }

    @Override
    public Descriptor bindDescriptor(Descriptor descriptor, Locator fromLocator, ResolveOptions opts) {
        return FileResolver.bind(descriptor, fromLocator);
    }

    @Override
    public Map<String, Descriptor> getResolutionDependencies(Descriptor descriptor, ResolveOptions opts) {
        return Collections.emptyMap();
    }

    @Override
    public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies,
                                       ResolveOptions opts) {
        FileUtils.readBindings(descriptor, "", FileResolver.PROTOCOL);
        throw new CannotKnow("the file: tarball hash of " + StructUtils.stringifyDescriptor(descriptor) +
                             " (Yarn hashes the file on every install)");
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        return LinkResolver.only(getCandidates(descriptor, dependencies, opts).get(0), locators);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        throw new CannotKnow("the file: tarball contents of " + StructUtils.stringifyLocator(locator) +
                             " (Yarn reads the file on every install)");
    }
}
