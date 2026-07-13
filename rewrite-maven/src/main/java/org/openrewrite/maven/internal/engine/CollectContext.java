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

import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystem;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-run state the collector shares with {@link EngineDescriptorReader} and {@link PinnedVersionResolver}. Both are
 * stateless components wired into the {@link RepositorySystem} at bootstrap, so the run-specific inputs (the effective
 * settings, reactor, pom cache, request repositories, pinned versions) travel on the session's config properties — the
 * same mechanism {@link org.openrewrite.maven.engine.HttpSenderTransporterFactory} uses to resolve its sender. The
 * component reads it back with {@link #from} from the session it is handed.
 * <p>
 * {@link #servedBy} and {@link #descriptorFailures} are the collect's accumulating outputs: the {@code gav → repository}
 * attribution for every descriptor read, and the GAVs whose descriptor Maven tolerated (missing/invalid) so the
 * collector can fail the direct ones and warn on the transitive ones ({@link EngineDependencyCollector}).
 */
@Value
class CollectContext {

    static final String SESSION_KEY = "org.openrewrite.maven.internal.engine.collectContext";

    RepositorySystem system;
    Path materializeDir;
    MavenPomCache pomCache;
    ReactorWorkspace reactor;
    EffectiveSettings settings;
    ExecutionContext ctx;
    List<MavenRepository> requestRepositories;
    Collection<ResolvedGroupArtifactVersion> pinnedSnapshotVersions;

    // Cumulative across every collect sharing the collector's DataPool: a descriptor served warm from the pool skips
    // EngineDescriptorReader entirely (descriptorReads == 0), so per-collect attribution would drop it. Keying the
    // attribution on the collector (not the collect) keeps a gav's repository / effective dependencies attributed on the
    // warm reads that follow — the reactor's later modules and any repeat coordinate.
    Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy;
    Map<GroupArtifactVersion, List<GroupArtifact>> declaredDependencies;

    Map<GroupArtifactVersion, DescriptorFailure> descriptorFailures = new ConcurrentHashMap<>();

    /** Descriptor reads (effective-model builds) this collect performed; 0 on a warm re-collect served from the DataPool. */
    AtomicInteger descriptorReads = new AtomicInteger();

    static CollectContext from(RepositorySystemSession session) {
        Object value = session.getConfigProperties().get(SESSION_KEY);
        if (!(value instanceof CollectContext)) {
            throw new IllegalStateException("no CollectContext in session config property '" + SESSION_KEY + "'");
        }
        return (CollectContext) value;
    }

    /** A descriptor Maven's policy tolerated: a dependency's own pom missing, or its model invalid. */
    @Value
    static class DescriptorFailure {
        boolean missing;
        String reason;
        Map<MavenRepository, String> repositoryResponses;
    }
}
