/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.maven.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.With;
import lombok.experimental.NonFinal;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.PropertyPlaceholderHelper;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.MavenExecutionContextView;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.internal.MavenParsingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.VersionRequirement;
import org.openrewrite.maven.tree.ManagedDependency.Defined;
import org.openrewrite.maven.tree.ManagedDependency.Imported;

import java.util.*;
import java.util.function.UnaryOperator;

import static java.util.Collections.*;
import static org.openrewrite.internal.StringUtils.matchesGlob;

@JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class, property = "@ref")
@Getter
@Builder
public class ResolvedPom {

    public static final PropertyPlaceholderHelper placeholderHelper = new PropertyPlaceholderHelper("${", "}", null);

    // https://maven.apache.org/ref/3.6.3/maven-model-builder/super-pom.html
    private static final ResolvedPom SUPER_POM = new ResolvedPom(
            new Pom(null, null, null, null, null, null, emptyMap(), emptyList(), emptyList(), singletonList(MavenRepository.MAVEN_CENTRAL), emptyList(), emptyList(), null, null),
            emptyList()
    );

    @With
    Pom requested;

    @With
    Iterable<String> activeProfiles;

    public ResolvedPom(Pom requested, Iterable<String> activeProfiles) {
        this(requested, activeProfiles, emptyMap(), emptyList(), null, emptyList(), emptyList(), emptyList(), emptyList());
    }

    @JsonCreator
    ResolvedPom(Pom requested, Iterable<String> activeProfiles, Map<String, String> properties, List<ResolvedManagedDependency> dependencyManagement, @Nullable List<MavenRepository> initialRepositories, List<MavenRepository> repositories, List<Dependency> requestedDependencies, List<Plugin> plugins, List<Plugin> pluginManagement) {
        this.requested = requested;
        this.activeProfiles = activeProfiles;
        this.properties = properties;
        this.dependencyManagement = dependencyManagement;
        this.initialRepositories = initialRepositories;
        this.repositories = repositories;
        this.requestedDependencies = requestedDependencies;
        this.plugins = plugins;
        this.pluginManagement = pluginManagement;
    }

    @NonFinal
    @Builder.Default
    Map<String, String> properties = emptyMap();

    @NonFinal
    @Builder.Default
    List<ResolvedManagedDependency> dependencyManagement = emptyList();

    @NonFinal
    @Builder.Default
    List<MavenRepository> initialRepositories = emptyList();

    @NonFinal
    @Builder.Default
    List<MavenRepository> repositories = emptyList();

    @NonFinal
    @Builder.Default
    List<Dependency> requestedDependencies = emptyList();

    @NonFinal
    @Builder.Default
    List<Plugin> plugins = emptyList();

    @NonFinal
    @Builder.Default
    List<Plugin> pluginManagement = emptyList();


    /**
     * Deduplicate dependencies and dependency management dependencies
     *
     * @return This POM after deduplication.
     */
    @SuppressWarnings("UnnecessaryLocalVariable")
    public ResolvedPom deduplicate() {
        Set<UniqueDependencyKey> uniqueManagedDependencies = new HashSet<>(dependencyManagement.size());

        List<ResolvedManagedDependency> dedupMd = ListUtils.map(dependencyManagement, dm -> uniqueManagedDependencies.add(new UniqueDependencyKey(dm.getGav(), dm.getType(), dm.getClassifier(), dm.getScope())) ?
                dm : null);
        dependencyManagement = dedupMd;

        uniqueManagedDependencies.clear();
        List<Dependency> dedupD = ListUtils.map(requestedDependencies, d -> uniqueManagedDependencies.add(new UniqueDependencyKey(d.getGav(), d.getType(), d.getClassifier(), d.getScope())) ?
                d : null);
        requestedDependencies = dedupD;
        return this;
    }

    @Value
    private static class UniqueDependencyKey {
        GroupArtifactVersion gav;

        @Nullable
        String type;

        @Nullable
        String classifier;

        Object scope;
    }

    /**
     * Whenever a change is made that may affect the effective properties, dependency management,
     * dependencies, etc. of a POM, this can be called to re-resolve the POM.
     *
     * @param ctx        An execution context containing any maven-specific requirements.
     * @param downloader A POM downloader to download dependencies and parents.
     * @return A new instance with dependencies re-resolved or the same instance if no resolved dependencies have changed.
     * @throws MavenDownloadingException When problems are encountered downloading dependencies or parents.
     */
    @SuppressWarnings("DuplicatedCode")
    public ResolvedPom resolve(ExecutionContext ctx, MavenPomDownloader downloader) throws MavenDownloadingException {
        ResolvedPom resolved = new ResolvedPom(
                requested,
                activeProfiles,
                emptyMap(),
                emptyList(),
                initialRepositories,
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList()
        ).resolver(ctx, downloader).resolve();

        for (Map.Entry<String, String> property : resolved.getProperties().entrySet()) {
            if (properties == null || (property.getValue() != null && !property.getValue().equals(properties.get(property.getKey())))) {
                return resolved;
            }
        }

        List<Dependency> resolvedRequestedDependencies = resolved.getRequestedDependencies();
        if (requestedDependencies == null || requestedDependencies.size() != resolvedRequestedDependencies.size()) {
            return resolved;
        }
        for (int i = 0; i < resolvedRequestedDependencies.size(); i++) {
            if (!requestedDependencies.get(i).equals(resolvedRequestedDependencies.get(i))) {
                return resolved;
            }
        }

        List<ResolvedManagedDependency> resolvedDependencyManagement = resolved.getDependencyManagement();
        if (dependencyManagement == null || dependencyManagement.size() != resolvedDependencyManagement.size()) {
            return resolved;
        }
        for (int i = 0; i < resolvedDependencyManagement.size(); i++) {
            // TODO does ResolvedPom's equals work well enough to match on BOM imports?
            if (!dependencyManagement.get(i).equals(resolvedDependencyManagement.get(i))) {
                return resolved;
            }
        }

        List<MavenRepository> resolvedRepositories = resolved.getRepositories();
        if (repositories == null || repositories.size() != resolvedRepositories.size()) {
            return resolved;
        }
        for (int i = 0; i < resolvedRepositories.size(); i++) {
            if (!repositories.get(i).equals(resolvedRepositories.get(i))) {
                return resolved;
            }
        }

        List<Plugin> resolvedPlugins = resolved.getPlugins();
        if (plugins == null || plugins.size() != resolvedPlugins.size()) {
            return resolved;
        }
        for (int i = 0; i < resolvedPlugins.size(); i++) {
            if (!plugins.get(i).equals(resolvedPlugins.get(i))) {
                return resolved;
            }
        }

        List<Plugin> resolvedPluginManagement = resolved.getPluginManagement();
        if (pluginManagement == null || pluginManagement.size() != resolvedPluginManagement.size()) {
            return resolved;
        }
        for (int i = 0; i < resolvedPluginManagement.size(); i++) {
            if (!pluginManagement.get(i).equals(resolvedPluginManagement.get(i))) {
                return resolved;
            }
        }

        return this;
    }

    Resolver resolver(ExecutionContext ctx, MavenPomDownloader downloader) {
        return new Resolver(ctx, downloader);
    }

    public ResolvedGroupArtifactVersion getGav() {
        return requested.getGav();
    }

    public String getGroupId() {
        return requested.getGroupId();
    }

    public String getArtifactId() {
        return requested.getArtifactId();
    }

    public String getVersion() {
        return requested.getVersion();
    }

    @SuppressWarnings("unused")
    @Nullable
    public String getDatedSnapshotVersion() {
        return requested.getDatedSnapshotVersion();
    }

    public String getPackaging() {
        return requested.getPackaging() == null ? "jar" : requested.getPackaging();
    }

    @Nullable
    public String getValue(@Nullable String value) {
        if (value == null) {
            return null;
        }
        return placeholderHelper.replacePlaceholders(value, this::getProperty);
    }

    @Nullable
    private String getProperty(@Nullable String property) {
        if (property == null) {
            return null;
        }
        switch (property) {
            case "groupId":
            case "project.groupId":
            case "pom.groupId":
                return requested.getGroupId();
            case "project.parent.groupId":
            case "parent.groupId":
                return requested.getParent() != null ? requested.getParent().getGroupId() : null;
            case "artifactId":
            case "project.artifactId":
            case "pom.artifactId":
                return requested.getArtifactId(); // cannot be inherited from parent
            case "project.parent.artifactId":
            case "parent.artifactId":
                return requested.getParent() == null ? null : requested.getParent().getArtifactId();
            case "version":
            case "project.version":
            case "pom.version":
                return requested.getVersion();
            case "project.parent.version":
            case "parent.version":
                return requested.getParent() != null ? requested.getParent().getVersion() : null;
        }

        return System.getProperty(property, properties.get(property));
    }

    @Nullable
    public String getManagedVersion(@Nullable String groupId, String artifactId, @Nullable String type, @Nullable String classifier) {
        for (ResolvedManagedDependency dm : dependencyManagement) {
            if (dm.matches(groupId, artifactId, type, classifier)) {
                return getValue(dm.getVersion());
            }
        }

        return null;
    }

    public List<GroupArtifact> getManagedExclusions(String groupId, String artifactId, @Nullable String type, @Nullable String classifier) {
        for (ResolvedManagedDependency dm : dependencyManagement) {
            if (dm.matches(groupId, artifactId, type, classifier)) {
                return dm.getExclusions() == null ? emptyList() : dm.getExclusions();
            }
        }
        return emptyList();
    }

    @Nullable
    public Scope getManagedScope(String groupId, String artifactId, @Nullable String type, @Nullable String classifier) {
        for (ResolvedManagedDependency dm : dependencyManagement) {
            if (dm.matches(groupId, artifactId, type, classifier)) {
                return dm.getScope();
            }
        }
        return null;
    }

    public GroupArtifactVersion getValues(GroupArtifactVersion gav) {
        return gav.withGroupId(getValue(gav.getGroupId()))
                .withArtifactId(getValue(gav.getArtifactId()))
                .withVersion(getValue(gav.getVersion()));
    }

    public GroupArtifact getValues(GroupArtifact ga) {
        return ga.withGroupId(getValue(ga.getGroupId()))
                .withArtifactId(getValue(ga.getArtifactId()));
    }

    @Value
    class Resolver {
        ExecutionContext ctx;
        MavenPomDownloader downloader;

        public ResolvedPom resolve() throws MavenDownloadingException {
            resolveParentsRecursively(requested);
            return ResolvedPom.this;
        }

        void resolveParentsRecursively(Pom requested) throws MavenDownloadingException {
            List<Pom> pomAncestry = new ArrayList<>();
            pomAncestry.add(requested);

            if (initialRepositories != null) {
                mergeRepositories(initialRepositories);
            }
            resolveParentPropertiesAndRepositoriesRecursively(new ArrayList<>(pomAncestry));
            if (initialRepositories == null) {
                initialRepositories = repositories;
            }

            //Once properties have been merged, update any property placeholders in the resolved gav
            //coordinates. This is important to do early because any system properties used within the coordinates
            //are transient and will not be available once pom has been serialized/deserialized into a different VM.
            Pom pomReference = ResolvedPom.this.requested;
            pomReference = pomReference.withGav(pomReference.getGav().withRepository(getValue(pomReference.getGav().getRepository())));
            pomReference = pomReference.withGav(pomReference.getGav().withGroupId(getValue(pomReference.getGav().getGroupId())));
            pomReference = pomReference.withGav(pomReference.getGav().withArtifactId(getValue(pomReference.getGav().getArtifactId())));
            pomReference = pomReference.withGav(pomReference.getGav().withVersion(getValue(pomReference.getGav().getVersion())));
            pomReference = pomReference.withGav(pomReference.getGav().withDatedSnapshotVersion(getValue(pomReference.getGav().getDatedSnapshotVersion())));
            if (ResolvedPom.this.requested != pomReference) {
                ResolvedPom.this.requested = pomReference;
            }

            resolveParentDependenciesRecursively(new ArrayList<>(pomAncestry));
            resolveParentPluginsRecursively(new ArrayList<>(pomAncestry));
        }

        private void resolveParentPropertiesAndRepositoriesRecursively(List<Pom> pomAncestry) throws MavenDownloadingException {
            Pom pom = pomAncestry.get(0);

            //Resolve properties
            for (Profile profile : pom.getProfiles()) {
                if (profile.isActive(activeProfiles)) {
                    mergeProperties(profile.getProperties(), pom);
                }
            }
            mergeProperties(pom.getProperties(), pom);

            //Resolve repositories (which may rely on properties ^^^)
            for (Profile profile : pom.getProfiles()) {
                if (profile.isActive(activeProfiles)) {
                    mergeRepositories(profile.getRepositories());
                }
            }
            mergeRepositories(pom.getRepositories());

            if (pom.getParent() != null) {
                Pom parentPom = resolveParentPom(pom);

                for (Pom ancestor : pomAncestry) {
                    if (ancestor.getGav().equals(parentPom.getGav())) {
                        // parent cycle
                        return;
                    }
                }

                pomAncestry.add(0, parentPom);
                resolveParentPropertiesAndRepositoriesRecursively(pomAncestry);
            }
        }

        private void resolveParentDependenciesRecursively(List<Pom> pomAncestry) throws MavenDownloadingException {
            Pom pom = pomAncestry.get(0);

            for (Profile profile : pom.getProfiles()) {
                if (profile.isActive(activeProfiles)) {
                    mergeDependencyManagement(profile.getDependencyManagement(), pom);
                    mergeRequestedDependencies(profile.getDependencies());
                }
            }

            mergeDependencyManagement(pom.getDependencyManagement(), pom);
            mergeRequestedDependencies(pom.getDependencies());

            if (pom.getParent() != null) {
                Pom parentPom = resolveParentPom(pom);

                MavenExecutionContextView.view(ctx)
                        .getResolutionListener()
                        .parent(parentPom, pom);

                for (Pom ancestor : pomAncestry) {
                    if (ancestor.getGav().equals(parentPom.getGav())) {
                        // parent cycle
                        return;
                    }
                }

                pomAncestry.add(0, parentPom);
                resolveParentDependenciesRecursively(pomAncestry);
            }
        }

        private void resolveParentPluginsRecursively(List<Pom> pomAncestry) throws MavenDownloadingException {
            Pom pom = pomAncestry.get(0);

            for (Profile profile : pom.getProfiles()) {
                if (profile.isActive(activeProfiles)) {
                    mergePluginManagement(profile.getPluginManagement());
                    mergePlugins(profile.getPlugins());
                }
            }

            mergePluginManagement(pom.getPluginManagement());
            mergePlugins(pom.getPlugins());

            if (pom.getParent() != null) {
                Pom parentPom = resolveParentPom(pom);

                MavenExecutionContextView.view(ctx)
                        .getResolutionListener()
                        .parent(parentPom, pom);

                for (Pom ancestor : pomAncestry) {
                    if (ancestor.getGav().equals(parentPom.getGav())) {
                        // parent cycle
                        return;
                    }
                }

                pomAncestry.add(0, parentPom);
                resolveParentPluginsRecursively(pomAncestry);
            }

        }

        private Pom resolveParentPom(Pom pom) throws MavenDownloadingException {
            @SuppressWarnings("DataFlowIssue") GroupArtifactVersion gav = getValues(pom.getParent().getGav());
            if (gav.getVersion() == null) {
                throw new MavenParsingException("Parent version must always specify a version " + gav);
            }

            VersionRequirement newRequirement = VersionRequirement.fromVersion(gav.getVersion(), 0);
            GroupArtifact ga = new GroupArtifact(gav.getGroupId(), gav.getArtifactId());
            String newRequiredVersion = newRequirement.resolve(ga, downloader, getRepositories());
            if (newRequiredVersion == null) {
                throw new MavenParsingException("Could not resolve version for [" + ga + "] matching version requirements " + newRequirement);
            }
            gav = gav.withVersion(newRequiredVersion);

            return downloader.download(gav,
                    pom.getParent().getRelativePath(), ResolvedPom.this, repositories);
        }

        private void mergeRequestedDependencies(List<Dependency> incomingRequestedDependencies) {
            if (!incomingRequestedDependencies.isEmpty()) {
                if (requestedDependencies == null || requestedDependencies.isEmpty()) {
                    //It is possible for the dependencies to be an empty, immutable list.
                    //If it's empty, we ensure to create a mutable list.
                    requestedDependencies = new ArrayList<>(incomingRequestedDependencies);
                } else {
                    requestedDependencies.addAll(incomingRequestedDependencies);
                }
            }
        }

        @Value
        private class PluginKey {
            String groupId;
            String artifactId;
        }

        private PluginKey getPluginKey(Plugin plugin) {
            return new PluginKey(
                    plugin.getGroupId(),
                    plugin.getArtifactId()
            );
        }

        private List<Dependency> mergePluginDependencies(List<Dependency> dependencies, List<Dependency> incomingDependencies) {
            if (incomingDependencies.isEmpty()) {
                return dependencies;
            }
            if (dependencies.isEmpty()) {
                return incomingDependencies;
            }

            List<Dependency> merged = new ArrayList<>();
            Set<GroupArtifact> uniqueDependencies = new HashSet<>();
            for (Dependency dependency : dependencies) {
                merged.add(dependency);
                uniqueDependencies.add(new GroupArtifact(dependency.getGroupId(), dependency.getArtifactId()));
            }
            for (Dependency dependency : incomingDependencies) {
                if (!uniqueDependencies.contains(new GroupArtifact(dependency.getGroupId(), dependency.getArtifactId()))) {
                    merged.add(dependency);
                }
            }

            return merged;
        }

        private @Nullable JsonNode mergePluginConfigurations(@Nullable JsonNode configuration, @Nullable JsonNode incomingConfiguration) {
            if (!(incomingConfiguration instanceof ObjectNode)) {
                return configuration;
            }
            if (!(configuration instanceof ObjectNode)) {
                return incomingConfiguration;
            }

            ObjectNode ret = incomingConfiguration.deepCopy();
            Iterator<Map.Entry<String, JsonNode>> fields = configuration.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> conf = fields.next();
                JsonNode parentConf = ret.get(conf.getKey());
                JsonNode parentCombine = parentConf != null ? parentConf.get("combine.children") : null;
                if (parentCombine != null && "append".equals(parentCombine.asText())) {
                    JsonNode selfCombine = conf.getValue().get("combine.self");
                    if (selfCombine != null && "override".equals(selfCombine.asText())) {
                        ret.set(conf.getKey(), conf.getValue());
                    } else {
                        ret.set(conf.getKey(), combineLists(conf.getValue(), parentConf));
                    }
                } else {
                    ret.set(conf.getKey(), conf.getValue());
                }
            }
            return ret;
        }

        private JsonNode combineLists(JsonNode list, JsonNode incomingList) {
            ObjectNode ret = incomingList.deepCopy();
            ArrayList<String> keys = new ArrayList<>();
            ret.fieldNames().forEachRemaining(keys::add);
            keys.remove("combine.children");
            // If no keys remaining, it's an empty list, we return the other one
            if (keys.isEmpty()) {
                return list.deepCopy();
            }
            // We can only have one key remaining in a list
            String arrayElemField = keys.get(0);

            // Copy elements of the list
            JsonNode retNode = ret.get(arrayElemField);
            JsonNode node = list.get(arrayElemField);
            if (!(retNode instanceof ArrayNode)) {
                ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
                arrayNode.add(retNode);
                ret.set(arrayElemField, arrayNode);
                retNode = arrayNode;
            }
            if (node instanceof ArrayNode) {
                ((ArrayNode) retNode).addAll((ArrayNode) node);
            } else if (node != null) {
                ((ArrayNode) retNode).add(node);
            }

            // Check if combine.children is overridden
            JsonNode listCombine = list.get("combine.children");
            if (listCombine != null) {
                ret.set("combine.children", listCombine);
            }

            return ret;
        }

        private List<Plugin.Execution> mergePluginExecutions(List<Plugin.Execution> executions, List<Plugin.Execution> incomingExecutions) {
            return executions.isEmpty() ? incomingExecutions : executions; // TODO
        }

        private Plugin mergePlugins(Plugin plugin, Plugin incoming) {
            return new Plugin(
                    plugin.getGroupId(),
                    plugin.getArtifactId(),
                    Optional.ofNullable(plugin.getVersion()).orElse(incoming.getVersion()),
                    Optional.ofNullable(plugin.getExtensions()).orElse(incoming.getExtensions()),
                    Optional.ofNullable(plugin.getInherited()).orElse(incoming.getInherited()),
                    mergePluginConfigurations(plugin.getConfiguration(), incoming.getConfiguration()),
                    mergePluginDependencies(plugin.getDependencies(), incoming.getDependencies()),
                    mergePluginExecutions(plugin.getExecutions(), incoming.getExecutions())
            );
        }

        private void mergePlugins(List<Plugin> plugins, List<Plugin> incomingPlugins) {
            Map<PluginKey, Plugin> pluginMap = new HashMap<>();
            plugins.forEach(p -> pluginMap.put(getPluginKey(p), p));

            for (Plugin incomingPlugin : incomingPlugins) {
                if ("false".equals(incomingPlugin.getInherited())) {
                    continue;
                }
                Plugin plugin = pluginMap.get(getPluginKey(incomingPlugin));
                if (plugin != null) {
                    plugins.remove(plugin);
                    plugins.add(mergePlugins(plugin, incomingPlugin));
                } else {
                    plugins.add(incomingPlugin);
                }
            }
        }

        private void mergePlugins(List<Plugin> incomingPlugins) {
            if (!incomingPlugins.isEmpty()) {
                if (plugins == null || plugins.isEmpty()) {
                    //It is possible for the plugins to be an empty, immutable list.
                    //If it's empty, we ensure to create a mutable list.
                    plugins = new ArrayList<>();
                }
                mergePlugins(plugins, incomingPlugins);
            }
        }

        private void mergePluginManagement(List<Plugin> incomingPlugins) {
            if (!incomingPlugins.isEmpty()) {
                if (pluginManagement == null || pluginManagement.isEmpty()) {
                    //It is possible for the plugins to be an empty, immutable list.
                    //If it's empty, we ensure to create a mutable list.
                    pluginManagement = new ArrayList<>();
                }
                mergePlugins(pluginManagement, incomingPlugins);
            }
        }

        private void mergeRepositories(List<MavenRepository> incomingRepositories) {
            if (!incomingRepositories.isEmpty()) {
                if (repositories == null || repositories.isEmpty()) {
                    //It is possible for the repositories to be an empty, immutable list.
                    //If it's empty, we ensure to create a mutable list.
                    repositories = new ArrayList<>(incomingRepositories.size());
                }

                nextRepository:
                for (MavenRepository incomingRepository : incomingRepositories) {
                    @SuppressWarnings("ConstantConditions")
                    MavenRepository incoming = new MavenRepository(
                            getValue(incomingRepository.getId()),
                            getValue(incomingRepository.getUri()),
                            incomingRepository.getReleases(),
                            incomingRepository.getSnapshots(),
                            incomingRepository.isKnownToExist(),
                            incomingRepository.getUsername(),
                            incomingRepository.getPassword(),
                            incomingRepository.getDeriveMetadataIfMissing()
                    );

                    if (incoming.getId() != null) {
                        for (MavenRepository repository : repositories) {
                            if (incoming.getId().equals(repository.getId())) {
                                continue nextRepository;
                            }
                        }
                    }
                    repositories.add(incoming);
                }
            }
        }

        private void mergeProperties(Map<String, String> incomingProperties, Pom pom) {
            if (!incomingProperties.isEmpty()) {
                if (properties == null || properties.isEmpty()) {
                    //It is possible for the properties to be an empty, immutable map.
                    //If it's empty, we ensure to create a mutable map.
                    properties = new HashMap<>(incomingProperties.size());
                }
                for (Map.Entry<String, String> property : incomingProperties.entrySet()) {
                    MavenExecutionContextView.view(ctx)
                            .getResolutionListener()
                            .property(property.getKey(), property.getValue(), pom);
                    if (!properties.containsKey(property.getKey())) {
                        properties.put(property.getKey(), property.getValue());
                    }
                }
            }
        }

        private void mergeDependencyManagement(List<ManagedDependency> incomingDependencyManagement, Pom pom) throws MavenDownloadingException {
            if (!incomingDependencyManagement.isEmpty()) {
                if (dependencyManagement == null || dependencyManagement.isEmpty()) {
                    dependencyManagement = new ArrayList<>();
                }
                for (ManagedDependency d : incomingDependencyManagement) {
                    if (d instanceof Imported) {
                        ResolvedPom bom = downloader.download(getValues(((Imported) d).getGav()), null, ResolvedPom.this, repositories)
                                .resolve(activeProfiles, downloader, initialRepositories, ctx);
                        MavenExecutionContextView.view(ctx)
                                .getResolutionListener()
                                .bomImport(bom.getGav(), pom);
                        dependencyManagement.addAll(ListUtils.map(bom.getDependencyManagement(), dm -> dm
                                .withRequestedBom(d)
                                .withBomGav(bom.getGav())));
                    } else if (d instanceof Defined) {
                        Defined defined = (Defined) d;
                        MavenExecutionContextView.view(ctx)
                                .getResolutionListener()
                                .dependencyManagement(defined.withGav(getValues(defined.getGav())), pom);
                        dependencyManagement.add(new ResolvedManagedDependency(
                                getValues(defined.getGav()),
                                defined.getScope() == null ? null : Scope.fromName(getValue(defined.getScope())),
                                getValue(defined.getType()),
                                getValue(defined.getClassifier()),
                                ListUtils.map(defined.getExclusions(), (UnaryOperator<GroupArtifact>) ResolvedPom.this::getValues),
                                defined,
                                null,
                                null
                        ));
                    }
                }
            }
        }
    }

    public List<ResolvedDependency> resolveDependencies(Scope scope, MavenPomDownloader downloader, ExecutionContext ctx) throws MavenDownloadingExceptions {
        return resolveDependencies(scope, new HashMap<>(), downloader, ctx);
    }

    public List<ResolvedDependency> resolveDependencies(Scope scope, Map<GroupArtifact, VersionRequirement> requirements,
                                                        MavenPomDownloader downloader, ExecutionContext ctx) throws MavenDownloadingExceptions {
        List<ResolvedDependency> dependencies = new ArrayList<>();

        List<DependencyAndDependent> dependenciesAtDepth = new ArrayList<>();
        for (Dependency requestedDependency : getRequestedDependencies()) {
            Dependency d = getValues(requestedDependency, 0);
            Scope dScope = Scope.fromName(d.getScope());
            if (dScope == scope || dScope.isInClasspathOf(scope)) {
                dependenciesAtDepth.add(new DependencyAndDependent(requestedDependency, Scope.Compile, null, requestedDependency, this));
            }
        }

        MavenDownloadingExceptions exceptions = null;
        int depth = 0;
        while (!dependenciesAtDepth.isEmpty()) {
            List<DependencyAndDependent> dependenciesAtNextDepth = new ArrayList<>();

            for (DependencyAndDependent dd : dependenciesAtDepth) {
                //First get the dependency (relative to the pom it was defined in)
                Dependency d = dd.getDefinedIn().getValues(dd.getDependency(), depth);
                //The dependency may be modified by the current pom's managed dependencies
                d = getValues(d, depth);
                try {
                    if (d.getVersion() == null) {
                        throw new MavenDownloadingException("No version provided", null, dd.getDependency().getGav());
                    }

                    if (d.getType() != null && (!"jar".equals(d.getType()) && !"pom".equals(d.getType()))) {
                        continue;
                    }

                    GroupArtifact ga = new GroupArtifact(d.getGroupId(), d.getArtifactId());
                    VersionRequirement existingRequirement = requirements.get(ga);
                    if (existingRequirement == null) {
                        VersionRequirement newRequirement = VersionRequirement.fromVersion(d.getVersion(), depth);
                        requirements.put(ga, newRequirement);
                        String newRequiredVersion = newRequirement.resolve(ga, downloader, getRepositories());
                        if (newRequiredVersion == null) {
                            throw new MavenParsingException("Could not resolve version for [" + ga + "] matching version requirements " + newRequirement);
                        }
                        d = d.withGav(d.getGav().withVersion(newRequiredVersion));
                    } else {
                        VersionRequirement newRequirement = existingRequirement.addRequirement(d.getVersion());
                        requirements.put(ga, newRequirement);

                        String existingRequiredVersion = existingRequirement.resolve(ga, downloader, getRepositories());
                        String newRequiredVersion = newRequirement.resolve(ga, downloader, getRepositories());
                        if (newRequiredVersion == null) {
                            throw new MavenParsingException("Could not resolve version for [" + ga + "] matching version requirements " + newRequirement);
                        }
                        d = d.withGav(d.getGav().withVersion(newRequiredVersion));

                        if (!Objects.equals(existingRequiredVersion, newRequiredVersion)) {
                            // start over from the top with the knowledge of this new requirement and throwing
                            // away any in progress resolution because this requirement could cause a change
                            // to just about anything we've seen to this point
                            MavenExecutionContextView.view(ctx)
                                    .getResolutionListener()
                                    .clear();
                            return resolveDependencies(scope, requirements, downloader, ctx);
                        } else if (contains(dependencies, ga, d.getClassifier())) {
                            // we've already resolved this previously and the requirement didn't change,
                            // so just skip and continue on
                            continue;
                        }
                    }

                    if ((d.getGav().getGroupId() != null && d.getGav().getGroupId().startsWith("${") && d.getGav().getGroupId().endsWith("}")) ||
                        (d.getGav().getArtifactId().startsWith("${") && d.getGav().getArtifactId().endsWith("}")) ||
                        (d.getGav().getVersion() != null && d.getGav().getVersion().startsWith("${") && d.getGav().getVersion().endsWith("}"))) {
                        throw new MavenDownloadingException("Could not resolve property", null, d.getGav());
                    }

                    Pom dPom = downloader.download(d.getGav(), null, dd.definedIn, getRepositories());

                    MavenPomCache cache = MavenExecutionContextView.view(ctx).getPomCache();
                    ResolvedPom resolvedPom = cache.getResolvedDependencyPom(dPom.getGav());
                    if (resolvedPom == null) {
                        resolvedPom = new ResolvedPom(dPom, getActiveProfiles(), emptyMap(),
                                emptyList(), initialRepositories, emptyList(), emptyList(), emptyList(), emptyList());
                        resolvedPom.resolver(ctx, downloader).resolveParentsRecursively(dPom);
                        cache.putResolvedDependencyPom(dPom.getGav(), resolvedPom);
                    }

                    ResolvedDependency resolved = new ResolvedDependency(dPom.getRepository(),
                            resolvedPom.getGav(), dd.getDependency(), emptyList(),
                            resolvedPom.getRequested().getLicenses(),
                            resolvedPom.getValue(dd.getDependency().getType()),
                            resolvedPom.getValue(dd.getDependency().getClassifier()),
                            Boolean.valueOf(resolvedPom.getValue(dd.getDependency().getOptional())),
                            depth,
                            emptyList());

                    MavenExecutionContextView.view(ctx)
                            .getResolutionListener()
                            .dependency(scope, resolved, dd.getDefinedIn());

                    // build link between the including dependency and this one
                    ResolvedDependency includedBy = dd.getDependent();
                    if (includedBy != null) {
                        if (includedBy.getDependencies().isEmpty()) {
                            includedBy.unsafeSetDependencies(new ArrayList<>());
                        }
                        includedBy.getDependencies().add(resolved);
                    }

                    if (dd.getScope().transitiveOf(scope) == scope) {
                        dependencies.add(resolved);
                    } else {
                        continue;
                    }

                    nextDependency:
                    for (Dependency d2 : resolvedPom.getRequestedDependencies()) {
                        if (d2.getGroupId() == null) {
                            d2 = d2.withGav(d2.getGav().withGroupId(resolvedPom.getGroupId()));
                        }
                        String optional = resolvedPom.getValue(d2.getOptional());
                        if (optional != null && Boolean.parseBoolean(optional.trim())) {
                            continue;
                        }
                        if (d.getExclusions() != null) {
                            for (GroupArtifact exclusion : d.getExclusions()) {
                                if (matchesGlob(getValue(d2.getGroupId()), getValue(exclusion.getGroupId())) &&
                                    matchesGlob(getValue(d2.getArtifactId()), getValue(exclusion.getArtifactId()))) {
                                    if (resolved.getEffectiveExclusions().isEmpty()) {
                                        resolved.unsafeSetEffectiveExclusions(new ArrayList<>());
                                    }
                                    resolved.getEffectiveExclusions().add(exclusion);
                                    continue nextDependency;
                                }
                            }
                        }

                        Scope d2Scope = getDependencyScope(d2, resolvedPom);
                        if (d2Scope.isInClasspathOf(dd.getScope())) {
                            dependenciesAtNextDepth.add(new DependencyAndDependent(d2, d2Scope, resolved, dd.getRootDependent(), resolvedPom));
                        }
                    }
                } catch (MavenDownloadingException e) {
                    exceptions = MavenDownloadingExceptions.append(exceptions, e.setRoot(dd.getRootDependent().getGav()));
                }
            }

            dependenciesAtDepth = dependenciesAtNextDepth;
            depth++;
        }

        if (exceptions != null) {
            throw exceptions;
        }

        return dependencies;
    }

    private boolean contains(List<ResolvedDependency> dependencies, GroupArtifact ga, @Nullable String classifier) {
        for (ResolvedDependency it : dependencies) {
            if (it.getGroupId().equals(ga.getGroupId()) && it.getArtifactId().equals(ga.getArtifactId()) &&
                (Objects.equals(classifier, it.getClassifier()))) {
                return true;
            }
        }
        return false;
    }

    private Scope getDependencyScope(Dependency d2, ResolvedPom containingPom) {
        Scope scopeInContainingPom;
        if (d2.getScope() != null) {
            scopeInContainingPom = Scope.fromName(getValue(d2.getScope()));
        } else {
            //noinspection ConstantConditions
            scopeInContainingPom = containingPom.getManagedScope(getValue(d2.getGroupId()), getValue(d2.getArtifactId()), getValue(d2.getType()),
                    getValue(d2.getClassifier()));
            if (scopeInContainingPom == null) {
                scopeInContainingPom = Scope.Compile;
            }
        }
        //noinspection ConstantConditions
        Scope scopeInThisProject = getManagedScope(getValue(d2.getGroupId()), getValue(d2.getArtifactId()), getValue(d2.getType()),
                getValue(d2.getClassifier()));
        // project POM's dependency management overrules the containingPom's dependencyManagement
        // IFF the dependency is in the runtime classpath of the containingPom;
        // if the dependency was not already in the classpath of the containingPom, then project POM cannot override scope / "promote" it into the classpath
        return scopeInThisProject == null ? scopeInContainingPom : (scopeInContainingPom.isInClasspathOf(scopeInThisProject) ? scopeInThisProject : scopeInContainingPom);
    }

    private Dependency getValues(Dependency dep, int depth) {
        Dependency d = dep.withGav(getValues(dep.getGav()))
                .withScope(getValue(dep.getScope()));

        if (d.getGroupId() == null) {
            return d;
        }

        String scope;
        if (d.getScope() == null) {
            Scope parsedScope = getManagedScope(d.getGroupId(), d.getArtifactId(), d.getType(), d.getClassifier());
            scope = parsedScope == null ? null : parsedScope.toString().toLowerCase();
        } else {
            scope = getValue(d.getScope());
        }

        List<GroupArtifact> managedExclusions = getManagedExclusions(d.getGroupId(), d.getArtifactId(), d.getType(), d.getClassifier());
        if (!managedExclusions.isEmpty()) {
            d = d.withExclusions(ListUtils.concatAll(d.getExclusions(), managedExclusions));
        }

        if (d.getClassifier() != null) {
            d = d.withClassifier(getValue(d.getClassifier()));
        }
        if (d.getType() != null) {
            d = d.withType(getValue(d.getType()));
        }
        String version = d.getVersion();
        if (d.getVersion() == null || depth > 0) {
            // dependency management overrides transitive dependency versions
            version = getManagedVersion(d.getGroupId(), d.getArtifactId(), d.getType(), d.getClassifier());
            if (version == null) {
                version = d.getVersion();
            }
        }

        return d
                .withGav(d.getGav().withVersion(version))
                .withScope(scope);
    }

    @Value
    private static class DependencyAndDependent {
        Dependency dependency;
        Scope scope;
        ResolvedDependency dependent;
        Dependency rootDependent;
        ResolvedPom definedIn;
    }
}
