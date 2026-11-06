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

import java.util.Map;

/**
 * Port of @yarnpkg/plugin-catalog sources/index.ts {@code reduceDependency} and sources/utils.ts (Yarn 4.10.3).
 */
final class Catalogs {

    static final String CATALOG_DESCRIPTOR_PREFIX = "catalog:";

    private Catalogs() {
    }

    static boolean isCatalogReference(String range) {
        return range.startsWith(CATALOG_DESCRIPTOR_PREFIX);
    }

    static Descriptor reduceDependency(Descriptor dependency, Project project, Resolver resolver,
                                       Resolver.ResolveOptions opts) {
        if (isCatalogReference(dependency.range)) {
            return resolveDescriptorFromCatalog(project, dependency, resolver, opts);
        }
        return dependency;
    }

    /**
     * The catalog a range references, {@code ""} for {@code catalog:}'s default one; null for no catalog reference.
     */
    static @Nullable String catalogOf(String range) {
        if (!isCatalogReference(range)) {
            return null;
        }
        String name = range.substring(CATALOG_DESCRIPTOR_PREFIX.length());
        return name.isEmpty() ? "" : name;
    }

    static @Nullable String getCatalogName(Descriptor dependency) {
        String name = dependency.range.substring(CATALOG_DESCRIPTOR_PREFIX.length());
        return name.isEmpty() ? null : name;
    }

    static String getCatalogEntryName(Descriptor dependency) {
        return dependency.scope != null && !dependency.scope.isEmpty() ?
                "@" + dependency.scope + "/" + dependency.name : dependency.name;
    }

    @SuppressWarnings("unchecked")
    static Descriptor resolveDescriptorFromCatalog(Project project, Descriptor dependency, Resolver resolver,
                                                   Resolver.ResolveOptions opts) {
        String catalogName = getCatalogName(dependency);
        Map<String, @Nullable Object> catalog;
        Configuration options = project.configuration;
        if (catalogName == null) {
            catalog = options.getMap("catalog");
        } else {
            Object named = options.getMap("catalogs").get(catalogName);
            catalog = named instanceof Map ? (Map<String, @Nullable Object>) named : null;
        }
        String displayName = catalogName == null ? "default catalog" : "catalog \"" + catalogName + "\"";
        if (catalog == null || catalog.isEmpty()) {
            throw new ReportError(ReportError.RESOLUTION_FAILED, StructUtils.stringifyDescriptor(dependency) + ": " +
                                                                 displayName + " not found or empty");
        }
        Object resolvedRange = catalog.get(getCatalogEntryName(dependency));
        if (!Js.truthy(resolvedRange)) {
            throw new ReportError(ReportError.RESOLUTION_FAILED, StructUtils.stringifyDescriptor(dependency) + ": " +
                                                                 "entry not found in " + displayName);
        }
        Descriptor normalizedDescriptor = project.configuration.normalizeDependency(
                StructUtils.makeDescriptor(dependency, (String) resolvedRange));
        if (options.getRelease().keepsCatalogRangesNoResolverSupports() &&
            !resolver.supportsDescriptor(normalizedDescriptor, opts)) {
            return normalizedDescriptor;
        }
        return resolver.bindDescriptor(normalizedDescriptor, project.getTopLevelWorkspace().getAnchoredLocator(),
                opts);
    }
}
