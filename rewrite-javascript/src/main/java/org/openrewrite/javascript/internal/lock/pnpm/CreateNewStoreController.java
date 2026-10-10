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
import org.openrewrite.javascript.internal.lock.JsYaml;

import java.time.Instant;

/**
 * Port of @pnpm/store.connection-manager src/index.ts and createNewStoreController.ts (pnpm 11.2.2): the npm resolver
 * of the store controller pnpm creates, which checks the named registries.
 */
final class CreateNewStoreController {

    private final RegistryMetadata registry;
    private final Instant now;

    private @Nullable PnpmError unlessLocked;

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
     * The store controller pnpm creates unless the env document of a lock the inputs do not hold records the release:
     * false when it fails, which pnpm does either way, since its next store controller takes the same config.
     */
    boolean createStoreControllerUnlessLocked(PnpmOptions options) {
        try {
            createStoreController(options);
            return true;
        } catch (PnpmError e) {
            unlessLocked = e;
            return false;
        }
    }

    /**
     * What pnpm ends with when {@code e} comes before its next store controller: e, or a store controller's failure
     * that came first unless the release was locked.
     */
    RuntimeException beforeNextStoreController(RuntimeException e) {
        PnpmError failed = unlessLocked;
        if (failed == null || e instanceof PnpmError && failed.getCode().equals(((PnpmError) e).getCode()) &&
                              String.valueOf(failed.getMessage()).equals(e.getMessage())) {
            return e;
        }
        String other = e instanceof CannotKnow ? ((CannotKnow) e).getSubject() : "with " +
                (e instanceof PnpmError ? "[" + ((PnpmError) e).getCode() + "] " : "") + e.getMessage();
        return new CannotKnow("whether pnpm fails with [" + failed.getCode() + "] " + failed.getMessage() +
                              " (unless the env document of the workspace's " + LockfileReader.WANTED_LOCKFILE +
                              " records the release) or else " + other);
    }

    NpmResolver createNewStoreController(PnpmOptions options, @Nullable Object fetchFullMetadata) {
        boolean fullMetadata = fetchFullMetadata != null && fetchFullMetadata != JsYaml.UNDEFINED ?
                Js.truthy(fetchFullMetadata) : ("time-based".equals(options.get("resolutionMode")) ||
                                                "no-downgrade".equals(options.get("trustPolicy"))) &&
                                               !options.isTrue("registrySupportsTimeField");
        return new NpmResolver(registry, options.getRegistries(),
                NpmResolver.mergeNamedRegistries(options.getNamedRegistries()), options.get("saveWorkspaceProtocol"),
                !Boolean.FALSE.equals(options.get("minimumReleaseAgeIgnoreMissingTime")), fullMetadata, now);
    }
}
