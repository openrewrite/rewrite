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
package org.openrewrite.javascript.internal.lock.pnpm;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.V8Date;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;
import org.openrewrite.javascript.semver.NodeSemver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.openrewrite.javascript.internal.lock.UriComponent.encodeURIComponent;

/**
 * Port of @pnpm/resolving.npm-resolver src/createNpmResolutionVerifier.ts and src/fetchAttestationPublishedAt.ts
 * (pnpm 11.2.2).
 */
final class NpmResolutionVerifier implements ResolutionVerifier {

    static final String MINIMUM_RELEASE_AGE_VIOLATION_CODE = "MINIMUM_RELEASE_AGE_VIOLATION";
    static final String TRUST_DOWNGRADE_VIOLATION_CODE = "TRUST_DOWNGRADE";

    private static final Map<String, String> BUILTIN_NAMED_REGISTRIES = new LinkedHashMap<>();

    static {
        BUILTIN_NAMED_REGISTRIES.put("gh", "https://npm.pkg.github.com/");
    }

    @Value
    @Builder
    static class Options {
        @Nullable
        Double minimumReleaseAge;

        @Nullable
        List<String> minimumReleaseAgeExclude;

        boolean ignoreMissingTimeField;

        @Nullable
        String trustPolicy;

        @Nullable
        List<String> trustPolicyExclude;

        @Nullable
        Double trustPolicyIgnoreAfter;

        @NonNull
        Map<String, String> registries;

        @Nullable
        Map<String, @Nullable Object> namedRegistries;

        @NonNull
        RegistryMetadata fetch;

        @NonNull
        Instant now;

        @Builder.Default
        PnpmRelease release = PnpmRelease.PORTED;
    }

    private final Options opts;
    private final boolean ageCheckActive;
    private final boolean trustCheckActive;
    private final double cutoff;
    private final @Nullable Function<String, Object> excludePolicy;
    private final @Nullable Function<String, Object> trustExcludePolicy;
    private final List<String> namedRegistryPrefixes;

    private final Map<String, @Nullable Map<String, @Nullable Object>> abbreviatedMetaCache = new HashMap<>();
    private final Map<String, RuntimeException> abbreviatedMetaErrors = new HashMap<>();
    private final Map<String, Lookup<@Nullable Object>> publishedAtCache = new HashMap<>();
    private final Map<String, Lookup<@Nullable Object>> fullMetaCache = new HashMap<>();
    private final Map<String, Lookup<Map<String, @Nullable Object>>> fullMetaForTrustCache = new HashMap<>();

    private NpmResolutionVerifier(Options opts) {
        this.opts = opts;
        this.ageCheckActive = isActive(opts.getMinimumReleaseAge());
        this.trustCheckActive = "no-downgrade".equals(opts.getTrustPolicy());
        this.cutoff = ageCheckActive ?
                opts.getNow().toEpochMilli() - opts.getMinimumReleaseAge() * 60 * 1000 : 0;
        this.excludePolicy = opts.getMinimumReleaseAgeExclude() != null &&
                             !opts.getMinimumReleaseAgeExclude().isEmpty() ?
                PackageVersionPolicy.createPackageVersionPolicyOrThrow(opts.getMinimumReleaseAgeExclude(),
                        "minimumReleaseAgeExclude", opts.getRelease()) : null;
        this.trustExcludePolicy = opts.getTrustPolicyExclude() != null && !opts.getTrustPolicyExclude().isEmpty() ?
                PackageVersionPolicy.createPackageVersionPolicyOrThrow(opts.getTrustPolicyExclude(),
                        "trustPolicyExclude", opts.getRelease()) : null;
        this.namedRegistryPrefixes = namedRegistryPrefixes(opts.getNamedRegistries());
    }

    /**
     * {@code createNpmResolutionVerifier}: null when no policy is active, which from 11.5.2 the tarball URL binding
     * always is ({@link PnpmRelease#bindsALockedTarballUrlToTheRegistry}).
     */
    static @Nullable NpmResolutionVerifier createNpmResolutionVerifier(Options opts) {
        if (!isActive(opts.getMinimumReleaseAge()) && !"no-downgrade".equals(opts.getTrustPolicy()) &&
            !opts.getRelease().bindsALockedTarballUrlToTheRegistry()) {
            return null;
        }
        return new NpmResolutionVerifier(opts);
    }

    private static boolean isActive(@Nullable Double minimumReleaseAge) {
        return minimumReleaseAge != null && minimumReleaseAge != 0 && !minimumReleaseAge.isNaN();
    }

    private static List<String> namedRegistryPrefixes(@Nullable Map<String, @Nullable Object> namedRegistries) {
        Map<String, @Nullable Object> all = new LinkedHashMap<>(BUILTIN_NAMED_REGISTRIES);
        if (namedRegistries != null) {
            for (String key : Js.keys(namedRegistries)) {
                all.put(key, namedRegistries.get(key));
            }
        }
        List<String> prefixes = new ArrayList<>();
        for (String key : Js.keys(all)) {
            WhatwgUrl parsed = WhatwgUrl.tryParse(Js.string(all.get(key)));
            if (parsed == null) {
                continue;
            }
            String pathname = parsed.getPathname().endsWith("/") ? parsed.getPathname() : parsed.getPathname() + "/";
            prefixes.add(parsed.getOrigin() + pathname);
        }
        prefixes.sort((a, b) -> b.length() - a.length());
        return prefixes;
    }

    @Override
    public Result verify(@Nullable Object resolution, String name, String version) {
        return verify(resolution, name, version, null);
    }

    @Override
    public Result verify(@Nullable Object resolution, String name, String version,
                         @Nullable String nonSemverVersion) {
        if (!isNpmRegistryResolution(resolution, opts.getRelease())) {
            return Result.OK;
        }
        PnpmRelease release = opts.getRelease();
        @SuppressWarnings("unchecked")
        Map<String, @Nullable Object> registryResolution = (Map<String, @Nullable Object>) resolution;
        // from 11.9.0 a registry tarball without an integrity is a violation, ahead of a non-semver dep path's pass,
        // and so are a non-semver version and a tarball that is no string
        // (PnpmRelease#verifiesALockedTarballsIntegrity)
        Object integrity = registryResolution.get("integrity");
        if (!(integrity instanceof String && !((String) integrity).isEmpty()) &&
            release.verifiesALockedTarballsIntegrity()) {
            return new Result(false, MISSING_TARBALL_INTEGRITY_VIOLATION_CODE,
                    "has no \"integrity\" field, so its downloaded tarball cannot be verified");
        }
        if (nonSemverVersion != null && release.bindsALockedTarballUrlToTheRegistry()) {
            return Result.OK;
        }
        if (NodeSemver.valid(version, NodeSemver.Options.DEFAULT) == null) {
            return release.verifiesALockedTarballsIntegrity() ? new Result(false, TARBALL_URL_MISMATCH_VIOLATION_CODE,
                    "has a non-semver version (\"" + version + "\") and so cannot be verified against the registry's " +
                    "published metadata") : Result.OK;
        }
        Object tarballUrl = registryResolution.get("tarball");
        if (tarballUrl != null && tarballUrl != Js.UNDEFINED && !(tarballUrl instanceof String) &&
            release.verifiesALockedTarballsIntegrity()) {
            return new Result(false, TARBALL_URL_MISMATCH_VIOLATION_CODE,
                    "has a non-string \"tarball\" field, so its URL cannot be verified");
        }
        if (tarballUrl instanceof String && release.bindsALockedTarballUrlToTheRegistry()) {
            Result urlViolation = runTarballUrlCheck(pickRegistryForVersion(name, tarballUrl), name, version,
                    (String) tarballUrl);
            if (urlViolation != null) {
                return urlViolation;
            }
        }
        boolean ageApplies = ageCheckActive && !PackageVersionPolicy.isExcluded(excludePolicy, name, version);
        boolean trustApplies = trustCheckActive &&
                               !PackageVersionPolicy.isExcluded(trustExcludePolicy, name, version);
        if (!ageApplies && !trustApplies) {
            return Result.OK;
        }
        String registry = pickRegistryForVersion(name, tarballUrl);
        if (ageApplies) {
            Result ageViolation = runAgeCheck(registry, name, version);
            if (ageViolation != null) {
                return ageViolation;
            }
        }
        if (trustApplies) {
            Result trustViolation = runTrustCheck(registry, name, version);
            if (trustViolation != null) {
                return trustViolation;
            }
        }
        return Result.OK;
    }

    static final String TARBALL_URL_MISMATCH_VIOLATION_CODE = "TARBALL_URL_MISMATCH";
    static final String MISSING_TARBALL_INTEGRITY_VIOLATION_CODE = "MISSING_TARBALL_INTEGRITY";

    // runTarballUrlCheck (11.5.2): the lock's tarball URL is the one the registry's metadata lists for the version
    @SuppressWarnings("unchecked")
    private @Nullable Result runTarballUrlCheck(String registry, String name, String version,
                                                String lockfileTarball) {
        Map<String, @Nullable Object> meta = fetchAbbreviatedMeta(registry, name);
        // from 11.9.0 the metadata's fetch error fails the verification; before, the URL could not be verified
        // (PnpmRelease#failsVerificationOnARegistryFetchError)
        RuntimeException error = abbreviatedMetaErrors.get(registry + "\0" + name);
        if (error != null && opts.getRelease().failsVerificationOnARegistryFetchError()) {
            throw error;
        }
        Object versions = meta == null ? null : meta.get("versions");
        Object manifest = versions instanceof Map ? ((Map<String, @Nullable Object>) versions).get(version) : null;
        Object registryTarball = property(property(manifest, "dist"), "tarball");
        if (registryTarball != null && registryTarball != Js.UNDEFINED &&
            canonicalTarballUrl(lockfileTarball).equals(canonicalTarballUrl(Js.string(registryTarball)))) {
            return null;
        }
        return new Result(false, TARBALL_URL_MISMATCH_VIOLATION_CODE, registryTarball == null ||
                                                                      registryTarball == Js.UNDEFINED ?
                "could not be verified against the registry's published metadata" :
                "has a tarball URL (" + lockfileTarball + ") that does not match the registry's published " +
                "metadata (" + Js.string(registryTarball) + ")");
    }

    // canonicalTarballUrl (11.5.2): normalizeRegistryUrl, %2f as /, and the scheme dropped
    private static String canonicalTarballUrl(String url) {
        WhatwgUrl parsed = WhatwgUrl.tryParse(url);
        String normalized = (parsed == null ? url : parsed.toString()).replaceAll("(?i)%2f", "/");
        int schemeEnd = normalized.indexOf("://");
        return schemeEnd == -1 ? normalized : normalized.substring(schemeEnd + 3);
    }

    private @Nullable Result runAgeCheck(String registry, String name, String version) {
        Object published;
        try {
            published = fetchPublishedAt(registry, name, version);
        } catch (PnpmError | JsError e) {
            // from 11.9.0 a fetch error fails the verification (PnpmRelease#failsVerificationOnARegistryFetchError)
            if (opts.getRelease().failsVerificationOnARegistryFetchError()) {
                throw e;
            }
            return new Result(false, MINIMUM_RELEASE_AGE_VIOLATION_CODE,
                    uncheckable("minimumReleaseAge", Js.message(e)));
        }
        if (!Js.truthy(published)) {
            if (opts.isIgnoreMissingTimeField()) {
                return null;
            }
            return new Result(false, MINIMUM_RELEASE_AGE_VIOLATION_CODE,
                    uncheckable("minimumReleaseAge", "version not present in registry manifest"));
        }
        double ts = timeValue(published);
        if (Double.isNaN(ts)) {
            return new Result(false, MINIMUM_RELEASE_AGE_VIOLATION_CODE, "publish timestamp is not a valid date");
        }
        if (ts > cutoff) {
            return new Result(false, MINIMUM_RELEASE_AGE_VIOLATION_CODE, "was published at " + V8Date.toISOString(ts) +
                    ", within the minimumReleaseAge cutoff (" + V8Date.toISOString(cutoff) + ")");
        }
        return null;
    }

    // new Date(value).getTime()
    private static double timeValue(Object value) {
        if (value instanceof Number) {
            return V8Date.timeClip(((Number) value).doubleValue());
        }
        return V8Date.dateParse(Js.string(value));
    }

    private @Nullable Result runTrustCheck(String registry, String name, String version) {
        Map<String, @Nullable Object> meta;
        try {
            meta = fetchFullMetaForTrust(registry, name);
        } catch (PnpmError e) {
            // from 11.9.0 a fetch error fails the verification (PnpmRelease#failsVerificationOnARegistryFetchError)
            if (opts.getRelease().failsVerificationOnARegistryFetchError()) {
                throw e;
            }
            return new Result(false, TRUST_DOWNGRADE_VIOLATION_CODE, uncheckable("trustPolicy", e.getMessage()));
        }
        try {
            TrustChecks.failIfTrustDowngraded(meta, version, trustExcludePolicy, opts.getTrustPolicyIgnoreAfter(),
                    opts.getNow(), opts.getRelease());
        } catch (PnpmError e) {
            return new Result(false, TRUST_DOWNGRADE_VIOLATION_CODE, e.getMessage());
        }
        return null;
    }

    private Map<String, @Nullable Object> fetchFullMetaForTrust(String registry, String name) {
        return fullMetaForTrustCache.computeIfAbsent(registry + "\0" + name,
                k -> Lookup.of(() -> fetchFullMetadata(registry, name))).get();
    }

    private @Nullable Object fetchPublishedAt(String registry, String name, String version) {
        return publishedAtCache.computeIfAbsent(registry + "\0" + name + "\0" + version,
                k -> Lookup.of(() -> resolvePublishedAt(registry, name, version))).get();
    }

    private @Nullable Object resolvePublishedAt(String registry, String name, String version) {
        String abbreviatedShortcut = tryAbbreviatedModifiedShortcut(registry, name, version);
        if (abbreviatedShortcut != null) {
            return abbreviatedShortcut;
        }
        String attestationTime = fetchAttestationPublishedAt(name, version, registry);
        if (attestationTime != null) {
            return attestationTime;
        }
        Object fullMetaTime = fetchFullMetaTime(registry, name);
        return fullMetaTime instanceof Map ? ((Map<?, ?>) fullMetaTime).get(version) : null;
    }

    private @Nullable String tryAbbreviatedModifiedShortcut(String registry, String name, String version) {
        Map<String, @Nullable Object> meta = fetchAbbreviatedMeta(registry, name);
        Object modified = meta == null ? null : meta.get("modified");
        if (!(modified instanceof String)) {
            return null;
        }
        double modifiedMs = V8Date.dateParse((String) modified);
        if (Double.isNaN(modifiedMs)) {
            return null;
        }
        if (modifiedMs >= cutoff) {
            return null;
        }
        Object versions = meta.get("versions");
        if (!Js.truthy(versions) || !in(version, versions)) {
            return null;
        }
        return (String) modified;
    }

    // the `in` operator, which throws over a primitive
    private static boolean in(String key, Object object) {
        if (object instanceof Map) {
            return ((Map<?, ?>) object).containsKey(key);
        }
        if (object instanceof List) {
            return Js.ownKeys(object).contains(key);
        }
        throw new JsError(JsError.ERROR,
                "Cannot use 'in' operator to search for '" + key + "' in " + Js.string(object));
    }

    private @Nullable Map<String, @Nullable Object> fetchAbbreviatedMeta(String registry, String name) {
        String cacheKey = registry + "\0" + name;
        if (!abbreviatedMetaCache.containsKey(cacheKey)) {
            Map<String, @Nullable Object> meta;
            try {
                // from 11.5.2 each version's tarball URL is kept, which the URL binding compares
                meta = opts.getRelease().bindsALockedTarballUrlToTheRegistry() ?
                        fetchMetadata(registry, name, false, MODIFIED_AND_VERSIONS, Collections.emptySet()) :
                        fetchMetadata(registry, name, false, MODIFIED, VERSIONS);
            } catch (PnpmError e) {
                meta = null;
                abbreviatedMetaErrors.put(cacheKey, e);
            }
            abbreviatedMetaCache.put(cacheKey, meta);
        }
        return abbreviatedMetaCache.get(cacheKey);
    }

    private @Nullable Object fetchFullMetaTime(String registry, String name) {
        return fullMetaCache.computeIfAbsent(registry + "\0" + name, k -> Lookup.of(() -> {
            Map<String, @Nullable Object> meta = fetchMetadata(registry, name, true, TIME, Collections.emptySet());
            if (meta == null) {
                throw new JsError(JsError.ERROR, "Cannot read properties of null (reading 'time')");
            }
            return meta.get("time");
        })).get();
    }

    private static final Set<String> MODIFIED = Collections.singleton("modified");
    private static final Set<String> VERSIONS = Collections.singleton("versions");
    private static final Set<String> MODIFIED_AND_VERSIONS = new HashSet<>(Arrays.asList("modified", "versions"));
    private static final Set<String> TIME = Collections.singleton("time");

    /**
     * {@code fetchMetadataFromFromRegistry}, keeping the members the caller reads (see
     * {@link Manifests#parseMembers}), or pnpm's error for a 404 or a body that is not JSON.
     */
    private @Nullable Map<String, @Nullable Object> fetchMetadata(String registry, String pkgName,
                                                                  boolean fullMetadata, Set<String> whole,
                                                                  Set<String> keysOnly) {
        String uri = toUri(pkgName, registry);
        String text = opts.getFetch().packument(registry, pkgName, fullMetadata);
        if (text == null) {
            throw new PnpmError("FETCH_404", "GET " + PnpmError.requestUrl(uri, opts.getRelease()) +
                                             ": Not Found - 404");
        }
        try {
            return Manifests.parseMembers(text, uri, whole, keysOnly);
        } catch (PnpmError e) {
            throw new PnpmError("BROKEN_METADATA_JSON", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, @Nullable Object> fetchFullMetadata(String registry, String pkgName) {
        String uri = toUri(pkgName, registry);
        String text = opts.getFetch().packument(registry, pkgName, true);
        if (text == null) {
            throw new PnpmError("FETCH_404", "GET " + PnpmError.requestUrl(uri, opts.getRelease()) +
                                             ": Not Found - 404");
        }
        Object meta;
        try {
            meta = Manifests.parseValue(text, uri);
        } catch (PnpmError e) {
            throw new PnpmError("BROKEN_METADATA_JSON", e.getMessage());
        }
        if (!(meta instanceof Map)) {
            throw new CannotKnow("what pnpm makes of the document at " + uri + " (JSON, but not an object)");
        }
        return (Map<String, @Nullable Object>) meta;
    }

    private static String toUri(String pkgName, String registry) {
        String encodedName = !pkgName.isEmpty() && pkgName.charAt(0) == '@' ?
                "@" + encodeURIComponent(pkgName.substring(1)) : encodeURIComponent(pkgName);
        WhatwgUrl url = WhatwgUrl.tryParse(encodedName, registry.endsWith("/") ? registry : registry + "/");
        if (url == null) {
            throw new PnpmError("INVALID_URL", "Invalid URL");
        }
        return url.toString();
    }

    /**
     * {@code fetchAttestationPublishedAt}: the earliest {@code integratedTime} of the version's attestations.
     */
    private @Nullable String fetchAttestationPublishedAt(String pkgName, String version, String registry) {
        String text = opts.getFetch().attestations(registry, pkgName, version);
        if (text == null) {
            return null;
        }
        Object body;
        try {
            body = Manifests.parseValue(text, "attestations");
        } catch (PnpmError e) {
            return null;
        }
        return extractPublishedAt(body);
    }

    private static @Nullable String extractPublishedAt(@Nullable Object body) {
        Object attestations = property(body, "attestations");
        if (!(attestations instanceof List)) {
            return null;
        }
        Double earliestSeconds = null;
        for (Object attestation : (List<?>) attestations) {
            Double seconds = readEarliestIntegratedTime(attestation);
            if (seconds == null) {
                continue;
            }
            if (earliestSeconds == null || seconds < earliestSeconds) {
                earliestSeconds = seconds;
            }
        }
        return earliestSeconds == null ? null : V8Date.toISOString(earliestSeconds * 1000);
    }

    private static @Nullable Double readEarliestIntegratedTime(@Nullable Object attestation) {
        Object tlogEntries = property(property(property(attestation, "bundle"), "verificationMaterial"),
                "tlogEntries");
        if (!(tlogEntries instanceof List)) {
            return null;
        }
        Double earliest = null;
        for (Object entry : (List<?>) tlogEntries) {
            if (!(entry instanceof Map)) {
                continue;
            }
            Double seconds = parseIntegratedTimeSeconds(((Map<?, ?>) entry).get("integratedTime"));
            if (seconds == null) {
                continue;
            }
            if (earliest == null || seconds < earliest) {
                earliest = seconds;
            }
        }
        return earliest;
    }

    private static @Nullable Double parseIntegratedTimeSeconds(@Nullable Object raw) {
        double seconds = raw instanceof String ? Js.toNumber((String) raw) :
                raw instanceof Number ? ((Number) raw).doubleValue() : Double.NaN;
        if (Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds <= 0) {
            return null;
        }
        return seconds;
    }

    private String pickRegistryForVersion(String name, @Nullable Object tarballUrl) {
        if (Js.truthy(tarballUrl) && opts.getRelease().bindsALockedTarballUrlToTheRegistry()) {
            // 11.5.2 compares the canonical URLs, scheme and %2f aside
            String normalized = canonicalTarballUrl(Js.string(tarballUrl));
            for (String prefix : namedRegistryPrefixes) {
                if (normalized.startsWith(canonicalTarballUrl(prefix))) {
                    return prefix;
                }
            }
        } else if (Js.truthy(tarballUrl)) {
            WhatwgUrl parsed = WhatwgUrl.tryParse(Js.string(tarballUrl));
            if (parsed != null) {
                String normalized = parsed.toString();
                for (String prefix : namedRegistryPrefixes) {
                    if (normalized.startsWith(prefix)) {
                        return prefix;
                    }
                }
            }
        }
        return PickRegistryForPackage.pickRegistryForPackage(opts.getRegistries(), name, null);
    }

    private static String uncheckable(String policy, @Nullable String why) {
        return "could not be checked against " + policy + " (" + why + ")";
    }

    static boolean isNpmRegistryResolution(@Nullable Object resolution) {
        return isNpmRegistryResolution(resolution, PnpmRelease.PORTED);
    }

    /**
     * {@code isNpmRegistryResolution} as {@code release} reads it: from 11.5.2 a tarball that is neither http nor https
     * is none ({@link PnpmRelease#verifiesOnlyHttpTarballs}); from 11.9.0 {@code isRegistryTarballResolution}, any
     * untyped resolution, with or without a tarball or an integrity, but a git-hosted one told by its URL
     * ({@link PnpmRelease#verifiesALockedTarballsIntegrity} and
     * {@link PnpmRelease#recognizesOnlyCommitArchivesAsGitHosted}, of the same release), asked where the two answer
     * otherwise.
     */
    static boolean isNpmRegistryResolution(@Nullable Object resolution, PnpmRelease release) {
        boolean before = isNpmRegistryResolutionBefore1190(resolution, release);
        boolean registryTarball = isRegistryTarballResolution(resolution);
        return before == registryTarball || !release.verifiesALockedTarballsIntegrity() ? before : registryTarball;
    }

    // isRegistryTarballResolution (11.9.0)
    private static boolean isRegistryTarballResolution(@Nullable Object resolution) {
        if (!(resolution instanceof Map)) {
            return false;
        }
        Map<?, ?> map = (Map<?, ?>) resolution;
        if (map.containsKey("type") && map.get("type") != null && map.get("type") != Js.UNDEFINED) {
            return false;
        }
        Object tarball = map.get("tarball");
        if (tarball instanceof String) {
            if (LockfileUtils.isGitHostedCommitArchive(tarball)) {
                return false;
            }
            WhatwgUrl url = WhatwgUrl.tryParse((String) tarball);
            String protocol = url == null ? null : url.getProtocol();
            return protocol == null || "http:".equals(protocol) || "https:".equals(protocol);
        }
        return true;
    }

    // isNpmRegistryResolution of the releases before 11.9.0
    private static boolean isNpmRegistryResolutionBefore1190(@Nullable Object resolution, PnpmRelease release) {
        if (!(resolution instanceof Map)) {
            return false;
        }
        Map<?, ?> map = (Map<?, ?>) resolution;
        if (map.containsKey("type") && map.get("type") != null && map.get("type") != Js.UNDEFINED) {
            return false;
        }
        if (map.containsKey("gitHosted") && Js.truthy(map.get("gitHosted"))) {
            return false;
        }
        Object tarball = map.get("tarball");
        if (tarball instanceof String) {
            WhatwgUrl url = WhatwgUrl.tryParse((String) tarball);
            String protocol = url == null ? null : url.getProtocol();
            if (protocol != null && !"http:".equals(protocol) && !"https:".equals(protocol) &&
                release.verifiesOnlyHttpTarballs()) {
                return false;
            }
        }
        return map.containsKey("tarball") || map.containsKey("integrity");
    }

    private static @Nullable Object property(@Nullable Object value, String key) {
        return value instanceof Map ? ((Map<?, ?>) value).get(key) : null;
    }

    /**
     * A memoized lookup that settles once, as the promise pnpm caches does: its value or the error it threw.
     */
    private static final class Lookup<T> {
        private final @Nullable T value;
        private final @Nullable RuntimeException error;

        private Lookup(@Nullable T value, @Nullable RuntimeException error) {
            this.value = value;
            this.error = error;
        }

        static <T> Lookup<T> of(Supplier<T> supplier) {
            try {
                return new Lookup<>(supplier.get(), null);
            } catch (PnpmError | JsError e) {
                return new Lookup<>(null, e);
            }
        }

        T get() {
            if (error != null) {
                throw error;
            }
            return value;
        }
    }
}
