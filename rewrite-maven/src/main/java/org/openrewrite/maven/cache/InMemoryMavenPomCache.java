/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import lombok.Value;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenMetadata;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.net.URI;
import java.util.Optional;

public class InMemoryMavenPomCache implements MavenPomCache {
    @Value
    private static class MetadataKey {
        URI repository;
        GroupArtifactVersion gav;
    }

    private final Cache<ResolvedGroupArtifactVersion, Optional<Pom>> pomCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .build();

    private final Cache<MetadataKey, Optional<MavenMetadata>> mavenMetadataCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .build();

    private final Cache<MavenRepository, Optional<MavenRepository>> repositoryCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .build();

    public InMemoryMavenPomCache() {
        CaffeineCacheMetrics.monitor(Metrics.globalRegistry, pomCache, "Maven POMs");
        CaffeineCacheMetrics.monitor(Metrics.globalRegistry, mavenMetadataCache, "Maven metadata");
        CaffeineCacheMetrics.monitor(Metrics.globalRegistry, repositoryCache, "Maven repositories");
    }

    @Nullable
    @Override
    public Optional<MavenMetadata> getMavenMetadata(URI repo, GroupArtifactVersion gav) {
        return mavenMetadataCache.getIfPresent(new MetadataKey(repo, gav));
    }

    @Override
    public void putMavenMetadata(URI repo, GroupArtifactVersion gav, @Nullable MavenMetadata metadata) {
        mavenMetadataCache.put(new MetadataKey(repo, gav), Optional.ofNullable(metadata));
    }

    @Nullable
    @Override
    public Optional<Pom> getPom(ResolvedGroupArtifactVersion gav) {
        return pomCache.getIfPresent(gav);
    }

    @Override
    public void putPom(ResolvedGroupArtifactVersion gav, @Nullable Pom pom) {
        pomCache.put(gav, Optional.ofNullable(pom));
    }

    @Override
    @Nullable
    public Optional<MavenRepository> getNormalizedRepository(MavenRepository repository) {
        return repositoryCache.getIfPresent(repository);
    }

    @Override
    public void putNormalizedRepository(MavenRepository repository, MavenRepository normalized) {
        repositoryCache.put(repository, Optional.ofNullable(normalized));
    }
}
