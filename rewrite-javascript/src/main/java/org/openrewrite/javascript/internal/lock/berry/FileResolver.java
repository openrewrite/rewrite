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
 * Port of @yarnpkg/plugin-file sources/FileResolver.ts and constants.ts (Yarn 4.10.3).
 */
final class FileResolver implements Resolver {

    static final String PROTOCOL = "file:";
    static final Pattern FILE_REGEXP = Pattern.compile("^(?:[a-zA-Z]:[\\\\/]|\\.{0,2}/)");

    @Override
    public boolean supportsDescriptor(Descriptor descriptor, ResolveOptions opts) {
        if (FILE_REGEXP.matcher(descriptor.range).find()) {
            return true;
        }
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
        return bind(descriptor, fromLocator);
    }

    static Descriptor bind(Descriptor descriptor, Locator fromLocator) {
        if (FILE_REGEXP.matcher(descriptor.range).find()) {
            descriptor = StructUtils.makeDescriptor(descriptor, PROTOCOL + descriptor.range);
        }
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
        FileUtils.readBindings(descriptor, null, PROTOCOL);
        throw new CannotKnow("the file: folder contents of " + StructUtils.stringifyDescriptor(descriptor) +
                             " (Yarn hashes the folder on every install)");
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        return LinkResolver.only(getCandidates(descriptor, dependencies, opts).get(0), locators);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        throw new CannotKnow("the file: folder contents of " + StructUtils.stringifyLocator(locator) +
                             " (Yarn hashes the folder on every install)");
    }
}
