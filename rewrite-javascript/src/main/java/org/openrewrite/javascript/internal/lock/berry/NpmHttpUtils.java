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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of @yarnpkg/plugin-npm sources/npmHttpUtils.ts {@code getPackageMetadata}, sources/npmConfigUtils.ts
 * (registries, the minimal age gate) and sources/NpmSemverFetcher.ts {@code isConventionalTarballUrl}
 * (Yarn 4.10.3): the full packument of a name from its scope's registry, cut to the fields Yarn caches.
 */
final class NpmHttpUtils {

    static final String PROTOCOL = "npm:";

    private static final List<String> CACHED_FIELDS = Arrays.asList("name", "dist.tarball", "bin", "scripts", "os",
            "cpu", "libc", "dependencies", "dependenciesMeta", "optionalDependencies", "peerDependencies",
            "peerDependenciesMeta", "deprecated");

    /**
     * Where the full packument of a package comes from: {@code GET <registry>/<name>} with no abbreviated
     * Accept header.
     */
    interface Registry {
        /**
         * The packument as parsed JSON, null when the registry answers 404.
         */
        @Nullable
        Object getPackument(String registry, String identUrl);
    }

    private NpmHttpUtils() {
    }

    static String getIdentUrl(Ident ident) {
        return ident.scope != null && !ident.scope.isEmpty() ? "/@" + ident.scope + "%2f" + ident.name :
                "/" + ident.name;
    }

    static String normalizeRegistry(String registry) {
        return registry.replaceAll("/\\z", "");
    }

    /**
     * {@code getScopeRegistry}: the scope's {@code npmScopes} entry, whose {@code npmRegistryServer} defaults to
     * the public registry and not to the project's own; else {@code npmRegistryServer}.
     */
    static String getScopeRegistry(@Nullable String scope, BerryOptions options) {
        Object scopeConfiguration = scope == null ? null : options.getMap("npmScopes").get(scope);
        if (!(scopeConfiguration instanceof Map)) {
            if ("jsr".equals(scope)) {
                return normalizeRegistry("https://npm.jsr.io/");
            }
            return normalizeRegistry(options.getString("npmRegistryServer"));
        }
        Object scopeRegistry = ((Map<?, ?>) scopeConfiguration).get("npmRegistryServer");
        if (scopeRegistry == null) {
            return normalizeRegistry(options.getString("npmRegistryServer"));
        }
        return normalizeRegistry((String) scopeRegistry);
    }

    /**
     * {@code getPackageMetadata} as the network answers it: {@code dist-tags}, each version cut to the cached
     * fields, and {@code time}.
     *
     * @throws ReportError YN0035 when the registry has no such package
     */
    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> getPackageMetadata(Ident ident, Project project, Registry registry) {
        String registryUrl = getScopeRegistry(ident.scope, project.configuration.getOptions());
        Object packument = registry.getPackument(registryUrl, getIdentUrl(ident));
        if (packument == null) {
            throw new ReportError(ReportError.NETWORK_ERROR, "Package not found");
        }
        Map<String, @Nullable Object> picked = new LinkedHashMap<>();
        picked.put("dist-tags", Js.get(packument, "dist-tags"));
        Map<String, @Nullable Object> versions = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Object> version : Js.entries(Js.get(packument, "versions"))) {
            versions.put(version.getKey(), pick(version.getValue()));
        }
        picked.put("versions", versions);
        picked.put("time", Js.get(packument, "time"));
        return picked;
    }

    // lodash-style pick of the cached field paths
    private static Map<String, @Nullable Object> pick(@Nullable Object value) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        for (String field : CACHED_FIELDS) {
            int dot = field.indexOf('.');
            if (dot == -1) {
                Object v = Js.get(value, field);
                if (v != Syml.UNDEFINED) {
                    out.put(field, v);
                }
                continue;
            }
            Object parent = Js.get(value, field.substring(0, dot));
            Object v = Js.get(parent, field.substring(dot + 1));
            if (v != Syml.UNDEFINED) {
                Map<String, @Nullable Object> nested = new LinkedHashMap<>();
                nested.put(field.substring(dot + 1), v);
                out.put(field.substring(0, dot), nested);
            }
        }
        return out;
    }

    /**
     * {@code NpmSemverFetcher.isConventionalTarballUrl}: {@code <registry>/<name>/-/<name>-<version>.tgz}, the
     * npmjs and yarnpkg hosts taken as one.
     */
    static boolean isConventionalTarballUrl(Locator locator, @Nullable Object url, BerryOptions options) {
        String registry = getScopeRegistry(locator.scope, options);
        String path = getLocatorUrl(locator);
        String u = String.valueOf(url).replaceAll("^https?:(//(?:[^/]+\\.)?npmjs.org(?:\\z|/))", "https:$1");
        registry = registry.replaceAll("^https://registry\\.npmjs\\.org(\\z|/)", "https://registry.yarnpkg.com$1");
        u = u.replaceAll("^https://registry\\.npmjs\\.org(\\z|/)", "https://registry.yarnpkg.com$1");
        return u.equals(registry + path) || u.equals(registry + path.replace("%2f", "/"));
    }

    static String getLocatorUrl(Locator locator) {
        String version = SemverUtils.clean(locator.reference.substring(PROTOCOL.length()));
        if (version == null) {
            throw new ReportError(ReportError.RESOLVER_NOT_FOUND, "The npm semver resolver got selected, but the " +
                                                                  "version isn't semver");
        }
        return getIdentUrl(locator) + "/-/" + locator.name + "-" + version + ".tgz";
    }

    /**
     * {@code isPackageApproved}: outside a minimal age gate, or preapproved. The gate is not ported: a project
     * that sets one is something the engine cannot know yet.
     */
    static boolean isPackageApproved(BerryOptions options) {
        Object gate = options.get("npmMinimalAgeGate");
        if (gate instanceof Number && ((Number) gate).doubleValue() != 0) {
            throw new CannotKnow("which versions npmMinimalAgeGate " + Js.string(gate) + " quarantines");
        }
        return true;
    }
}
