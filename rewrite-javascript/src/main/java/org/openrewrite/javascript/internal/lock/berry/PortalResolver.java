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
import org.openrewrite.javascript.internal.lock.PosixPath;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Port of @yarnpkg/plugin-link sources/PortalResolver.ts and PortalFetcher.ts's manifest lookup (Yarn 4.10.3).
 */
final class PortalResolver implements Resolver {

    static final String PROTOCOL = "portal:";

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
        return LinkResolver.only(getCandidates(descriptor, dependencies, opts).get(0), locators);
    }

    @Override
    public Package resolve(Locator locator, ResolveOptions opts) {
        Project project = opts.getProject();
        Manifest manifest = find(locator, project);
        Configuration configuration = project.configuration;
        return new Package(locator, manifest.version != null && !manifest.version.isEmpty() ? manifest.version :
                "0.0.0", manifest.languageName != null && !manifest.languageName.isEmpty() ? manifest.languageName :
                configuration.getString("defaultLanguageName"), Package.SOFT, manifest.getConditions(),
                configuration.normalizeDependencyMap(manifest.dependencies), manifest.peerDependencies,
                manifest.dependenciesMeta, manifest.peerDependenciesMeta, manifest.bin);
    }

    /**
     * The target's package.json, after PortalFetcher's bindings: the path joined to the directory of the workspace
     * that depends on it.
     */
    private static Manifest find(Locator locator, Project project) {
        StructUtils.FileStyleRange range = StructUtils.parseFileStyleRange(locator.reference, PROTOCOL);
        String path = range.getPath();
        Workspace workspace = path.startsWith("/") ? null : project.tryWorkspaceByLocator(range.getParentLocator());
        if (workspace == null) {
            throw new CannotKnow("the package.json of " + StructUtils.stringifyLocator(locator) + " (a path from " +
                                 "outside the project's workspaces)");
        }
        String dir = PosixPath.normalize(".".equals(workspace.getRelativeCwd()) ? path :
                workspace.getRelativeCwd() + "/" + path).replaceAll("/+$", "");
        if ("..".equals(dir) || dir.startsWith("../")) {
            throw new CannotKnow("the package.json of " + StructUtils.stringifyLocator(locator) + " (outside the " +
                                 "project)");
        }
        Manifest manifest = project.readManifest(dir);
        if (manifest == null) {
            throw new JsError(JsError.ERROR, "ENOENT: no such file or directory, open '" + dir + "/package.json'");
        }
        return manifest;
    }
}
