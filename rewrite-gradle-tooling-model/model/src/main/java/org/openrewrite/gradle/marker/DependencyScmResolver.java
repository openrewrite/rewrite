/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.gradle.marker;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.query.ArtifactResolutionQuery;
import org.gradle.api.artifacts.result.ArtifactResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.invocation.Gradle;
import org.gradle.maven.MavenModule;
import org.gradle.maven.MavenPomArtifact;
import org.gradle.util.GradleVersion;
import org.jspecify.annotations.Nullable;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.maven.tree.Scm;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds the effective {@code <scm>} of the POMs of the dependencies Gradle resolved, as
 * {@link ResolvedPom#getScm()} defines it, so that a dependency resolved by Gradle says which source repository it
 * was built from just as one resolved by Maven does.
 * <p>
 * Gradle hands out each POM, and each of its parents, through an artifact resolution query, so they come from the
 * repositories, credentials, mirrors and caches of the build, and a build that is offline stays offline. One
 * instance serves one build: a POM Gradle provided is asked for and read once, however many configurations and
 * projects share it, and one it could not provide is asked for once for each project and each buildscript.
 * <p>
 * Nothing here may fail the model build. A component that has no POM, such as one from an Ivy or flat directory
 * repository, or a POM that cannot be read, has no {@code <scm>}.
 */
final class DependencyScmResolver {
    private static final Map<Gradle, DependencyScmResolver> BY_BUILD = Collections.synchronizedMap(new WeakHashMap<>());

    // ArtifactResolutionQuery.forModule(String, String, String)
    private static final boolean QUERY_FOR_MODULE = GradleVersion.current().compareTo(GradleVersion.version("4.5")) >= 0;

    private static final int MAX_PARENTS = 32;

    /**
     * Each POM Gradle provided, by {@code group:artifact:version}.
     */
    private final Map<String, Pom> poms = new ConcurrentHashMap<>();

    /**
     * What Gradle could not provide, by {@link Scope}: it depends on the repositories that were asked, which differ
     * between projects and between a project and its buildscript.
     */
    private final Map<String, Set<String>> missing = new ConcurrentHashMap<>();

    static Scope forProject(Project project) {
        return forBuild(project).new Scope(project.getPath(), project.getDependencies(), project.getConfigurations());
    }

    static Scope forBuildscript(Project project) {
        return forBuild(project).new Scope(project.getPath() + "#buildscript", project.getBuildscript().getDependencies(),
                project.getBuildscript().getConfigurations());
    }

    private static DependencyScmResolver forBuild(Project project) {
        return BY_BUILD.computeIfAbsent(project.getGradle(), gradle -> new DependencyScmResolver());
    }

    static void clearCaches() {
        BY_BUILD.clear();
    }

    /**
     * The dependencies and repositories of a project, or of its buildscript. Not to be kept beyond the building of
     * that project's model, since it holds on to the project.
     */
    final class Scope {
        private final String name;
        private final DependencyHandler dependencies;
        private final ConfigurationContainer configurations;

        private Scope(String name, DependencyHandler dependencies, ConfigurationContainer configurations) {
            this.name = name;
            this.dependencies = dependencies;
            this.configurations = configurations;
        }

        /**
         * @param conf A configuration of this scope that has been resolved already.
         * @return The effective {@code <scm>} of each of the configuration's external module components that has
         * one, by {@link #gav(String, String, String)}.
         */
        Map<String, Scm> scmByGav(Configuration conf) {
            try {
                Map<String, ModuleComponentIdentifier> components = new LinkedHashMap<>();
                for (ResolvedComponentResult component : conf.getIncoming().getResolutionResult().getAllComponents()) {
                    ComponentIdentifier id = component.getId();
                    if (id instanceof ModuleComponentIdentifier) {
                        ModuleComponentIdentifier module = (ModuleComponentIdentifier) id;
                        components.put(gav(module.getGroup(), module.getModule(), module.getVersion()), module);
                    }
                }
                if (components.isEmpty()) {
                    return Collections.emptyMap();
                }
                // No lock is held while Gradle resolves: at worst two projects configured at once ask for the same POM
                fetch(dependencies, configurations, components,
                        missing.computeIfAbsent(name, it -> Collections.newSetFromMap(new ConcurrentHashMap<>())));
                Map<String, Scm> scmByGav = new HashMap<>();
                for (String gav : components.keySet()) {
                    Scm scm = effectiveScm(gav);
                    if (scm != null) {
                        scmByGav.put(gav, scm);
                    }
                }
                return scmByGav;
            } catch (Exception | LinkageError e) {
                // The model is complete without this, and is not to fail for the lack of it
                return Collections.emptyMap();
            }
        }
    }

    static String gav(@Nullable String group, String artifact, @Nullable String version) {
        return group + ":" + artifact + ":" + version;
    }

    private void fetch(DependencyHandler dependencies, ConfigurationContainer configurations,
                       Map<String, ModuleComponentIdentifier> components, Set<String> missingHere) {
        List<ModuleComponentIdentifier> unknown = new ArrayList<>();
        for (Map.Entry<String, ModuleComponentIdentifier> component : components.entrySet()) {
            if (!poms.containsKey(component.getKey()) && !missingHere.contains(component.getKey())) {
                unknown.add(component.getValue());
            }
        }
        if (!unknown.isEmpty()) {
            for (ModuleComponentIdentifier id : unknown) {
                // until Gradle says otherwise
                missingHere.add(gav(id.getGroup(), id.getModule(), id.getVersion()));
            }
            read(dependencies.createArtifactResolutionQuery().forComponents(unknown), missingHere);
        }

        // Parents are no components of the configuration, so they are asked for by their coordinates, level by level.
        // A POM read for an earlier configuration may still lack a parent that these repositories can provide.
        Set<String> children = new LinkedHashSet<>(components.keySet());
        for (int depth = 0; depth < MAX_PARENTS && !children.isEmpty(); depth++) {
            Map<String, GroupArtifactVersion> parents = new LinkedHashMap<>();
            for (String child : children) {
                GroupArtifactVersion parent = parentOf(poms.get(child));
                if (parent != null) {
                    parents.put(gav(parent.getGroupId(), parent.getArtifactId(), parent.getVersion()), parent);
                }
            }
            children = new LinkedHashSet<>(parents.keySet());
            parents.keySet().removeIf(parent -> poms.containsKey(parent) || missingHere.contains(parent));
            if (parents.isEmpty()) {
                continue;
            }
            missingHere.addAll(parents.keySet());
            if (QUERY_FOR_MODULE) {
                ArtifactResolutionQuery query = dependencies.createArtifactResolutionQuery();
                for (GroupArtifactVersion parent : parents.values()) {
                    //noinspection DataFlowIssue
                    query = query.forModule(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
                }
                read(query, missingHere);
            } else {
                readFromDetachedConfiguration(dependencies, configurations, parents.values(), missingHere);
            }
        }
    }

    private void read(ArtifactResolutionQuery query, Set<String> missingHere) {
        //noinspection unchecked
        for (ComponentArtifactsResult component : query
                .withArtifacts(MavenModule.class, MavenPomArtifact.class)
                .execute()
                .getResolvedComponents()) {
            if (!(component.getId() instanceof ModuleComponentIdentifier)) {
                continue;
            }
            ModuleComponentIdentifier id = (ModuleComponentIdentifier) component.getId();
            String gav = gav(id.getGroup(), id.getModule(), id.getVersion());
            for (ArtifactResult artifact : component.getArtifacts(MavenPomArtifact.class)) {
                if (artifact instanceof ResolvedArtifactResult) {
                    Pom pom = parse(((ResolvedArtifactResult) artifact).getFile());
                    if (pom != null) {
                        poms.put(gav, pom);
                        missingHere.remove(gav);
                    }
                    break;
                }
            }
        }
    }

    /**
     * Before Gradle 4.5 a query takes only the components of a resolved configuration.
     */
    private void readFromDetachedConfiguration(DependencyHandler dependencies, ConfigurationContainer configurations,
                                               Collection<GroupArtifactVersion> gavs, Set<String> missingHere) {
        // A configuration resolves a module to one version, so two versions of a parent go in two configurations
        List<Map<String, Dependency>> batches = new ArrayList<>();
        for (GroupArtifactVersion gav : gavs) {
            String module = gav.getGroupId() + ":" + gav.getArtifactId();
            Map<String, Dependency> batch = null;
            for (Map<String, Dependency> candidate : batches) {
                if (!candidate.containsKey(module)) {
                    batch = candidate;
                    break;
                }
            }
            if (batch == null) {
                batch = new LinkedHashMap<>();
                batches.add(batch);
            }
            batch.put(module, dependencies.create(module + ":" + gav.getVersion() + "@pom"));
        }
        for (Map<String, Dependency> batch : batches) {
            Configuration detached = configurations.detachedConfiguration(batch.values().toArray(new Dependency[0]));
            detached.setTransitive(false);
            for (ResolvedArtifact artifact : detached.getResolvedConfiguration().getLenientConfiguration().getArtifacts()) {
                String gav = gav(artifact.getModuleVersion().getId().getGroup(), artifact.getModuleVersion().getId().getName(),
                        artifact.getModuleVersion().getId().getVersion());
                Pom pom = parse(artifact.getFile());
                if (pom != null) {
                    poms.put(gav, pom);
                    missingHere.remove(gav);
                }
            }
        }
    }

    private static @Nullable Pom parse(File pomFile) {
        try (InputStream in = Files.newInputStream(pomFile.toPath())) {
            return RawPom.parse(in, null).toPom(null, null);
        } catch (Exception | LinkageError e) {
            return null;
        }
    }

    /**
     * @return The coordinates of the POM's parent, or {@code null} when it has none or they cannot be told.
     */
    private static @Nullable GroupArtifactVersion parentOf(@Nullable Pom pom) {
        if (pom == null) {
            return null;
        }
        Parent parent = pom.getParent();
        if (parent == null) {
            return null;
        }
        // A published POM names its parent literally, except that a property of its own may hold the version
        String groupId = pom.getValue(parent.getGroupId());
        String artifactId = pom.getValue(parent.getArtifactId());
        String version = pom.getValue(parent.getVersion());
        if (groupId == null || artifactId == null || version == null ||
                groupId.contains("${") || artifactId.contains("${") || version.contains("${")) {
            return null;
        }
        return new GroupArtifactVersion(groupId, artifactId, version);
    }

    /**
     * As {@link ResolvedPom#getScm()}: each value from the nearest POM that declares it, placeholders resolved
     * against the child, and the child's artifactId not appended to what is inherited.
     */
    private @Nullable Scm effectiveScm(String gav) {
        Pom requested = poms.get(gav);
        if (requested == null) {
            return null;
        }
        Scm scm = null;
        Map<String, String> properties = new HashMap<>();
        Set<String> seen = new HashSet<>();
        Pom pom = requested;
        String pomGav = gav;
        while (pom != null && seen.add(pomGav) && seen.size() <= MAX_PARENTS) {
            for (Map.Entry<String, String> property : pom.getProperties().entrySet()) {
                if (!properties.containsKey(property.getKey())) {
                    properties.put(property.getKey(), property.getValue());
                }
            }
            Scm declared = pom.getScm();
            if (declared != null) {
                scm = scm == null ? declared : new Scm(
                        scm.getConnection() == null ? declared.getConnection() : scm.getConnection(),
                        scm.getDeveloperConnection() == null ? declared.getDeveloperConnection() : scm.getDeveloperConnection(),
                        scm.getUrl() == null ? declared.getUrl() : scm.getUrl(),
                        scm.getTag() == null ? declared.getTag() : scm.getTag());
            }
            GroupArtifactVersion parent = parentOf(pom);
            if (parent == null) {
                break;
            }
            pomGav = gav(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
            pom = poms.get(pomGav);
        }
        if (scm == null) {
            return null;
        }
        ResolvedPom effective = ResolvedPom.builder().requested(requested).properties(properties).build();
        return new Scm(effective.getValue(scm.getConnection()), effective.getValue(scm.getDeveloperConnection()),
                effective.getValue(scm.getUrl()), effective.getValue(scm.getTag()));
    }
}
