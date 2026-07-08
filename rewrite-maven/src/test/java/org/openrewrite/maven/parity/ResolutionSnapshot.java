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
package org.openrewrite.maven.parity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;

import java.util.*;

/**
 * Canonical projection of a {@link MavenResolutionResult} (plus thrown errors and listener
 * events) into a deterministic, JSON-serializable form. Captures per-scope ordering, nested
 * graph shape via {@code children} indices, instance threading via {@code requestedRef}
 * declaration-index references, the event multiset, normalized errors, and identity probes.
 */
@Getter
public class ResolutionSnapshot {
    private static final JsonNodeFactory F = JsonNodeFactory.instance;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ObjectNode json;

    private ResolutionSnapshot(ObjectNode json) {
        this.json = json;
    }

    public static ResolutionSnapshot of(MavenResolutionResult mrr, List<Throwable> errors,
                                        List<RecordingResolutionListener.Event> events) {
        return of(mrr, errors, events, new SnapshotNormalizer(), null);
    }

    /**
     * @param ctx when present, used to probe {@code ResolvedPom.resolve()} no-change instance
     *            identity against the same (hermetic) context that produced the resolution.
     */
    public static ResolutionSnapshot of(MavenResolutionResult mrr, List<Throwable> errors,
                                        List<RecordingResolutionListener.Event> events,
                                        SnapshotNormalizer normalizer, @Nullable ExecutionContext ctx) {
        // Aggregate events before the resolve() probe below appends new ones to a live listener
        SortedMap<String, Integer> eventMultiset = new TreeMap<>();
        for (RecordingResolutionListener.Event event : events) {
            eventMultiset.merge(requireNonNullElse(normalizer.normalize(event.toString())), 1, Integer::sum);
        }

        ObjectNode root = F.objectNode();
        root.set("pom", pomNode(mrr, normalizer));
        root.set("scopes", scopesNode(mrr, normalizer));
        root.set("errors", errorsNode(errors, normalizer));

        // Flattened GAVs only; serializing the live parent/module references recurses the whole reactor
        root.put("parentGav", mrr.getParent() == null ? null : gav(mrr.getParent().getPom().getGav()));
        ArrayNode moduleGavs = root.putArray("moduleGavs");
        for (MavenResolutionResult module : mrr.getModules()) {
            moduleGavs.add(gav(module.getPom().getGav()));
        }

        ObjectNode eventsNode = root.putObject("events");
        eventMultiset.forEach(eventsNode::put);

        ObjectNode identity = root.putObject("identity");
        identity.put("requestedThreading", requestedThreadingProbe(mrr));
        if (ctx == null) {
            identity.putNull("resolveNoChange");
        } else {
            identity.put("resolveNoChange", resolveNoChangeProbe(mrr, ctx));
        }
        identity.put("dmThreading", dmThreadingProbe(mrr));

        return new ResolutionSnapshot(root);
    }

    private static ObjectNode pomNode(MavenResolutionResult mrr, SnapshotNormalizer normalizer) {
        ResolvedPom pom = mrr.getPom();
        ObjectNode node = F.objectNode();
        node.put("gav", gav(pom.getGav()));
        node.put("packaging", pom.getPackaging());

        ObjectNode properties = node.putObject("properties");
        new TreeMap<>(pom.getProperties()).forEach((k, v) -> properties.put(k, normalizer.normalize(v)));

        Map<ManagedDependency, Integer> dmDeclarationIndex = identityIndex(pom.getRequested().getDependencyManagement());
        ArrayNode dm = node.putArray("dependencyManagement");
        for (ResolvedManagedDependency d : pom.getDependencyManagement()) {
            ObjectNode e = dm.addObject();
            e.put("gact", d.getGroupId() + ":" + d.getArtifactId() + ":" +
                    (d.getClassifier() == null ? "" : d.getClassifier()) + ":" + d.getType());
            e.put("version", normalizer.normalize(d.getVersion()));
            e.put("scope", d.getScope() == null ? null : d.getScope().name());
            ArrayNode exclusions = e.putArray("exclusions");
            if (d.getExclusions() != null) {
                for (GroupArtifact ga : d.getExclusions()) {
                    exclusions.add(ga.getGroupId() + ":" + ga.getArtifactId());
                }
            }
            e.put("bomGav", d.getBomGav() == null ? null : gav(d.getBomGav()));
            e.put("requestedRef", dmRequestedRef(d, dmDeclarationIndex));
        }

        ArrayNode repositories = node.putArray("repositories");
        for (MavenRepository repo : pom.getRepositories()) {
            ObjectNode e = repositories.addObject();
            e.put("id", repo.getId());
            e.put("uri", normalizer.normalize(repo.getUri()));
        }

        ArrayNode pluginRepositories = node.putArray("pluginRepositories");
        //noinspection ConstantValue -- null on LSTs serialized before the field existed
        if (pom.getPluginRepositories() != null) {
            for (MavenRepository repo : pom.getPluginRepositories()) {
                ObjectNode e = pluginRepositories.addObject();
                e.put("id", repo.getId());
                e.put("uri", normalizer.normalize(repo.getUri()));
            }
        }

        ArrayNode subprojects = node.putArray("subprojects");
        //noinspection ConstantValue
        if (pom.getSubprojects() != null) {
            pom.getSubprojects().forEach(subprojects::add);
        }

        node.set("plugins", pluginsNode(pom.getPlugins(), normalizer));
        node.set("pluginManagement", pluginsNode(pom.getPluginManagement(), normalizer));
        return node;
    }

    private static String dmRequestedRef(ResolvedManagedDependency d, Map<ManagedDependency, Integer> declarationIndex) {
        Integer i = declarationIndex.get(d.getRequested());
        if (i != null) {
            return "dm[" + i + "]";
        }
        if (d.getRequestedBom() != null) {
            Integer bom = declarationIndex.get(d.getRequestedBom());
            if (bom != null) {
                return "bom[" + bom + "]";
            }
        }
        // Declared in a parent or profile; the instance is not a root pom declaration
        return "inherited:" + d.getGroupId() + ":" + d.getArtifactId();
    }

    private static ArrayNode pluginsNode(List<Plugin> plugins, SnapshotNormalizer normalizer) {
        ArrayNode node = F.arrayNode();
        for (Plugin plugin : plugins) {
            ObjectNode e = node.addObject();
            e.put("ga", plugin.getGroupId() + ":" + plugin.getArtifactId());
            e.put("version", normalizer.normalize(plugin.getVersion()));
            ArrayNode executions = e.putArray("executions");
            for (Plugin.Execution execution : plugin.getExecutions()) {
                executions.add(execution.getId() + ":" + execution.getPhase() + ":[" +
                        (execution.getGoals() == null ? "" : String.join(",", execution.getGoals())) + "]");
            }
        }
        return node;
    }

    private static ObjectNode scopesNode(MavenResolutionResult mrr, SnapshotNormalizer normalizer) {
        Map<Dependency, Integer> requestedIndex = identityIndex(mrr.getPom().getRequestedDependencies());
        ObjectNode scopes = F.objectNode();
        for (Map.Entry<Scope, List<ResolvedDependency>> scope : mrr.getDependencies().entrySet()) {
            List<ResolvedDependency> flat = scope.getValue();
            Map<ResolvedDependency, Integer> nodeIndex = identityIndex(flat);
            ArrayNode list = scopes.putArray(scope.getKey().name());
            for (int i = 0; i < flat.size(); i++) {
                ResolvedDependency d = flat.get(i);
                ObjectNode e = list.addObject();
                e.put("i", i);
                e.put("gav", gav(d.getGav()));
                e.put("dated", normalizer.normalize(d.getGav().getDatedSnapshotVersion()));
                e.put("depth", d.getDepth());
                e.put("repo", d.getRepository() == null ? null : normalizer.normalize(d.getRepository().getUri()));
                e.put("requestedRef", nodeRequestedRef(d, i, flat, requestedIndex));
                ArrayNode children = e.putArray("children");
                for (ResolvedDependency child : d.getDependencies()) {
                    // -1 makes a flat-list/nested-graph identity break visible in the diff
                    children.add(nodeIndex.getOrDefault(child, -1));
                }
                e.put("type", d.getType());
                if (d.getClassifier() != null) {
                    e.put("classifier", d.getClassifier());
                }
                if (d.getOptional() != null) {
                    e.put("optional", d.getOptional());
                }
                if (!d.getEffectiveExclusions().isEmpty()) {
                    ArrayNode effectiveExclusions = e.putArray("effectiveExclusions");
                    for (GroupArtifact ga : d.getEffectiveExclusions()) {
                        effectiveExclusions.add(ga.getGroupId() + ":" + ga.getArtifactId());
                    }
                }
                if (d.getLicenses() != null && !d.getLicenses().isEmpty()) {
                    ArrayNode licenses = e.putArray("licenses");
                    for (License license : d.getLicenses()) {
                        licenses.add(license.getName() + "|" + license.getType());
                    }
                }
            }
        }
        return scopes;
    }

    private static String nodeRequestedRef(ResolvedDependency d, int i, List<ResolvedDependency> flat,
                                           Map<Dependency, Integer> requestedIndex) {
        Integer rootIdx = requestedIndex.get(d.getRequested());
        if (rootIdx != null) {
            return "root[" + rootIdx + "]";
        }
        for (int j = 0; j < i; j++) {
            if (flat.get(j).getRequested() == d.getRequested()) {
                return "node[" + j + "]";
            }
        }
        Dependency requested = d.getRequested();
        return "val:" + requested.getGroupId() + ":" + requested.getArtifactId() + ":" + requested.getVersion() +
                (requested.getClassifier() == null ? "" : ":" + requested.getClassifier());
    }

    private static ArrayNode errorsNode(List<Throwable> errors, SnapshotNormalizer normalizer) {
        ArrayNode node = F.arrayNode();
        for (Throwable error : errors) {
            ObjectNode e = node.addObject();
            e.put("type", error.getClass().getSimpleName());
            e.put("message", normalizer.normalizeMessage(error.getMessage()));
        }
        return node;
    }

    private static boolean requestedThreadingProbe(MavenResolutionResult mrr) {
        Map<Dependency, Integer> requestedIndex = identityIndex(mrr.getPom().getRequestedDependencies());
        for (List<ResolvedDependency> scope : mrr.getDependencies().values()) {
            for (ResolvedDependency d : scope) {
                if (d.getDepth() == 0 &&
                        (!requestedIndex.containsKey(d.getRequested()) || mrr.getResolvedDependency(d.getRequested()) == null)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean dmThreadingProbe(MavenResolutionResult mrr) {
        for (ResolvedManagedDependency dm : mrr.getPom().getDependencyManagement()) {
            if (mrr.getResolvedManagedDependency(dm.getRequested()) == null) {
                return false;
            }
            if (dm.getRequestedBom() != null && mrr.getResolvedManagedDependency(dm.getRequestedBom()) == null) {
                return false;
            }
        }
        return true;
    }

    private static boolean resolveNoChangeProbe(MavenResolutionResult mrr, ExecutionContext ctx) {
        try {
            MavenPomDownloader downloader = new MavenPomDownloader(mrr.getProjectPoms(), ctx);
            return mrr.getPom().resolve(ctx, downloader) == mrr.getPom();
        } catch (MavenDownloadingException e) {
            return false;
        }
    }

    private static <T> Map<T, Integer> identityIndex(List<T> list) {
        Map<T, Integer> index = new IdentityHashMap<>();
        for (int i = 0; i < list.size(); i++) {
            index.putIfAbsent(list.get(i), i);
        }
        return index;
    }

    private static String gav(ResolvedGroupArtifactVersion gav) {
        return gav.getGroupId() + ":" + gav.getArtifactId() + ":" + gav.getVersion();
    }

    private static String requireNonNullElse(@Nullable String s) {
        return s == null ? "null" : s;
    }

    public String toJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return toJson();
    }
}
