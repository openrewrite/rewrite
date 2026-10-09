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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Port of @yarnpkg/core sources/Configuration.ts past loading, with its plugins' hooks (Yarn 4.10.3).
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

    /**
     * {@code findProjectCwd}, in directories above where Yarn runs: the nearest holding a yarn.lock, else the farthest
     * holding a package.json; -1 for none. {@code holds} answers by path from there ({@code ../yarn.lock}).
     */
    static int findProjectCwd(Predicate<String> holds, int levels) {
        int projectCwd = -1;
        StringBuilder up = new StringBuilder();
        for (int depth = 0; depth <= levels; depth++, up.append("../")) {
            if (holds.test(up + "yarn.lock")) {
                return depth;
            }
            if (holds.test(up + "package.json")) {
                projectCwd = depth;
            }
        }
        return projectCwd;
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

    Resolver makeResolver() {
        List<Resolver> resolvers = new ArrayList<>();
        resolvers.add(new VirtualResolver());
        resolvers.add(new WorkspaceResolver());
        for (String plugin : release.getPlugins()) {
            resolvers.addAll(resolvers(plugin));
        }
        return new MultiResolver(resolvers.toArray(new Resolver[0]));
    }

    /**
     * {@code makeFetcher}, as far as the fetchers that reach a lockfile.
     */
    Fetcher makeFetcher() {
        List<Fetcher> fetchers = new ArrayList<>();
        fetchers.add(new WorkspaceFetcher());
        for (String plugin : release.getPlugins()) {
            fetchers.addAll(fetchers(plugin));
        }
        return new MultiFetcher(fetchers.toArray(new Fetcher[0]));
    }

    private static List<Resolver> resolvers(String plugin) {
        switch (plugin) {
            case "@yarnpkg/plugin-exec":
                return singletonList(new ExecResolver());
            case "@yarnpkg/plugin-file":
                return asList(new TarballFileResolver(), new FileResolver());
            case "@yarnpkg/plugin-git":
                return singletonList(new GitResolver());
            case "@yarnpkg/plugin-http":
                return singletonList(new TarballHttpResolver());
            case "@yarnpkg/plugin-jsr":
                return singletonList(new JsrResolver());
            case "@yarnpkg/plugin-link":
                return asList(new LinkResolver(), new PortalResolver());
            case "@yarnpkg/plugin-npm":
                return asList(new NpmRemapResolver(), new NpmSemverResolver(), new NpmTagResolver());
            case "@yarnpkg/plugin-patch":
                return singletonList(new PatchResolver());
            default:
                return emptyList();
        }
    }

    private static List<Fetcher> fetchers(String plugin) {
        switch (plugin) {
            case "@yarnpkg/plugin-git":
                return singletonList(new GitFetcher());
            case "@yarnpkg/plugin-http":
                return singletonList(new TarballHttpFetcher());
            case "@yarnpkg/plugin-jsr":
                return singletonList(new JsrFetcher());
            case "@yarnpkg/plugin-link":
                return singletonList(new LinkFetcher());
            case "@yarnpkg/plugin-npm":
                return asList(new NpmHttpFetcher(), new NpmSemverFetcher());
            case "@yarnpkg/plugin-patch":
                return singletonList(new PatchFetcher());
            default:
                return emptyList();
        }
    }

    /**
     * {@code reduceHook(hooks => hooks.reduceDependency, ...)} over the core plugin, then the bundled ones in order.
     */
    Descriptor reduceDependency(Descriptor dependency, Project project, Locator locator, Resolver resolver,
                                Resolver.ResolveOptions opts) {
        Descriptor value = Resolutions.reduceDependency(dependency, project, locator, resolver, opts);
        for (String plugin : release.getPlugins()) {
            if ("@yarnpkg/plugin-compat".equals(plugin)) {
                value = CompatPatches.reduceDependency(value, release);
            } else if ("@yarnpkg/plugin-catalog".equals(plugin)) {
                value = Catalogs.reduceDependency(value, project, resolver, opts);
            }
        }
        return value;
    }
}
