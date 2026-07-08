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

import org.openrewrite.maven.engine.HttpSenderTransporterFactory;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.impl.VersionResolver;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.spi.connector.filter.RemoteRepositoryFilterSource;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.transport.file.FileTransporterFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * The collect-side {@code RepositorySystem}: the stock no-DI supplier with two components replaced through its
 * {@code protected create*()} hooks — {@link EngineDescriptorReader} (poms read via Phase 2's model building) and
 * {@link PinnedVersionResolver} (pinned snapshots win transitively). The transport and remote-repository-filter
 * overrides duplicate {@code EngineRepositorySystemSupplier} (which is package-private in the engine module): the
 * {@link HttpSenderTransporterFactory} is the sole http/https transport, and the {@code prefixes.txt}/groupId filter
 * sources stay off (SPIKE-RESULTS discrepancy #1). This is a distinct system from {@code MavenEngine}'s because the
 * collector needs the descriptor reader wired in; the session template it is driven with still mirrors Maven 3.9's.
 */
class EngineCollectSystemSupplier extends RepositorySystemSupplier {

    @Override
    protected ArtifactDescriptorReader createArtifactDescriptorReader() {
        return new EngineDescriptorReader();
    }

    @Override
    protected VersionResolver createVersionResolver() {
        return new PinnedVersionResolver(super.createVersionResolver());
    }

    @Override
    protected Map<String, TransporterFactory> createTransporterFactories() {
        Map<String, TransporterFactory> factories = new HashMap<>();
        factories.put(FileTransporterFactory.NAME, new FileTransporterFactory());
        factories.put("openrewrite-http", new HttpSenderTransporterFactory());
        return factories;
    }

    @Override
    protected Map<String, RemoteRepositoryFilterSource> createRemoteRepositoryFilterSources() {
        return new HashMap<>();
    }
}
