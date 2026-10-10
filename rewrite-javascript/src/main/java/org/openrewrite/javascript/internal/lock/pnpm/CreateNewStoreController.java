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

/**
 * Port of @pnpm/store.connection-manager src/createNewStoreController.ts (pnpm 11.2.2): the npm resolver of the store
 * controller an install creates.
 */
final class CreateNewStoreController {

    private CreateNewStoreController() {
    }

    static NpmResolver createNewStoreController(PnpmOptions options, @Nullable Object fetchFullMetadata,
                                                RegistryMetadata registry, Instant now) {
        boolean fullMetadata = fetchFullMetadata != null && fetchFullMetadata != JsYaml.UNDEFINED ?
                Js.truthy(fetchFullMetadata) : ("time-based".equals(options.get("resolutionMode")) ||
                                                "no-downgrade".equals(options.get("trustPolicy"))) &&
                                               !options.isTrue("registrySupportsTimeField");
        return new NpmResolver(registry, options.getRegistries(),
                NpmResolver.mergeNamedRegistries(options.getNamedRegistries()), options.get("saveWorkspaceProtocol"),
                !Boolean.FALSE.equals(options.get("minimumReleaseAgeIgnoreMissingTime")), fullMetadata, now);
    }
}
