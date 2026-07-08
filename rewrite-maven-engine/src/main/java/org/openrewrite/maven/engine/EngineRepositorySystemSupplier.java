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
package org.openrewrite.maven.engine;

import org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.eclipse.aether.transport.file.FileTransporterFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * The stock no-DI supplier ({@code RepositorySystemSupplier}) customized purely through its {@code protected create*()}
 * override points — no container, no reflection. Two deliberate departures from the bare mvn3 supplier:
 * <ul>
 *   <li>{@link #createTransporterFactories()} drops the stock Apache http transport, leaving
 *       {@link HttpSenderTransporterFactory} as the <em>only</em> http/https transport (plus file).</li>
 *   <li>{@link #createRemoteRepositoryFilterSources()} returns nothing: the bare mvn3 supplier enables GroupId and
 *       Prefixes filter sources ({@code DEFAULT_ENABLED=true}) that fetch {@code .meta/prefixes.txt} out of band and
 *       even reach live Central — non-hermetic and a parity divergence (Maven 3.9's resolver does not). Disabling
 *       them is spike-proven necessary (SPIKE-RESULTS discrepancy #1, DESIGN §5.3).</li>
 * </ul>
 */
class EngineRepositorySystemSupplier extends RepositorySystemSupplier {

    @Override
    protected Map<String, TransporterFactory> createTransporterFactories() {
        Map<String, TransporterFactory> factories = new HashMap<>();
        factories.put(FileTransporterFactory.NAME, new FileTransporterFactory());
        // Deliberate absence of ApacheTransporterFactory: HttpSenderTransporter is the sole http/https transport.
        factories.put("openrewrite-http", new HttpSenderTransporterFactory());
        return factories;
    }

    @Override
    protected Map<String, RemoteRepositoryFilterSource> createRemoteRepositoryFilterSources() {
        return new HashMap<>();
    }
}
