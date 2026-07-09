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

import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.ModelCache;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositoryCache;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession;

/**
 * The model builder's {@link ModelCache} (flat {@code (g,a,v,tag) → Object} for {@code RAW} models and {@code IMPORT}
 * dependency-management) backed by the engine's shared session {@link RepositoryCache}, so warm entries survive across
 * the sessions one engine serves.
 * <p>
 * Reactor GAVs are deliberately <em>not</em> cached here — {@link ReactorWorkspace} is their authoritative source and
 * its own model cache is dropped on {@link ReactorWorkspace#bumpEpoch()}, so a re-resolution after an in-place
 * {@code UpdateMavenModel} mutation re-reads the printed bytes (DESIGN §5.5). Caching them would also be actively wrong:
 * Maven 3.9 {@code readParent}, on a RAW cache hit whose model carries a {@code pomFile} (which a workspace parent must,
 * to be read as a {@code FileModelSource}), cross-checks that file against the child's {@code relativePath} and, for an
 * in-memory root that resolves to none, distrusts the entry and re-resolves against the repositories. Skipping reactor
 * GAVs keeps every reactor parent resolving through the workspace. Non-reactor GAVs cache normally and stay warm.
 */
public class EngineModelCache implements ModelCache {

    private final RepositoryCache cache;
    private final RepositorySystemSession session;
    private final ReactorWorkspace reactor;

    public EngineModelCache(RepositoryCache cache, RepositorySystemSession session, ReactorWorkspace reactor) {
        this.cache = cache;
        this.session = session;
        this.reactor = reactor;
    }

    @Override
    public void put(String groupId, String artifactId, String version, String tag, Object data) {
        if (!reactor.isReactorMember(groupId, artifactId, version)) {
            cache.put(session, key(groupId, artifactId, version, tag), data);
        }
    }

    @Override
    public Object get(String groupId, String artifactId, String version, String tag) {
        if (reactor.isReactorMember(groupId, artifactId, version)) {
            return null;
        }
        Object hit = cache.get(session, key(groupId, artifactId, version, tag));
        if (EngineProfiler.ENABLED) {
            (hit == null ? EngineProfiler.modelCacheMisses : EngineProfiler.modelCacheHits).incrementAndGet();
        }
        return hit;
    }

    private static String key(String groupId, String artifactId, String version, String tag) {
        return "modelcache:" + groupId + ':' + artifactId + ':' + version + ':' + tag;
    }
}
