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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Port of @pnpm/store.connection-manager src/index.ts and createNewStoreController.ts (pnpm 11.2.2): the npm resolver
 * of the store controller pnpm creates, which checks the named registries, and its resolution verifiers.
 */
final class CreateNewStoreController {

    private final RegistryMetadata registry;
    private final Instant now;

    /**
     * The Node.js version the host runs pnpm on, null where it does not say.
     */
    private final @Nullable String nodeVersion;

    CreateNewStoreController(RegistryMetadata registry, Instant now) {
        this(registry, now, null);
    }

    CreateNewStoreController(RegistryMetadata registry, Instant now, @Nullable String nodeVersion) {
        this.registry = registry;
        this.now = now;
        this.nodeVersion = nodeVersion;
    }

    /**
     * store/index's ReadOnlyStoreIndex of 11.7.0, which frozenStore opens the store index with
     * ({@link PnpmRelease#opensAFrozenStoreReadOnly}): Node.js must open a SQLite "immutable" URI, and the store
     * must be seeded, which no input says.
     */
    private void openFrozenStore() {
        if (nodeVersion == null) {
            throw new CannotKnow("the Node.js version of the host, which frozenStore needs to be >=22.15.0, " +
                                 ">=23.11.0, or >=24.0.0");
        }
        String version = nodeVersion.startsWith("v") ? nodeVersion.substring(1) : nodeVersion;
        String[] parts = version.split("\\.", -1);
        double major = number(parts[0]);
        double minor = parts.length > 1 ? number(parts[1]) : Double.NaN;
        boolean supported = !(major < 22) && (major != 22 || minor >= 15) && (major != 23 || minor >= 11);
        if (!supported) {
            throw new PnpmError("FROZEN_STORE_UNSUPPORTED_NODE", "frozenStore opens the store index read-only via a " +
                    "SQLite \"immutable\" URI, which requires Node.js >=22.15.0, >=23.11.0, or >=24.0.0, but the " +
                    "current version is " + version + ". Upgrade Node.js, or run without frozenStore.");
        }
        throw new CannotKnow("whether the store frozenStore opens read-only is seeded (its index.db and files)");
    }

    // Number(s) of a version's digits: NaN for anything else
    private static double number(String s) {
        if (s.isEmpty()) {
            return 0;
        }
        for (char c : s.toCharArray()) {
            if (c < '0' || c > '9') {
                return Double.NaN;
            }
        }
        return Double.parseDouble(s);
    }

    /**
     * {@code createStoreController({ ...config, ...context })}: the config's own fetchFullMetadata.
     */
    NpmResolver createStoreController(PnpmOptions options) {
        return createNewStoreController(options, options.get("fetchFullMetadata"));
    }

    /**
     * The store controller installDeps creates for the install command: before 11.13.0 install.ts passes
     * getFetchFullMetadata's {@code supportedArchitectures.libc && true} over the config's fetchFullMetadata, from
     * 11.13.0 nothing ({@link PnpmRelease#fetchesFullMetadataForLibcAndNoDowngrade}).
     */
    NpmResolver createInstallStoreController(PnpmOptions options) {
        return createNewStoreController(options, options.getFetchFullMetadata(), options.get("fetchFullMetadata"));
    }

    /**
     * The failure of the store controller pnpm creates, instead of throwing it; null when it is created.
     */
    @Nullable
    PnpmError storeControllerFailure(PnpmOptions options) {
        try {
            createStoreController(options);
            return null;
        } catch (PnpmError e) {
            return e;
        }
    }

    NpmResolver createNewStoreController(PnpmOptions options, @Nullable Object fetchFullMetadata) {
        return createNewStoreController(options, fetchFullMetadata, fetchFullMetadata);
    }

    /**
     * createNewStoreController where 11.2.2's caller passes {@code fetchFullMetadata} and 11.13.0's
     * {@code fetchFullMetadataFrom11130}, each of which, where set, decides whether the resolver fetches full
     * packuments: unset, 11.2.2 fetches them under time-based resolution or trustPolicy no-downgrade where the registry
     * has no time field, 11.13.0 (shouldFetchFullMetadata) wherever supportedArchitectures sets libc, under
     * no-downgrade, and under time-based resolution where the registry has no time field
     * ({@link PnpmRelease#fetchesFullMetadataForLibcAndNoDowngrade}), asked where the two differ.
     */
    private NpmResolver createNewStoreController(PnpmOptions options, @Nullable Object fetchFullMetadata,
                                                 @Nullable Object fetchFullMetadataFrom11130) {
        if (options.isTrue("frozenStore") && options.getRelease().opensAFrozenStoreReadOnly()) {
            openFrozenStore();
        }
        boolean timeBased = "time-based".equals(options.get("resolutionMode"));
        boolean noDowngrade = "no-downgrade".equals(options.get("trustPolicy"));
        boolean noTimeField = !options.isTrue("registrySupportsTimeField");
        Map<String, @Nullable Object> supportedArchitectures = options.getSupportedArchitectures();
        Object libc = supportedArchitectures == null ? null : supportedArchitectures.get("libc");
        boolean before = fetchFullMetadata != null && fetchFullMetadata != Js.UNDEFINED ?
                Js.truthy(fetchFullMetadata) : (timeBased || noDowngrade) && noTimeField;
        boolean after = fetchFullMetadataFrom11130 != null && fetchFullMetadataFrom11130 != Js.UNDEFINED ?
                Js.truthy(fetchFullMetadataFrom11130) :
                libc != null && libc != Js.UNDEFINED || noDowngrade || timeBased && noTimeField;
        boolean fullMetadata = before == after || !options.getRelease().fetchesFullMetadataForLibcAndNoDowngrade() ?
                before : after;
        PnpmRelease release = options.getRelease();
        Double minimumReleaseAge = options.getNumber("minimumReleaseAge");
        return new NpmResolver(registry, options.getRegistries(), namedRegistries(options),
                options.get("saveWorkspaceProtocol"),
                !Boolean.FALSE.equals(options.get("minimumReleaseAgeIgnoreMissingTime")), fullMetadata, now, release,
                minimumReleaseAge != null && minimumReleaseAge != 0 && !minimumReleaseAge.isNaN() &&
                Boolean.TRUE.equals(options.get("minimumReleaseAgeStrict")));
    }

    // the named registries the npm resolver reads, each checked, from 11.1.0
    static Map<String, String> namedRegistries(PnpmOptions options) {
        return options.getRelease().readsNamedRegistries() ?
                NpmResolver.mergeNamedRegistries(options.getNamedRegistries()) : Collections.emptyMap();
    }

    /**
     * createClient's resolutionVerifiers: the npm verifier when minimumReleaseAge or trustPolicy is active.
     */
    List<ResolutionVerifier> resolutionVerifiers(PnpmOptions options) {
        NpmResolutionVerifier npmVerifier = NpmResolutionVerifier.createNpmResolutionVerifier(
                NpmResolutionVerifier.Options.builder()
                        .minimumReleaseAge(options.getNumber("minimumReleaseAge"))
                        .minimumReleaseAgeExclude(options.getStrings("minimumReleaseAgeExclude"))
                        .ignoreMissingTimeField(
                                Boolean.TRUE.equals(options.get("minimumReleaseAgeIgnoreMissingTime")))
                        .trustPolicy(options.get("trustPolicy") == null ? null :
                                Js.string(options.get("trustPolicy")))
                        .trustPolicyExclude(options.getStrings("trustPolicyExclude"))
                        .trustPolicyIgnoreAfter(options.getNumber("trustPolicyIgnoreAfter"))
                        .registries(options.getRegistries())
                        .namedRegistries(options.getNamedRegistries())
                        .fetch(registry)
                        .now(now)
                        .release(options.getRelease())
                        .build());
        return npmVerifier == null ? Collections.emptyList() : Collections.singletonList(npmVerifier);
    }
}
