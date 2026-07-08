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
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystem;
import org.openrewrite.maven.engine.shaded.org.eclipse.aether.RepositorySystemSession;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
    private final Path materializeDir;

    public EngineEffectivePom(RepositorySystem system, RepositorySystemSession session,
                              List<MavenRepository> requestRepositories, Path materializeDir) {
        this.system = system;
        this.session = session;
        this.requestRepositories = requestRepositories;
        this.materializeDir = materializeDir;
    }

    public EngineModelBuildingOutcome build(byte[] requestedPomXml, Pom requested, EffectiveSettings settings,
                                            ReactorWorkspace reactor, ExecutionContext ctx) {
        MavenPomCache pomCache = MavenExecutionContextView.view(ctx).getPomCache();
        CacheBridge bridge = new CacheBridge(system, session, pomCache, materializeDir);

        DefaultModelBuilder builder = new EngineModelBuilderFactory().newInstance();
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setModelSource(new StringModelSource(new String(requestedPomXml, StandardCharsets.UTF_8), sourceLabel(requested)));
        request.setModelResolver(new EngineModelResolver(bridge, reactor, requestRepositories));
        request.setWorkspaceModelResolver(reactor);
        // A fresh per-build model cache: the model-building walk (resolvePom for parents/BOMs) must run each build so
        // the servedBy gav→repo attribution the mappers depend on is complete even on a warm run. Byte downloads are
        // still avoided by the MavenPomCache bytes region; only the cheap re-parse repeats. Reusing the shared session
        // cache would let a second resolve serve a BOM's model from cache, skipping resolvePom and emptying servedBy.
        request.setModelCache(new EngineModelCache(new DefaultRepositoryCache(), session, reactor));
        request.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        request.setProcessPlugins(false);
        request.setTwoPhaseBuilding(false);
        request.setLocationTracking(true);
        request.setProfiles(settings.getExternalProfiles());
        request.setActiveProfileIds(settings.getActiveProfiles());
        request.setSystemProperties(systemProperties());
        request.setUserProperties(userProperties(settings.getUserProperties()));

        try {
            ModelBuildingResult result = builder.build(request);
            return EngineModelBuildingOutcome.success(result, bridge.servedBy());
        } catch (ModelBuildingException e) {
            return EngineModelBuildingOutcome.failure(ModelParityErrorMapper.map(e, bridge), bridge.servedBy());
        }
    }

    private static String sourceLabel(Pom requested) {
        return requested.getSourcePath() != null ? requested.getSourcePath().toString() : requested.getGav().toString();
    }

    // Maven's own precedence has POM properties beat system properties, so os/jdk profile activation reads these
    // without them overriding the model (DESIGN §9). A copy keeps the model builder off the live System.getProperties().
    private static Properties systemProperties() {
        Properties properties = new Properties();
        properties.putAll(System.getProperties());
        return properties;
    }

    private static Properties userProperties(Map<String, String> userProperties) {
        Properties properties = new Properties();
        properties.putAll(userProperties);
        return properties;
    }
}
