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
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Port of @yarnpkg/plugin-exec sources/ExecResolver.ts (Yarn 4.10.3).
 */
final class ExecResolver implements Resolver {

    private static final String PROTOCOL = "exec:";

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
        if (opts.getProject().configuration.getRelease().limitsExecToWorkspaces() &&
            opts.getProject().tryWorkspaceByLocator(fromLocator) == null) {
            throw new ReportError(ReportError.INVALID_MANIFEST, StructUtils.stringifyLocator(fromLocator) + " lists " +
                                                                StructUtils.stringifyDescriptor(descriptor) + " as " +
                                                                "dependency, but only workspaces can depend on exec: " +
                                                                "packages.");
        }
        return StructUtils.bindDescriptor(descriptor,
                Collections.singletonMap("locator", StructUtils.stringifyLocator(fromLocator)));
    }

    @Override
    public Map<String, Descriptor> getResolutionDependencies(Descriptor descriptor, ResolveOptions opts) {
        return Collections.emptyMap();
    }

    /**
     * {@code getCandidates} up to the generator: execUtils' parseSpec (the same as fileUtils') and loadGeneratorFile's
     * bindings; the generator's hash needs the file Yarn runs.
     */
    @Override
    public List<Locator> getCandidates(Descriptor descriptor, Map<String, Package> dependencies,
                                       ResolveOptions opts) {
        FileUtils.Spec spec = FileUtils.parseSpec(descriptor.range);
        if (spec.getParentLocator() == null) {
            throw new JsError(JsError.ERROR, "Assertion failed: The descriptor should have been bound");
        }
        StructUtils.parseFileStyleRange(StructUtils.makeRange(PROTOCOL, spec.getPath(), spec.getPath(),
                Collections.singletonMap("locator", StructUtils.stringifyLocator(spec.getParentLocator()))), PROTOCOL);
        throw new CannotKnow("what the generator of " + StructUtils.stringifyDescriptor(descriptor) + " makes " +
                             "(Yarn runs it on every install)");
    }

    @Override
    public Satisfying getSatisfying(Descriptor descriptor, Map<String, Package> dependencies,
                                    List<Locator> locators, ResolveOptions opts) {
        return LinkResolver.only(getCandidates(descriptor, dependencies, opts).get(0), locators);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        throw new CannotKnow("what the generator of " + StructUtils.stringifyLocator(locator) + " makes (Yarn " +
                             "runs it on every install)");
    }
}
