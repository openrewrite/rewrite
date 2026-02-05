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
package org.openrewrite.maven;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public class UpdateMavenModel<P> extends MavenVisitor<P> {

    /**
     * Key for storing fresh Pom objects in the execution context.
     * When a project POM is updated, its fresh Pom is stored here
     * so that downstream POMs can resolve against up-to-date data.
     */
    private static final String FRESH_POMS_KEY = "org.openrewrite.maven.UpdateMavenModel.freshPoms";

    /**
     * Key for storing fresh MavenResolutionResult objects in the execution context.
     * When a project POM is updated, fresh resolution results for the entire module
     * hierarchy are stored here so child POMs can use up-to-date dependency management.
     */
    private static final String FRESH_MARKERS_KEY = "org.openrewrite.maven.UpdateMavenModel.freshMarkers";

    /**
     * Get the map of fresh Pom objects from the execution context.
     * This allows re-resolution to see updated project POMs that were
     * modified earlier in the same recipe run.
     *
     * @param ctx The execution context
     * @return A map of source path to fresh Pom
     */
    @SuppressWarnings("unchecked")
    public static Map<Path, Pom> getFreshPoms(ExecutionContext ctx) {
        return ctx.computeMessageIfAbsent(FRESH_POMS_KEY, k -> new HashMap<>());
    }

    /**
     * Get the map of fresh MavenResolutionResult objects from the execution context.
     * When a parent POM is modified, the fresh resolution result (with updated dependency
     * management) is stored here so child POMs can use it.
     *
     * @param ctx The execution context
     * @return A map of source path to fresh MavenResolutionResult
     */
    @SuppressWarnings("unchecked")
    public static Map<Path, MavenResolutionResult> getFreshMarkers(ExecutionContext ctx) {
        return ctx.computeMessageIfAbsent(FRESH_MARKERS_KEY, k -> new HashMap<>());
    }

    /**
     * Get the fresh MavenResolutionResult for the given source path, if available.
     *
     * @param sourcePath The source path of the POM
     * @param ctx The execution context
     * @return The fresh MavenResolutionResult, or null if not available
     */
    public static @Nullable MavenResolutionResult getFreshMarker(Path sourcePath, ExecutionContext ctx) {
        return getFreshMarkers(ctx).get(sourcePath);
    }

    @Override
    public Xml visitDocument(Xml.Document document, P p) {
        if (!(p instanceof ExecutionContext)) {
            throw new IllegalArgumentException("UpdateMavenModel must be provided an ExecutionContext");
        }
        ExecutionContext ctx = (ExecutionContext) p;

        MavenResolutionResult resolutionResult = getResolutionResult();
        Pom requested = resolutionResult.getPom().getRequested();
        requested.getProperties().clear();

        Optional<Xml.Tag> properties = document.getRoot().getChild("properties");
        if (properties.isPresent()) {
            for (final Xml.Tag propertyTag : properties.get().getChildren()) {
                requested.getProperties().put(propertyTag.getName(),
                        propertyTag.getValue().orElse(""));
            }
        }
        // for backwards compatibility with ASTs that were serialized before userProperties was added
        //noinspection ConstantValue
        if (resolutionResult.getUserProperties() != null) {
            requested.getProperties().putAll(resolutionResult.getUserProperties());
        }

        Optional<Xml.Tag> parent = document.getRoot().getChild("parent");
        if (parent.isPresent()) {
            Parent updatedParent = new Parent(new GroupArtifactVersion(
                    parent.get().getChildValue("groupId").orElse(null),
                    parent.get().getChildValue("artifactId").orElseThrow(() -> new IllegalStateException("GAV must have artifactId")),
                    parent.get().getChildValue("version").orElse(null)
            ), parent.get().getChildValue("relativePath").orElse(null));
            requested = requested.withParent(updatedParent);
        } else if (requested.getParent() != null) {
            requested = requested.withParent(null);
        }

        Optional<Xml.Tag> dependencies = document.getRoot().getChild("dependencies");
        if (dependencies.isPresent()) {
            List<Xml.Tag> eachDependency = dependencies.get().getChildren("dependency");
            List<Dependency> requestedDependencies = new ArrayList<>(eachDependency.size());
            for (Xml.Tag dependency : eachDependency) {
                requestedDependencies.add(Dependency.builder()
                        .gav(new GroupArtifactVersion(
                                dependency.getChildValue("groupId").orElse(null),
                                dependency.getChildValue("artifactId").orElseThrow(() -> new IllegalStateException("Dependency must have artifactId")),
                                dependency.getChildValue("version").orElse(null)
                        ))
                        .classifier(dependency.getChildValue("classifier").orElse(null))
                        .type(dependency.getChildValue("type").orElse(null))
                        .scope(dependency.getChildValue("scope").orElse("compile"))
                        .exclusions(mapExclusions(dependency))
                        .optional(dependency.getChildValue("optional").orElse(null))
                        .build()
                );
            }
            requested = requested.withDependencies(requestedDependencies);
        } else if (!requested.getDependencies().isEmpty()) {
            requested = requested.withDependencies(emptyList());
        }

        Optional<Xml.Tag> dependencyManagement = document.getRoot().getChild("dependencyManagement");
        if (dependencyManagement.isPresent()) {
            dependencies = dependencyManagement.get().getChild("dependencies");
            if (dependencies.isPresent()) {
                List<Xml.Tag> eachDependency = dependencies.get().getChildren("dependency");
                List<ManagedDependency> requestedManagedDependencies = new ArrayList<>(eachDependency.size());
                for (Xml.Tag dependency : eachDependency) {
                    String scope = dependency.getChildValue("scope").orElse(null);
                    GroupArtifactVersion gav = new GroupArtifactVersion(
                            dependency.getChildValue("groupId").orElse(null),
                            dependency.getChildValue("artifactId").orElseThrow(() -> new IllegalStateException("Dependency must have artifactId")),
                            dependency.getChildValue("version").orElse(null)
                    );

                    if ("import".equals(scope)) {
                        requestedManagedDependencies.add(new ManagedDependency.Imported(gav));
                    } else {
                        requestedManagedDependencies.add(new ManagedDependency.Defined(gav, scope,
                                dependency.getChildValue("type").orElse(null),
                                dependency.getChildValue("classifier").orElse(null),
                                mapExclusions(dependency)));
                    }
                }
                requested = requested.withDependencyManagement(requestedManagedDependencies);
            }
        } else if (!requested.getDependencyManagement().isEmpty()) {
            requested = requested.withDependencyManagement(emptyList());
        }

        Optional<Xml.Tag> repos = document.getRoot().getChild("repositories");
        if (repos.isPresent()) {
            requested = requested.withRepositories(repos.get().getChildren("repository").stream().map(t -> new MavenRepository(
                    t.getChildValue("id").orElse(null),
                    t.getChildValue("url").get(),
                    t.getChild("releases").flatMap(s -> s.getChildValue("enabled")).orElse(null),
                    t.getChild("snapshots").flatMap(s -> s.getChildValue("enabled")).orElse(null),
                    null,
                    null,
                    null
            )).collect(toList()));
        } else {
            requested = requested.withRepositories(emptyList());
        }

        try {
            MavenResolutionResult updated = updateResult(ctx, resolutionResult.withPom(resolutionResult.getPom().withRequested(requested)),
                    resolutionResult.getProjectPoms());

            // Store fresh data in execution context so downstream POMs see updated data
            storeFreshData(updated, ctx);

            return document.withMarkers(document.getMarkers().computeByType(getResolutionResult(),
                    (original, ignored) -> updated));
        } catch (MavenDownloadingExceptions e) {
            return e.warn(document);
        }
    }

    private @Nullable List<GroupArtifact> mapExclusions(Xml.Tag tag) {
        return tag.getChild("exclusions")
                .map(exclusions -> {
                    List<Xml.Tag> eachExclusion = exclusions.getChildren("exclusion");
                    List<GroupArtifact> requestedExclusions = new ArrayList<>(eachExclusion.size());
                    for (Xml.Tag exclusion : eachExclusion) {
                        requestedExclusions.add(new GroupArtifact(
                                exclusion.getChildValue("groupId").orElse(null),
                                exclusion.getChildValue("artifactId").orElse(null)
                        ));
                    }
                    return requestedExclusions;
                })
                .orElse(null);
    }

    /**
     * Store fresh Pom and MavenResolutionResult data for this POM and all its modules.
     * This enables downstream POMs to see the updated dependency management when they
     * are re-resolved.
     */
    private void storeFreshData(MavenResolutionResult mrr, ExecutionContext ctx) {
        Pom freshPom = mrr.getPom().getRequested();
        Path sourcePath = freshPom.getSourcePath();
        if (sourcePath != null) {
            getFreshPoms(ctx).put(sourcePath, freshPom);
            getFreshMarkers(ctx).put(sourcePath, mrr);
        }
        // Also store fresh data for all modules
        for (MavenResolutionResult module : mrr.getModules()) {
            storeFreshData(module, ctx);
        }
    }

    private MavenResolutionResult updateResult(ExecutionContext ctx, MavenResolutionResult resolutionResult, Map<Path, Pom> projectPoms) throws MavenDownloadingExceptions {
        // Overlay fresh Poms from ExecutionContext onto projectPoms
        // This ensures we see updated parent data from earlier recipe modifications
        Map<Path, Pom> effectiveProjectPoms = new HashMap<>(projectPoms);
        effectiveProjectPoms.putAll(getFreshPoms(ctx));

        MavenPomDownloader downloader = new MavenPomDownloader(effectiveProjectPoms, ctx, getResolutionResult().getMavenSettings(),
                getResolutionResult().getActiveProfiles());

        AtomicReference<MavenDownloadingExceptions> exceptions = new AtomicReference<>();
        try {
            ResolvedPom resolved = resolutionResult.getPom().resolve(ctx, downloader);
            MavenResolutionResult mrr = resolutionResult
                    .withPom(resolved)
                    .withModules(ListUtils.map(resolutionResult.getModules(), module -> {
                        try {
                            return updateResult(ctx, module, projectPoms);
                        } catch (MavenDownloadingExceptions e) {
                            exceptions.set(MavenDownloadingExceptions.append(exceptions.get(), e));
                            return module;
                        }
                    }))
                    .resolveDependencies(downloader, ctx);
            if (exceptions.get() != null) {
                throw exceptions.get();
            }
            return mrr;
        } catch (MavenDownloadingExceptions e) {
            throw MavenDownloadingExceptions.append(exceptions.get(), e);
        } catch (MavenDownloadingException e) {
            throw MavenDownloadingExceptions.append(exceptions.get(), e);
        }
    }
}
