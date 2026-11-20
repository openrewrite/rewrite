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

/**
 * Port of @yarnpkg/core sources/CorePlugin.ts {@code reduceDependency} (Yarn 4.10.3).
 */
final class Resolutions {

    private Resolutions() {
    }

    static Descriptor reduceDependency(Descriptor dependency, Project project, Locator locator,
                                       Resolver resolver, Resolver.ResolveOptions opts) {
        Configuration configuration = project.configuration;
        for (Manifest.ResolutionEntry entry : project.getTopLevelWorkspace().getManifest().resolutions) {
            Resolution pattern = entry.getPattern();
            if (pattern.getFrom() != null) {
                if (!pattern.getFrom().getFullName().equals(StructUtils.stringifyIdent(locator))) {
                    continue;
                }
                String description = pattern.getFrom().getDescription();
                Locator normalizedFrom = configuration.normalizeLocator(StructUtils.makeLocator(
                        StructUtils.parseIdent(pattern.getFrom().getFullName()),
                        description != null ? description : locator.reference));
                if (!normalizedFrom.locatorHash.equals(locator.locatorHash)) {
                    continue;
                }
            }
            if (!pattern.getDescriptor().getFullName().equals(StructUtils.stringifyIdent(dependency))) {
                continue;
            }
            String description = pattern.getDescriptor().getDescription();
            Descriptor normalizedDescriptor = configuration.normalizeDependency(StructUtils.makeDescriptor(
                    StructUtils.parseLocator(pattern.getDescriptor().getFullName()),
                    description != null ? description : dependency.range));
            if (!normalizedDescriptor.descriptorHash.equals(dependency.descriptorHash)) {
                continue;
            }
            return resolver.bindDescriptor(
                    configuration.normalizeDependency(StructUtils.makeDescriptor(dependency, entry.getReference())),
                    project.getTopLevelWorkspace().getAnchoredLocator(), opts);
        }
        return dependency;
    }
}
