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

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/core sources/Configuration.ts (Yarn 4.10.3) past loading: the settings of {@link BerryOptions},
 * the data of the project's Yarn release, the dependency normalizers, the package extensions and the
 * {@code reduceDependency} hooks of the plugins that have one, in the CLI's plugin order (core, compat, catalog).
 */
@Getter
final class Configuration {

    static final Pattern TAG_REGEXP = Pattern.compile("^(?!v)[a-z0-9._-]+\\z", Pattern.CASE_INSENSITIVE);

    private final BerryOptions options;
    private final YarnRelease release;

    private @Nullable Map<String, List<PackageExtensions.RangeExtensions>> packageExtensions;

    Configuration(BerryOptions options, YarnRelease release) {
        this.options = options;
        this.release = release;
    }

    @Nullable
    Object get(String key) {
        return options.get(key);
    }

    String defaultProtocol() {
        return options.getString("defaultProtocol");
    }

    Locator normalizeLocator(Locator locator) {
        if (SemverUtils.validRange(locator.reference) != null) {
            return StructUtils.makeLocator(locator, defaultProtocol() + locator.reference);
        }
        if (TAG_REGEXP.matcher(locator.reference).find()) {
            return StructUtils.makeLocator(locator, defaultProtocol() + locator.reference);
        }
        return locator;
    }

    Descriptor normalizeDependency(Descriptor dependency) {
        if (SemverUtils.validRange(dependency.range) != null) {
            return StructUtils.makeDescriptor(dependency, defaultProtocol() + dependency.range);
        }
        if (TAG_REGEXP.matcher(dependency.range).find()) {
            return StructUtils.makeDescriptor(dependency, defaultProtocol() + dependency.range);
        }
        return dependency;
    }

    Map<String, Descriptor> normalizeDependencyMap(Map<String, Descriptor> dependencyMap) {
        Map<String, Descriptor> out = new LinkedHashMap<>();
        for (Map.Entry<String, Descriptor> entry : dependencyMap.entrySet()) {
            out.put(entry.getKey(), normalizeDependency(entry.getValue()));
        }
        return out;
    }

    /**
     * {@code getPackageExtensions}: the release's builtin table (the compat plugin's
     * {@code registerPackageExtensions}), then the {@code packageExtensions} setting, computed once.
     */
    Map<String, List<PackageExtensions.RangeExtensions>> getPackageExtensions() {
        if (packageExtensions == null) {
            packageExtensions = PackageExtensions.load(release, options);
        }
        return packageExtensions;
    }

    /**
     * {@code normalizePackage}: a copy with the matching extensions applied, the implicit peers added and the
     * dependency maps sorted by descriptor.
     */
    Package normalizePackage(Package original) {
        return PackageExtensions.normalizePackage(this, original, getPackageExtensions());
    }

    /**
     * {@code reduceHook(hooks => hooks.reduceDependency, ...)} over the plugins in order.
     */
    Descriptor reduceDependency(Descriptor dependency, Project project, Locator locator, Resolver resolver,
                                Resolver.ResolveOptions opts) {
        Descriptor value = Resolutions.reduceDependency(dependency, project, locator, resolver, opts);
        value = CompatPatches.reduceDependency(value, release);
        if (release.isCatalogs()) {
            value = Catalogs.reduceDependency(value, project, resolver, opts);
        }
        return value;
    }
}
