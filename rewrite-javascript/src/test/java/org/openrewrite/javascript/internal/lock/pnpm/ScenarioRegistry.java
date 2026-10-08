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

/**
 * The registry documents a scenario recorded for the phase that read a lock.
 */
final class ScenarioRegistry implements RegistryMetadata {

    /**
     * A document the recording does not hold.
     */
    static final class RouteNotRecorded extends RuntimeException {
        RouteNotRecorded(String route) {
            super("not recorded: " + route);
        }
    }

    private final PnpmScenarios.Scenario scenario;
    private final String phase;

    ScenarioRegistry(PnpmScenarios.Scenario scenario, String phase) {
        this.scenario = scenario;
        this.phase = phase;
    }

    @Override
    public @Nullable String packument(String registry, String name, boolean fullMetadata) {
        String document = scenario.packument(name, fullMetadata, phase);
        if (document == null) {
            throw new RouteNotRecorded("GET " + registry + name + (fullMetadata ? " (full)" : ""));
        }
        return document;
    }

    // the recorder never saves this route; every request pnpm made for it answered 404
    @Override
    public @Nullable String attestations(String registry, String name, String version) {
        return null;
    }
}
