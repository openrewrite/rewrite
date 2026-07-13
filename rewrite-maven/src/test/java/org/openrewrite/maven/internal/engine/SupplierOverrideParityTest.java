/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.maven.internal.engine;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.engine.EngineRepositorySystemSupplier;
import org.openrewrite.maven.engine.HttpSenderTransporterFactory;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.spi.connector.transport.TransporterFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The transport-monopoly and RRF-off overrides are owned by exactly one class ({@link EngineRepositorySystemSupplier}):
 * the collect-side supplier subclasses it rather than re-declaring them, so both {@code RepositorySystem}s expose the
 * identical override set (only {@code file} + {@code HttpSender} http/https transports, no Apache transport; no remote
 * repository filter sources). Proves the phase3-results-a deviation #2 duplication is gone.
 */
class SupplierOverrideParityTest {

    // Same-package probe exposing the protected create*() factories on the collect supplier.
    private static final class Probe extends EngineCollectSystemSupplier {
        Map<String, TransporterFactory> transporters() {
            return createTransporterFactories();
        }

        Map<String, ?> filterSources() {
            return createRemoteRepositoryFilterSources();
        }
    }

    @Test
    void collectSupplierInheritsTheEngineOverrideOwner() {
        assertThat(EngineRepositorySystemSupplier.class)
                .as("the collect supplier must inherit the single override owner, not re-declare the overrides")
                .isAssignableFrom(EngineCollectSystemSupplier.class);
    }

    @Test
    void collectSupplierExposesOnlyFileAndHttpSenderTransports() {
        Map<String, TransporterFactory> transporters = new Probe().transporters();
        assertThat(transporters.keySet()).containsExactlyInAnyOrder("file", "openrewrite-http");
        assertThat(transporters.get("openrewrite-http")).isInstanceOf(HttpSenderTransporterFactory.class);
        assertThat(transporters.values())
                .noneMatch(f -> f.getClass().getName().toLowerCase().contains("apache"));
    }

    @Test
    void collectSupplierKeepsRemoteRepositoryFilteringOff() {
        assertThat(new Probe().filterSources()).isEmpty();
    }
}
