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
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.graph.DependencyCycle;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.graph.DependencyNode;
import org.openrewrite.maven.tree.GroupArtifact;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.util.List;
import java.util.Map;

/**
 * What {@link EngineDependencyCollector} hands the slice-B graph mapper: the single verbose collect's root
 * {@link DependencyNode} (all scopes, losers retained childless with {@code NODE_DATA_WINNER}, premanaged state readable
 * through {@code DependencyManagerUtils}), the {@code gav → repository} attribution every descriptor read accumulated,
 * the dependency cycles Maven recorded and tolerated ({@code CollectResult.getCycles()}), and the per-node failures the
 * collector surfaces.
 * <p>
 * {@code directFailures} are the direct (depth-1) dependencies whose descriptor Maven's policy tolerated but rewrite
 * fails on — already shaped as {@link MavenDownloadingException} with root-GA attribution, for the mapper to aggregate.
 * {@code toleratedTransitiveFailures} are the transitive descriptors Maven tolerated and rewrite tolerates too, exposed
 * so the mapper can emit warn events without failing the resolution.
 */
@Value
public class EngineCollectOutcome {

    /** The verbose root; {@code null} only when the collect failed so hard aether returned no partial graph. */
    @Nullable DependencyNode root;

    Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy;

    /** {@code gav → effective (parent-merged, managed-injected) declared dependencies} for the exclusion-report post-pass. */
    Map<GroupArtifactVersion, List<GroupArtifact>> declaredDependencies;

    List<DependencyCycle> cycles;

    List<MavenDownloadingException> directFailures;

    List<GroupArtifactVersion> toleratedTransitiveFailures;

    /** Effective-model builds this collect performed (0 on a warm re-collect served entirely from the DataPool). */
    int descriptorReads;
}
