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
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.Picomatch;
import org.openrewrite.javascript.internal.lock.UriComponent;
import org.openrewrite.javascript.internal.lock.RegistrySource;
import org.openrewrite.javascript.internal.lock.V8Date;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;

/**
 * Port of @yarnpkg/plugin-npm npmHttpUtils.ts, npmConfigUtils.ts and NpmSemverFetcher.ts (Yarn 4.10.3).
 */
final class NpmHttpUtils {

    static final String PROTOCOL = "npm:";

    private static final List<String> CACHED_FIELDS = Arrays.asList("name", "dist.tarball", "bin", "scripts", "os",
            "cpu", "libc", "dependencies", "dependenciesMeta", "optionalDependencies", "peerDependencies",
            "peerDependenciesMeta", "deprecated");

    // micromatch 4.0.5 isMatch under its default options
    private static final Picomatch.Options MICROMATCH = new Picomatch.Options(Picomatch.Release.V2_3_1, false, false);

    private NpmHttpUtils() {
    }

    static String getIdentUrl(Ident ident) {
        return ident.scope != null && !ident.scope.isEmpty() ? "/@" + ident.scope + "%2f" + ident.name :
                "/" + ident.name;
    }

    /**
     * {@code getIdentUrl} as the release builds it: the scope and name encoded from 4.15.0.
     */
    static String getIdentUrl(Ident ident, YarnRelease release) {
        if (!release.encodesRegistryPaths()) {
            return getIdentUrl(ident);
        }
        return ident.scope != null && !ident.scope.isEmpty() ?
                "/@" + encodeURIComponent(ident.scope) + "%2f" + encodeURIComponent(ident.name) :
                "/" + encodeURIComponent(ident.name);
    }

    private static String encodeURIComponent(String component) {
        try {
            return UriComponent.encodeURIComponent(component);
        } catch (IllegalArgumentException e) {
            throw new JsError(JsError.URI_ERROR, "URI malformed");
        }
    }

    static String normalizeRegistry(String registry) {
        return registry.replaceAll("/\\z", "");
    }

    /**
     * {@code getScopeRegistry}: the scope's {@code npmScopes} entry, whose {@code npmRegistryServer} defaults to
     * the public registry and not to the project's own; else {@code npmRegistryServer}.
     */
    static String getScopeRegistry(@Nullable String scope, Configuration configuration) {
        Configuration options = configuration;
        Object scopeConfiguration = scope == null ? null : options.getMap("npmScopes").get(scope);
        if (!(scopeConfiguration instanceof Map)) {
            if ("jsr".equals(scope) && configuration.getRelease().sendsJsrScopeToJsr()) {
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
     * {@code getPackageMetadata} as the network answers it: {@code dist-tags}, each version cut to the cached fields,
     * and {@code time}; throws ReportError YN0035 when the registry has no such package.
     */
    static Map<String, @Nullable Object> getPackageMetadata(Ident ident, Project project, RegistrySource registry) {
        if (project.configuration.getBoolean("enableOfflineMode")) {
            throw new CannotKnow("enableOfflineMode: what Yarn resolves depends on its cache, which holds the " +
                                 "only versions it offers");
        }
        String registryUrl = getScopeRegistry(ident.scope, project.configuration);
        return project.packageMetadata.computeIfAbsent(registryUrl + getIdentUrl(ident,
                project.configuration.getRelease()), key -> {
            String packument = registry.packument(registryUrl, StructUtils.stringifyIdent(ident), true,
                    getAuthenticationHeader(registryUrl, ident.scope, project.configuration));
            if (packument == null) {
                throw new ReportError(ReportError.NETWORK_ERROR, "Package not found");
            }
            return pickMetadata(Js.jsonParse(packument));
        });
    }

    /**
     * {@code get} of a package's tarball: a path joins the scope's registry, a url is fetched as it is.
     * @throws ReportError YN0035 when the server does not have it
     */
    static byte[] get(String path, Locator locator, String version, Project project, RegistrySource registry) {
        String registryUrl = getScopeRegistry(locator.scope, project.configuration);
        byte[] tarball = registry.tarball(path.startsWith("/") ? registryUrl + path : path,
                StructUtils.stringifyIdent(locator), version,
                getAuthenticationHeader(registryUrl, locator.scope, project.configuration));
        if (tarball == null) {
            throw new ReportError(ReportError.NETWORK_ERROR, "Package not found");
        }
        return tarball;
    }

    /**
     * {@code getAuthenticationHeader} of a GET: a scoped package's credentials on a best effort, another's only under
     * npmAlwaysAuth, where no credentials are Yarn's YN0033.
     */
    static @Nullable String getAuthenticationHeader(String registry, @Nullable String scope,
                                                    Configuration configuration) {
        boolean bestEffort = scope != null && !scope.isEmpty();
        Map<String, @Nullable Object> effectiveConfiguration = getAuthConfiguration(registry, scope, configuration);
        if (!bestEffort && !Boolean.TRUE.equals(effectiveConfiguration.get("npmAlwaysAuth"))) {
            return null;
        }
        Object npmAuthToken = effectiveConfiguration.get("npmAuthToken");
        if (Js.truthy(npmAuthToken)) {
            return "Bearer " + Js.string(npmAuthToken);
        }
        Object npmAuthIdent = effectiveConfiguration.get("npmAuthIdent");
        if (Js.truthy(npmAuthIdent)) {
            String ident = Js.string(npmAuthIdent);
            return "Basic " + (ident.contains(":") ?
                    Base64.getEncoder().encodeToString(ident.getBytes(StandardCharsets.UTF_8)) : ident);
        }
        if (!bestEffort) {
            throw new ReportError(ReportError.AUTHENTICATION_NOT_FOUND, "No authentication configured for request");
        }
        return null;
    }

    // getAuthConfiguration: the scope's entry when it holds credentials, the registry's, else the top level's
    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> getAuthConfiguration(String registry, @Nullable String scope,
                                                                      Configuration configuration) {
        Object scopeConfiguration = scope == null ? null : configuration.getMap("npmScopes").get(scope);
        if (scopeConfiguration instanceof Map && (Js.truthy(((Map<?, ?>) scopeConfiguration).get("npmAuthIdent")) ||
                                                  Js.truthy(((Map<?, ?>) scopeConfiguration).get("npmAuthToken")))) {
            return (Map<String, @Nullable Object>) scopeConfiguration;
        }
        Map<String, @Nullable Object> registries = configuration.getMap("npmRegistries");
        String normalizedRegistry = normalizeRegistry(registry);
        Object entry = registries.containsKey(normalizedRegistry) ? registries.get(normalizedRegistry) :
                registries.get(normalizedRegistry.replaceFirst("^[a-z]+:", ""));
        if (entry instanceof Map) {
            return (Map<String, @Nullable Object>) entry;
        }
        Map<String, @Nullable Object> topLevel = new LinkedHashMap<>();
        for (String key : new String[]{"npmAlwaysAuth", "npmAuthIdent", "npmAuthToken"}) {
            topLevel.put(key, configuration.get(key));
        }
        return topLevel;
    }

    private static Map<String, @Nullable Object> pickMetadata(@Nullable Object packument) {
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

    // es-toolkit/compat's pick of the cached field paths
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
    static boolean isConventionalTarballUrl(Locator locator, @Nullable Object url, Configuration configuration) {
        String registry = getScopeRegistry(locator.scope, configuration);
        String path = getLocatorUrl(locator, configuration.getRelease());
        String u = String.valueOf(url).replaceAll("^https?:(//(?:[^/]+\\.)?npmjs.org(?:\\z|/))", "https:$1");
        registry = registry.replaceAll("^https://registry\\.npmjs\\.org(\\z|/)", "https://registry.yarnpkg.com$1");
        u = u.replaceAll("^https://registry\\.npmjs\\.org(\\z|/)", "https://registry.yarnpkg.com$1");
        return u.equals(registry + path) || u.equals(registry + path.replace("%2f", "/"));
    }

    /**
     * NpmSemverFetcher {@code getLocatorUrl}, the name and version encoded from 4.15.0.
     */
    static String getLocatorUrl(Locator locator, YarnRelease release) {
        String version = YarnpkgSemverUtils.clean(locator.reference.substring(PROTOCOL.length()));
        if (version == null) {
            throw new ReportError(ReportError.RESOLVER_NOT_FOUND, "The npm semver resolver got selected, but the " +
                                                                  "version isn't semver");
        }
        if (release.encodesRegistryPaths()) {
            return getIdentUrl(locator, release) + "/-/" + encodeURIComponent(locator.name) + "-" +
                   encodeURIComponent(version) + ".tgz";
        }
        return getIdentUrl(locator) + "/-/" + locator.name + "-" + version + ".tgz";
    }

    /**
     * npmConfigUtils {@code isPackageApproved}: not quarantined by {@code npmMinimalAgeGate}, or preapproved.
     */
    static boolean isPackageApproved(Project project, Ident ident, @Nullable Object version,
                                     @Nullable Object publishTimes) {
        return !shouldBeQuarantined(project, ident, version, publishTimes) ||
               isPreapproved(project.configuration, ident, version);
    }

    /**
     * npmConfigUtils {@code getMinimalAgeGate} (Yarn 4.17.0): a scoped package's {@code npmScopes} entry's gate, which
     * the entry's own default fills, before the global one; the global one before 4.17.0.
     */
    static @Nullable Object getMinimalAgeGate(Ident ident, Configuration configuration) {
        if (configuration.getRelease().gatesAgePerScope() && ident.scope != null && !ident.scope.isEmpty()) {
            Object scopeConfiguration = configuration.getMap("npmScopes").get(ident.scope);
            Object scopeGate = scopeConfiguration instanceof Map ?
                    ((Map<?, ?>) scopeConfiguration).get("npmMinimalAgeGate") : null;
            if (scopeGate instanceof Number) {
                return scopeGate;
            }
        }
        return configuration.get("npmMinimalAgeGate");
    }

    private static boolean shouldBeQuarantined(Project project, Ident ident, @Nullable Object version,
                                               @Nullable Object publishTimes) {
        Object minimalAgeGate = getMinimalAgeGate(ident, project.configuration);
        if (!Js.truthy(minimalAgeGate)) {
            return false;
        }
        if (!project.configuration.getRelease().quarantinesVersionsWithoutPublishTime()) {
            // new Date(publishTimes[version]): no times throw, no time of the version is an Invalid Date, no age
            if (publishTimes == null || publishTimes == Syml.UNDEFINED) {
                throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of " +
                                                      (publishTimes == null ? "null" : "undefined") +
                                                      " (reading '" + Js.string(version) + "')");
            }
            if (Js.get(publishTimes, Js.string(version)) == Syml.UNDEFINED) {
                return false;
            }
        }
        Object versionTime = Js.get(publishTimes, Js.string(version));
        if (versionTime == Syml.UNDEFINED) {
            return true;
        }
        double ageMinutes = (project.now.toEpochMilli() - getTime(versionTime, ident, version)) / 60 / 1000;
        return ageMinutes < ((Number) minimalAgeGate).doubleValue();
    }

    // new Date(value).getTime()
    private static double getTime(@Nullable Object value, Ident ident, @Nullable Object version) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? 1 : 0;
        }
        if (value instanceof Number) {
            return V8Date.timeClip(((Number) value).doubleValue());
        }
        String text = Js.string(value);
        String what = "the publish time \"" + text + "\" of " + StructUtils.stringifyIdent(ident) + "@" +
                      Js.string(version);
        Double time = V8Date.parse(text);
        if (time == null) {
            throw new CannotKnow("the host's time zone (" + what + " names no offset)");
        }
        if (!V8Date.isDateTimeString(text) && !(value instanceof Map)) {
            throw new CannotKnow("how V8's legacy date parser reads " + what);
        }
        return time;
    }

    private static boolean isPreapproved(Configuration configuration, Ident ident, @Nullable Object version) {
        Object entries = configuration.get("npmPreapprovedPackages");
        if (entries instanceof List) {
            for (Object entry : (List<?>) entries) {
                if (checkIdent(ident, version, Js.string(entry))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean checkIdent(Ident ident, @Nullable Object version, String entry) {
        Descriptor validator = StructUtils.tryParseDescriptor(entry);
        if (validator == null) {
            return false;
        }
        if (!validator.identHash.equals(ident.identHash) &&
            !isMatch(StructUtils.stringifyIdent(ident), StructUtils.stringifyIdent(validator))) {
            return false;
        }
        if ("unknown".equals(validator.range)) {
            return true;
        }
        YarnpkgSemverUtils.Range validatorRange = YarnpkgSemverUtils.validRange(validator.range);
        if (validatorRange == null) {
            return false;
        }
        return version instanceof String && validatorRange.test((String) version);
    }

    private static boolean isMatch(String str, String pattern) {
        return Picomatch.matcher(pattern, MICROMATCH).test(str);
    }
}
