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

import org.openrewrite.maven.engine.EngineRepositorySystemSupplier;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.impl.ArtifactDescriptorReader;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.impl.MetadataResolver;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.impl.VersionResolver;

/**
 * The collect-side {@code RepositorySystem}: {@link EngineRepositorySystemSupplier} (which owns the transport-monopoly
 * and RRF-off overrides — the {@link org.openrewrite.maven.engine.HttpSenderTransporterFactory} is the sole http/https
 * transport and the {@code prefixes.txt}/groupId filter sources stay off) with three collect-specific components wired
 * in through its {@code protected create*()} hooks: {@link EngineDescriptorReader} (poms read via Phase 2's model
 * building), {@link PinnedVersionResolver} (pinned snapshots win transitively), and {@link RegionMetadataResolver}
 * (metadata reads/writes route through {@code MavenPomCache}'s metadata region). This is a distinct system from
 * {@code MavenEngine}'s because the collector needs the descriptor reader wired in; extending the shared supplier keeps
 * the transport/filter override set identical to the model-side system by construction.
 */
class EngineCollectSystemSupplier extends EngineRepositorySystemSupplier {

    @Override
    protected ArtifactDescriptorReader createArtifactDescriptorReader() {
        return new EngineDescriptorReader();
    }

    @Override
    protected VersionResolver createVersionResolver() {
        return new PinnedVersionResolver(super.createVersionResolver());
    }

    @Override
    protected MetadataResolver createMetadataResolver() {
        return new RegionMetadataResolver(super.createMetadataResolver());
    }
}
