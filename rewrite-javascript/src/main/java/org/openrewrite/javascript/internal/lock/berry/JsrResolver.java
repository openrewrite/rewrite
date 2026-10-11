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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;

/**
 * Port of @yarnpkg/plugin-jsr sources/JsrResolver.ts and helpers.ts (Yarn 4.10.3).
 */
final class JsrResolver implements Resolver {

    static final String PROTOCOL = "jsr:";

    static Descriptor convertDescriptorFromJsrToNpm(Descriptor dependency) {
        String rangeWithoutProtocol = dependency.range.substring(PROTOCOL.length());
        if (YarnpkgSemverUtils.validRange(rangeWithoutProtocol) != null) {
            return StructUtils.makeDescriptor(dependency, NpmHttpUtils.PROTOCOL +
                    StructUtils.stringifyIdent(StructUtils.wrapIdentIntoScope(dependency, "jsr")) + "@" +
                    rangeWithoutProtocol);
        }
        Descriptor parsedRange = StructUtils.tryParseDescriptor(rangeWithoutProtocol, true);
        if (parsedRange != null) {
            return StructUtils.makeDescriptor(dependency, NpmHttpUtils.PROTOCOL +
                    StructUtils.stringifyIdent(StructUtils.wrapIdentIntoScope(parsedRange, "jsr")) + "@" +
                    parsedRange.range);
        }
        throw new JsError(JsError.ERROR, "Invalid range: " + dependency.range);
    }

    static Locator convertLocatorFromJsrToNpm(Locator locator) {
        return StructUtils.makeLocator(StructUtils.wrapIdentIntoScope(locator, "jsr"),
                NpmHttpUtils.PROTOCOL + locator.reference.substring(PROTOCOL.length()));
    }

    static Locator convertLocatorFromNpmToJsr(Locator locator) {
        return StructUtils.makeLocator(StructUtils.unwrapIdentFromScope(locator, "jsr"),
                PROTOCOL + locator.reference.substring(NpmHttpUtils.PROTOCOL.length()));
    }

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
        return opts.getResolver().shouldPersistResolution(convertLocatorFromJsrToNpm(locator), opts);
    }

    @Override
    public Descriptor bindDescriptor(Descriptor descriptor, Locator fromLocator, ResolveOptions opts) {
        return descriptor;
    }

    @Override
    public Map<String, Descriptor> getResolutionDependencies(Descriptor descriptor, ResolveOptions opts) {
        return Collections.singletonMap("inner", convertDescriptorFromJsrToNpm(descriptor));
    }

    @Override
    public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies,
                                       ResolveOptions opts) {
        Descriptor nextDescriptor = opts.getProject().configuration.normalizeDependency(
                convertDescriptorFromJsrToNpm(descriptor));
        List<Locator> out = new ArrayList<>();
        for (Locator candidate : opts.getResolver().getCandidates(nextDescriptor, dependencies, opts)) {
            out.add(convertLocatorFromNpmToJsr(candidate));
        }
        return out;
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        Descriptor nextDescriptor = opts.getProject().configuration.normalizeDependency(
                convertDescriptorFromJsrToNpm(descriptor));
        return opts.getResolver().getSatisfying(nextDescriptor, dependencies, locators, opts);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        Package resolved = opts.getResolver().resolve(convertLocatorFromJsrToNpm(locator), opts);
        return StructUtils.renamePackage(resolved, convertLocatorFromNpmToJsr(resolved));
    }
}
