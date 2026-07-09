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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.DefaultModelBuilder;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.ModelBuildingException;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.ModelBuildingRequest;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.ModelBuildingResult;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.StringModelSource;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.DefaultRepositoryCache;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositoryCache;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystem;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * The service B2 consumes: real Maven's {@link DefaultModelBuilder} wired to rewrite's seams, producing an effective
 * {@link ModelBuildingResult} from real pom XML (DESIGN §0 XML-first). The request is configured per DESIGN §4.1 —
 * {@code validationLevel=MINIMAL}, {@code processPlugins=false}, {@code locationTracking=true}, the
 * {@link EngineModelBuilderFactory} whose no-op {@code PluginManagementInjector} keeps management out of the effective
 * plugin list, external profiles + user properties from {@link EffectiveSettings}, {@link ReactorWorkspace} as the
 * {@code WorkspaceModelResolver}, {@link EngineModelResolver} over {@link CacheBridge} as the {@code ModelResolver}, and
 * an epoch-keyed {@link EngineModelCache}.
 * <p>
 * Constructed once per resolution run (stable engine + session + base request repositories); {@code build} is called per
 * project pom. The pom-bytes cache comes from the {@link ExecutionContext} so it outlives the session (the warm path).
 */
public class EngineEffectivePom {

    private final RepositorySystem system;
    private final RepositorySystemSession session;
    private final List<MavenRepository> requestRepositories;
    private final @Nullable RepositoryCache modelCacheStore;

    /**
     * @param modelCacheStore backs the per-build model cache. {@code null} gives a fresh store per build — the
     *                        model-building walk (resolvePom for parents/BOMs) then runs each build so the
     *                        {@code servedBy} gav→repo attribution the mappers read is complete on its own (the root
     *                        project build, whose servedBy is not accumulated elsewhere). A shared store lets warm
     *                        parents/BOMs serve their model from cache across builds — safe where servedBy is
     *                        accumulated at a higher level (the collector's descriptor reads, see
     *                        {@link EngineDependencyCollector}).
     */
    public EngineEffectivePom(RepositorySystem system, RepositorySystemSession session,
                              List<MavenRepository> requestRepositories, @Nullable RepositoryCache modelCacheStore) {
        this.system = system;
        this.session = session;
        this.requestRepositories = requestRepositories;
        this.modelCacheStore = modelCacheStore;
    }

    public EngineModelBuildingOutcome build(byte[] requestedPomXml, Pom requested, EffectiveSettings settings,
                                            ReactorWorkspace reactor, ExecutionContext ctx) {
        MavenPomCache pomCache = MavenExecutionContextView.view(ctx).getPomCache();
        CacheBridge bridge = new CacheBridge(system, session, pomCache);

        DefaultModelBuilder builder = EngineModelBuilderFactory.shared();
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(new String(requestedPomXml, StandardCharsets.UTF_8), sourceLabel(requested)));
        request.setModelResolver(new EngineModelResolver(bridge, reactor, requestRepositories));
        request.setWorkspaceModelResolver(reactor);
        request.setModelCache(new EngineModelCache(
                modelCacheStore != null ? modelCacheStore : new DefaultRepositoryCache(), session, reactor));
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        request.setProcessPlugins(false);
        request.setTwoPhaseBuilding(false);
        request.setLocationTracking(true);
        request.setProfiles(settings.getExternalProfiles());
        request.setActiveProfileIds(settings.getActiveProfiles());
        request.setSystemProperties(systemProperties(MavenExecutionContextView.view(ctx).getActivationSystemProperties()));
        request.setUserProperties(userProperties(settings.getUserProperties()));

        long t0 = EngineProfiler.ENABLED ? System.nanoTime() : 0;
        try {
            ModelBuildingResult result = builder.build(request);
            return EngineModelBuildingOutcome.success(result, bridge.servedBy());
        } catch (ModelBuildingException e) {
            return EngineModelBuildingOutcome.failure(ModelParityErrorMapper.map(e, bridge), bridge.servedBy());
        } finally {
            if (EngineProfiler.ENABLED) {
                EngineProfiler.modelBuildNanos.addAndGet(System.nanoTime() - t0);
                EngineProfiler.modelBuilds.incrementAndGet();
            }
        }
    }

    private static String sourceLabel(Pom requested) {
        return requested.getSourcePath() != null ? requested.getSourcePath().toString() : requested.getGav().toString();
    }

    // Maven's own precedence has POM properties beat system properties, so os/jdk profile activation reads these
    // without them overriding the model (DESIGN §9). Defaults to a copy of the live System.getProperties() (Maven
    // parity); a caller-pinned map (MavenExecutionContextView.setActivationSystemProperties) makes <os>/<jdk> profile
    // activation reproducible across machines.
    private static Properties systemProperties(@Nullable Map<String, String> pinned) {
        Properties properties = new Properties();
        if (pinned == null) {
            properties.putAll(System.getProperties());
        } else {
            properties.putAll(pinned);
        }
        return properties;
    }

    private static Properties userProperties(Map<String, String> userProperties) {
        Properties properties = new Properties();
        properties.putAll(userProperties);
        return properties;
    }
}
