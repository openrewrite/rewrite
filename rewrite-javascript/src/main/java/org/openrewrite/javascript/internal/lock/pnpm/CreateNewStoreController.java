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

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Port of @pnpm/store.connection-manager src/index.ts and createNewStoreController.ts (pnpm 11.2.2): the npm resolver
 * of the store controller pnpm creates, which checks the named registries, and its resolution verifiers.
 */
final class CreateNewStoreController {

    private final RegistryMetadata registry;
    private final Instant now;

    CreateNewStoreController(RegistryMetadata registry, Instant now) {
        this.registry = registry;
        this.now = now;
    }

    /**
     * {@code createStoreController({ ...config, ...context })}: the config's own fetchFullMetadata.
     */
    NpmResolver createStoreController(PnpmOptions options) {
        return createNewStoreController(options, options.get("fetchFullMetadata"));
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
        boolean fullMetadata = fetchFullMetadata != null && fetchFullMetadata != Js.UNDEFINED ?
                Js.truthy(fetchFullMetadata) : ("time-based".equals(options.get("resolutionMode")) ||
                                                "no-downgrade".equals(options.get("trustPolicy"))) &&
                                               !options.isTrue("registrySupportsTimeField");
        return new NpmResolver(registry, options.getRegistries(),
                NpmResolver.mergeNamedRegistries(options.getNamedRegistries()), options.get("saveWorkspaceProtocol"),
                !Boolean.FALSE.equals(options.get("minimumReleaseAgeIgnoreMissingTime")), fullMetadata, now);
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
                        .build());
        return npmVerifier == null ? Collections.emptyList() : Collections.singletonList(npmVerifier);
    }
}
