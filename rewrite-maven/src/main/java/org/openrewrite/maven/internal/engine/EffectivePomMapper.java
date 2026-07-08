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

import com.fasterxml.jackson.databind.JsonNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Build;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Dependency;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.DependencyManagement;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Exclusion;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.InputLocation;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Model;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Plugin;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.PluginExecution;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Profile;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.Repository;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.RepositoryPolicy;
import org.openrewrite.maven.engine.shaded.org.apache.maven.model.building.ModelBuildingResult;
import org.openrewrite.maven.tree.*;

import java.util.*;

/**
 * Projects real Maven's model-building output into the frozen {@link ResolvedPom} (DESIGN §4.1). This is the
 * identity-contract-critical layer: {@code requestedDependencies} thread the exact {@link org.openrewrite.maven.tree.Dependency}
 * instances of the declaring {@link Pom}s, and every {@link ResolvedManagedDependency#getRequested() requested}/{@link
 * ResolvedManagedDependency#getRequestedBom() requestedBom} is the same {@link ManagedDependency} instance found in a
 * declaring pom's {@code <dependencyManagement>}, so {@code getResolvedDependency}/{@code getResolvedManagedDependency}
 * keep matching by reference.
 * <p>
 * Deliberate field sourcing: {@code properties} are the RAW-lineage merge (child→parent, active-profile properties
 * first, first-wins) with parser/ctx-injected properties overlaid — <em>not</em> the interpolated effective map, which
 * {@code getValue()} still produces lazily; {@code dependencyManagement}/{@code plugins}/{@code repositories} come from
 * the effective model. Managed entries join to their declaring instances by {@link InputLocation} line/column (never
 * GACT or positional in the effective model — {@code ${project.groupId}} defaults and de-duping break those), and BOM
 * provenance is stamped by {@link BomGavAttributor}.
 */
public class EffectivePomMapper {

    private final MavenPomCache pomCache;
    private final BomGavAttributor bomGavAttributor;

    public EffectivePomMapper(MavenPomCache pomCache, BomGavAttributor bomGavAttributor) {
        this.pomCache = pomCache;
        this.bomGavAttributor = bomGavAttributor;
    }

    public ResolvedPom map(EngineModelBuildingOutcome outcome, Pom requested, List<String> activeProfiles,
                           Map<String, String> injectedProperties) {
        ModelBuildingResult result = requireResult(outcome);
        Model effective = result.getEffectiveModel();
        List<String> lineage = result.getModelIds();
        Set<String> lineageIds = new HashSet<>(lineage);
        String rootModelId = lineage.get(0);
        Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy = outcome.getServedBy();

        // Real contributing models (the inheritance lineage + imported BOMs); the super-POM contributes an empty model
        // id and is excluded, so its injected pluginManagement/repositories/pluginRepositories never surface — rewrite
        // only carries declared/inherited entries. Derived from the lineage (not servedBy) so it holds on a warm cache,
        // where a served-from-bytes parent is absent from servedBy (shadow mode warms the cache before the engine runs).
        Set<String> knownModelIds = knownModelIds(lineage, servedBy);

        Map<String, String> properties = mergeProperties(result, lineage, injectedProperties);
        List<ResolvedManagedDependency> dependencyManagement =
                dependencyManagement(result, requested, servedBy, lineageIds, rootModelId);
        List<org.openrewrite.maven.tree.Dependency> requestedDependencies =
                requestedDependencies(result, requested, servedBy, lineage, rootModelId);
        List<MavenRepository> repositories = repositories(effective.getRepositories(), knownModelIds);
        List<MavenRepository> pluginRepositories = repositories(effective.getPluginRepositories(), knownModelIds);
        List<org.openrewrite.maven.tree.Plugin> plugins = plugins(buildPlugins(effective), knownModelIds);
        List<org.openrewrite.maven.tree.Plugin> pluginManagement = plugins(buildPluginManagement(effective), knownModelIds);

        return ResolvedPom.builder()
                .requested(requested)
                .activeProfiles(activeProfiles)
                .properties(properties)
                .dependencyManagement(dependencyManagement)
                .dependencyManagementSorted(false)
                .initialRepositories(repositories)
                .repositories(repositories)
                .pluginRepositories(pluginRepositories)
                .requestedDependencies(requestedDependencies)
                .plugins(plugins)
                .pluginManagement(pluginManagement)
                .subprojects(requested.getSubprojects())
                .build();
    }

    // ---- properties (raw lineage merge + injected overlay) ----

    private Map<String, String> mergeProperties(ModelBuildingResult result, List<String> lineage,
                                                Map<String, String> injected) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (String id : lineage) {
            for (Profile profile : result.getActivePomProfiles(id)) {
                putAllIfAbsent(merged, profile.getProperties());
            }
            Model raw = result.getRawModel(id);
            if (raw != null) {
                putAllIfAbsent(merged, raw.getProperties());
            }
        }
        // Injected properties stay visible (getProperties()/getValue()) but do not override a POM-declared value (DESIGN §9).
        for (Map.Entry<String, String> entry : injected.entrySet()) {
            merged.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return merged;
    }

    private static void putAllIfAbsent(Map<String, String> target, Properties properties) {
        for (String name : properties.stringPropertyNames()) {
            if (!target.containsKey(name)) {
                // An empty <tag/> is null in rewrite's RawPom but "" in Maven's model; keep RawPom's representation.
                String value = properties.getProperty(name);
                target.put(name, value.isEmpty() ? null : value);
            }
        }
    }

    // ---- dependency management ----

    private List<ResolvedManagedDependency> dependencyManagement(
            ModelBuildingResult result, Pom requested, Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy,
            Set<String> lineageIds, String rootModelId) {
        DependencyManagement dm = result.getEffectiveModel().getDependencyManagement();
        if (dm == null || dm.getDependencies().isEmpty()) {
            return new ArrayList<>();
        }
        BomGavAttributor.Attribution attribution = bomGavAttributor.attribute(requested, servedBy);
        List<ResolvedManagedDependency> managed = new ArrayList<>(dm.getDependencies().size());
        for (Dependency e : dm.getDependencies()) {
            GroupArtifactVersion gav = new GroupArtifactVersion(e.getGroupId(), e.getArtifactId(), e.getVersion());
            Scope scope = e.getScope() == null ? null : Scope.fromName(e.getScope());
            List<GroupArtifact> exclusions = exclusions(e.getExclusions());
            InputLocation location = e.getLocation("");
            String modelId = location == null ? rootModelId : location.getSource().getModelId();

            ManagedDependency requestedInstance =
                    joinRequested(result, modelId, location, e, requested, servedBy, lineageIds, rootModelId);
            ManagedDependency requestedBom = null;
            ResolvedGroupArtifactVersion bomGav = null;
            if (!lineageIds.contains(modelId)) {
                BomGavAttributor.Match match = attribution.match(
                        BomGavAttributor.key(e.getGroupId(), e.getArtifactId(), e.getClassifier(), e.getType()));
                if (match != null) {
                    requestedBom = match.getRequestedBom();
                    bomGav = match.getBomGav();
                }
            }
            managed.add(new ResolvedManagedDependency(gav, scope, e.getType(), e.getClassifier(),
                    exclusions, requestedInstance, requestedBom, bomGav));
        }
        return managed;
    }

    /**
     * The declaring {@link ManagedDependency} instance for an effective management entry. Lineage models (root + parents,
     * available via {@code getRawModel}) join by {@link InputLocation} line/column then map by index into the declaring
     * pom's {@code <dependencyManagement>} (raw order == parsed order). Imported BOM models have no {@code getRawModel},
     * so fall back to a within-pom GACT match — collisions inside a single raw pom are not the case the line/column rule
     * guards against.
     */
    private ManagedDependency joinRequested(
            ModelBuildingResult result, String modelId, @Nullable InputLocation location, Dependency effective,
            Pom requested, Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy, Set<String> lineageIds,
            String rootModelId) {
        Pom declaring = declaringPom(modelId, rootModelId, requested, servedBy);
        List<ManagedDependency> declared = declaring == null ? Collections.emptyList() : declaring.getDependencyManagement();

        if (location != null && lineageIds.contains(modelId)) {
            int index = indexByLocation(result.getRawModel(modelId), location);
            if (index >= 0 && index < declared.size()) {
                return declared.get(index);
            }
        }
        for (ManagedDependency candidate : declared) {
            if (candidate.getGroupId().equals(effective.getGroupId()) &&
                    candidate.getArtifactId().equals(effective.getArtifactId()) &&
                    Objects.equals(classifierOf(candidate), effective.getClassifier())) {
                return candidate;
            }
        }
        // Never seen for the fixtures; keep the requested field non-null so the reference-lookup contract holds.
        return new ManagedDependency.Defined(
                new GroupArtifactVersion(effective.getGroupId(), effective.getArtifactId(), effective.getVersion()),
                effective.getScope(), effective.getType(), effective.getClassifier(), null);
    }

    private static int indexByLocation(@Nullable Model rawModel, InputLocation location) {
        if (rawModel == null || rawModel.getDependencyManagement() == null) {
            return -1;
        }
        List<Dependency> raw = rawModel.getDependencyManagement().getDependencies();
        for (int i = 0; i < raw.size(); i++) {
            InputLocation candidate = raw.get(i).getLocation("");
            if (candidate != null && candidate.getLineNumber() == location.getLineNumber() &&
                    candidate.getColumnNumber() == location.getColumnNumber()) {
                return i;
            }
        }
        return -1;
    }

    private static @Nullable String classifierOf(ManagedDependency md) {
        return md instanceof ManagedDependency.Defined ? ((ManagedDependency.Defined) md).getClassifier() : null;
    }

    // ---- requested dependencies (GA-keyed child-wins, threading original instances) ----

    private List<org.openrewrite.maven.tree.Dependency> requestedDependencies(
            ModelBuildingResult result, Pom requested,
            Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy, List<String> lineage, String rootModelId) {
        List<org.openrewrite.maven.tree.Dependency> requestedDependencies = new ArrayList<>();
        Set<GroupArtifact> seen = new HashSet<>();
        for (String id : lineage) {
            Pom pom = declaringPom(id, rootModelId, requested, servedBy);
            if (pom == null) {
                continue;
            }
            Set<String> activeProfileIds = activeProfileIds(result, id);
            for (org.openrewrite.maven.tree.Profile profile : pom.getProfiles()) {
                if (activeProfileIds.contains(profile.getId())) {
                    threadDependencies(profile.getDependencies(), seen, requestedDependencies);
                }
            }
            threadDependencies(pom.getDependencies(), seen, requestedDependencies);
        }
        return requestedDependencies;
    }

    private static void threadDependencies(List<org.openrewrite.maven.tree.Dependency> incoming, Set<GroupArtifact> seen,
                                           List<org.openrewrite.maven.tree.Dependency> target) {
        for (org.openrewrite.maven.tree.Dependency dependency : incoming) {
            if (seen.add(new GroupArtifact(dependency.getGroupId(), dependency.getArtifactId()))) {
                target.add(dependency);
            }
        }
    }

    private static Set<String> activeProfileIds(ModelBuildingResult result, String modelId) {
        Set<String> ids = new HashSet<>();
        for (Profile profile : result.getActivePomProfiles(modelId)) {
            ids.add(profile.getId());
        }
        return ids;
    }

    // ---- repositories ----

    private static List<MavenRepository> repositories(List<Repository> repositories, Set<String> knownModelIds) {
        List<MavenRepository> result = new ArrayList<>();
        for (Repository repo : repositories) {
            if (!isKnownModel(repo.getLocation(""), knownModelIds)) {
                continue;
            }
            result.add(new MavenRepository(repo.getId(), repo.getUrl(),
                    enabled(repo.getReleases()), enabled(repo.getSnapshots()), false, null, null, null, null));
        }
        return result;
    }

    private static Set<String> knownModelIds(List<String> lineage, Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy) {
        Set<String> known = new HashSet<>();
        for (String id : lineage) {
            // The super-POM's inheritance entry carries an empty model id; every real model has a g:a:v id.
            if (!id.isEmpty()) {
                known.add(id);
            }
        }
        for (ResolvedGroupArtifactVersion key : servedBy.keySet()) {
            known.add(key.getGroupId() + ":" + key.getArtifactId() + ":" + key.getVersion());
        }
        return known;
    }

    // A declared/inherited element carries a real contributing model id; the super-POM's do not (it is never fetched).
    private static boolean isKnownModel(@Nullable InputLocation location, Set<String> knownModelIds) {
        return location == null || knownModelIds.contains(location.getSource().getModelId());
    }

    private static @Nullable String enabled(@Nullable RepositoryPolicy policy) {
        return policy == null ? null : policy.getEnabled();
    }

    // ---- plugins ----

    private static List<Plugin> buildPlugins(Model effective) {
        Build build = effective.getBuild();
        return build == null ? Collections.emptyList() : build.getPlugins();
    }

    private static List<Plugin> buildPluginManagement(Model effective) {
        Build build = effective.getBuild();
        if (build == null || build.getPluginManagement() == null) {
            return Collections.emptyList();
        }
        return build.getPluginManagement().getPlugins();
    }

    private static List<org.openrewrite.maven.tree.Plugin> plugins(List<Plugin> plugins, Set<String> knownModelIds) {
        List<org.openrewrite.maven.tree.Plugin> result = new ArrayList<>(plugins.size());
        for (Plugin plugin : plugins) {
            if (!isKnownModel(plugin.getLocation(""), knownModelIds)) {
                continue;
            }
            result.add(new org.openrewrite.maven.tree.Plugin(
                    plugin.getGroupId(),
                    plugin.getArtifactId(),
                    plugin.getVersion(),
                    plugin.getExtensions(),
                    plugin.getInherited(),
                    PluginConfigurations.toJson(plugin.getConfiguration()),
                    dependencies(plugin.getDependencies()),
                    executions(plugin.getExecutions())));
        }
        return result;
    }

    private static List<org.openrewrite.maven.tree.Plugin.Execution> executions(List<PluginExecution> executions) {
        List<org.openrewrite.maven.tree.Plugin.Execution> result = new ArrayList<>(executions.size());
        for (PluginExecution execution : executions) {
            JsonNode configuration = PluginConfigurations.toJson(execution.getConfiguration());
            result.add(new org.openrewrite.maven.tree.Plugin.Execution(
                    execution.getId(),
                    execution.getGoals() == null ? null : new ArrayList<>(execution.getGoals()),
                    execution.getPhase(),
                    execution.getInherited(),
                    configuration));
        }
        return result;
    }

    private static List<org.openrewrite.maven.tree.Dependency> dependencies(List<Dependency> dependencies) {
        List<org.openrewrite.maven.tree.Dependency> result = new ArrayList<>(dependencies.size());
        for (Dependency dependency : dependencies) {
            result.add(org.openrewrite.maven.tree.Dependency.builder()
                    .gav(new GroupArtifactVersion(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()))
                    .classifier(dependency.getClassifier())
                    .type(dependency.getType())
                    .scope(dependency.getScope())
                    .exclusions(exclusions(dependency.getExclusions()))
                    .optional(dependency.getOptional())
                    .build());
        }
        return result;
    }

    private static @Nullable List<GroupArtifact> exclusions(@Nullable List<Exclusion> exclusions) {
        if (exclusions == null || exclusions.isEmpty()) {
            return null;
        }
        List<GroupArtifact> result = new ArrayList<>(exclusions.size());
        for (Exclusion exclusion : exclusions) {
            result.add(new GroupArtifact(exclusion.getGroupId(), exclusion.getArtifactId()));
        }
        return result;
    }

    // ---- shared helpers ----

    private @Nullable Pom declaringPom(String modelId, String rootModelId, Pom requested,
                                       Map<ResolvedGroupArtifactVersion, MavenRepository> servedBy) {
        if (modelId.equals(rootModelId)) {
            return requested;
        }
        String[] coordinates = modelId.split(":");
        if (coordinates.length < 3) {
            return null;
        }
        for (ResolvedGroupArtifactVersion key : servedBy.keySet()) {
            if (coordinates[0].equals(key.getGroupId()) && coordinates[1].equals(key.getArtifactId()) &&
                    coordinates[2].equals(key.getVersion())) {
                try {
                    Optional<Pom> pom = pomCache.getPom(key);
                    //noinspection OptionalAssignedToNull
                    if (pom != null && pom.isPresent()) {
                        return pom.get();
                    }
                } catch (Exception ignored) {
                    // fall through to the next candidate / null
                }
            }
        }
        return null;
    }

    private static ModelBuildingResult requireResult(EngineModelBuildingOutcome outcome) {
        ModelBuildingResult result = outcome.getResult();
        if (result == null) {
            throw new IllegalArgumentException("Cannot map a failed model-building outcome: " + outcome.getFailure());
        }
        return result;
    }

    /**
     * A {@code ResolvedPom.resolve()}-style no-change identity gate for the re-resolution loop: returns {@code previous}
     * when a freshly mapped pom is field-for-field equal (the signal recipes rely on), else {@code fresh}. Mirrors the
     * comparison cascade in {@link ResolvedPom#resolve}.
     */
    public static ResolvedPom sameIfUnchanged(ResolvedPom previous, ResolvedPom fresh) {
        if (!previous.getVersion().equals(fresh.getVersion()) ||
                !previous.getProperties().equals(fresh.getProperties()) ||
                !previous.getRequestedDependencies().equals(fresh.getRequestedDependencies()) ||
                !previous.getDependencyManagement().equals(fresh.getDependencyManagement()) ||
                !previous.getRepositories().equals(fresh.getRepositories()) ||
                !previous.getPlugins().equals(fresh.getPlugins()) ||
                !previous.getPluginManagement().equals(fresh.getPluginManagement())) {
            return fresh;
        }
        return previous;
    }
}
